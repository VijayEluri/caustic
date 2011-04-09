package net.microscraper.client.impl;

import net.microscraper.client.AbstractResult;
import net.microscraper.client.Client;
import net.microscraper.client.Publisher;
import net.microscraper.client.AbstractResult.Result;
import net.microscraper.client.impl.SQLInterface.SQLInterfaceException;

public class SQLPublisher implements Publisher {
	
	public static final String TABLE_NAME = "results";
	
	public static final String ID_COLUMN_NAME = "id";
	
	public static final String CALLING_SCRAPER_NUMBER = "calling_scraper_number";
	public static final String SCRAPER_NUMBER = "scraper_number";
	public static final String SCRAPER_REF = "scraper_ref";
	public static final String SCRAPER_VALUE = "value";
	
	private final SQLInterface inter;
	public SQLPublisher(SQLInterface sql_interface) throws SQLInterfaceException {
		inter = sql_interface;
		
		try {
			String create_table_sql = "CREATE TABLE " + inter.quoteField(TABLE_NAME) + " (" +
				inter.quoteField(ID_COLUMN_NAME) + " " + inter.idColumnType() + " " + inter.keyColumnDefinition() + ", " +
				inter.quoteField(CALLING_SCRAPER_NUMBER) + " " + inter.intColumnType() + ", " +
				inter.quoteField(SCRAPER_NUMBER) + " " + inter.intColumnType() + ", " +
				inter.quoteField(SCRAPER_REF) + " " + inter.dataColumnType() + ", " + 
				inter.quoteField(SCRAPER_VALUE) + " " + inter.dataColumnType() + " )";
			inter.execute(create_table_sql);
		} catch(SQLInterfaceException e) { // Table may just already exist -- test.
			inter.query("SELECT * FROM " + inter.quoteField(TABLE_NAME));
		}
	}
	
	public void publish(AbstractResult[] results) throws PublisherException {
		for(int i = 0; i < results.length; i ++) {
			try {
				Result[] entries = results[i].children();
				for(int j = 0; j < entries.length ; j ++ ) {
					String insert_sql = "INSERT INTO " + inter.quoteField(TABLE_NAME) + " (" +
						inter.quoteField(CALLING_SCRAPER_NUMBER) + ", " +
						inter.quoteField(SCRAPER_NUMBER) + ", " + 
						inter.quoteField(SCRAPER_REF) + ", " +
						inter.quoteField(SCRAPER_VALUE) +
						") VALUES (" + 
						inter.quoteValue(Integer.toString(entries[j].caller.num())) + ", " +
						inter.quoteValue(Integer.toString(entries[j].num())) + ", " +
						inter.quoteValue(entries[j].ref.toString()) + ", " +
						inter.quoteValue(entries[j].value) + " )";
					inter.execute(insert_sql);
				}
			} catch(SQLInterfaceException e) {
				Client.context().log.e(e);
				throw new PublisherException();
			}
		}
	}
}