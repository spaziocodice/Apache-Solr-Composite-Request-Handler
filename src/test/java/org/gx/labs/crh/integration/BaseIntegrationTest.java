package org.gx.labs.crh.integration;

import org.apache.solr.SolrJettyTestBase;
import org.junit.BeforeClass;

/**
 * A supertype layer for running Solr based integration tests.
 * 
 * @author agazzarini
 * @since 1.0
 * @see http://andreagazzarini.blogspot.com/2016/11/quickly-debug-your-solr-add-on.html
 * @see https://andreagazzarini.blogspot.com/2015/10/how-to-do-integration-tests-with-solr.html
 */
public abstract class BaseIntegrationTest extends SolrJettyTestBase {
	@BeforeClass
	public static void init() throws Exception {
		System.setProperty("solr.data.dir", initCoreDataDir.getAbsolutePath());
		System.setProperty("solr.core.name", "collection1");
		initCore("solrconfig.xml", "schema.xml", "src/solr");
	}
}