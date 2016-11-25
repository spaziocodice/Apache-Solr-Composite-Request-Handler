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
	
	@SuppressWarnings("unchecked")
	public SolrQueryResponse clone(final SolrQueryResponse response) {
		final SolrQueryResponse clone = new SolrQueryResponse();
//		clone.setAllValues(response.getValues());
//d		System.out.println(response.getValues().hashCode());
		return clone;
	}
	
	SolrRequestHandler requestHandler(final SolrQueryRequest request, final String name) {
		return core(request).getRequestHandler(name);
	}
	
	SolrCore core(final SolrQueryRequest request) {
		return request.getCore();
	}
	
	@Override
	public String getDescription() {
		return "A RequestHandler that wraps two or more request handlers in chain.";
	}
}