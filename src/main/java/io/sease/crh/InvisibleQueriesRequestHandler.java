package io.sease.crh;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.solr.common.params.SolrParams.toSolrParams;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;
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
 * A {@link SolrRequestHandler} that subsequently invokes several children {@link SolrRequestHandler}s.
 * 
 * @author agazzarini
 * @since 1.0
 */
public class InvisibleQueriesRequestHandler extends RequestHandlerBase {
	private final static DocList EMPTY_DOCLIST = new DocSlice(0, 0, new int[0], new float[0], 0, 0f);
	
	final static String RESPONSE_KEY = "response"; // If only SolrQueryResponse.RESPONSE_KEY would be public ;)
	final static String RESPONSE_HEADER_KEY = "responseHeader"; // If only SolrQueryResponse.RESPONSE_HEADER_KEY would be public ;)
	final static String CHAIN_KEY= "chain";
	
	List<String> chain;
	
	@Override
	@SuppressWarnings("rawtypes")
	public void init(final NamedList args) { 
		chain = stream(toSolrParams(args).get(CHAIN_KEY, "").split(","))
					.map(ref -> ref.trim())
					.filter(ref -> !ref.isEmpty())
					.collect(toList());
	}
	
	@Override
	public void handleRequestBody(
			final SolrQueryRequest request, 
			final SolrQueryResponse response) throws Exception {
		final SolrParams params = request.getParams();

		response.setAllValues(
			chain.stream()
				.map(refName -> { return requestHandler(request, refName); })
				.filter(SearchHandler.class::isInstance) 
				.map(handler -> { return executeQuery(request, response, params, handler); })
				.filter(qresponse -> howManyFound(qresponse) > 0)
				.findFirst()
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
	 * @return the query response, that is, the result of the handler's query execution.
	 */
	@SuppressWarnings("unchecked")
	NamedList<Object> executeQuery(
			final SolrQueryRequest request, 
			final SolrQueryResponse response, 
			final SolrParams params, 
			final SolrRequestHandler handler) {
		try(final SolrQueryRequest scopedRequest = newFrom(request, params)) {
			final SolrQueryResponse scopedResponse = newFrom(response);
			handler.handleRequest(
					scopedRequest, 
					scopedResponse); 
			return scopedResponse.getValues();	
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
	 * @return the {@link RequestHandler} associated with the given name.
	 */
	SolrRequestHandler requestHandler(final SolrQueryRequest request, final String name) {
		return core(request).getRequestHandler(name);
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