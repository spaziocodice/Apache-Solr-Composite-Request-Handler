package io.sease.crh;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.solr.common.params.SolrParams.toSolrParams;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RTimerTree;
import org.eclipse.jetty.http.HttpParser.RequestHandler;

/**
 * A {@link SolrRequestHandler} which orchestrates two or more {@link SolrRequestHandler} instances.
 * Each handler needs to be defined, as usual, within the solrconfig.xml.
 * <br/> <br/>
 * Other than declaring the chain members (i.e. the request handlers that will be orchestrated), this component allows
 * you to define a "transition" rule, that is: a cardinality-based rule, which will be applied on the results of a
 * give search handler in order to indicate if a further processing is needed (i.e. if the control should be passed
 * to the next handler in the chain) or not (i.e. if the response of the current handler is what we are going to
 * return to the caller).
 * <br/> <br/>
 * Initially there was just one rule for controlling the flow between the configured handlers: if the response of the
 * nth handler was empty (i.e. zero results) then the control would flow through the next member.
 *
 * With the enhancement described above, it's possible to manage interesting scenarios, with chains like the following:
 *
 * <ul>
 *     <li>
 *         <b>eq1</b>: the query response must produce exactly one result; if the response contains 0 or more than 1
 *         result, then the flow must proceed ahead, to the next handler.
 *     </li>
 *     <li>
 *         <b>gt0</b>: the query response must produce at least 1 result. In case of empty response the system will
 *         invoke the next handler.
 *     </li>
 *     <li>
 *         <b>always</b>: "always" is a null-object rule, which actually doesn nothing. It must be the last rule in the
 *         chain, marking the corresponding handler as a last chance for getting a positive response.
 *     </li>
 * </ul>
 *
 * @author agazzarini
 * @since 1.0
 */
public class CompositeRequestHandler extends RequestHandlerBase {
	private final static DocList EMPTY_DOCLIST = new DocSlice(0, 0, new int[0], new float[0], 0, 0f);
	private final static String EMPTY_STRING = "";
	
	private final static String RESPONSE_KEY = "response"; // If only SolrQueryResponse.RESPONSE_KEY would be public ;)
	private final static String RESPONSE_HEADER_KEY = "responseHeader"; // If only SolrQueryResponse.RESPONSE_HEADER_KEY would be public ;)

	final static String CHAIN_KEY= "chain";
	final static String RULES_KEY= "rules";

	final static String LESSER_THAN_KEYWORD = "lt";
	final static String GREATER_THAN_KEYWORD = "gt";
	final static String EQUAL_KEYWORD = "eq";

	final static IntPredicate ALWAYS = x -> true;

	final static Function<String, IntPredicate> TO_RULE = expression -> {
		if (expression.startsWith(GREATER_THAN_KEYWORD)) {
			return x -> x > parseInt(expression.substring(GREATER_THAN_KEYWORD.length()));
		} else if (expression.startsWith(LESSER_THAN_KEYWORD)) {
			return x -> x < parseInt(expression.substring(LESSER_THAN_KEYWORD.length()));
		} else if (expression.startsWith(EQUAL_KEYWORD)) {
			return x -> x == parseInt(expression.substring(EQUAL_KEYWORD.length()));
		} else return x -> true;
	};

	List<String> chain;
	private Map<String, IntPredicate> rules = new HashMap<>();

	@Override
	public void init(final NamedList args) {
		chain = stream(toSolrParams(args).get(CHAIN_KEY, EMPTY_STRING).split(","))
					.map(String::trim)
					.filter(ref -> !ref.isEmpty())
					.collect(toList());

		if (chain.isEmpty()) {
			throw new SolrException(
					SolrException.ErrorCode.SERVER_ERROR,
					"The chain parameter requires at least one request handler reference.");
		}

		final Iterator<String> iterator = chain.iterator();
		stream(toSolrParams(args).get(RULES_KEY, EMPTY_STRING).split(","))
				.map(String::trim)
				.filter(rule -> !rule.isEmpty())
				.map(TO_RULE)
				.forEach(predicate -> rules.put(iterator.next(), predicate));

		rules.put(chain.get(chain.size() - 1), ALWAYS);
	}
	
	@Override
	public void handleRequestBody(
			final SolrQueryRequest request, 
			final SolrQueryResponse response) {
		response.setAllValues(
			chain.stream()
				.map(refName -> requestHandler(request, refName))
				.filter(pair -> pair.getValue() instanceof SearchHandler)
				.map(pair -> executeQuery(request, response, request.getParams(), pair.getValue(), pair.getKey()))
				.filter (responsePair -> rules.getOrDefault(responsePair.getKey(), ALWAYS).test(howManyFound(responsePair.getValue())))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElse(emptyResponse(request, response)));
	}
	
