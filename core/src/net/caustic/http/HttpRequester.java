package net.caustic.http;

import java.util.Hashtable;

/**
 * An interface to make HTTP requests, returning {@link HttpResponse}.  Implementations should
 * not follow redirects automatically.
 * @author talos
 *
 */
interface HttpRequester {


	/**
	 * Make an HTTP Head request.  This does not return anything, but it should add any cookies
	 * from response headers to the {@link HttpBrowser}'s cookie store.
	 * @param url the URL to HTTP Head.
	 * @param requestHeaders {@link Hashtable} of headers, mapping {@link String} to {@link String}.
	 * @return A {@link HttpResponse}.
	 * @throws InterruptedException If the user interrupted the request.
	 * @throws HttpRequestException If the request could not be made.
	 */
	public abstract HttpResponse head(String url, Hashtable requestHeaders)
			throws InterruptedException, HttpRequestException;
	
	/**
	 * Make an HTTP Get request.  This returns the body of the response, and adds cookies to the cookie jar.
	 * @param url the URL to HTTP Get.
	 * @param requestHeaders {@link Hashtable} of headers, mapping {@link String} to {@link String}.
	 * @return A {@link HttpResponse}.
	 * @throws InterruptedException If the user interrupted the request.
	 * @throws HttpRequestException If the request could not be made.
	 */
	public abstract HttpResponse get(String url, Hashtable requestHeaders)
			throws InterruptedException, HttpRequestException;
	
	/**
	 * Make an HTTP Post request with a {@link String} to encode into post data.
	 * This returns the body of the response, and adds cookies to the cookie jar.
	 * @param url the URL to HTTP Get.
	 * @param requestHeaders {@link Hashtable} of headers, mapping {@link String} to {@link String}.
	 * @param encodedPostData {@link String} of post data.  Should already be encoded.
	 * @return A {@link HttpResponse}.
	 * @throws InterruptedException If the user interrupted the request.
	 * @throws HttpRequestException If the request could not be made.
	 */
	public abstract HttpResponse post(String url, Hashtable requestHeaders, String encodedPostData)
			throws InterruptedException, HttpRequestException;

	/**
	 * @param timeoutMilliseconds How many milliseconds to wait for a response from the remote server
	 * before giving up.
	 */	
	public abstract void setTimeout(int timeoutMilliseconds);
	
}
