package net.microscraper;

import java.util.Hashtable;

import net.microscraper.executable.FindOneExecutable;
import net.microscraper.executable.SpawnedScraperExecutable;

/**
 * A {@link Hashtable} wrapper with String keys and values.
 * @author john
 * @see Hashtable
 * @see SpawnedScraperExecutable
 * @see FindOneExecutable
 *
 */
public interface Variables {
	/**
	 * 
	 * @param key A String key.
	 * @return A String value.
	 * @throws NullPointerException if the specified key is null
	 * @throws MissingVariableException if this {@link Variables} contains no mapping,
	 * @see Hashtable#get
	 * @see #containsKey(String key)
	 */
	public String get(String key) throws MissingVariableException;
	
	/**
	 * Tests if the specified object is a key in this {@link Variables}. 
	 * @param key The possible key 
	 * @return <code>true</code> if and only if the specified String is a key
	 * in this {@link Variables}.
	 * @throws NullPointerException if the key is <code>null</code>
	 * @see Hashtable#containsKey
	 * @see #get(String key)
	 */
	public boolean containsKey(String key);
}