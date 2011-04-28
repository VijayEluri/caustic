package net.microscraper.database.schema;

import java.util.Vector;

import net.microscraper.client.Interfaces.Regexp.NoMatches;
import net.microscraper.client.Mustache.MissingVariable;
import net.microscraper.client.Utils.HashtableWithNulls;
import net.microscraper.client.Variables;
import net.microscraper.database.Attribute.AttributeDefinition;
import net.microscraper.database.Database.ResourceNotFoundException;
import net.microscraper.database.Execution;
import net.microscraper.database.Execution.FatalExecutionException;
import net.microscraper.database.Execution.Status;
import net.microscraper.database.Model.ModelDefinition;
import net.microscraper.database.Relationship.RelationshipDefinition;
import net.microscraper.database.Resource;
import net.microscraper.database.schema.Regexp.RegexpExecution;
import net.microscraper.database.schema.WebPage.WebPageExecution;

public class Scraper extends Resource {
	protected final HashtableWithNulls executions = new HashtableWithNulls();
	
	private static final RelationshipDefinition WEB_PAGES =
		new RelationshipDefinition( "web_pages", WebPage.class );
	private static final RelationshipDefinition SOURCE_SCRAPERS =
		new RelationshipDefinition( "source_scrapers", Scraper.class);
	private static final RelationshipDefinition REGEXPS =
		new RelationshipDefinition( "regexps", Regexp.class);
	
	public ModelDefinition definition() {
		return new ModelDefinition() {
			public AttributeDefinition[] attributes() {
				return new AttributeDefinition[] { };
			}
			public RelationshipDefinition[] relationships() {
				return new RelationshipDefinition[] {
					WEB_PAGES, SOURCE_SCRAPERS, REGEXPS
				};
			}
		};
	}
	
	private String substituteValue = null;
	
	public void substitute(String value) {
		substituteValue = value;
	}
	
	public boolean isOneToMany() {		
		if((getNumberOfRelatedResources(WEB_PAGES) + getNumberOfRelatedResources(SOURCE_SCRAPERS)) > 1 ||
				getNumberOfRelatedResources(REGEXPS) > 1) // one-to-many if pulling from multiple sources, or multiple regexps.
			return true;
		return false;
	}
	
	public ScraperExecution[] getExecutions(Execution caller) throws ResourceNotFoundException {
		if(substituteValue != null) {
			return new ScraperExecution[] {new ScraperExecution(this, caller, substituteValue) };
		}
		
		if(!executions.containsKey(caller)) {
			Resource[] regexps =  getRelatedResources(REGEXPS);
			Resource[] scrapers = getRelatedResources(SOURCE_SCRAPERS); 
			Resource[] webPages = getRelatedResources(WEB_PAGES);
			for(int i = 0 ; i < regexps.length ; i ++ ) {
				for(int j = 0 ; j < scrapers.length ; j ++) {
					new ScraperExecutionFromScraper(this, caller, (Regexp) regexps[i], (Scraper) scrapers[j]);
				}
				for(int j = 0 ; j < webPages.length ; j ++) {
					new ScraperExecutionFromWebPage(this, caller, (Regexp) regexps[i], (WebPage) webPages[j]);
				}
			}
		}
		Vector executionsForCaller = (Vector) executions.get(caller);
		ScraperExecution[] executionsAry = new ScraperExecution[executionsForCaller.size()];
		executionsForCaller.copyInto(executionsAry);
		return executionsAry;
	}
	
	private Status execute(Execution caller, Variables extraVariables) throws ResourceNotFoundException, FatalExecutionException {
		ScraperExecution[] scrapers = getExecutions(caller);
		Status compoundStatus = Status.IN_PROGRESS;
		for(int i = 0 ; i < scrapers.length ; i++) {
			if(extraVariables != null)
				scrapers[i].addVariables(extraVariables);
			if(compoundStatus != Status.FAILURE) {
				compoundStatus = scrapers[i].execute();
			}
		}
		return compoundStatus;
	}
	
	public Status execute(Variables extraVariables) throws ResourceNotFoundException, FatalExecutionException {
		return execute(null, extraVariables);
	}
	
