package net.microscraper.json;

import java.io.IOException;
import java.util.Vector;

import net.microscraper.client.DeserializationException;
import net.microscraper.client.Deserializer;
import net.microscraper.database.Database;
import net.microscraper.database.Scope;
import net.microscraper.http.HttpBrowser;
import net.microscraper.instruction.Action;
import net.microscraper.instruction.Find;
import net.microscraper.instruction.Instruction;
import net.microscraper.instruction.InstructionPromise;
import net.microscraper.instruction.Load;
import net.microscraper.log.Logger;
import net.microscraper.regexp.RegexpCompiler;
import net.microscraper.regexp.RegexpUtils;
import net.microscraper.template.HashtableTemplate;
import net.microscraper.template.Template;
import net.microscraper.template.TemplateCompilationException;
import net.microscraper.uri.MalformedUriException;
import net.microscraper.uri.RemoteToLocalSchemeResolutionException;
import net.microscraper.uri.URILoader;
import net.microscraper.uri.UriResolver;
import net.microscraper.util.Encoder;
import net.microscraper.util.Execution;
import net.microscraper.util.StringUtils;

public class JsonDeserializer implements Deserializer {
	
	/**
	 * The {@link UriResolver} to use when resolving URIs.
	 */
	private final UriResolver uriResolver;
	
	/**
	 * The {@link URILoader} to use when loading the contents of URIs.
	 */
	private final URILoader uriLoader;
	
	/**
	 * The {@link JsonParser} used to parse JSON objects.
	 */
	private final JsonParser parser;
	
	/**
	 * The {@link RegexpCompiler} to use when deserializing {@link Find}s.
	 */
	private final RegexpCompiler compiler;
	
	/**
	 * The {@link HttpBrowser} to use when deserializing {@link Load}s.
	 */
	private final HttpBrowser browser;
	
	/**
	 * The {@link Encoder} to use when deserializing {@link Load}s.
	 */
	private final Encoder encoder;
	