	/**
	 * Returns the total count of matches associated with the given query response.
	 * 
	 * @param qresponse the "response" portion of the {@link QueryResponse}.
	 * @return the total count of matches associated with the given query response.
	 */
	int howManyFound(final NamedList<?> qresponse) {
		final ResultContext context = (ResultContext)qresponse.get(RESPONSE_KEY);
		return context != null ? context.getDocList().size() : 0;
	}
	 
	/**
	 * Returns a Null Object response, indicating no handler produced a match.
	 * 
	 * @param request the current {@link SolrQueryRequest}.
	 * @param response the current {@link SolrQueryResponse}.
	 * @return a Null Object response, indicating no handler produced a match.
	 */
	NamedList<Object> emptyResponse(final SolrQueryRequest request, final SolrQueryResponse response) {
		final SimpleOrderedMap<Object> empty = new SimpleOrderedMap<>();
		empty.add(RESPONSE_KEY, 
				new BasicResultContext(
					EMPTY_DOCLIST, 
					response.getReturnFields(),
					request.getSearcher(),
					null,
					request));
		
		empty.add(RESPONSE_HEADER_KEY, new SimpleOrderedMap<>());
		return empty;
	}
	
	/**
	 * Executes the given handler (query) logic.
	 * @param request the current {@link SolrQueryRequest}.
	 * @param response the current {@link SolrQueryResponse}.
	 * @param params the request parameters.
	 * @param handler the executor handler.
	 * @param name the executor name.
	 * @return the query response, that is, the result of the handler's query execution.
	 */
	@SuppressWarnings("unchecked")
	Map.Entry<String, NamedList<Object>> executeQuery(
			final SolrQueryRequest request, 
			final SolrQueryResponse response, 
			final SolrParams params, 
			final SolrRequestHandler handler,
			final String name) {
		try(final SolrQueryRequest scopedRequest = newFrom(request, params)) {
			final SolrQueryResponse scopedResponse = newFrom(response);
			handler.handleRequest(
					scopedRequest, 
					scopedResponse); 
			return new AbstractMap.SimpleEntry<String, NamedList<Object>>(name, scopedResponse.getValues());
		}
	}
	
	/**
	 * Creates a new {@link SolrQueryRequest} from a given prototype and injects there a set of params. 
	 * 
	 * @param request the prototype {@link SolrQueryRequest}.
	 * @param params the parameters that will be injected.
	 * @return a {@link SolrQueryRequest} clone.
	 */
	public SolrQueryRequest newFrom(final SolrQueryRequest request, final SolrParams params) {
		return new SolrQueryRequestBase(
				request.getCore(), 
				new ModifiableSolrParams(params), 
				new RTimerTree()) {
			@Override
			public Map<Object, Object> getContext() {
				return request.getContext();
			}
			
			@Override
			public SolrIndexSearcher getSearcher() {
				return request.getSearcher();
			}
		};
	}
	
	/**
	 * Creates a new {@link SolrQueryResponse} from a given prototype. 
	 * 
	 * @param response the original {@link SolrQueryResponse}.
	 * @return a clone of the incoming response.
	 */
	public SolrQueryResponse newFrom(final SolrQueryResponse response) {
		final SolrQueryResponse clone = new SolrQueryResponse();
		clone.addResponseHeader(response.getResponseHeader());
		return clone;
	}
	
	/**
	 * Returns the {@link RequestHandler} associated with the given name.
	 * 
	 * @param request the current {@link SolrQueryRequest}.
	 * @param name the name of the requested {@link RequestHandler}.
	 * @return the {@link RequestHandler} associated with the given name (actually a pair that includes also its name).
	 */
	Map.Entry<String, SolrRequestHandler> requestHandler(final SolrQueryRequest request, final String name) throws IllegalArgumentException{
		return new AbstractMap.SimpleEntry<>(
				name,
				core(request).getRequestHandler(name));
	}
	
	/**
	 * Returns the {@link SolrCore} associated with the given request.
	 * 
	 * @param request the current {@link SolrQueryRequest}.
	 * @return the {@link SolrCore} associated with the given request.
	 */
	SolrCore core(final SolrQueryRequest request) {
		return request.getCore();
	}
	
	@Override
	public String getDescription() {
		return "A RequestHandler that wraps two or more request handlers in chain.";
	}
}