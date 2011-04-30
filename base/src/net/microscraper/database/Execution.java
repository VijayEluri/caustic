package net.microscraper.database;

import java.util.Vector;

import net.microscraper.client.Mustache;
import net.microscraper.client.Mustache.MissingVariable;
import net.microscraper.client.Mustache.TemplateException;
import net.microscraper.client.Variables;
import net.microscraper.database.Attribute.AttributeDefinition;
import net.microscraper.database.Database.ResourceNotFoundException;
import net.microscraper.database.Relationship.RelationshipDefinition;
import net.microscraper.database.Resource.OneToOneResource;

public abstract class Execution {
	private static int count = 0;

	private final Vector calledExecutions = new Vector();
	private final Variables extraVariables = new Variables();
	private final Execution caller;	
	private final Resource resource;

	private Status lastStatus = new Status();
	private String result = null;
	public final int id;
	
	protected Execution(Resource resource, Execution caller) {
		id = count++;
		this.resource = resource;
		this.caller = caller;
		if(caller != null) {
			this.caller.addCalledExecution(this);
		}
	}
	
	public final Execution getSourceExecution() {
		if(isOneToMany() || caller == null) {
			return this;
		} else {
			return caller.getSourceExecution();
		}
	}
	
	private final void addCalledExecution(Execution called) {
		getSourceExecution().calledExecutions.addElement(called);
	}
	
	protected final Variables getVariables() {
		Variables variables = new Variables().merge(extraVariables);
		for(int i = 0 ; i < calledExecutions.size() ; i ++) {
			Execution calledExecution = (Execution) calledExecutions.elementAt(i);
			if(!calledExecution.isOneToMany()) {
				variables.merge(calledExecution.getLocalVariables());
			}
		}
		// Ascend up the source tree.
		if(getSourceExecution() == this) {
			return variables;
		} else {
			return variables.merge(getSourceExecution().getVariables());
		}
	}

	public void addVariables(Variables extraVariables) {
		this.extraVariables.merge(extraVariables);
	}
	
	protected final boolean isOneToMany() {
		return resource.isOneToMany();
	}
	
	protected final Execution callResource(OneToOneResource resource) throws ExecutionFatality {
		return resource.executionFromExecution(getSourceExecution());
	}
	public final Status getStatus() {
		return lastStatus;
	}
	
	public String getPublishName() {
		return resource.ref().toString();
	}
	
	protected final String getAttributeValue(AttributeDefinition def)
				throws ExecutionFatality, MissingVariable {
		try {
			return (String) Mustache.compile(this, resource.getStringAttribute(def), getVariables());
		} catch(TemplateException e) {
			throw new ExecutionFatality(this, e);
		}
	}
	protected Resource[] getRelatedResources(RelationshipDefinition def) throws ExecutionFatality {
		try {
			return resource.getRelatedResources(def);
		} catch(ResourceNotFoundException e) {
			throw new ExecutionFatality(this, e);
		}
	}
	
	protected final Variables getLocalVariables() {
		if(result != null && resource.isPublishedToVariables()) {
			Variables variables = new Variables();
			variables.put(resource.ref().title, result);
			return variables;
		}
		return null;
	}
	
	public final Status safeExecute() throws ExecutionFatality {
		if(lastStatus.shouldRetry()) {
			Status status = new Status();
			try {
				result = privateExecute();
			} catch(ExecutionDelay e) {
				status.addDelay(e);
			} catch(ExecutionFailure e) {
				status.addFailure(e);
			} catch(StatusException e) {
				status.merge(e.getStatus());
			}
			lastStatus = status;
		}
		return lastStatus;
	}
	public final String unsafeExecute() throws ExecutionDelay, ExecutionFailure, ExecutionFatality, StatusException {
		if(result == null) {
			String result = privateExecute();
			lastStatus = new Status();
			return result;
		} else {
			return result;
		}
	}
	
	protected abstract String privateExecute()
		throws ExecutionDelay, ExecutionFailure, ExecutionFatality, StatusException;
	
	public final boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(!(obj instanceof Execution))
			return false;
		if(this.hashCode() == obj.hashCode())
			return true;
		return false;
	}
	
	public final int hashCode() {
		return id;
	}
	
	public static interface ExecutionProblem {
		public Execution callerExecution();
		public String reason();
		public boolean equals(Object obj);
	}
	
	public static abstract class DefaultExecutionProblem extends Exception implements ExecutionProblem {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1875711681202830444L;
		private Execution caller;
		public DefaultExecutionProblem(Execution caller) {
			this.caller = caller;
		}
		public Execution callerExecution() {
			return caller;
		}
		public final boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(!(obj instanceof ExecutionProblem))
				return false;
			ExecutionProblem that = (ExecutionProblem) obj;
			if(this.callerExecution().equals(that.callerExecution()) && this.reason().equals(that.reason()))
				return true;
			return false;
		}
	}
	
	public static abstract class ExecutionDelay extends DefaultExecutionProblem {
		public ExecutionDelay(Execution caller) {
			super(caller);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1887704359270171496L;
	}
	
	public static class ExecutionFailure extends DefaultExecutionProblem {
		private final Throwable throwable;
		public ExecutionFailure(Execution caller, Throwable e) {
			super(caller);
			this.throwable = e;
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -5646674827768905150L;

		public String reason() {
			return throwable.getMessage();
		}
	}
	
	public static class ExecutionFatality extends DefaultExecutionProblem {
		private final Throwable throwable;
		public ExecutionFatality(Execution caller, Throwable e) {
			super(caller);
			this.throwable = e;
		}
		
		public String reason() {
			return throwable.getMessage();
		}
		/**
		 * 
		 */
		private static final long serialVersionUID = -5646674827768905150L;
	}
	
	// A way of tossing status through exceptions.
	public static class StatusException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3155033955368165156L;
		private final Status status;
		public StatusException(Status status) {
			this.status = status;
		}
		public Status getStatus() {
			return status;
		}
	}
}
