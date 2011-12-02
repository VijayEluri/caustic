package net.caustic.client;

import static org.junit.Assert.*;
import static net.caustic.util.StringUtils.quote;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import mockit.Expectations;
import mockit.NonStrict;
import mockit.Verifications;
import mockit.VerificationsInOrder;
import net.caustic.Executable;
import net.caustic.LogScraperListener;
import net.caustic.Scraper;
import net.caustic.ScraperListener;
import net.caustic.database.Database;
import net.caustic.database.DatabaseListener;
import net.caustic.database.MemoryDatabase;
import net.caustic.log.Logger;
import net.caustic.log.SystemErrLogger;
import net.caustic.scope.Scope;
import net.caustic.scope.SerializedScope;
import net.caustic.util.StringUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test {@link ScraperInterface} with calls to actual sites.
 * @author realest
 *
 */
public class ScraperIntegrationTest {
	
	/**
	 * How many milliseconds to wait for a scraper to go idle.
	 */
	private static final int SCRAPER_WAIT_TIME = 20000;
	private static final String demosDir = "../demos/";
	private @NonStrict ScraperListener listener;
	private Hashtable<String, String> input;
	private Scraper scraper;
	private Logger logger = new SystemErrLogger();
	private Database db;
	
	@Before
	public void setUp() throws Exception {
		db = new MemoryDatabase();
		scraper = new Scraper(db, listener);
		scraper.register(logger);
		scraper.setAutoRun(true);
		
		input = new Hashtable<String, String>();
	}
	/*
	@After
	public void tearDown() throws Exception {
		//join(scope);
	}*/
	
	@Test
	public void testScrapeStuck() throws Exception {
		final Scope scope = scraper.scrape(demosDir + "simple-google.json", input);
		join(scope);
		
		new Verifications() {{
			listener.onScopeComplete(scope, 0, 1, 0);
		}};
	}

