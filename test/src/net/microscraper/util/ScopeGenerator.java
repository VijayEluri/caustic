package net.microscraper.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.microscraper.database.Scope;
import static net.microscraper.util.TestUtils.*;

/**
 * A class to keep track of scope generation during tests.
 * @author realest
 *
 */
public class ScopeGenerator implements Iterator<Scope> {
	private final List<Scope> scopes = new ArrayList<Scope>();
	private final UUIDFactory factory = new JavaUtilUUIDFactory();
	
	/**
	 * Always <code>true</code>.
	 */
	public boolean hasNext() { return true; }
	
	/**
	 * Increment the {@link ScopeGenerator} and return the next scope.
	 */
	public Scope next() {
		scopes.add(new Scope(factory.get(), randomString()));
		return scopes.get(scopes.size() - 1);
	}
	
	/**
	 * Decrement the {@link ScopeGenerator}.
	 */
	public void remove() {
		scopes.remove(scopes.size() - 1);
	}
	
	/**
	 * 
	 * @return How many times the {@link ScopeGenerator} has been incremented.
	 */
	public int count() {
		return scopes.size();
	}

	
	/**
	 * 
	 * @return A {@link List} of all the {@link Scope}s generated by this {@link ScopeGenerator}.
	 */
	public List<Scope> all() {
		return scopes;
	}
	
	/**
	 * 
	 * @return An {@link Object} that can be used as a jmockit argument matcher against the first
	 * {@link Scope} created by this {@link ScopeGenerator}.
	 */
	public Object matchFirst() {
		return new Object() {
			public boolean isValid(Scope scope) {
				return scope.equals(scopes.get(0));
			}
		};
	}
	

	/**
	 * 
	 * @return An {@link Object} that can be used as a jmockit argument matcher against any
	 * {@link Scope} created by this {@link ScopeGenerator}.
	 */
	public Object matchWithin() {
		return new Object() {
			public boolean isValid(Scope scope) {
				return scopes.contains(scope);
			}
		};
	}
}