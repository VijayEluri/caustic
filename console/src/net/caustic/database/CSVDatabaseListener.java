package net.caustic.database;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

import net.caustic.database.DatabaseListener;
import net.caustic.database.DatabaseListenerException;
import net.caustic.scope.Scope;

/**
 * Record database transactions into a CSV.
 * @author talos
 *
 */
public class CSVDatabaseListener implements DatabaseListener {

	private final CSVWriter writer;	
	public CSVDatabaseListener(File file, char separator) throws IOException {
		
		writer = new CSVWriter(new FileWriter(file), separator);
		writer.writeNext(new String[] { "source", "scope", "name", "value" });
	}
	
	@Override
	public void onPut(Scope scope, String key, String value)
			throws DatabaseListenerException {
		write(null, scope, key, value);
	}

	@Override
	public void onNewScope(Scope scope) throws DatabaseListenerException {

	}

	@Override
	public void onNewScope(Scope parent, String key, Scope child)
			throws DatabaseListenerException {
		write(parent, child, key, null);
	}

	@Override
	public void onNewScope(Scope parent, String key, String value, Scope child)
			throws DatabaseListenerException {
		write(parent, child, key, value);
	}
	
	private void write(Scope parent, Scope child, String name, String value) {
		writer.writeNext(new String[] { parent.asString(), child.asString(), name, value } );
	}
}
