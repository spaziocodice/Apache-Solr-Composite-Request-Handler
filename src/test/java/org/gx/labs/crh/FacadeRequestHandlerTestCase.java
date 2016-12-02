package org.gx.labs.crh;

import static org.mockito.Mockito.mock;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
	
	private final static String [] chain = { "/rh1", "/rh2", "/rh3" };
	
	@Before
	public void setUp() {
		rh1 = mock(SolrRequestHandler.class);
		rh2 = mock(SolrRequestHandler.class);
		rh3 = mock(SolrRequestHandler.class);
		
		cut = new FacadeRequestHandler();
//		cut = new FacadeRequestHandler() {
//			@Override
//			SolrRequestHandler requestHandler(final SolrQueryRequest request, final String name) {
//				if ("/rh1".equals(name)) {
//					return rh1;
//				} else if ("/rh2".equals(name)) {
//					return rh2;
//				} else if ("/rh3".equals(name)) {
//					return rh3;
//				} 
//				
//				throw new IllegalArgumentException("Unknown request handler: " + name);
//			}
//		};
	}
	
	@Test
	public void cloneResponse() {
		final Object response = new Object();
		final NamedList<Object> responseHeader = new SimpleOrderedMap<>();
		
		final SolrQueryResponse qresponse = mock(SolrQueryResponse.class);
		when(qresponse.getResponse()).thenReturn(response);
		when(qresponse.getResponseHeader()).thenReturn(responseHeader);
		
		final SolrQueryResponse clone = cut.clone(qresponse);
		
		assertNull(clone.getResponse());
		assertSame(responseHeader, clone.getResponseHeader());
	}
	
	public void stopAtFirstNonZeroResult() {
//		when(rh2.handleRequest(
//				any(SolrQueryRequest.class), 
//				any(SolrQueryResponse.class)));
	}
}
