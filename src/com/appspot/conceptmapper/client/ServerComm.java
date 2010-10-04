package com.appspot.conceptmapper.client;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.Queue;

import com.appspot.conceptmapper.client.PropositionService.ArgTreeWithHistory;
import com.appspot.conceptmapper.client.PropositionService.PropTreeWithHistory;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

/* for remote calls that change data on the server (i.e. adding/removing/modifying 
 * propositions or arguments this class queues calls so that they arrive to the 
 * server in order, one after another.  This avoids inconsistent states, where, for instance
 * the server receives a request to delete a proposition before it has finished adding
 * the proposition.  For remote calls that don't change data however, I don't bother with
 * queuing the remote calls.
 */
public class ServerComm {
	private static PropositionServiceAsync propositionService = GWT
			.create(PropositionService.class);

	private static Queue<Command> commandQueue = new LinkedList<Command>();
	private static boolean callInProgress = false;

	private static void message(String string) {
		GWT.log(string);
		ConceptMapper.message(string);
	}

	private interface Command {
		public void execute();
	}

	private static void dispatchCommand() {
		if (commandQueue.isEmpty()) {
			callInProgress = false;
		} else {
			callInProgress = true;
			commandQueue.poll().execute();
		}
	}

	private static void queueCommand(Command command) {
		commandQueue.add(command);
		if (callInProgress == false) {
			dispatchCommand();
		}
	}

	public static interface LocalCallback<T> {
		public void call(T t);
	}

	private static class ServerCallback<T> implements AsyncCallback<T> {
		LocalCallback<T> localCallback;
		String successMessage;

		public ServerCallback(LocalCallback<T> localCallback,
				String successMessage) {
			this.localCallback = localCallback;
			this.successMessage = successMessage;
		}

		@Override
		public void onFailure(Throwable caught) {
			message("Error: " + caught.getMessage());
			caught.printStackTrace();
		}

		@Override
		public void onSuccess(T result) {
			if (localCallback != null) {
				localCallback.call(result);
			}
			if (successMessage != null) {
				message(successMessage);
			}
		}
	}

	private static abstract class ServerCallbackWithDispatch<T> implements
			AsyncCallback<T> {
		String successMessage;

		public ServerCallbackWithDispatch(String successMessage) {
			this.successMessage = successMessage;
		}

		@Override
		public void onFailure(Throwable caught) {
			dispatchCommand();
			message("Error: " + caught.getMessage());
			caught.printStackTrace();
		}

		@Override
		public void onSuccess(T result) {
			dispatchCommand();
			if (successMessage != null) {
				message(successMessage);
			}
			doOnSuccess(result);
		}

		public abstract void doOnSuccess(T result);
	}

	public static void fetchProps(LocalCallback<Proposition[]> localCallback) {
		propositionService.getAllProps(new ServerCallback<Proposition[]>(
				localCallback, "Server Reports Success Fetching Props"));
	}

	public static void getPropTree(Proposition proposition,
			LocalCallback<Proposition> localCallback) {
		propositionService.getPropositionTree(proposition.id,
				new ServerCallback<Proposition>(localCallback,
						"Server Reports Success Getting Prop Tree"));
	}

	public static void getChanges(Change change, List<Proposition> props,
			List<Argument> args,
			LocalCallback<SortedMap<Date, Change>> localCallback) {
		Long changeID = (change == null) ? null : change.id;
		List<Long> propIDs = new LinkedList<Long>();
		for (Proposition prop : props) {
			propIDs.add(prop.id);
		}
		List<Long> argIDs = new LinkedList<Long>();
		for (Argument arg : args) {
			argIDs.add(arg.id);
		}

		propositionService.getRevisions(changeID, propIDs, argIDs,
				new ServerCallback<SortedMap<Date, Change>>(localCallback,
						"Server Reports Success Fetching Changes"));
	}

	public static void getPropositionCurrentVersionAndHistory(Proposition prop,
			LocalCallback<PropTreeWithHistory> localCallback) {
		propositionService
				.getPropositionCurrentVersionAndHistory(
						prop.id,
						new ServerCallback<PropTreeWithHistory>(localCallback,
								"Server Reports Success Fetching Proposition and History"));
	}

	public static void getArgumentCurrentVersionAndHistory(Argument arg,
			LocalCallback<ArgTreeWithHistory> localCallback) {
		propositionService
				.getArgumentCurrentVersionAndHistory(
						arg.id,
						new ServerCallback<ArgTreeWithHistory>(localCallback,
								"Server Reports Success Fetching Argument and History"));
	}

	public static void searchPropositions(String string, Proposition prop,
			LocalCallback<List<Proposition>> localCallback) {
		Long id = null;
		if (prop != null) {
			id = prop.id;
		}

		propositionService.searchPropositions(string, id,
				new ServerCallback<List<Proposition>>(localCallback, null));
	}

	public static void addArgument(boolean pro, Proposition parentProp,
			Argument newArg, Proposition newProp) {
		class CommandAdd extends ServerCallbackWithDispatch<Argument> implements
				Command {
			boolean pro;
			Proposition parentProp;
			Argument newArg;
			Proposition newProp;

			CommandAdd(String message) {
				super(message);
			}

			@Override
			public void execute() {
				propositionService.addArgument(parentProp.id, pro, this);
			}

			@Override
			public void doOnSuccess(Argument result) {
				newProp.id = result.props.get(0).id;
				newArg.id = result.id;
			}
		}
		CommandAdd command = new CommandAdd(
				"Server Reports Successful Argument Add");
		command.pro = pro;
		command.parentProp = parentProp;
		command.newArg = newArg;
		command.newProp = newProp;
		queueCommand(command);
	}

	public static void removeProposition(Proposition prop) {
		class CommandRemove extends ServerCallbackWithDispatch<Void> implements
				Command {
			Proposition prop;

			public CommandRemove(String message) {
				super(message);
			}

			@Override
			public void execute() {
				propositionService.removeProposition(prop.id, this);
			}

			@Override
			public void doOnSuccess(Void result) {
			}
		}
		CommandRemove command = new CommandRemove(
				"Server Reports Successful Proposition Delete");
		command.prop = prop;
		queueCommand(command);
	}

	public static void updateProposition(Proposition prop) {
		class CommandUpdate extends ServerCallbackWithDispatch<Void> implements
				Command {
			Proposition prop;

			public CommandUpdate(String message) {
				super(message);
			}

			@Override
			public void execute() {
				propositionService.updateProposition(prop.id,
						prop.getContent(), this);
			}

			@Override
			public void doOnSuccess(Void result) {
			}
		}
		CommandUpdate command = new CommandUpdate(
				"Server Reports Successful Proposition Update");
		command.prop = prop;

		queueCommand(command);
	}

	public static void addProposition(Proposition newProposition,
			Argument parentArgument, int position) {
		class CommandAdd extends ServerCallbackWithDispatch<Long> implements
				Command {
			Proposition newProposition;
			Argument parentArgument;
			int position;

			public CommandAdd(String message) {
				super(message);
			}

			@Override
			public void execute() {
				if (parentArgument != null)
					propositionService.addProposition(parentArgument.id,
							position, this);
				else
					propositionService.addProposition(null, 0, this);
			}

			@Override
			public void doOnSuccess(Long result) {
				newProposition.id = result;
			}
		}

		CommandAdd command = new CommandAdd(
				"Server Reports Successful Proposition Add");
		command.newProposition = newProposition;
		command.parentArgument = parentArgument;
		command.position = position;

		queueCommand(command);
	}
}