	private Execution deserialize(String jsonString, Database database, Scope scope, String baseUri,
			String openTagString, String closeTagString)
			throws DeserializationException, JsonException, TemplateCompilationException,
			IOException, MalformedUriException, InterruptedException, RemoteToLocalSchemeResolutionException {
		final Execution result;
		
		// Parse non-objects as URIs after substitution with variables
		if(!parser.isJsonObject(jsonString)) {
			if(jsonString.equalsIgnoreCase(SELF)) {
				String loadedJSONString = uriLoader.load(baseUri);
				result = deserialize(loadedJSONString, database, scope, baseUri, openTagString, closeTagString);
			} else {
				Execution uriSub = new Template(jsonString, openTagString, closeTagString, database)
						.subEncoded(scope, encoder);
				if(uriSub.isSuccessful()) {
					String uriPath = (String) uriSub.getExecuted();
					String uriToLoad = uriResolver.resolve(baseUri, uriPath);
					String loadedJSONString = uriLoader.load(uriToLoad);
					
					result = deserialize(loadedJSONString, database, scope, uriToLoad, openTagString, closeTagString);
				} else {
					result = uriSub;
				}
			}
		} else {
			
			JsonObject initialObj = parser.parse(jsonString);
			
			Template name = null;
			//Boolean shouldPersistValue = null;
			
			// InstructionPromise children.
			Vector children = new Vector();
			
			// populated for Load action
			Template url = null;
			String method = null;
			Template postData = null;
			HashtableTemplate posts = new HashtableTemplate();
			HashtableTemplate cookies = new HashtableTemplate();
			HashtableTemplate headers = new HashtableTemplate();
			
			// Populated for Find action
			Template pattern = null;
			Boolean isCaseInsensitive = null;
			Boolean isMultiline = null;
			Boolean doesDotMatchNewline = null;
			Template replace = null;
			Integer min = null;
			Integer max = null;
			Integer match = null;
			
			// This vector expands if EXTENDS objects are specified.
			Vector jsonObjects = new Vector();
			jsonObjects.add(initialObj);
			
			for(int i = 0 ; i < jsonObjects.size() ; i ++) {
				JsonObject obj = (JsonObject) jsonObjects.get(i);
				JsonIterator iterator = obj.keys();
				
				// Case-insensitive loop over key names.
				while(iterator.hasNext()) {
					String key = iterator.next();
					
					/** Attributes for Instruction. **/
					if(key.equalsIgnoreCase(EXTENDS)) {
						Vector extendsStrings = new Vector();
						Vector extendsObjects = new Vector();
						
						if(obj.isJsonObject(key)) {
							extendsObjects.add(obj.getJsonObject(key));
						} else if(obj.isJsonArray(key)) {
							JsonArray array = obj.getJsonArray(key);
							for(int j = 0 ; j < array.length(); j ++) {
								if(array.isJsonObject(j)) {
									extendsObjects.add(array.getJsonObject(j));
								} else if(array.isString(j)) {
									extendsStrings.add(array.getString(j));
								} else {
									throw new DeserializationException(EXTENDS + " array elements must be strings or objects.");
								}
							}
						} else if(obj.isString(key)) {
							extendsStrings.add(obj.getString(key));
						}
						
						for(int j = 0 ; j < extendsObjects.size() ; j ++) {
							jsonObjects.add(extendsObjects.elementAt(j));
						}
						for(int j = 0 ; j < extendsStrings.size() ; j ++) {
							Template extendsUriTemplate = new Template(obj.getString(key), openTagString, closeTagString, database);
							Execution uriSubstitution = extendsUriTemplate.subEncoded(scope, encoder);
							if(uriSubstitution.isSuccessful()) {
								String uriPath = (String) uriSubstitution.getExecuted();
								String uriToLoad = uriResolver.resolve(baseUri, uriPath);
								String loadedJSONString = uriLoader.load(uriToLoad);
								
								jsonObjects.add(parser.parse(loadedJSONString));
							} else {
								return uriSubstitution; // can't substitute uri to load EXTENDS reference, missing-variable out.
							}
						}
					} else if(key.equalsIgnoreCase(THEN)) {
						Vector thenStrings = new Vector();
						Vector thenObjects = new Vector(); // Strings are added to this too -- their deserialization is handled later.
						
						if(obj.isJsonObject(key)) {
							thenObjects.add(obj.getString(key));
						} else if (obj.isJsonArray(key)) {
							JsonArray array = obj.getJsonArray(key);
							for(int j = 0 ; j < array.length() ; j ++) {
								if(array.isJsonObject(j)) {
									thenObjects.add(array.getString(j));
								} else if(array.isString(j)) {
									thenStrings.add(array.getString(j));
								} else {
									throw new DeserializationException(THEN + " array elements must be strings or objects.");
								}
							}
						} else if(obj.isString(key)) {
							thenStrings.add(obj.getString(key));
						} else {
							throw new DeserializationException(StringUtils.quote(key) +
									" must be a String reference to another " +
									" instruction, an object with another instruction, or an array with any number " +
									" of both.");
						}
						
						for(int j = 0 ; j < thenStrings.size(); j ++) {
							String thenString = (String) thenStrings.elementAt(j);
							if(thenString.equalsIgnoreCase(SELF)) {
								//children.add(new InstructionPromise(this, jsonString, baseUri.toString()));
								children.add(new InstructionPromise(this, database, SELF, baseUri));
							} else {
								children.add(new InstructionPromise(this, database, thenString, baseUri));
							}
						}
						for(int j = 0 ; j < thenObjects.size(); j ++ ) {
							String thenObjectAsString = (String) thenObjects.elementAt(j);
							children.add(new InstructionPromise(this, database, thenObjectAsString, baseUri));
						}
					} else if(key.equalsIgnoreCase(NAME)) {
						name = new Template(obj.getString(key), openTagString, closeTagString, database);
					/*} else if(key.equalsIgnoreCase(SAVE)) {
						shouldPersistValue = Boolean.valueOf(obj.getBoolean(key));
						*/
					/** Load-only attributes. **/
					} else if(key.equalsIgnoreCase(LOAD)) {
						url = new Template(obj.getString(key), openTagString, closeTagString, database);
					} else if(key.equalsIgnoreCase(METHOD)) {
						method = obj.getString(key);
					} else if(key.equalsIgnoreCase(POSTS)) {
						if(obj.isJsonObject(key)) {
							posts.merge(deserializeHashtableTemplate(obj.getJsonObject(key), database, openTagString, closeTagString));
						} else if(obj.isString(key)) {
							postData = new Template(obj.getString(key), openTagString, closeTagString, database);
						} else {
							throw new DeserializationException(StringUtils.quote(key) +
									" must be a String with post data or an object with name-value-pairs.");				
						}
					} else if(key.equalsIgnoreCase(COOKIES)) {
						cookies.merge(deserializeHashtableTemplate(obj.getJsonObject(key), database, openTagString, closeTagString));
					} else if(key.equalsIgnoreCase(HEADERS)) {
						headers.merge(deserializeHashtableTemplate(obj.getJsonObject(key), database, openTagString, closeTagString));
						
					/** Pattern attributes. **/
					} else if(key.equalsIgnoreCase(FIND)) {
						pattern = new Template(obj.getString(key), openTagString, closeTagString, database);
					} else if(key.equalsIgnoreCase(IS_CASE_INSENSITIVE)) {
						isCaseInsensitive = Boolean.valueOf(obj.getBoolean(key));
					} else if(key.equalsIgnoreCase(DOES_DOT_MATCH_ALL)) {
						doesDotMatchNewline = Boolean.valueOf(obj.getBoolean(key));
					} else if(key.equalsIgnoreCase(IS_MULTILINE)) {
						isMultiline = Boolean.valueOf(obj.getBoolean(key));
						
						
					/** Find-only attributes. **/
					} else if(key.equalsIgnoreCase(REPLACE)) {
						replace = new Template(obj.getString(key), openTagString, closeTagString, database);
					} else if(key.equalsIgnoreCase(MIN_MATCH)) {
						min = Integer.valueOf(obj.getInt(key));
					} else if(key.equalsIgnoreCase(MAX_MATCH)) {
						max = Integer.valueOf(obj.getInt(key));
					} else if(key.equalsIgnoreCase(MATCH)) {
						match = Integer.valueOf(obj.getInt(key));
						
					/** OK for all. **/
					} else if(key.equalsIgnoreCase(DESCRIPTION)) {
						
					} else {
						throw new DeserializationException(StringUtils.quote(key) + " is not a valid key.");
					}
				}
			}
			
			Action action = null;
			if(url != null && pattern != null) {
				// Can't define two actions.
				throw new DeserializationException("Cannot define both " + FIND + " and " + LOAD);
			} else if(url != null) {
				// We have a Load
				Load load = new Load(browser, encoder, url);
				action = load;
				
				if(method != null) {
					load.setMethod(method);
				}
				if(postData != null) {
					load.setPostData(postData);
				} else {
					load.addPosts(posts);
				}
				load.addCookies(cookies);
				load.addHeaders(headers);
			} else if(pattern != null) {
				// We have a Find
				Find find = new Find(compiler, pattern);
				action = find;
				
				if(replace != null) {
					find.setReplacement(replace);
				}
				if(match != null) {
					if(min != null || max != null) {
						throw new DeserializationException("Cannot define " + MIN_MATCH + " or " + MAX_MATCH + 
								" in addition to " + MATCH + " in " + FIND);
					}
					find.setMaxMatch(match.intValue());
					find.setMinMatch(match.intValue());
				}
				if(min != null) {
					find.setMinMatch(min.intValue());
				}
				if(max != null) {
					find.setMaxMatch(max.intValue());
				}
				if(min != null && max != null) {
					if(RegexpUtils.isValidRange(min.intValue(), max.intValue()) == false) {
						throw new DeserializationException("Range " + StringUtils.quote(min) + " to " +
								StringUtils.quote(max) + " is not valid for " + FIND);
					}
				}
				if(isCaseInsensitive != null) {
					find.setIsCaseInsensitive(isCaseInsensitive.booleanValue());
				}
				if(doesDotMatchNewline != null) {
					find.setDoesDotMatchAll(doesDotMatchNewline.booleanValue());
				}
				if(isMultiline != null) {
					find.setIsMultiline(isMultiline.booleanValue());
				}
			}
			
			if(action != null) {
				
				Instruction instruction = new Instruction(action, database);
				/*if(shouldPersistValue != null) {
					instruction.setShouldPersistValue(shouldPersistValue.booleanValue());
				}*/
				if(name != null) {
					instruction.setName(name);
				}
				for(int i = 0 ; i < children.size(); i ++ ) {
					instruction.addChild((InstructionPromise) children.elementAt(i));
				}
				
				result = Execution.success(instruction);
			} else {
				throw new DeserializationException("Must define " + FIND + " or " + LOAD);
			}
		}
		return result;
	}
	
