package net.microscraper.database.sql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import net.microscraper.database.IOTable;
import net.microscraper.database.TableManipulationException;
import net.microscraper.util.StringUtils;
import net.microscraper.uuid.UUID;

/**
 * A SQL implementation of {@link Updateable} using {@link SQLConnection}.
 * 
 * @author talos
 *
 */
public class SQLTable implements IOTable {
	
	/**
	 * {@link SQLConnection} used in this {@link SQLTable}.
	 */
	private final SQLConnection connection;
	
	/**
	 * {@link String} name of the table in SQL.
	 */
	private final String name;

	/**
	 * The {@link String} name of the scope column.
	 */
	private final String scopeColumnName;
	
	/**
	 * Check a {@link String} for backticks, which cause problems in column or
	 * table names.
	 * @param stringToCheck The {@link String} to check.
	 * @throws IllegalArgumentException If <code>stringToCheck</code>
	 * contains a backtick.
	 */
	private void preventIllegalBacktick(String stringToCheck) throws IllegalArgumentException {
		if(stringToCheck.indexOf('`') != -1 ) {
			throw new IllegalArgumentException("Illegal name for SQL " +
					StringUtils.quote(stringToCheck) +
					" to  because it contains a backtick at index " +
				Integer.toString(stringToCheck.indexOf('`')));
		}
	}
	
	/**
	 * Obtain a {@link SQLResultSet} a scope and set of columns.  Remember to
	 * close {@link SQLResultSet}.
	 * @param scope
	 * @param columnNames
	 * @return
	 * @throws IOException
	 */
	private SQLResultSet getResultSet(UUID scope, String[] columnNames)  throws IOException {
		try {
			SQLPreparedStatement select = connection.prepareStatement(
					"SELECT `" + StringUtils.join(columnNames, "`, `") + "` " +
					"FROM `" + name + "` " +
					"WHERE `" + scopeColumnName + "` = ?");
			select.bindStrings(new String[] { scope.asString() });
			return select.executeQuery();
		} catch(SQLConnectionException e) {
			throw new IOException(e);
		}
	}
	
	public SQLTable(SQLConnection connection, String name, String idColumnName) throws SQLConnectionException {
		this.name = name;
		this.connection = connection;
		this.scopeColumnName = idColumnName;
	}
	
	@Override
	public void addColumn(String columnName) throws TableManipulationException {
		preventIllegalBacktick(columnName);
		
		try {
			String type = connection.textColumnType();
			SQLPreparedStatement alterTable = 
					connection.prepareStatement(
							"ALTER TABLE `" + name + "` " +
							" ADD COLUMN `" + columnName + "`" + 
							type);
			alterTable.execute();
			connection.runBatch();
		} catch(SQLConnectionException e) {
			throw new TableManipulationException(e);
		}
	}
	
	@Override
	public boolean hasColumn(String columnName) throws IOException {
		try {
			SQLPreparedStatement select =
					connection.prepareStatement(
							"SELECT * FROM `" + name + "`");
			SQLResultSet rs = select.executeQuery();
			boolean hasColumn = rs.hasColumnName(columnName);
			rs.close();
			
			return hasColumn;
		} catch(SQLConnectionException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public void insert(UUID id, Map<String, String> map) throws TableManipulationException {
		String[] columnNames = new String[map.size() + 1];
		String[] parameters = new String[map.size() + 1];
		String[] columnValues = new String[map.size() + 1];
		
		int i = 1;
		for(Map.Entry<String, String> entry : map.entrySet()) {
			columnNames[i] = "`" + entry.getKey() + "`";
			parameters[i] = "?";
			columnValues[i] = entry.getValue();
			i++;
		}
		columnNames[0] = scopeColumnName;
		parameters[0] = "?";
		columnValues[0] = id.asString();
		
		try {
			SQLPreparedStatement insert = connection.prepareStatement(
					"INSERT INTO `" + name + "` " +
							"(" + StringUtils.join(columnNames, ", ") + ") " +
							"VALUES (" + StringUtils.join(parameters, ", ") + ")");
			insert.bindStrings(columnValues);
			insert.execute();
		} catch(SQLConnectionException e) {
			throw new TableManipulationException(e);
		}
	}

	@Override
	public void update(UUID id, Map<String, String> map)
			throws TableManipulationException {
		String[] setStatements = new String[map.size()];
		String[] values = new String[map.size() + 1];
		
		int i = 0;
		for(Map.Entry<String, String> entry : map.entrySet()) {
			setStatements[i] = "`" + entry.getKey() + "` = ? ";
			values[i] = entry.getValue();
		}
		values[values.length - 1] = id.asString();
		
		String set = " SET " + StringUtils.join(setStatements, ", ");
		
		try {
			SQLPreparedStatement update = connection.prepareStatement(
					"UPDATE `" + name + "` " + set +
					"WHERE `" + scopeColumnName + "` = ?");
			update.bindStrings(values);
			update.execute();
		} catch (SQLConnectionException e) {
			throw new TableManipulationException(e);
		}
	}

	@Override
	public List<Map<String, String>> select(UUID scope, String[] columnNames) throws IOException {
		try  {
			SQLResultSet rs = getResultSet(scope, columnNames);
			
			List<Map<String, String>> results = new ArrayList<Map<String, String>>();
			while(rs.next()) {
				Map<String, String> map = new HashMap<String, String>();
				for(String columnName : columnNames) {
					map.put(columnName, rs.getString(columnName));
				}
				results.add(map);
			}
			rs.close();
			return results;
		} catch(SQLConnectionException e) {
			throw new IOException(e);
		}
	}

	@Override
	public List<String> select(UUID scope, String columnName) throws IOException {
		try  {
			SQLResultSet rs = getResultSet(scope, new String[] { columnName} );
			
			List<String> results = new ArrayList<String>();
			while(rs.next()) {
				results.add(rs.getString(columnName));
			}
			rs.close();
			return results;
		} catch(SQLConnectionException e) {
			throw new IOException(e);
		}
		
	}
}