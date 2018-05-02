package io.sease.crh.integration;

import java.io.IOException;

import org.junit.Test;

/**
 * A "dummy" integration test for debugging the RequestHandler directly in Solr. 
 * 
 * @author agazzarini
 * @see http://andreagazzarini.blogspot.it/2016/11/quickly-debug-your-solr-add-on.html
 */
public class Debugger extends BaseIntegrationTest {
	/**
	 * Starts Solr and waits for the Enter key pressure.
	 * 
	 * @throws IOException in case of I/O failure.
	 */
	@Test
    public void start() throws IOException {
        System.out.println("Press [Enter] to stop Solr");
        System.in.read(); 
    }
}