	/**
	 * Deserialize a {@link HashtableTemplate} from a {@link JsonObject} hash.
	 * @param jsonObject Input {@link JsonObject} hash.
	 * @param database
	 * @param openTagString
	 * @param closeTagString
	 * @return A {@link HashtableTemplate}.
	 * @throws JsonException If there was a problem parsing the JSON.
	 * @throws TemplateCompilationException If a {@link Template} could not be compiled.
	 */
	private HashtableTemplate deserializeHashtableTemplate(JsonObject jsonObject,
			Database database, String openTagString, String closeTagString)
				throws JsonException, TemplateCompilationException {
		HashtableTemplate result = new HashtableTemplate();
		JsonIterator iter = jsonObject.keys();
		while(iter.hasNext()) {
			String key = (String) iter.next();
			String value = jsonObject.getString(key);
			result.put(new Template(key, openTagString, closeTagString, database),
					new Template(value, openTagString, closeTagString, database));
		}
		return result;
	}
	
	/**
	 * Key for {@link Find#replacement} value deserializing from JSON.
	 */
	public static final String REPLACE = "replace";
	
	/**
	 * Key for {@link Find#tests} value deserializing from JSON.
	 */
	//public static final String TESTS = "tests";

	/**
	 * Conveniently deserialize {@link Find#minMatch} and {@link Find#maxMatch}.
	 * If this exists in an object, both {@link #maxMatch} and {@link #minMatch} are its
	 * value.<p>
	 */
	public static final String MATCH = "match";

