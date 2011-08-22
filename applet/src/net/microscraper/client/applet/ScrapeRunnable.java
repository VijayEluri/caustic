package net.microscraper.client.applet;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import net.microscraper.client.Microscraper;

public class ScrapeRunnable implements Runnable {
	private final Microscraper client;
	private final String instructionURI;
	private final Hashtable<String, String> defaults;
		
	public ScrapeRunnable(Microscraper client, String instructionURI, Hashtable<String, String> defaults) {
		this.client = client;
		this.instructionURI = instructionURI;
		this.defaults = defaults;
	}
	
	@Override
	public void run() {
		try {
			AccessController.doPrivileged(new ScrapeAction());
		} catch(Throwable e) {
			//client.e(e);
		}
	}
	
	private class ScrapeAction implements PrivilegedAction<Void> {
		public Void run() {
			try {
				//client.scrapeWithURI(instructionURI, formEncodedDefaults);
				client.scrapeFromUri(instructionURI, defaults);
			} catch(Throwable e) {
				//client.e(e);
			}
			return null;
		}
	}
}