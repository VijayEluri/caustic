package net.caustic.executor;

import java.util.Vector;

import net.caustic.database.Database;
import net.caustic.database.DatabaseException;
import net.caustic.database.DatabaseView;
import net.caustic.http.HttpBrowser;
import net.caustic.instruction.Instruction;
import net.caustic.util.VectorUtils;

/**
 * An executor that blocks the current thread on {@link #execute()}.
 * @author talos
 *
 */
public class SyncExecutor {	
	/**
	 * 
	 * @param executables An array of {@link Executable}s that should be executed, along with their
	 * children.
	 * @return An array of {@link Executable}s that are missing variables necessary for execution.
	 * @throws DatabaseException
	 * @throws InterruptedException
	 */
	private Executable[] loop(Executable[] executables) throws DatabaseException, InterruptedException {
		Vector queue = new Vector();
		VectorUtils.arrayIntoVector(executables, queue);
		
		Vector stuck = new Vector();

		while(queue.size() > 0) {
			Executable executable = (Executable) queue.elementAt(0);
			queue.removeElementAt(0); // we're running it, remove from queue.
			
			Executable[] children = executable.execute();
			if(children != null) { // success
				VectorUtils.arrayIntoVector(children, queue); // add children to queue
			} else {
				if(executable.isMissingTags()) {
					stuck.add(executable);
				} else {
					// failed TODO
				}
			}
		}
		
		Executable[] stuckExecutablesAry = new Executable[stuck.size()];
		stuck.copyInto(stuckExecutablesAry);
		return stuckExecutablesAry;
	}
	
	public void execute(Instruction instruction, Database db, String source, 
			HttpBrowser browser) throws InterruptedException, DatabaseException {		
		Executable[] executables = new Executable[] {
				new Executable(instruction, new DatabaseView(db), source, browser) };
		while(executables.length > 0) {
			executables = loop(executables);
			if(Executable.allAreStuck(executables)) {
				// all stuck, break out
				break;
			}
		}
	}
}
