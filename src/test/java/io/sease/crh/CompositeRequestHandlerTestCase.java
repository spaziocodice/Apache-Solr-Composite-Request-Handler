package io.sease.crh;

import static io.sease.crh.CompositeRequestHandler.RESPONSE_KEY;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

/**
 * Unit test for {@link CompositeRequestHandler}.
 * 
 * @author agazzarini
 * @since 1.0
 */
public class CompositeRequestHandlerTestCase extends BaseUnitTest {
	@Before
	public void setUp() {
		rh1 = mock(SearchHandler.class);
		rh2 = mock(SearchHandler.class);
		rh3 = mock(SearchHandler.class);
		
		cut = new CompositeRequestHandler();
		
		qrequest = mock(SolrQueryRequest.class);
		qresponse = mock(SolrQueryResponse.class);
		
		args = new SimpleOrderedMap<>();
		args.add(
				CompositeRequestHandler.CHAIN_KEY,
				CHAIN.stream().collect(joining(",")));
		
		params = new ModifiableSolrParams().add(SAMPLE_KEY, SAMPLE_VALUE);
	}
	
	@Test
	public void cloneResponse() {
		final Object response = new Object();
		final NamedList<Object> responseHeader = new SimpleOrderedMap<>();
	
		when(qresponse.getResponse()).thenReturn(response);
		when(qresponse.getResponseHeader()).thenReturn(responseHeader);
		
		final SolrQueryResponse clone = cut.newFrom(qresponse);
		
		assertNull(clone.getResponse());
		assertSame(responseHeader, clone.getResponseHeader());
	}
	
	@Test
	public void chainIsDefinedOnInit() {
		cut.init(args);
		assertEquals(CHAIN, cut.chain);
	}
	
	@Test(expected = SolrException.class)
	public void emptyChain() {
		final NamedList<Object> args = new SimpleOrderedMap<>();
		cut.init(args);
		
		assertTrue(cut.chain.isEmpty());
	}

	@Test
	public void zeroDocFound() {
		assertEquals(
				0, 
				cut.howManyFound(cut.emptyResponse(qrequest, qresponse)));
	}
	
	@Test
	public void zeroDocFoundInCaseOfNoResultContext() {
		assertEquals(0, cut.howManyFound(new SolrQueryResponse()));
	}
	
	@Test
	public void newRequest() {
		final SolrQueryRequest newRequest = cut.newFrom(qrequest, params);
 
		verify(qrequest).getCore();

		assertNotSame(newRequest, qrequest);
		assertNotSame(newRequest.getParams(), params);
		
		assertEquals(params.size(), newRequest.getParams().toNamedList().size());
		assertEquals(
				newRequest.getParams().get(SAMPLE_KEY),
				params.get(SAMPLE_KEY));
	}
	
	@Test
	public void executeQuery() {
		final SolrRequestHandler handler = mock(SolrRequestHandler.class);
		
		final Map.Entry<String, SolrQueryResponse> result = cut.executeQuery(qrequest, qresponse, params, handler, REQUEST_HANDLER_1_NAME);

		verify(handler).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		assertEquals(1.0, Optional.ofNullable(result.getValue())
							.map(SolrQueryResponse::getValues)
							.map(list -> list.get(RESPONSE_KEY))
							.map(ResultContext.class::cast)
							.map(ResultContext::getDocList)
							.map(DocList::matches)
							.orElse(1L),0);
	}
	
	@Test
	public void getCore() {
		cut.core(qrequest);
		verify(qrequest).getCore();
	}
	
	@Test
	public void getRequestHandler() {
		final SolrCore core = mock(SolrCore.class);
		
		when(qrequest.getCore()).thenReturn(core);
		when(core.getRequestHandler(REQUEST_HANDLER_1_NAME)).thenReturn(rh1);
		when(core.getRequestHandler(REQUEST_HANDLER_2_NAME)).thenReturn(rh2);
		when(core.getRequestHandler(REQUEST_HANDLER_3_NAME)).thenReturn(rh3);

		assertSame(rh1, cut.requestHandler(qrequest, REQUEST_HANDLER_1_NAME).getValue());
		assertSame(rh2, cut.requestHandler(qrequest, REQUEST_HANDLER_2_NAME).getValue());
		assertSame(rh3, cut.requestHandler(qrequest, REQUEST_HANDLER_3_NAME).getValue());
	}
}