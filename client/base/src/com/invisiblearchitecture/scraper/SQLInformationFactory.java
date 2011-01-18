package com.invisiblearchitecture.scraper;

import java.util.Hashtable;
import java.util.Vector;

import com.invisiblearchitecture.scraper.SQLInterface.CursorInterface;
import com.invisiblearchitecture.scraper.SQLInterface.SQLInterfaceException;

public class SQLInformationFactory implements InformationFactory {
	private final HttpInterface httpInterface;
	private final RegexInterface regexInterface;
	private final LogInterface logger;
	private final Collector collector;
	private final Publisher publisher;
	
	private final SQLInterface sqlInterface;
	
	/* Keep a single cookie store for the whole information factory. */
	private final CookieStoreInterface cookieStore;
	private int count = 0;
	
	public SQLInformationFactory(SQLInterface sqlInt,
			Collector c, Publisher p, HttpInterface httpInt,
			RegexInterface regexInt, LogInterface l) throws SQLInterfaceException {
		
		sqlInterface = sqlInt;
		
		regexInterface = regexInt;
		httpInterface = httpInt;
		cookieStore = httpInt.newCookieStore();
		logger = l;
		collector = c;
		publisher = p;
	}

	@Override
	public Information get(String namespace, String type) {
		try {
			String[] types = parentInformationTypes(type);
			String[] namespaces = parentNamespaces(namespace);
			Information information = new Information(collector, namespace, type, count++,
					fieldsToPublish(types), interpreters(namespaces, types),
					gatherers(namespaces, types), publisher,
					cookieStore,
					/*httpInterface.newCookieStore(),*/ logger);
			information.putFields(defaultFields(namespaces, types));
			
			return information;
		} catch(SQLInterfaceException e) {
			logger.e("Error creating information.", e);
			throw new IllegalArgumentException("Unable to get information for namespace " + namespace + ", type " + type);
		}
	}
	
	/**
	 * Recursively obtain all the types for this information.
	 * @param type
	 * @return
	 */
	private String[] parentInformationTypes (String type) throws SQLInterfaceException {
		return simpleRecursiveQuery("SELECT Parent FROM InformationParents WHERE Child = ", type, ";", 
			"Parent");
	}
	
	/**
	 * Recursively obtain all namespaces.
	 * @param namespace
	 * @return
	 */
	private String[] parentNamespaces (String namespace) throws SQLInterfaceException {
		return simpleRecursiveQuery("SELECT Parent FROM NamespaceParents WHERE Child = ", namespace, ";",
				"Parent");
	}
	
	/**
	 * Execute a statement with a single parameter recursively.
	 * @param statement
	 * @param initialValue Seed "Child" value, used as the first parameter in the statement. Included in the results.
	 * @param parentColumnName Name of the column to get data from.
	 * @return An array of strings generated by recursive query.
	 * @throws SQLException 
	 */
	private String[] simpleRecursiveQuery(String sqlBefore, String initialValue, String sqlAfter, String parentColumnName) throws SQLInterfaceException {
		/*ParameterMetaData metadata = statement.getParameterMetaData();
		if (metadata.getParameterCount() != 1) {
			throw new IllegalArgumentException("Wrong number of parameters for a simple recursive query.");
		}*/
		String parent = initialValue;
		Vector vector = new Vector();
		do {
			vector.addElement(parent);
			//statement.bindString(1, parent);
			CursorInterface rs = sqlInterface.query(sqlBefore + " '" + parent + "' " + sqlAfter);
			try {
				parent = rs.getString(parentColumnName);
			} catch (Exception e) { // no more results.
				parent = null;
			}
			rs.close();
		} while(parent != null);
		String[] types = new String[vector.size()];
		vector.copyInto(types);
		return types;
	}
	
	private String[] fieldsToPublish (String[] types) throws SQLInterfaceException {
		CursorInterface rs = sqlInterface.query("SELECT Name FROM PublishFields WHERE Type IN ("
				+ flattenArray(types) + ");");
		Vector vector = new Vector();
		while(rs.next()) {
			vector.addElement(rs.getString("Name"));
		}
		rs.close();
		String[] fieldsToPublishAry = new String[vector.size()];
		vector.copyInto(fieldsToPublishAry);
		logger.i("Fields to publish: " + vector.toString());
		return fieldsToPublishAry;
	}
	
	private Interpreter[] interpreters (String[] namespaces, String[] types) {
		Vector vector = new Vector();
		try {
			CursorInterface rs = sqlInterface.query("SELECT SourceField, Regex, Number, DestinationField " +
					"FROM FieldsToFields WHERE Namespace IN (" + flattenArray(namespaces) +
					") AND Type IN (" + flattenArray(types) + ");");
			while(rs.next()) {
				String sourceField = rs.getString("SourceField");
				String regex = rs.getString("Regex");
				int number = rs.getInt("Number");
				String destinationField = rs.getString("DestinationField");
				PatternInterface pattern = null;
				if(regex != null) {
					pattern = regexInterface.compile(regex);
				}
				vector.addElement(new Interpreter.ToField(sourceField, pattern, number, destinationField, logger));
			}
			rs.close();
		} catch(SQLInterfaceException e) {
			logger.e("SQL error creating interpreterToField", e);
		}
		
		try {
			CursorInterface rs = sqlInterface.query("SELECT SourceField, Regex, DestinationNamespace, DestinationType, DestinationField" +
					" FROM FieldsToInformations " +
					" WHERE Namespace IN (" + flattenArray(namespaces) +
					") AND Type IN (" + flattenArray(types) + ");");
			while(rs.next()) {
				String sourceField = rs.getString("SourceField");
				String regex = rs.getString("Regex");
				String destinationNamespace = rs.getString("DestinationNamespace");
				String destinationType = rs.getString("DestinationType");
				String destinationField = rs.getString("DestinationField");
				PatternInterface pattern = null;
				if(regex != null) {
					pattern = regexInterface.compile(regex);
				}
				vector.addElement(new Interpreter.ToInformation(this, sourceField, pattern,
						destinationNamespace, destinationType, destinationField, logger));
			}
			rs.close();
		} catch(SQLInterfaceException e) {
			logger.e("SQL error creating interpreterToInformation", e);
		}
		Interpreter[] interpreters = new Interpreter[vector.size()];
		vector.copyInto(interpreters);
		return interpreters;
	}
	
