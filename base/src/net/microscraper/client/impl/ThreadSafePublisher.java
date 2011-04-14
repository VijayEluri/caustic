package net.microscraper.client.impl;

import java.util.Vector;

import net.microscraper.client.Publisher;
import net.microscraper.database.Result;

public class ThreadSafePublisher implements Publisher {
	Vector results = new Vector();
	
	public boolean live() {
		return true;
	}
	public void publish(Result result) throws PublisherException {
		results.add(result);
	}
	
	/**
	 * Pull out the oldest result.
	 * @return
	 */
	public Result unshift() {
		try {
			return (Result) results.remove(0);
		} catch(IndexOutOfBoundsException e) {
			return null;
		}
	}
}
