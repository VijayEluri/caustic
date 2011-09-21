package net.microscraper.instruction;

import net.microscraper.database.DatabasePersistException;
import net.microscraper.database.DatabaseReadException;
import net.microscraper.database.DatabaseView;
import net.microscraper.regexp.Pattern;
import net.microscraper.regexp.RegexpCompiler;
import net.microscraper.regexp.StringTemplate;
import net.microscraper.template.DependsOnTemplate;
import net.microscraper.template.StringSubstitution;
import net.microscraper.util.StaticStringTemplate;

public class Find {
	
	/**
	 * The {@link StringTemplate} that will be substituted into a {@link String}
	 * to use as the pattern.
	 */
	private final StringTemplate pattern;
	
	private boolean hasName = false;
	
	private StringTemplate name;
	
	/**
	 * Flag equivalent to {@link java.util.regex.Pattern#CASE_INSENSITIVE}
	 */
	private boolean isCaseInsensitive = false;
	
	/**
	 * Flag equivalent to {@link java.util.regex.Pattern#MULTILINE}
	 */
	private boolean isMultiline = false;
	
	/**
	 * Flag equivalent to {@link java.util.regex.Pattern#DOTALL}
	 */
	private boolean doesDotMatchNewline = true;
	
	/**
	 * The {@link RegexpCompiler} to use when compiling this {@link Find}.
	 */
	private final RegexpCompiler compiler;
	
	
	/**
	 * Value for when {@link #replacement} is the entire match.
	 */
	public static final String ENTIRE_MATCH = "$0";

	/**
	 * The {@link StringTemplate} that should be substituted evaluated for backreferences,
	 * then returned once for each match, if it is assigned by {@link #setReplacement(StringTemplate)}.
	 */
	private StringTemplate replacement = new StaticStringTemplate(ENTIRE_MATCH);
	
	/**
	 * The first of the parser's matches to export.
	 * This is 0-indexed, so <code>0</code> is the first match.
	 * <p>
	 * @see #maxMatch
	 */
	private int minMatch = Pattern.FIRST_MATCH;

	/**
	 * The last of the parser's matches to export.
	 * Negative numbers count backwards, so <code>-1</code> is the last match.
	 * <p>
	 * @see #minMatch
	 */
	private int maxMatch = Pattern.LAST_MATCH;

	//private Instruction[] children = new Instruction[] { };
	
	public Find(RegexpCompiler compiler, StringTemplate pattern) {
		this.compiler = compiler;
		this.pattern = pattern;
	}
	/*
	public void setChildren(Instruction[] children) {
		this.children = children;
	}*/
	
	public void setName(StringTemplate name) {
		this.hasName = true;
		this.name = name;
	}
	
	public void setReplacement(StringTemplate replacement) {
		this.replacement = replacement;
	}
	
	public void setMinMatch(int min) {
		this.minMatch = min;
	}
	
	public void setMaxMatch(int max) {
		this.maxMatch = max;
	}
	
	public void setIsCaseInsensitive(boolean isCaseInsensitive) {
		this.isCaseInsensitive = isCaseInsensitive;
	}
	
	public void setDoesDotMatchAll(boolean doesDotMatchAll) {
		this.doesDotMatchNewline = doesDotMatchAll;
	}
	
	public void setIsMultiline(boolean isMultiline) {
		this.isMultiline = isMultiline;
	}
	
	/**
	 * Use {@link #pattern}, substituted with {@link Variables}, to match against <code>source</code>.
	 */
	public FindResult execute(String source, DatabaseView inputView)
			throws DatabasePersistException, DatabaseReadException {
		if(source == null) {
			throw new IllegalArgumentException("Cannot execute Find without a source.");
		}
		
		final FindResult result;
		final StringSubstitution subName;
		final StringSubstitution subPattern = pattern.sub(inputView);
		final StringSubstitution subReplacement = replacement.sub(inputView);
		
		if(hasName) {
			subName = name.sub(inputView);
		} else {
			subName = subPattern; // if no name defined, default to the pattern.
		}
				
		if(subName.isMissingTags() ||
				subPattern.isMissingTags() ||
				subReplacement.isMissingTags()) { // One of the substitutions was not OK.
			result = FindResult.missingTags(StringSubstitution.combine(
					new DependsOnTemplate[] { subName, subPattern, subReplacement } ) );
			
		} else { // All the substitutions were OK.
			String resultName = subName.getSubstituted();
			
			String patternString = (String) subPattern.getSubstituted();
			Pattern pattern = compiler.compile(patternString, isCaseInsensitive, isMultiline, doesDotMatchNewline);
			
			String replacement = (String) subReplacement.getSubstituted();
			String[] matches = pattern.match(source, replacement, minMatch, maxMatch);
			
			
			if(matches.length == 0) { // No matches, fail out.
				result = FindResult.noMatchesFailure(pattern, minMatch, maxMatch, source);
			// We got at least 1 match.
			} else {
				result = FindResult.success(resultName, matches, hasName);
			}
		}
		return result;
	}
	
	/**
	 * @return The raw pattern template.
	 */
	public String toString() {
		return pattern.toString();
	}
}
