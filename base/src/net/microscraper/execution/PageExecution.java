package net.microscraper.execution;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import net.microscraper.client.BrowserException;
import net.microscraper.client.BrowserDelayException;
import net.microscraper.client.Interfaces.Regexp.InvalidRangeException;
import net.microscraper.client.Interfaces.Regexp.MissingGroupException;
import net.microscraper.client.Interfaces.Regexp.NoMatchesException;
import net.microscraper.client.MissingVariableException;
import net.microscraper.client.MustacheTemplateException;
import net.microscraper.model.DeserializationException;
import net.microscraper.model.Link;
import net.microscraper.model.Page;
import net.microscraper.model.Resource;

public class PageExecution extends BasicExecution {
	private final Link pageLink;
	private final ScraperExecution enclosingScraper;
	private final Context context;
	
	private String body = null;
	
	public PageExecution(Context context, ScraperExecution enclosingScraper, Link pageLink) {
		super(context, pageLink.location, enclosingScraper);
		this.enclosingScraper = enclosingScraper;
		this.context = context;
		this.pageLink = pageLink;
	}
	
	private void head(Page page) throws UnsupportedEncodingException, BrowserDelayException, MissingVariableException, BrowserException, MalformedURLException, MustacheTemplateException {
		context.head(enclosingScraper.compile(page.url),
				enclosingScraper.compileUnencoded(page.headers),
				enclosingScraper.compileEncoded(page.cookies));
	}
	
	private String get(Page page) throws UnsupportedEncodingException, BrowserDelayException, MissingVariableException, BrowserException, MalformedURLException, MustacheTemplateException, InvalidBodyMethodException {
		return context.get(enclosingScraper.compile(page.url),
				enclosingScraper.compileUnencoded(page.headers),
				enclosingScraper.compileEncoded(page.cookies),
				enclosingScraper.compile(page.terminates));
	}
	
	private String post(Page page) throws UnsupportedEncodingException, BrowserDelayException, MissingVariableException, BrowserException, MalformedURLException, MustacheTemplateException, InvalidBodyMethodException {	
		return context.post(enclosingScraper.compile(page.url),
				enclosingScraper.compileUnencoded(page.headers),
				enclosingScraper.compileEncoded(page.cookies),
				enclosingScraper.compile(page.terminates),
				enclosingScraper.compileEncoded(page.posts));
	}
	
	public String getBody() {
		return body;
	}

	public Execution[] children() {
		return new Execution[0];
	}
/*
	public Execution[] getChildren() {
		return new Execution[0];
	}
*/
	public boolean hasPublishName() {
		return false;
	}

	public String getPublishName() {
		return null;
	}

	public boolean hasPublishValue() {
		return false;
	}

	public String getPublishValue() {
		return null;
	}

	protected Resource generateResource() throws IOException, DeserializationException {
		return context.loadPage(pageLink);
	}
	
	/**
	 * @return The body of the page, if the {@link PageExecution}'s {@link Page.method} is
	 * {@link Page.Method.GET} or {@link Page.Method.POST}; <code>Null</code> if it is
	 * {@link Page.Method.HEAD}.
	 */
	protected Object generateResult(Resource resource)
			throws MissingVariableException, BrowserDelayException, ExecutionFailure {
		try {
			Page page = (Page) resource;
			// Temporary executions to do before.  Not published, executed each time.
			for(int i = 0 ; i < page.loadBeforeLinks.length ; i ++) {
				PageExecution pageBeforeExecution = new PageExecution(context, enclosingScraper, page.loadBeforeLinks[i]);
				Page pageBefore = (Page) pageBeforeExecution.generateResource();
				pageBeforeExecution.generateResult(pageBefore);
			}
			if(page.method.equals(Page.Method.GET)) {
				return get(page);
			} else if(page.method.equals(Page.Method.POST)) {
				return post(page);
			} else if(page.method.equals(Page.Method.HEAD)) {
				head(page);
				return null;
			} else {
				throw new InvalidBodyMethodException(page);
			}
		} catch(DeserializationException e) {
			throw new ExecutionFailure(e);
		} catch (UnsupportedEncodingException e) {
			throw new ExecutionFailure(e);
		} catch (MalformedURLException e) {
			throw new ExecutionFailure(e);
		} catch (BrowserException e) {
			throw new ExecutionFailure(e);
		} catch (MustacheTemplateException e) {
			throw new ExecutionFailure(e);
		} catch (InvalidBodyMethodException e) {
			throw new ExecutionFailure(e);
		} catch (IOException e) {
			throw new ExecutionFailure(e);
		}
	}

	/**
	 * An empty array, {@link PageExecution} does not have children.
	 */
	protected Execution[] generateChildren(Resource resource, Object result) {
		return new Execution[0];
	}
}
