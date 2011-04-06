package net.microscraper.client;

import net.microscraper.client.AbstractResult.Result;
import net.microscraper.client.Interfaces.SQL.SQLInterfaceException;

public class Publisher {
	
	public static final String TABLE_NAME = "results";
	
	public static final String ID_COLUMN_NAME = "id";
	
	public static final String CALLING_SCRAPER_NUMBER = "calling_scraper_number";
	public static final String SCRAPER_NUMBER = "scraper_number";
	public static final String SCRAPER_REF = "scraper_ref";
	public static final String SCRAPER_VALUE = "value";
	
	private final Interfaces.SQL inter;
	public Publisher(Interfaces.SQL sql_interface) throws SQLInterfaceException {
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
	
	public void publish(AbstractResult result) throws SQLInterfaceException {
		Result[] entries = result.children();
		for(int i = 0; i < entries.length ; i ++ ) {
			String insert_sql = "INSERT INTO " + inter.quoteField(TABLE_NAME) + " (" +
				inter.quoteField(CALLING_SCRAPER_NUMBER) + ", " +
				inter.quoteField(SCRAPER_NUMBER) + ", " + 
				inter.quoteField(SCRAPER_REF) + ", " +
				inter.quoteField(SCRAPER_VALUE) +
				") VALUES (" + 
				inter.quoteValue(Integer.toString(entries[i].caller.num())) + ", " +
				inter.quoteValue(Integer.toString(entries[i].num())) + ", " +
				inter.quoteValue(entries[i].ref.toString()) + ", " +
				inter.quoteValue(entries[i].value) + " )";
			inter.execute(insert_sql);
		}
	}
}