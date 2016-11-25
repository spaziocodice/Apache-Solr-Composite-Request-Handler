package org.gx.labs.crh;

import static java.util.Arrays.stream;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocSlice;

/**
 * A {@link SolrRequestHandler} that chains several children {@link SolrRequestHandler}s.
 * 
 * @author agazzarini
 * @since 1.0
 */
public class CompositeRequestHandler extends RequestHandlerBase {
	@Override
	public void handleRequestBody(
			final SolrQueryRequest request, 
			final SolrQueryResponse response) throws Exception {
		final SolrParams params = request.getParams();

		final ResultContext result = 
				stream(request.getParams().get("chain").split(","))
					.map(refName -> { return requestHandler(request, refName);})
					.map(handler -> {
							request.setParams(new ModifiableSolrParams(params));
							final SolrQueryResponse scopedResponse = clone(response);
							handler.handleRequest(request, scopedResponse); 
							return ((ResultContext)scopedResponse.getResponse());
						})
					.filter(context -> context.getDocList().size() > 0)
					.findFirst()
					.orElse(new BasicResultContext(
								new DocSlice(0, 0, new int[0], new float[0], 0, 0f), 
								response.getReturnFields(),
								request.getSearcher(),
								null,
								request));

		response.addResponse(result);
	}
	
	/**
	 * Clones a given response.
	 * 
	 * @param response the original {@link SolrQueryResponse}.
	 * @return a clone of the incoming response.
	 */
	public SolrQueryResponse clone(final SolrQueryResponse response) {
		return new SolrQueryResponse();
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