	private Gatherer[] gatherers (String[] namespaces, String[] types) {
		Vector vector = new Vector();
		try {
			CursorInterface rs = sqlInterface.query("SELECT GathererId" +
					" FROM Gatherers" +
					" WHERE Namespace IN (" + flattenArray(namespaces) + ")" +
					" AND Type IN (" + flattenArray(types) + ");");
			
			while(rs.next()) {
				String id = rs.getString("GathererId");
				vector.addElement(gatherer(id));
			}
			rs.close();
		} catch(SQLInterfaceException e) {
			logger.e("SQL error creating gatherers", e);
		}
		Gatherer[] gatherers = new Gatherer[vector.size()];
		vector.copyInto(gatherers);
		return gatherers;
	}
	
	private Gatherer gatherer(String id) {
		Gatherer gatherer = new Gatherer(id, httpInterface, regexInterface, logger);
	
		try {
			CursorInterface rs = sqlInterface.query("SELECT Url FROM Urls WHERE ID = '" + id + "';");

			while(rs.next()) {
				gatherer.addUrl(rs.getString("Url"));
			}
			rs.close();
		} catch(SQLInterfaceException e) {}
		try {
			CursorInterface rs = sqlInterface.query("SELECT RequestType, Name, Value" +
					" FROM Requests WHERE ID = '" + id + "' AND RequestType LIKE 'Header';");

			while(rs.next()) {
				gatherer.addHeader(rs.getString("Name"), rs.getString("Value"));
			}
			rs.close();
		} catch(SQLInterfaceException e) {
			logger.e("SQL error creating gatherer", e);
		}
		
		try {
			CursorInterface rs = sqlInterface.query("SELECT RequestType, Name, Value" +
					" FROM Requests WHERE ID = '" + id + "' AND RequestType LIKE 'Get';");

			while(rs.next()) {
				gatherer.addGet(rs.getString("Name"), rs.getString("Value"));
			}
			rs.close();
		} catch(SQLInterfaceException e) {}
		
		try {
			CursorInterface rs = sqlInterface.query("SELECT RequestType, Name, Value" +
					" FROM Requests WHERE ID = '" + id + "' AND RequestType LIKE 'Post';");
			while(rs.next()) {
				gatherer.addPost(rs.getString("Name"), rs.getString("Value"));
			}
			rs.close();
		} catch(SQLInterfaceException e) {}
		
		try {
			CursorInterface rs = sqlInterface.query("SELECT RequestType, Name, Value" +
					" FROM Requests WHERE ID = '" + id + "' AND RequestType LIKE 'Cookie';");
			while(rs.next()) {
				gatherer.addCookie(rs.getString("Name"), rs.getString("Value"));
			}
			rs.close();
		} catch(SQLInterfaceException e) {}
		
		try {
			CursorInterface rs = sqlInterface.query("SELECT Regex FROM Terminators WHERE ID = '" + id + "';");
			while(rs.next()) {
				PatternInterface terminator = regexInterface.compile(rs.getString("Regex"));
				gatherer.addTerminator(terminator);
			}
		} catch(SQLInterfaceException e) {}
		
		try {
			String[] parentIds = simpleRecursiveQuery("SELECT Parent FROM GathererParents WHERE Child = ", id, ";", "Parent");
			
			// Start at 1 to skip the seed ID, which we have already dealt with.
			for(int i = 1; i < parentIds.length; i++) {
				gatherer.addParentGatherer(gatherer(parentIds[i]));
			}
		} catch(SQLInterfaceException e) {}
		
		return gatherer;
	}
	
	private Hashtable defaultFields(String[] namespaces, String[] types) {
		Hashtable table = new Hashtable();				
		try {					
			CursorInterface rs = sqlInterface.query("SELECT Name, Value" +
					" FROM DefaultFields WHERE Namespace IN (" + flattenArray(namespaces) +
					") AND Type IN (" + flattenArray(types) + ");");
			
			while(rs.next()) {
				table.put(rs.getString("Name"), rs.getString("Value"));
			}
		
		}catch(SQLInterfaceException e) {}
		return table;
	}
	
	/**
	 * Turn an array of strings into SQL-usable format for an IN statement:
	 * ['blah', 'bleh', 'meh'] => "'blah', 'bleh', 'meh'"
	 * Does not modify original array.
	 * @param array
	 * @return
	 */
	private String flattenArray(String[] array) {
		if(array == null) return null;
		String[] n_array = new String[array.length];
		for(int i = 0; i < array.length; i++) {
			if(array[i] == null) continue;
			n_array[i] = "'" + array[i] + "'";
		}
		return Utils.join(n_array, ",");
	}
}
