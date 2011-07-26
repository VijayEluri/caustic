package net.microscraper.interfaces.json;

/**
 * A {@link JSONLocation} provides the location of JSON.  This is equivalent
 * to a URI with the exception of the fragment, which may identify a section
 * of JSON within a larger object.
 * @author realest
 *
 */
public interface JSONLocation {
	/**
	 * The separator used in {@link JSONLocation}'s fragment paths.
	 * Is <code>/</code>
	 */
	public static final String JSON_PATH_SEP = "/";
	
	/**
	 * Constructs a new {@link JSONLocation}
	 * by resolving the given {@link JSONLocation} against
	 * this {@link JSONLocation}
	 * @param jsonLocation The {@link JSONLocation} to resolve.
	 * @return The resulting {@link JSONLocation}.
	 * @throws JSONLocationException if there is an error resolving.
	 */
	public JSONLocation resolve(JSONLocation jsonLocation) throws JSONLocationException;
	
	/**
	 * Constructs a new {@link JSONLocation}
	 * by parsing the given string as a URI and then resolving it against this
	 * {@link JSONLocation}. 
	 * @param uri A {@link String} to parse as a URI.
	 * @return The resulting {@link JSONLocation}.
	 * @throws JSONLocationException if there is an error resolving.
	 */
	public JSONLocation resolve(String uri) throws JSONLocationException;
	
	/**
	 * Constructs a new {@link JSONLocation} 
	 * by resolving the given string as a path
	 * against this {@link JSONLocation}'s fragment.
	 * @param path A {@link String} referring to part of the original {@link JSONLocation}'s
	 * object.
	 * @return The resulting {@link JSONLocation}.
	 * @throws JSONLocationException if there is an error resolving.
	 */
	public JSONLocation resolveFragment(String path) throws JSONLocationException;

	/**
	 * Constructs a new {@link JSONLocation}
	 * by parsing the given integer as an index
	 * against this {@link JSONLocation}'s fragment.
	 * @param path An int referring to an index inside the original {@link JSONLocation}'s
	 * array.
	 * @return The resulting {@link JSONLocation}.
	 * @throws JSONLocationException if there is an error resolving.
	 */
	public JSONLocation resolveFragment(int index) throws JSONLocationException;
	
	/**
	 * 
	 * @return <code>True</code> if the {@link JSONLocation} is in the local
	 * filesystem, <code>false</code> otherwise.
	 */
	public boolean isFile();
	
	/**
	 * 
	 * @return <code>True</code> if the {@link JSONLocation} is on a server
	 * accessible by HTTP request, <code>false</code> otherwise.
	 */
	public boolean isHttp();
	
	/**
	 * 
	 * @return The scheme segment of the {@link JSONLocation}'s URI.
	 * @see java.net.URI#getScheme()
	 */
	public String getScheme();
	
	/**
	 * 
	 * @return The scheme specific part of the {@link JSONLocation}'s URI.
	 * @see java.net.URI#getSchemeSpecificPart()
	 */
	public String getSchemeSpecificPart();

	/**
	 * 
	 * @return The fragment part of the {@link JSONLocation}'s URI.
	 * @see java.net.URI#getFragment()
	 */
	public String getFragment();
	
	/**
	 * Tells whether or not this {@link JSONLocation} is absolute.
	 * A {@link JSONLocation} is absolute if, and only if, it has a
	 * scheme component. 
	 * @return <code>true</code> if, and only if, this {@link JSONLocation} is absolute
	 * @see java.net.URI#isAbsolute()
	 */
	public boolean isAbsolute();

	/**
	 * 
	 * @return The fragment part of the {@link JSONLocation}'s URI, exploded
	 * into an array based off of {@link #JSON_PATH_SEP}.
	 */
	public String[] explodeJSONPath();
	
	/**
	 * 
	 * @return Returns the content of this {@link JSONLocation} as a string. 
	 * @see java.net.URI#toString()
	 */
	public String toString();
	
	/**
	 * Tests this {@link JSONLocation} for equality with another object. 
	 * @param obj The object to compare.
	 * @return <code>True</code> if both objects are {@link JSONLocation}s 
	 * referring to the same location, <code>false</code> otherwise.
	 * @see java.net.URI#equals(Object)
	 */
	public boolean equals(Object obj);
}