	/**
	 * Key for {@link Find#minMatch} value when deserializing from JSON.
	 */
	public static final String MIN_MATCH = "min";

	/**
	 * Key for {@link Find#maxMatch} value when deserializing from JSON.
	 */
	public static final String MAX_MATCH = "max";
	
	/**
	 * Key for {@link Load#headers} when deserializing.
	 */
	public static final String HEADERS = "headers";
	
	/**
	 * Key for {@link Load#stops} when deserializing.
	 */
	//public static final String STOP = "stop";
		
	/**
	 * Key for {@link Load#posts} when deserializing. 
	 */
	public static final String POSTS = "posts";

	/**
	 * Key for {@link Load#url} when deserializing.
	 */
	public static final String LOAD = "load";
	
	/**
	 * Key for {@link Load#getMethod()} when deserializing. Default is {@link #DEFAULT_METHOD},
	 */
	public static final String METHOD = "method";
	
	/**
	 * Key for {@link Load#cookies} when deserializing. Default is {@link #DEFAULT_COOKIES}.
	 */
	public static final String COOKIES = "cookies";
	
	/**
	 * Key for {@link Instruction#children} when deserializing from JSON.
	 */
	public static final String THEN = "then";
	
	/**
	 * Key for {@link Instruction#name} value when deserializing {@link Instruction} from JSON.
	 */
	public static final String NAME = "name";
	
	/**
	 * Key for {@link Instruction#shouldSaveValue} value when deserializing from JSON.
	 */
	//public static final String SAVE = "save";
	
	/**
	 * Key for deserializing {@link PatternTemplate#pattern}.
	 */
	public static final String FIND = "find";
	
	/**
	 * Key for an object that will extend the current object.
	 */
	public static final String EXTENDS = "extends";
	
	/**
	 * Key for a self-reference.
	 */
	public static final String SELF = "$this";
	
	/**
	 * Key for description.
	 */
	public static final String DESCRIPTION = "description";
	
	/**
	 * Key for deserializing {@link PatternTemplate#isCaseInsensitive}.
	 */
	public static final String IS_CASE_INSENSITIVE = "case_insensitive";
	public static final boolean IS_CASE_INSENSITIVE_DEFAULT = false;
	
	/**
	 * Key for deserializing {@link PatternTemplate#isMultiline}.
	 */
	public static final String IS_MULTILINE = "multiline";
	public static final boolean IS_MULTILINE_DEFAULT = false;
	
	/** 
	 * Key for deserializing {@link PatternTemplate#doesDotMatchNewline}.
	 */
	public static final String DOES_DOT_MATCH_ALL = "dot_matches_all";
	public static final boolean DOES_DOT_MATCH_ALL_DEFAULT = true;

	/**
	 * 
	 * @param parser
	 * @param compiler
	 * @param browser
	 * @param encoder
	 * @param uriFactory
	 * @param database
	 */
	public JsonDeserializer(JsonParser parser, RegexpCompiler compiler, HttpBrowser browser,
			Encoder encoder, UriResolver uriResolver, URILoader uriLoader) {
		this.compiler = compiler;
		this.parser = parser;
		this.browser = browser;
		this.encoder = encoder;
		this.uriResolver = uriResolver;
		this.uriLoader = uriLoader;
	}
	
	public Execution deserializeString(String serializedString, Database database, Scope scope, String uri) {
		
		try {
			return deserialize(serializedString, database, scope, uri,
					Template.DEFAULT_OPEN_TAG, Template.DEFAULT_CLOSE_TAG);
		} catch(JsonException e) {
			return Execution.deserializationException(new DeserializationException(e));
		} catch (MalformedUriException e) {
			return Execution.deserializationException(new DeserializationException(e));
		} catch(IOException e) {
			return Execution.deserializationException(new DeserializationException(e));
		} catch(TemplateCompilationException e) {
			return Execution.deserializationException(new DeserializationException(e));
		} catch(InterruptedException e) {
			return Execution.deserializationException(new DeserializationException(e));
		} catch (RemoteToLocalSchemeResolutionException e) {
			return Execution.deserializationException(new DeserializationException(e));
		} catch (DeserializationException e) {
			return Execution.deserializationException(e);
		}
	}

	public void register(Logger logger) {
		browser.register(logger);
	}
}
