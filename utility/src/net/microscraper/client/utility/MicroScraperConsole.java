package net.microscraper.client.utility;

import java.text.DateFormat;

import net.microscraper.client.Client;
import net.microscraper.client.Client.MicroScraperClientException;
import net.microscraper.client.Interfaces;
import net.microscraper.client.Interfaces.SQL.SQLInterfaceException;
import net.microscraper.client.impl.ApacheBrowser;
import net.microscraper.client.impl.JDBCSQLite;
import net.microscraper.client.impl.JSONME;
import net.microscraper.client.impl.JavaUtilRegexInterface;
import net.microscraper.client.impl.SystemLogInterface;
import net.microscraper.database.schema.Default;

public class MicroScraperConsole {
	private final SystemLogInterface log = new SystemLogInterface();
	private final Client client = Client.initialize(
			new ApacheBrowser(),
			new JavaUtilRegexInterface(),
			new JSONME(),
			new Interfaces.Logger[] { log }
		);
	public static void main (String[] args) {
		new MicroScraperConsole(args);
	}
	public MicroScraperConsole(String[] args) {
		try {
			if(args.length < 1) {
				client.log.i("Must specify a URL to load the scraper object from.");
			} else {
				try {
					Interfaces.SQL sql_interface = new JDBCSQLite("./" + DateFormat.getTimeInstance() + ".sqlite");
					Default[] defaults = new Default[] {
						new Default("Borough_Number", "3"),
						new Default("House_Number", "373"),
						new Default("Street_Name", "Atlantic Ave"),
						new Default("Apartment_Number", "")
					};
					client.scrape(args[0], defaults, sql_interface);
				} catch (MicroScraperClientException e) {
					client.log.e(e);
				}
			}
			client.log.i("Finished execution.");
		} catch (SQLInterfaceException e) {
			client.log.e(e);
		}
	}
}