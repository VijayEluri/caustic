package net.caustic.util;

import net.caustic.database.Database;
import net.caustic.regexp.StringTemplate;
import net.caustic.scope.Scope;
import net.caustic.template.StringSubstitution;

/**
 * A {@link StringTemplate} interface that will always return its initialized
 * {@link String} value within the {@link StringSubstitution} it returns from
 * the {@link #sub(DatabaseView)} method.
 * @author talos
 *
 */
public class StaticStringTemplate extends StringTemplate {
	private final String staticValue;
	
	/**
	 * 
	 * @param staticValue The {@link String} that will always be contained in
	 * the {@link StringSubstitution} returned by {@link #sub(DatabaseView)}.
	 */
	public StaticStringTemplate(String staticValue) {
		this.staticValue = staticValue;
	}
	
	/**
	 * Always returns a successful {@link StringSubstitution} containing 
	 * the {@link String} this {@link StaticStringTemplate} was
	 * initialized with.
	 */
	public StringSubstitution sub(Database db, Scope scope) {
		return StringSubstitution.success(staticValue);
	}
	
	/**
	 * Returns the static value this {@link StaticStringTemplate} was
	 * initialized with.
	 */
	public String asString() {
		return staticValue;
	}
}