	@Test
	public void testScrapeStuckThenUnstuck() throws Exception {
		final Scope scope = scraper.scrape(demosDir + "simple-google.json", input);
		join(scope);
		
		new Verifications() {{
			listener.onScopeComplete(scope, 0, 1, 0);
		}};
		
		db.put(scope, "query", "hello");
		join(scope);

		new Verifications() {{
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	
	@Test
	public void testScrapeFail() throws Exception {	
		final Scope scope = scraper.scrape("path/to/nothing.json", input);
		join(scope);
		
		new Verifications() {{
			listener.onScopeComplete(scope, 0, 0, 1);
		}};
	}
	
	@Test
	public void testScrapeSimpleGoogle() throws Exception {
		input.put("query", "hello");
		final Scope scope = scraper.scrape(demosDir + "simple-google.json", input);
		join(scope);
		
		new VerificationsInOrder() {{
			listener.onNewScope(scope(0), scope(1), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(2), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(3), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(4), withPrefix("I say"));
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	
	/**
	 * Assure that we can start an instruction after it was paused.
	 * @throws Exception
	 */
	@Test
	public void testScrapeSimpleGooglePause() throws Exception {		
		input.put("query", "hello");
		scraper.setAutoRun(false);
		
		final List<Executable> resumes = new ArrayList<Executable>();
		new Expectations() {{
			listener.onPause((Scope) any, anyString, anyString, (Executable) any); times = 1;
			forEachInvocation = new Object() {
				public void exec(Scope scope, String instruction, String uri, Executable executable) {
					System.out.println("adding resume");
					resumes.add(executable);
				}
			};
		}};
		final Scope scope = scraper.scrape(demosDir + "simple-google.json", input);
		//join(scope); // to join here would hang!
		
		Thread.sleep(2000); // pausin'
		assertFalse(db.isScopeComplete(scope));
		new VerificationsInOrder() {{
			listener.onScopeComplete(scope, anyInt, anyInt, anyInt); times = 0;
		}};
		
		for(Executable resume : resumes) {
			scraper.submit(resume);
		}
		join(scope);
		
		new VerificationsInOrder() {{
			listener.onNewScope(scope(0), scope(1), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(2), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(3), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(4), withPrefix("I say"));
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	
	@Test
	public void testScrapeSimpleGoogleQuoted() throws Exception {		
		input.put("query", "hello");
		// it shouldn't make a difference if we quote a string.
		final Scope scope = scraper.scrape(quote(demosDir + "simple-google.json"), input);
		join(scope);
		
		new VerificationsInOrder() {{			
			listener.onNewScope(scope(0), scope(1), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(2), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(3), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(4), withPrefix("I say"));
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	
	@Test
	public void testScrapeSimpleGooglePointer() throws Exception {
		input.put("query", "hello");
		final Scope scope = scraper.scrape(demosDir + "pointer.json", input);
		join(scope);
		
		new VerificationsInOrder() {{			
			listener.onNewScope(scope(0), scope(1), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(2), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(3), withPrefix("I say"));
			listener.onNewScope(scope(0), scope(4), withPrefix("I say"));
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	
	@Test
	public void testArrayOfScrapes() throws Exception {		
		input.put("query", "hello");
		input.put("Number", "373");
		input.put("Street", "Atlantic Ave");
		input.put("Borough", "3");
		input.put("Apt", "");
				
		final Scope scope = scraper.scrape(demosDir + "array.json", input);
		join(scope);
		
		new Verifications() {{
			listener.onNewScope(scope(0), (Scope) any, withPrefix("I say ")); minTimes = 1;
			listener.onNewScope(scope(0), (Scope) any, "373 ATLANTIC AVENUE C"); minTimes = 1;
			listener.onNewScope(scope(0), (Scope) any, "373 ATLANTIC AVENUE CORPORATION"); minTimes = 1;
			listener.onScopeComplete(scope, 5, 0, 0);
		}};
	}
	
	@Test
	public void testArrayOfScrapesWithFail() throws Exception {		
		input.put("query", "hello");
		
		// one should fail, one should succeed.
		final Scope scope =
				scraper.scrape("[\"/path/to/nothing.json\", \"" + demosDir + "simple-google.json\"]", input);
		
		join(scope);
		
		new Verifications() {{
			listener.onNewScope(scope(0), (Scope) any, withPrefix("I say ")); minTimes = 1;
			listener.onScopeComplete(scope, 3, 0, 1);
		}};
	}
	
	/**
	 * Test several calls to scrape before joining.
	 * @throws Exception
	 */
	@Test
	public void testMultipleSimpleScrapes() throws Exception {
		final Scope failure = scraper.scrape("path/to/nothing.json", new Hashtable()); // should fail
		final Scope stuck = scraper.scrape(demosDir + "simple-google.json", new Hashtable()); // should get stuck
		final Scope success = scraper.scrape(demosDir + "simple-google.json", new Hashtable()); // should succeed		
		
		join(failure);
		join(stuck);
		join(success);
		db.put(success, "query", "hello");
		join(success);
		new Verifications() {{
			listener.onScopeComplete(failure, 0, 0, 1);
			listener.onScopeComplete(stuck, 0, 1, 0);
			listener.onScopeComplete(success, 2, 0, 0);
		}};
	}
	
	@Test
	public void testScrapeAllComplexGoogle() throws Exception {
		input.put("query", "hello");
		final Scope scope = scraper.scrape(demosDir + "complex-google.json", input);
		join(scope);
		
		new VerificationsInOrder() {{
			listener.onPut(scope, "query", "hello");
			
			// scopes named 'query'
			listener.onNewScope(scope, (Scope) any, anyString); minTimes = 1;
			
			// terminal scopes
			listener.onNewScope((Scope) any, (Scope) any, withPrefix("I say "));
		
			listener.onScopeComplete(scope, anyInt, 0, 0);
		}};
	}
	
	@Test
	public void testScrapeAllReferenceGoogle() throws Exception {
		input.put("query", "hello");
		final Scope scope = scraper.scrape(demosDir + "reference-google.json", input);
		join(scope);
		
		new VerificationsInOrder() {{
			listener.onPut(scope, "query", "hello");
			
			// scopes named 'query'
			listener.onNewScope(scope, (Scope) any, anyString); minTimes = 1;
			
			// terminal scopes
			listener.onNewScope((Scope) any, (Scope) any, withPrefix("I say "));
		
			listener.onScopeComplete(scope, anyInt, 0, 0);
		}};
	}
	
	@Test
	public void testScrapeAllNYCPropertyOwners() throws Exception {
		input.put("Number", "373");
		input.put("Street", "Atlantic Ave");
		input.put("Borough", "3");
		input.put("Apt", "");
		
		final Scope scope = scraper.scrape(demosDir + "nyc/nyc-property-owner.json", input);
		join(scope);
		
		new VerificationsInOrder() {{
			listener.onNewScope(scope, scope(1), "373 ATLANTIC AVENUE C");
			listener.onNewScope(scope, scope(2), "373 ATLANTIC AVENUE CORPORATION");
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	
	@Test
	public void testScrapeAllBKPropertyOwners() throws Exception {
		input.put("Number", "373");
		input.put("Street", "Atlantic Ave");
		
		final Scope scope = scraper.scrape(demosDir + "nyc/BK-property.json", input);
		join(scope);

		new VerificationsInOrder() {{
			listener.onNewScope(scope, scope(1), "373 ATLANTIC AVENUE C");
			listener.onNewScope(scope, scope(2), "373 ATLANTIC AVENUE CORPORATION");
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	

	@Test
	public void testScrapePauseBKPropertyOwners() throws Exception {
		input.put("Number", "373");
		input.put("Street", "Atlantic Ave");

		// catch executables
		final List<Executable> paused = new ArrayList<Executable>();
		new Expectations() {{
			listener.onPause((Scope) any, anyString, anyString, (Executable) any);
				forEachInvocation = new Object() {
					@SuppressWarnings("unused")
					public void run(Scope scope, String instruction,
							String uri, Executable executable) {
						paused.add(executable);
					}
				};
		}};
		
		scraper.setAutoRun(false);
		final Scope scope = scraper.scrape(demosDir + "nyc/BK-property.json", input);
		
		Thread.sleep(1000);
		assertFalse(db.isScopeComplete(scope));
		assertEquals(1, paused.size());
		
		scraper.submit(paused.get(0));
		
		join(scope);

		new VerificationsInOrder() {{
			listener.onNewScope(scope, scope(1), "373 ATLANTIC AVENUE C");
			listener.onNewScope(scope, scope(2), "373 ATLANTIC AVENUE CORPORATION");
			listener.onScopeComplete(scope, 2, 0, 0);
		}};
	}
	
		/*
	@Test
	public void testScrapeNYCIncentives() throws Exception {
		BasicNameValuePair[] extraVariables = new BasicNameValuePair[] {
				new BasicNameValuePair("Borough Number", "1"),
				new BasicNameValuePair("Block", "1171"),
				new BasicNameValuePair("Lot", "63")
		};
		new Expectations() {
			
			{
				publisher.publishResult(
						(String) withNull(),
						anyString,
						withEqual(nycIncentives),
						0,
						(JSONLocation) withNull(),
						(Integer) withNull());
				$ = "Problem with initial page"; times = 1;
				
				publisher.publishResult(
						"VIEWSTATE",
						anyString,
						withEqual(nycIncentives.resolve("#/finds_one/0")),
						0,
						withEqual(nycIncentives),
						0);
				$ = "Problem with Viewstate"; times = 1;

				publisher.publishResult(
						"EVENTVALIDATION",
						anyString,
						withEqual(eventValidation),
						0,
						withEqual(nycIncentives),
						0);
				$ = "Problem with EventValidation"; times = 1;
				
				publisher.publishResult(
						(String) withNull(),
						anyString,
						withEqual(nycIncentives.resolve("#/then")),
						0,
						withEqual(nycIncentives),
						0);
				$ = "Problem with data page"; times = 1;
				
				publisher.publishResult(
						"Benefit Name",
						"421A-Newly constructed Multiple Dwelling Residential Property",
						withEqual(nycIncentives.resolve("#/then/finds_one/0")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Benefit Name result"; times = 1;
				
				publisher.publishResult(
						"Benefit Amount",
						withPrefix("$"),
						withEqual(nycIncentives.resolve("#/then/finds_one/1")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Benefit Name result"; times = 1;
				

				publisher.publishResult(
						"Current Benefit Year",
						"16",
						withEqual(nycIncentives.resolve("#/then/finds_one/2")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Benefit Year result"; times = 1;

				publisher.publishResult(
						"Number of Benefit Years",
						"20",
						withEqual(nycIncentives.resolve("#/then/finds_one/3")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Number of Benefit Years result"; times = 1;
				
				publisher.publishResult(
						"Benefit Type",
						"Completion",
						withEqual(nycIncentives.resolve("#/then/finds_one/4")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Benefit Type result"; times = 1;

				publisher.publishResult(
						"Benefit Start Date",
						"July 01, 1996",
						withEqual(nycIncentives.resolve("#/then/finds_one/5")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Benefit Start Date result"; times = 1;

				publisher.publishResult(
						"Benefit End Date",
						"June 30, 2016",
						withEqual(nycIncentives.resolve("#/then/finds_one/6")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Benefit Start Date result"; times = 1;

				publisher.publishResult(
						"Ineligible Commercial %",
						"00.0000%",
						withEqual(nycIncentives.resolve("#/then/finds_one/7")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Ineligible Commercial % result"; times = 1;
				

				publisher.publishResult(
						"Base Year",
						"Year ending June 30, 1993",
						withEqual(nycIncentives.resolve("#/then/finds_one/8")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Base Year result"; times = 1;

				publisher.publishResult(
						"Base Year Assessed Value",
						"$5,500,000",
						withEqual(nycIncentives.resolve("#/then/finds_one/9")),
						0,
						withEqual(nycIncentives.resolve("#/then")),
						0);
				$ = "Problem with Base Year Assessed Value result"; times = 1;
				
			}
		};
		
		testScrape(nycIncentives, extraVariables);
		
	}
	

	@Test
	public void testScrapeNYCIncentivesSimple() throws Exception {
		BasicNameValuePair[] extraVariables = new BasicNameValuePair[] {
				new BasicNameValuePair("Borough Number", "1"),
				new BasicNameValuePair("Block", "1171"),
				new BasicNameValuePair("Lot", "63")
		};
		
		new Expectations() {
			{
				publisher.publishResult(
						(String) withNull(),
						anyString,
						withEqual(nycIncentivesSimple),
						0,
						(JSONLocation) withNull(),
						(Integer) withNull());
				$ = "Problem with initial page"; times = 1;
				
				// Dependent page load.
				publisher.publishResult(
						(String) withNull(),
						anyString,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0,
						withEqual(nycIncentivesSimple),
						0);
				$ = "Problem with data page"; times = 1;
				
				publisher.publishResult(
						"Benefit Name",
						"421A-Newly constructed Multiple Dwelling Residential Property",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/0")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Benefit Name result"; times = 1;
				
				publisher.publishResult(
						"Benefit Amount",
						withPrefix("$"),
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/1")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Benefit Amount result"; times = 1;

				publisher.publishResult(
						"Current Benefit Year",
						"16",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/2")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Benefit Year result"; times = 1;

				publisher.publishResult(
						"Number of Benefit Years",
						"20",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/3")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Number of Benefit Years result"; times = 1;
				
				publisher.publishResult(
						"Benefit Type",
						"Completion",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/4")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Benefit Type result"; times = 1;

				publisher.publishResult(
						"Benefit Start Date",
						"July 01, 1996",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/5")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Benefit Start Date result"; times = 1;

				publisher.publishResult(
						"Benefit End Date",
						"June 30, 2016",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/6")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Benefit Start Date result"; times = 1;

				publisher.publishResult(
						"Ineligible Commercial %",
						"00.0000%",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/7")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Ineligible Commercial % result"; times = 1;
				

				publisher.publishResult(
						"Base Year",
						"Year ending June 30, 1993",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/8")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Base Year result"; times = 1;

				publisher.publishResult(
						"Base Year Assessed Value",
						"$5,500,000",
						withEqual(nycIncentivesSimple.resolve("#/then/finds_one/9")),
						0,
						withEqual(nycIncentivesSimple.resolve("#/then")),
						0);
				$ = "Problem with Base Year Assessed Value result"; times = 1;
			}
		};
		
		testScrape(nycIncentivesSimple, extraVariables);
	}
	*/
	/**
	 * Convenience method to test one Scraper with our mock publisher.
	 * @param String {@link URIInterface} location of the {@link Scraper}
	 * instructions.
	 * @param extraVariables Array of {@link BasicNameValuePair}s to
	 * use as extra {@link Variables}.
	 * @throws Exception If the test failed.
	 */
	/*private void testScrape(URIInterface location,
			BasicNameValuePair[] extraVariables) throws Exception {
		try {
			client.scrapeAll(location, extraVariables);
		} catch(BrowserException e) {
			throw new Exception("Error loading the page.", e);
		}
	}*/
	
	/**
	 * Convenience method to generate a matching scope from an int.
	 * Does not generate a name, because that is not used to check
	 * scope uniqueness.
	 * @param scopeNumber The <code>int</code> number of the scope.
	 * @return A {@link Scope}
	 */
	private Scope scope(int scopeNumber) {
		return new SerializedScope(Integer.toString(scopeNumber), "");
	}
	
	/**
	 * Wait {@link #SCRAPER_WAIT_TIME} for {@link #db} to be complete
	 * on <code>scope</code>.
	 * @throws InterruptedException
	 */
	private void join(Scope scope) throws Exception {
		final int cycle = 50;
		int timer = 0;
		while(!db.isScopeComplete(scope)) {
			Thread.sleep(cycle);
			timer += cycle;
			if(timer > SCRAPER_WAIT_TIME) {
				scraper.interrupt();
				throw new InterruptedException("Scraper not idle after " + SCRAPER_WAIT_TIME + " milliseconds.");
			}
		}
	}
}
