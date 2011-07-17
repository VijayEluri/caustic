package net.microscraper.client.executable;

import java.io.UnsupportedEncodingException;

import net.microscraper.client.MissingVariableException;
import net.microscraper.client.MustacheTemplateException;
import net.microscraper.client.Variables;
import net.microscraper.client.interfaces.BrowserDelayException;
import net.microscraper.client.interfaces.BrowserException;
import net.microscraper.client.interfaces.Interfaces;
import net.microscraper.client.interfaces.NetInterfaceException;
import net.microscraper.client.interfaces.PatternInterface;
import net.microscraper.client.interfaces.URLInterface;
import net.microscraper.server.Instruction;
import net.microscraper.server.instruction.Page;

/**
 * When {@link #run}, {@link PageExecutable} makes an HTTP request according to
 * the instructions from {@link Page}.
 * @author john
 *
 */
public class PageExecutable extends ScraperExecutable {
	public PageExecutable(Interfaces interfaces,
			Page page, Variables variables, Result source) {
		super(interfaces, page, variables, source);
	}
	
	private PatternInterface[] getStopBecause(Page page) throws MissingVariableException, MustacheTemplateException {
		PatternInterface[] stopPatterns = new PatternInterface[page.getStopBecause().length];
		for(int i  = 0 ; i < stopPatterns.length ; i++) {
			stopPatterns[i] = new RegexpExecutable(getInterfaces(), page.getStopBecause()[i], getVariables()).getPattern();
		}
		return stopPatterns;
	}
	
	private URLInterface getURLFor(Page page) throws NetInterfaceException, MissingVariableException, MustacheTemplateException {
		return getInterfaces().netInterface.getURL(page.getTemplate().compile(getVariables()));
	}
	
	private void head(Page page) throws UnsupportedEncodingException,
				BrowserDelayException, MissingVariableException,
				BrowserException, MustacheTemplateException,
				NetInterfaceException {
		getInterfaces().browser.head(true, getURLFor(page), 
				MustacheUnencodedNameValuePair.compile(page.getHeaders(), getVariables()),
				MustacheEncodedNameValuePair.compile(page.getCookies(), getVariables(), getInterfaces().encoding));
	}
	
	private String get(Page page) throws UnsupportedEncodingException,
				BrowserDelayException, MissingVariableException,
				BrowserException, MustacheTemplateException,
				NetInterfaceException {
		return getInterfaces().browser.get(true, getURLFor(page),
				MustacheUnencodedNameValuePair.compile(page.getHeaders(), getVariables()),
				MustacheEncodedNameValuePair.compile(page.getCookies(), getVariables(), getInterfaces().encoding),
				getStopBecause(page));
	}
	
	private String post(Page page) throws UnsupportedEncodingException,
				BrowserDelayException, MissingVariableException,
				BrowserException, MustacheTemplateException,
				NetInterfaceException {	
		return getInterfaces().browser.post(true, getURLFor(page),
				MustacheUnencodedNameValuePair.compile(page.getHeaders(), getVariables()),
				MustacheEncodedNameValuePair.compile(page.getCookies(), getVariables(), getInterfaces().encoding),
				getStopBecause(page),
				MustacheEncodedNameValuePair.compile(page.getPosts(), getVariables(), getInterfaces().encoding));
	}
	
	/**
	 * @param page The {@link Page} {@link Instruction} whose {@link Page#method} should be executed.
	 * @return The body of the page, if the {@link PageExecutable}'s {@link Page.method} is
	 * {@link Page.Method.GET} or {@link Page.Method.POST}; <code>Null</code> if it is
	 * {@link Page.Method.HEAD}.
	 */
	protected String doMethod(Page page)
			throws MissingVariableException, BrowserDelayException, ExecutionFailure {
		try {
			// Temporary executions to do before.  Not published, executed each time.
			for(int i = 0 ; i < page.getPreload().length ; i ++) {
				doMethod((Page) page.getPreload()[i]);
			}
			if(page.getMethod().equals(Page.Method.GET)) {
				return get(page);
			} else if(page.getMethod().equals(Page.Method.POST)) {
				return post(page);
			} else if(page.getMethod().equals(Page.Method.HEAD)) {
				head(page);
			}
			return null;
		} catch (UnsupportedEncodingException e) {
			throw new ExecutionFailure(e);
		} catch (NetInterfaceException e) {
			throw new ExecutionFailure(e);
		} catch (MustacheTemplateException e) {
			throw new ExecutionFailure(e);
		}
	}
	protected Result[] generateResults() throws BrowserDelayException,
			MissingVariableException, MustacheTemplateException,
			ExecutionFailure {
		Page page = (Page) getResource();
		return new Result[] { generateResult(null, doMethod(page)) };
	}
}
