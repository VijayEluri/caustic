package net.microscraper.client;

import net.microscraper.client.Browser.BrowserException;
import net.microscraper.client.Interfaces.JSON;
import net.microscraper.client.Interfaces.JSON.JSONInterfaceException;
import net.microscraper.client.Interfaces.Logger;
import net.microscraper.client.Interfaces.Regexp;
import net.microscraper.database.Database.DatabaseException;
import net.microscraper.database.Database;
import net.microscraper.database.Execution.ExecutionFatality;
import net.microscraper.database.Reference;
import net.microscraper.database.Resource;
import net.microscraper.database.Status;

public class Client {
	//private static Client instance = new Client();
	public static Log log;
	public static Regexp regexp;
	public static JSON json;
	public static Browser browser;
	public static Publisher publisher;
	public static Database db = new Database();
	
	private static boolean initialized = false;
	
	private Client() { }
	public static void initialize(Browser browser, Interfaces.Regexp regexp,
			Interfaces.JSON json, Logger[] loggers, Publisher publisher) {
		Client.browser = browser;
		Client.regexp = regexp;
		Client.json = json;
		Client.publisher = publisher;
		Client.log = new Log();
		for(int i = 0; i < loggers.length ; i ++) {
			log.register(loggers[i]);
		}
		initialized = true;
	}
	
	public static void reset() {
		initialized = false;
	}
	
	public static void scrape(String json_url, Reference ref, Variables extraVariables) {
		if(!initialized)
			throw new IllegalStateException("Scraper not initialized.");
		
		//ResultRoot root = new ResultRoot();
		String raw_obj;
		try {
			log.i("Scraping '" + ref.toString() + "' from JSON loaded from " + json_url);
			
			raw_obj = browser.load(json_url);
			log.i("Raw scraping JSON: " + raw_obj);
			db.inflate(json.getTokener(raw_obj).nextValue());
			Resource resource = db.get(ref);
			
			// Loop while we're in progress, provided the number of missing variables is changing.
			Status curStatus = new Status();
			Status lastStatus;
			do {
				lastStatus = curStatus;
				curStatus = resource.execute(extraVariables);
			} while(curStatus.hasProgressedSince(lastStatus));
		}  catch(JSONInterfaceException e) {
			log.e(e);
		} catch(DatabaseException e) {
			log.e(e);
		} catch (InterruptedException e) {
			log.e(e);
		} catch (BrowserException e) {
			log.e(e);
		} catch (ExecutionFatality e) {
			log.e(e);
		}
	}
	
	public static class MicroScraperClientException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8899853760225376402L;

		public MicroScraperClientException(Throwable e) { super(e); }
	}
}
