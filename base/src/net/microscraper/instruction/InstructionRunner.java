package net.microscraper.instruction;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.microscraper.client.Loggable;
import net.microscraper.client.Logger;
import net.microscraper.database.Database;
import net.microscraper.impl.log.BasicLog;
import net.microscraper.util.Execution;
import net.microscraper.util.StringUtils;
import net.microscraper.util.Variables;
import net.microscraper.util.VectorUtils;

public class InstructionRunner implements Runnable, Loggable {
	
	private final Instruction instruction;
	private final Variables variables;
	private final String source;
	
	private final Vector queue = new Vector();
	private final Vector failedExecutables = new Vector();
	private final BasicLog log = new BasicLog();
	
	/**
	 * 
	 * @param instruction
	 * @param database
	 * @param defaults
	 * @param source
	 * @throws IOException If there was a problem writing to the database.
	 */
	public InstructionRunner(Instruction instruction, Database database, Hashtable defaults, String source)
			throws IOException {
		this.instruction = instruction;
		this.variables = Variables.fromHashtable(database, defaults);
		this.source = source;
	}
	
	private Vector getStuckExecutables() {
		Vector stuckExecutables = new Vector();
		Enumeration e = queue.elements();
		while(e.hasMoreElements()) {
			Executable executable = (Executable) e.nextElement();
			if(executable.isStuck()) {
				stuckExecutables.add(executable);
			}
		}
		return stuckExecutables;
	}
	
	public void run() {
		//Variables variables = Variables.fromHashtable(defaults);
		//Executable start = instruction.bind(source, variables);
		Executable start = new Executable(source, variables, instruction);
		queue.add(start);
		
		log.i("Starting to execute with " + StringUtils.quote(start) + " and variables " +
				StringUtils.quote(variables));
		
		// try - catch the entire loop for user interrupt.
		try {
			do {
				Executable executable = (Executable) queue.elementAt(0);
				queue.removeElementAt(0);
				
				// Try to execute the executable.
				log.i("Trying to execute " + StringUtils.quote(executable));
				Execution execution = executable.execute();
				
				// Evaluate the execution's success.
				if(execution.isSuccessful()) {
					// It's successful -- add the resultant executables onto the queue.
					Executable[] children = (Executable[]) execution.getExecuted();
					log.i("Executable " + StringUtils.quote(executable) + " successful." + 
							" Adding its " + children.length + " children to the queue.");
					VectorUtils.arrayIntoVector(children, queue);
					
				} else if(execution.isMissingVariables()) {
					// Try it again later.
					String[] missingVariables = execution.getMissingVariables();
					log.i("Executable " + StringUtils.quote(executable) + " is missing the " +
							" following variables: " +
							StringUtils.quoteJoin(missingVariables) + 
							" Placing at to the end of the queue. It is " +
							(executable.isStuck() ? "stuck." : "not stuck."));
					queue.add(executable);
				} else {
					// Log that execution has failed.
					log.i("Executable " + StringUtils.quote(executable) + " has failed because " +
							" of " + StringUtils.quote(execution.failedBecause()) + ". Removing" +
							" it from the queue.");
					failedExecutables.add(executable);
				}
				
				// End the loop when we run out of executables, or if they're all
				// stuck.
			} while(queue.size() > 0 && getStuckExecutables().size() < queue.size());
		
			log.i("Finished execution starting with " + StringUtils.quote(start) + " and " +
					"variables " + StringUtils.quote(variables));
		} catch(InterruptedException e) {
			log.i("Prematurely terminated execution starting with " + StringUtils.quote(start) + " and " +
					"variables " + StringUtils.quote(variables) + " because of user interrupt.");
		} catch(IOException e) {
			log.i("Prematurely terminated execution starting with " + StringUtils.quote(start) + " and " +
					"variables " + StringUtils.quote(variables) + " because the database could not be saved to: " +
					e.getMessage());
		}
		
		// Log information about stuck executables.
		Vector stuckExecutables = getStuckExecutables();
		if(stuckExecutables.size() > 0) {
			log.i("There were " + stuckExecutables.size() + " stuck executables: ");
			Enumeration e = stuckExecutables.elements();
			while(e.hasMoreElements()) {
				Executable stuck = (Executable) e.nextElement();
				log.i("Executable " + stuck + " was stuck on " + 
						// TODO LOD violation
						StringUtils.quoteJoin(stuck.lastExecution().getMissingVariables()));
			}
		}
		
		// Log inforamtion about failed executables.
		if(failedExecutables.size() > 0) {
			log.i("There were " + failedExecutables.size() + " failed executables: ");
			Enumeration e = stuckExecutables.elements();
			while(e.hasMoreElements()) {
				Executable failed = (Executable) e.nextElement();
				log.i("Executable " + failed + " failed because of " +
						// TODO LOD violation
						StringUtils.quote(failed.lastExecution().failedBecause()));
			}			
		}
		
		queue.clear();
	}

	public void register(Logger logger) {
		log.register(logger);
	}
}
