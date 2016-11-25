package org.gx.labs;

import org.apache.solr.SolrJettyTestBase;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.junit.BeforeClass;

/**
 * A supertype layer for running Solr based integration tests.
 * 
 * @author agazzarini
 * @since 1.0
 */
public abstract class BaseIntegrationTest extends SolrJettyTestBase {
	protected static JettySolrRunner SOLR;

	@BeforeClass
	public static void init() {
		System.setProperty("solr.data.dir", initCoreDataDir.getAbsolutePath());

		try {
			SOLR = createJetty("src/solr", 
					JettyConfig.builder()
						.setPort(8983)
						.setContext("/solr")
						.stopAtShutdown(true).build());
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}
}