	public Status execute(Execution caller) throws ResourceNotFoundException, FatalExecutionException {
		return execute(caller, null);
	}
	
	public static class ScraperExecution extends ResourceExecution {
		private RegexpExecution regexpExecution;
		//private final Hashtable matches = new Hashtable();
		private String match;
		private final Scraper scraper;
		private Status status = Status.IN_PROGRESS;
		private ScraperExecution(Scraper scraper, Execution caller, Regexp regexp) throws ResourceNotFoundException {
			super(scraper, caller);
			this.scraper = scraper;
			regexpExecution = regexp.getExecution(getSourceExecution());
			if(!scraper.executions.containsKey(caller)) {
				scraper.executions.put(caller, new Vector());
			}
			Vector executionsForCaller = (Vector) scraper.executions.get(caller);
			executionsForCaller.addElement(this);
		}
		private ScraperExecution(Scraper scraper, Execution caller, String match) {
			super(scraper, caller);
			this.status = Status.SUCCESSFUL;
			this.match = match;
			this.scraper = scraper;
		}
		protected boolean isOneToMany() {
			return scraper.isOneToMany();
		}
		protected Variables getLocalVariables() {
			if(match != null) {
				Variables variables = new Variables();
				variables.put(scraper.ref().title, match);
				return variables;
			} else {
				return null;
			}
		}
		// Replicate once we have a source.
		protected void execute(String source) throws NoMatches, MissingVariable, FatalExecutionException {
			String[] matches = regexpExecution.allMatches(source);
			status = Status.SUCCESSFUL;
			match = matches[0];
			for(int i = 1 ; i < matches.length ; i ++) {
				new ScraperExecution(scraper, getSourceExecution(), matches[i]);
			}
		}
		public String match() {
			return match;
		}
		protected Status execute() throws FatalExecutionException {
			return status;
		}
		public Status getStatus() {
			return status;
		}
		public String getPublishValue() {
			return match;
		}
	}
	
	private static class ScraperExecutionFromWebPage extends ScraperExecution {
		private final WebPageExecution sourceWebPageExecution;
		private Status status = Status.IN_PROGRESS;
		private ScraperExecutionFromWebPage(Scraper scraper, Execution caller, Regexp regexp, WebPage webPage)
				throws ResourceNotFoundException {
			super(scraper, caller, regexp);

			sourceWebPageExecution = webPage.getExecution(getSourceExecution());
		}
		protected Status execute() throws FatalExecutionException {
			status = Status.SUCCESSFUL;
			try {
				if(sourceWebPageExecution.execute() == Status.SUCCESSFUL) {
					try {
						execute(sourceWebPageExecution.load());
					} catch(NoMatches e) {
						status = Status.FAILURE;
					}
				} else {
					status = Status.IN_PROGRESS;
				}
			} catch (MissingVariable e) {
				status = Status.IN_PROGRESS;
			}
			return status;
		}
	}
	
	private static class ScraperExecutionFromScraper extends ScraperExecution {
		private Status status = Status.IN_PROGRESS;
		private final Scraper sourceScraper;
		private ScraperExecutionFromScraper(Scraper scraper, Execution caller, Regexp regexp, Scraper sourceScraper)
				throws ResourceNotFoundException {
			super(scraper, caller, regexp);
			this.sourceScraper = sourceScraper;
		}
		protected Status execute() throws FatalExecutionException {
			status = Status.SUCCESSFUL;
			try {
				if(sourceScraper.execute(getSourceExecution()) == Status.SUCCESSFUL) {
					ScraperExecution[] sourceScraperExecutions = sourceScraper.getExecutions(getSourceExecution());
					for(int i = 0 ; i < sourceScraperExecutions.length ; i ++) {
						try {
							execute(sourceScraperExecutions[i].match());
						} catch(MissingVariable e) {
							// FAILURE TRUMPS PROGRESS
							status = status == Status.FAILURE ? status : Status.IN_PROGRESS;
						} catch(NoMatches e) {
							status = Status.FAILURE;
						}
					}
				}
			} catch(ResourceNotFoundException e) {
				throw new FatalExecutionException(e); 
			}
			return status;
		}
	}
}