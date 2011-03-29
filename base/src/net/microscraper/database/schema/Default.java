package net.microscraper.database.schema;

import net.microscraper.client.AbstractResult;
import net.microscraper.client.Client;
import net.microscraper.client.Mustache;
import net.microscraper.client.Mustache.MissingVariable;
import net.microscraper.client.Mustache.TemplateException;
import net.microscraper.database.AbstractModel;
import net.microscraper.database.DatabaseException.PrematureRevivalException;
import net.microscraper.database.Reference;
import net.microscraper.database.Relationship;
import net.microscraper.database.Resource;

public class Default {
	private final String raw_value;
	private final Reference[] substituted_scraper_refs;
	protected Default(Resource resource) throws PrematureRevivalException {
		raw_value = resource.attribute_get(Model.VALUE);
		//substituted_scrapers = resource.relationship(Model.SCRAPERS);
		Resource[] substituted_scrapers = resource.relationship(Model.SCRAPERS);
		substituted_scraper_refs = new Reference[substituted_scrapers.length];
		for(int i = 0 ; i < substituted_scrapers.length ; i ++) {
			substituted_scraper_refs[i] = substituted_scrapers[i].ref;
		}
	}
	public Default(String name, String value) {
		substituted_scraper_refs = new Reference[] {
			new Reference(value)
		};
		raw_value = value;
	}
	/**
	 * Simulate the Default for the specified source.  This is done by inserting the value
	 * for each of the substituted scrapers.
	 * @param source
	 * @return True if any action has been taken, false otherwise.
	 * @throws PrematureRevivalException
	 * @throws TemplateException
	 */
	public void simulate(AbstractResult source) throws PrematureRevivalException, TemplateException {
		try {
			String value = Mustache.compile(raw_value, source.variables());
			for(int i = 0; i < substituted_scraper_refs.length; i ++) {
				source.addOneToOne(substituted_scraper_refs[i], value);
			}
		} catch (MissingVariable e) {
			Client.context().log.w(e);
		}
	}
	
	public static class Model extends AbstractModel {
		public static final String KEY = "default";
	
		public static final String VALUE = "value";
		
		public static final String SCRAPERS = "scrapers";
		
		protected String _key() { return KEY; }
		protected String[] _attributes() { return new String[] { VALUE }; }
		protected Relationship[] _relationships() {
			return new Relationship[] {
				new Relationship( SCRAPERS, new Scraper.Model())
			};
		}
	}
}
