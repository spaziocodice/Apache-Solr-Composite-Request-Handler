package org.gx.labs.crh;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;
/**
 * TODO : INCOMPLETE
 * @author agazzarini
 *
 */
public class FacadeRequestHandlerTestCase {
	private FacadeRequestHandler cut;
	
	private SolrRequestHandler rh1;
	private SolrRequestHandler rh2;
	private SolrRequestHandler rh3;
	
	private final static List<String> SAMPLE_VALID_CHAIN = asList("/rh1", "/rh2", "/rh3");
	
	@Before
	public void setUp() {
		rh1 = mock(SolrRequestHandler.class);
		rh2 = mock(SolrRequestHandler.class);
		rh3 = mock(SolrRequestHandler.class);
		
		cut = new FacadeRequestHandler();
	}
	
	@Test
	public void cloneResponse() {
		final Object response = new Object();
		final NamedList<Object> responseHeader = new SimpleOrderedMap<>();
		
		final SolrQueryResponse qresponse = mock(SolrQueryResponse.class);
		when(qresponse.getResponse()).thenReturn(response);
		when(qresponse.getResponseHeader()).thenReturn(responseHeader);
		
		final SolrQueryResponse clone = cut.newFrom(qresponse);
		
		assertNull(clone.getResponse());
		assertSame(responseHeader, clone.getResponseHeader());
	}
	
	@Test
	public void chainIsDefinedOnInit() {
		final NamedList<Object> args = new SimpleOrderedMap<>();
		args.add(
				FacadeRequestHandler.CHAIN_KEY, 
				SAMPLE_VALID_CHAIN.stream().collect(joining(",")));
		
		cut.init(args);
		
		assertEquals(SAMPLE_VALID_CHAIN, cut.chain);
	}
	
	@Test
	public void emptyChain() {
		final NamedList<Object> args = new SimpleOrderedMap<>();
		cut.init(args);
		
		assertTrue(cut.chain.isEmpty());
	}

	@Test
	public void zeroDocFound() {
		assertEquals(
				0, 
				cut.howManyFound(
						cut.emptyResponse(
								mock(SolrQueryRequest.class), 
								mock(SolrQueryResponse.class))));
	}
	
	@Test
	public void zeroDocFoundInCaseOfNoResultContext() {
		final NamedList<?> responseWhichDoesntContainResultContext = new SimpleOrderedMap<>();
		
		assertEquals(
				0, 
				cut.howManyFound(responseWhichDoesntContainResultContext));
	}
	
	@Test
	public void newRequest() {
		final SolrQueryRequest request = mock(SolrQueryRequest.class);
		final ModifiableSolrParams params = new ModifiableSolrParams().add("SAMPLE_KEY", "SAMPLE_VALUE");
		
		final SolrQueryRequest newRequest = cut.newFrom(request, params);
 
		verify(request).getCore();

		assertNotSame(newRequest, request);
		assertNotSame(newRequest.getParams(), params);
		
		assertEquals(params.size(), newRequest.getParams().toNamedList().size());
		assertEquals(
				newRequest.getParams().get("SAMPLE_KEY"), 
				params.get("SAMPLE_KEY"));
	}
	
	@Test
	public void executeQuery() {
		final SolrQueryRequest request = mock(SolrQueryRequest.class);
		final SolrParams params = new ModifiableSolrParams().add("SAMPLE_KEY", "SAMPLE_VALUE");
		final SolrQueryResponse response = mock(SolrQueryResponse.class);
		final SolrRequestHandler handler = mock(SolrRequestHandler.class);
		
		final NamedList<?> result = cut.executeQuery(request, response, params, handler);
		
		verify(handler).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		assertEquals(1, result.size());
	}
	
	@Test
	public void getCore() {
		final SolrQueryRequest request = mock(SolrQueryRequest.class);
		cut.core(request);
		
		verify(request).getCore();
	}
	
	@Test
	public void getRequestHandler() {
		final SolrQueryRequest request = mock(SolrQueryRequest.class);
		final SolrCore core = mock(SolrCore.class);
		
		when(request.getCore()).thenReturn(core);
		when(core.getRequestHandler("/rh1")).thenReturn(rh1);
		when(core.getRequestHandler("/rh2")).thenReturn(rh2);
		when(core.getRequestHandler("/rh3")).thenReturn(rh3);

		assertSame(rh1, cut.requestHandler(request, "/rh1"));
		assertSame(rh2, cut.requestHandler(request, "/rh2"));
		assertSame(rh3, cut.requestHandler(request, "/rh3"));
	}
}
