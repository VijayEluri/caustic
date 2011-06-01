package net.microscraper.test;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;

import mockit.Expectations;
import mockit.Mocked;
import net.microscraper.client.Client;
import net.microscraper.client.Log;
import net.microscraper.client.UnencodedNameValuePair;
import net.microscraper.client.impl.FileLoader;
import net.microscraper.client.impl.JSONME;
import net.microscraper.client.impl.JavaNetBrowser;
import net.microscraper.client.impl.JavaNetInterface;
import net.microscraper.client.impl.JavaUtilRegexInterface;
import net.microscraper.client.interfaces.Browser;
import net.microscraper.client.interfaces.BrowserException;
import net.microscraper.client.interfaces.JSONInterface;
import net.microscraper.client.interfaces.NetInterface;
import net.microscraper.client.interfaces.Publisher;
import net.microscraper.client.interfaces.URIInterface;
import net.microscraper.client.interfaces.URILoader;

import org.junit.Before;
import org.junit.Test;

public class ClientTest {
	private static final URI fixturesFolder = new File(System.getProperty("user.dir")).toURI().resolve("fixtures/");
	
	private final URILoader uriLoader = new FileLoader();
	private final JSONInterface jsonInterface = new JSONME(uriLoader);
	private final Log log = new Log();
	private final NetInterface netInterface = new JavaNetInterface(new JavaNetBrowser(log, 10000));
	private Client client;
	
	@Mocked Publisher publisher;
	
	@Before
	public void setUp() throws Exception {
		client = new Client(new JavaUtilRegexInterface(), log, netInterface, jsonInterface, Browser.UTF_8);
	}
	
	@Test
	public void testScrape() throws Exception {
		URIInterface location = netInterface.getURI(fixturesFolder.resolve("simple-google.json").toString());
		final String expectedPhrase = "what do we say after hello?";
		
		new Expectations() {
			{
				publisher.publishResult(
						(String) withNull(),
						anyString,
						withSuffix("simple-google.json#"),
						0,
						(String) withNull(),
						(Integer) withNull());
				
				publisher.publishResult(
						expectedPhrase,
						anyString,
						withSuffix("simple-google.json#finds_many.0"),
						anyInt,
						withSuffix("simple-google.json#"),
						0);
			}
		};
		
		UnencodedNameValuePair[] extraVariables = new UnencodedNameValuePair[] {
				new UnencodedNameValuePair("query", "hello")
		};
		try {
			client.scrape(location, extraVariables, publisher);
		} catch(BrowserException e) {
			throw new Exception("Error loading the page.", e);
		}
	}
	
	private void 
}
