package io.sease.crh;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

/**
 * Unit test for checking the invocation chain behavior.
 * 
 * @author agazzarini
 * @since 1.0
 */
public class InvocationChainTestCase extends BaseUnitTest {

	private final Answer<?> returnZeroResults = invocation -> {
		final Object[] args = invocation.getArguments();
		final SolrQueryResponse response = (SolrQueryResponse) args[1];

		final DocList docList = mock(DocList.class);
		when(docList.size()).thenReturn(0);

		final ResultContext oneResult = mock(ResultContext.class);
		when(oneResult.getDocList()).thenReturn(docList);

		response.addResponse(oneResult);
		return null;
	};

	private final Answer<?> returnJustOneResult = invocation -> {
		final Object[] args = invocation.getArguments();
		final SolrQueryResponse response = (SolrQueryResponse) args[1];

		final DocList docList = mock(DocList.class);
		when(docList.size()).thenReturn(1);

		final ResultContext oneResult = mock(ResultContext.class);
		when(oneResult.getDocList()).thenReturn(docList);

		response.addResponse(oneResult);
		return null;
	};

	private final Answer<?> returnMoreThanOneResult = invocation -> {
		final Object[] args = invocation.getArguments();
		final SolrQueryResponse response = (SolrQueryResponse) args[1];

		final DocList docList = mock(DocList.class);
		when(docList.size()).thenReturn(7);

		final ResultContext oneResult = mock(ResultContext.class);
		when(oneResult.getDocList()).thenReturn(docList);

		response.addResponse(oneResult);
		return null;
	};

	@Before
	public void setUp() {
		rh1 = mock(SearchHandler.class);
		rh2 = mock(SearchHandler.class);
		rh3 = mock(SearchHandler.class);

		qrequest = mock(SolrQueryRequest.class);
		qresponse = mock(SolrQueryResponse.class);
		
		args = new SimpleOrderedMap<>();
		args.add(
				CompositeRequestHandler.CHAIN_KEY,
				CHAIN.stream().collect(joining(",")));
		
		params = new ModifiableSolrParams().add(SAMPLE_KEY, SAMPLE_VALUE);

		final SolrCore core = mock(SolrCore.class);
		when(qrequest.getCore()).thenReturn(core);
		
		when(core.getRequestHandler(REQUEST_HANDLER_1_NAME)).thenReturn(rh1);
		when(core.getRequestHandler(REQUEST_HANDLER_2_NAME)).thenReturn(rh2);
		when(core.getRequestHandler(REQUEST_HANDLER_3_NAME)).thenReturn(rh3);
		
		cut = new CompositeRequestHandler();
	} 
	
	/**
	 * If the first handler in the chain answers with a response which matches the corresponding rule,
	 * then the rest of the chain has to be skept.
	 */
	@Test
	public void firstHandlerAnswersAsExpected() {
		final String [] matchingRules = {
				"eq1,gt0,always",
				"gt0,gt0,always",
				"always,eq1,always",
				"always"
		};

		stream(matchingRules).forEach(
				ruleSet -> {
					final NamedList<Object> initArgs = args.clone();
					initArgs.add(CompositeRequestHandler.RULES_KEY, ruleSet);

					doAnswer(returnJustOneResult)
						.when(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

					cut.init(initArgs);
					cut.handleRequestBody(qrequest, qresponse);

					verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
					verifyZeroInteractions(rh2,rh3);

					reset(rh1);
				}
		);
	}

	/**
	 * If the second handler in the chain answers with a response which matches the corresponding rule,
	 * then the rest of the chain has to be skept.
	 */
	@Test
	public void secondRingAnswersSkipRemainingHandlers() {
		final String [] matchingRules = {
				"eq1,gt0,always",
				"gt0,gt0,always"
		};

		stream(matchingRules).forEach(
				ruleSet -> {
					final NamedList<Object> initArgs = args.clone();
					initArgs.add(CompositeRequestHandler.RULES_KEY, ruleSet);

					doAnswer(returnZeroResults)
							.when(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

					doAnswer(returnMoreThanOneResult)
							.when(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

					cut.init(initArgs);
					cut.handleRequestBody(qrequest, qresponse);

					verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
					verify(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
					verifyZeroInteractions(rh3);

					reset(rh1, rh2);
				}
		);

		// Third case which cannot be included in the loop above

		final NamedList<Object> initArgs = args.clone();
		initArgs.add(CompositeRequestHandler.RULES_KEY, "eq1,eq1,always");

		doAnswer(returnZeroResults)
				.when(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

		doAnswer(returnJustOneResult)
				.when(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

		cut.init(initArgs);
		cut.handleRequestBody(qrequest, qresponse);

		verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verify(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verifyZeroInteractions(rh3);
	}

	/**
	 * If the second handler in the chain answers with a response which matches the corresponding rule,
	 * then the rest of the chain has to be skept.
	 */
	@Test
	public void thirdRingAnswersSkipRemainingHandlers() {
		final String [] matchingRules = {
				"eq1,eq1,always",
				"lt0,gt0,always"
		};

		stream(matchingRules).forEach(
				ruleSet -> {
					final NamedList<Object> initArgs = args.clone();
					initArgs.add(CompositeRequestHandler.RULES_KEY, ruleSet);

					doAnswer(returnMoreThanOneResult)
							.when(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

					doAnswer(returnZeroResults)
							.when(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

					cut.init(initArgs);
					cut.handleRequestBody(qrequest, qresponse);

					verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
					verify(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
					verify(rh3).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

					reset(rh1, rh2, rh3);
				}
		);

		doAnswer(returnJustOneResult)
			.when(rh3).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		
		cut.init(args);
		cut.handleRequestBody(qrequest, qresponse);
		
		verify(rh1).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verify(rh2).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		verify(rh3).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
	}		
}