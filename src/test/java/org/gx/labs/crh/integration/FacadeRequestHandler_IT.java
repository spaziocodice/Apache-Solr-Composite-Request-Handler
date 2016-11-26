package org.gx.labs.crh.integration;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.gx.labs.BaseIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration Test for {@link FacadeRequestHandler_IT}.
 * 
 * @author agazzarini
 * @see http://andreagazzarini.blogspot.it/2016/11/quickly-debug-your-solr-add-on.html
 * @see https://andreagazzarini.blogspot.it/2015/10/how-to-do-integration-tests-with-solr.html
 */
public class FacadeRequestHandler_IT extends BaseIntegrationTest {
	/**
	 * Indexes some sample data as test fixture.
	 * 
	 * @throws Exception hopefully never, otherwise the test will fail.
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		final SolrClient indexer = getSolrClient();
		final SolrInputDocument book1 = new SolrInputDocument();
		book1.setField("id","1");
		book1.setField("title","Apache Solr Essentials");
		book1.setField("author","Andrea Gazzarini");
		
		indexer.add(book1);
		indexer.commit();
	}
	
	@Test
    public void firstHandlerMatch() throws Exception {
		final SolrClient searcher = getSolrClient();
		final String query = "Apache Solr";
		
		final QueryResponse response = searcher.query(new SolrQuery(query));
		assertEquals(1, response.getResults().getNumFound());
		
		final SolrDocument match = response.getResults().iterator().next();
		assertEquals("1",match.getFieldValue("id"));
		assertNull(match.getFieldValue("title"));
		assertNull(match.getFieldValue("author"));
    }
	
	@Test
    public void secondHandlerMatch() throws Exception {
		final SolrClient searcher = getSolrClient();
		final String query = "Solr books";
		
		final QueryResponse response = searcher.query(new SolrQuery(query));
		assertEquals(1, response.getResults().getNumFound());
		
		final SolrDocument match = response.getResults().iterator().next();
		assertEquals("Apache Solr Essentials",match.getFieldValue("title"));
		assertNull(match.getFieldValue("id"));
		assertNull(match.getFieldValue("author"));
    }	
	
	@Test
    public void thirdHandlerMatch() throws Exception {
		final SolrClient searcher = getSolrClient();
		final String query = "Andrea Gazzarini";
		
		final QueryResponse response = searcher.query(new SolrQuery(query));
		assertEquals(1, response.getResults().getNumFound());
		
		final SolrDocument match = response.getResults().iterator().next();
		assertEquals("Andrea Gazzarini",match.getFieldValue("author"));
		assertNull(match.getFieldValue("id"));
		assertNull(match.getFieldValue("title"));
    }		
	
	@Test
	public void noMatch() throws Exception {
		final SolrClient searcher = getSolrClient();
		final String query = "Something that doesn't match";
		
		final QueryResponse response = searcher.query(new SolrQuery(query));
		assertEquals(0, response.getResults().getNumFound());		
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		final SolrClient indexer = getSolrClient();
		indexer.deleteByQuery("*:*");
		indexer.commit();
	}	
}