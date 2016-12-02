package org.gx.labs.crh;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;

public abstract class BaseUnitTest {
	protected final String SAMPLE_KEY = "SAMPLE_KEY";
	protected final String SAMPLE_VALUE = "SAMPLE_VALUE";
	
	protected final static String REQUEST_HANDLER_1_NAME = "/rh1";
	protected final static String REQUEST_HANDLER_2_NAME = "/rh2";
	protected final static String REQUEST_HANDLER_3_NAME = "/rh3";
	
	protected final static List<String> SAMPLE_VALID_CHAIN = asList(
			REQUEST_HANDLER_1_NAME, 
			REQUEST_HANDLER_2_NAME, 
			REQUEST_HANDLER_3_NAME);
	
	protected FacadeRequestHandler cut;
	
	protected SolrRequestHandler rh1;
	protected SolrRequestHandler rh2;
	protected SolrRequestHandler rh3;
	
	protected SolrQueryRequest qrequest;
	protected SolrQueryResponse qresponse;
	
	protected NamedList<Object> args;
	protected ModifiableSolrParams params;
}
