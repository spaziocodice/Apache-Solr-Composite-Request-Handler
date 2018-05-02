package io.sease.crh;

import static java.util.stream.Collectors.joining;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
/**
 * Unit test for checking the invocation chain behavior.
 * 
 * @author agazzarini
 * @since 1.0
 */
public class InvocationChainTestCase extends BaseUnitTest {
	
	private SolrCore core;
	
	private ResultContext positiveResult;
	private DocList docList;
	
	@Before
	public void setUp() {
		rh1 = mock(SearchHandler.class);
		rh2 = mock(SearchHandler.class);
		rh3 = mock(SearchHandler.class);

		qrequest = mock(SolrQueryRequest.class);
		qresponse = mock(SolrQueryResponse.class);
		
		args = new SimpleOrderedMap<>();
		args.add(
				InvisibleQueriesRequestHandler.CHAIN_KEY, 
				SAMPLE_VALID_CHAIN.stream().collect(joining(",")));
		
		params = new ModifiableSolrParams().add(SAMPLE_KEY, SAMPLE_VALUE);
		
		core = mock(SolrCore.class);
		when(qrequest.getCore()).thenReturn(core);
		
		when(core.getRequestHandler(REQUEST_HANDLER_1_NAME)).thenReturn(rh1);
		when(core.getRequestHandler(REQUEST_HANDLER_2_NAME)).thenReturn(rh2);
		when(core.getRequestHandler(REQUEST_HANDLER_3_NAME)).thenReturn(rh3);
		
		cut = new InvisibleQueriesRequestHandler();
		
		positiveResult = mock(ResultContext.class);
		docList = mock(DocList.class);
		when(docList.size()).thenReturn(1);
		when(positiveResult.getDocList()).thenReturn(docList);
	} 
	
	/**
	 * If the first handler in the chain answers with a no-zero result, then the rest of the chain has to be skept.
	 * 
	 * @throws Exception hopefully never, otherwise the test fails.
	 */
	@Test
	public void firstRingAnswersSkipRemainingHandlers() throws Exception {
		doAnswer(new Answer<Void>() {
		      public Void answer(final InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          final SolrQueryResponse response = (SolrQueryResponse) args[1];
		          response.addResponse(positiveResult);
		          return null;
		      }})
		.when(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		
		cut.init(args);
		cut.handleRequestBody(qrequest, qresponse);
		
		verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verifyZeroInteractions(rh2,rh3);
	}
	
	/**
	 * If the second handler in the chain answers with a no-zero result, then the rest of the chain has to be skept.
	 * 
	 * @throws Exception hopefully never, otherwise the test fails.
	 */
	@Test
	public void secondRingAnswersSkipRemainingHandlers() throws Exception {
		doAnswer(new Answer<Void>() {
		      public Void answer(final InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          final SolrQueryResponse response = (SolrQueryResponse) args[1];
		          response.addResponse(positiveResult);
		          return null;
		      }})
		.when(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		
		cut.init(args);
		cut.handleRequestBody(qrequest, qresponse);
		
		verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verify(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verifyZeroInteractions(rh3);
	}	
	
	/**
	 * If the third handler in the chain answers with a no-zero result, then all handlers need to be invoked.
	 * 
	 * @throws Exception hopefully never, otherwise the test fails.
	 */
	@Test
	public void thirdRingAnswersSkipRemainingHandlers() throws Exception {
		doAnswer(new Answer<Void>() {
		      public Void answer(final InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          final SolrQueryResponse response = (SolrQueryResponse) args[1];
		          response.addResponse(positiveResult);
		          return null;
		      }})
		.when(rh3).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		
		cut.init(args);
		cut.handleRequestBody(qrequest, qresponse);
		
		verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verify(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verify(rh3).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
	}		
}
