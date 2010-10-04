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

	public static void fetchProps(LocalCallback<Proposition[]> localCallback) {
		/*
		class AServerCallback extends ServerCallback<Proposition[]> {
			public LocalCallback<Proposition[]> localCallback;

			@Override
			public void onSuccess(Proposition[] result) {
				message();
				localCallback.call(result);
			}
		}
		;

		AServerCallback callback = new AServerCallback();
		callback.localCallback = localCallback;
*/
		propositionService.getAllProps(new ServerCallback<Proposition[]>(localCallback,"Server Reports Success Fetching Props" ));
	}

	public static void getPropTree(LocalCallback<Proposition> localCallback) {
		class ServerCallbackTemp implements AsyncCallback<Proposition> {

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSuccess(Proposition result) {
				// TODO Auto-generated method stub

			}

		}
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

	public static void getChanges(Change change, List<Proposition> props,
			List<Argument> args,
			LocalCallback<SortedMap<Date, Change>> localCallback) {
		/*
		 * class AServerCallback extends ServerCallback<SortedMap<Date, Change>>
		 * {
		 * 
		 * LocalCallback<SortedMap<Date, Change>> localCallback;
		 * 
		 * @Override public void onSuccess(SortedMap<Date, Change> result) {
		 * message("Server Reports Success Fetching Changes");
		 * localCallback.call(result); } }
		 */

		Long changeID = (change == null) ? null : change.id;
		// AServerCallback serverCallback = new AServerCallback();
		// serverCallback.localCallback = localCallback;
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
		/*
		class AServerCallback extends ServerCallback<PropTreeWithHistory> {
			LocalCallback<PropTreeWithHistory> localCallback;

			@Override
			public void onSuccess(PropTreeWithHistory result) {
				message("Server Reports Success Fetching Proposition and History");
				localCallback.call(result);

			}
		}
		AServerCallback serverCallback = new AServerCallback();
		serverCallback.localCallback = localCallback;*/
		propositionService.getPropositionCurrentVersionAndHistory(prop.id,
				new ServerCallback<PropTreeWithHistory>(localCallback,"Server Reports Success Fetching Proposition and History" ));
	}

	public static void getArgumentCurrentVersionAndHistory(Argument arg,
			LocalCallback<ArgTreeWithHistory> localCallback) {
		/*
		class AServerCallback extends ServerCallback<ArgTreeWithHistory> {
			LocalCallback<ArgTreeWithHistory> localCallback;

			@Override
			public void onSuccess(ArgTreeWithHistory result) {
				message("Server Reports Success Fetching Argument and History");
				localCallback.call(result);
			}
		}
		AServerCallback serverCallback = new AServerCallback();
		serverCallback.localCallback = localCallback;
		*/
		propositionService.getArgumentCurrentVersionAndHistory(arg.id,
				new ServerCallback<ArgTreeWithHistory>(localCallback, "Server Reports Success Fetching Argument and History"));
	}

	public static void searchPropositions(String string, Proposition prop,
			LocalCallback<List<Proposition>> localCallback) {
		/*
		class AServerCallback extends ServerCallback<List<Proposition>> {
			LocalCallback<List<Proposition>> localCallback;

			@Override
			public void onSuccess(List<Proposition> result) {
				// message("Server Reports Success Searching");
				localCallback.call(result);
			}
		}
		AServerCallback serverCallback = new AServerCallback();
		serverCallback.localCallback = localCallback;
		*/
		Long id = null;
		if (prop != null) {
			id = prop.id;
		}

		propositionService.searchPropositions(string, id, new ServerCallback<List<Proposition>>(localCallback,"Server Reports Success Searching"));
	}

	public static void addArgument(boolean pro, Proposition parentProp,
			Argument newArg, Proposition newProp) {
		class CommandAdd implements Command {
			boolean pro;
			Proposition parentProp;
			Argument newArg;
			Proposition newProp;

			@Override
			public void execute() {
				addArgumentCmd(pro, parentProp, newArg, newProp);
			}
		}
		CommandAdd command = new CommandAdd();
		command.pro = pro;
		command.parentProp = parentProp;
		command.newArg = newArg;
		command.newProp = newProp;
		queueCommand(command);
	}

	private static void addArgumentCmd(boolean pro, Proposition parentProp,
			Argument newArg, Proposition newProp) {
		class AddCallback implements AsyncCallback<Argument> {
			Proposition newProposition;
			Argument newArgument;

			@Override
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				message("Error: " + details);
				dispatchCommand();
			}

			@Override
			public void onSuccess(Argument result) {
				message("Server Reports Successful Argument Add");
				newProposition.id = result.props.get(0).id;
				newArgument.id = result.id;
				dispatchCommand();
			}
		}

		AddCallback addCallback = new AddCallback();
		addCallback.newProposition = newProp;
		addCallback.newArgument = newArg;

		propositionService.addArgument(parentProp.id, pro, addCallback);
	}

	public static void removeProposition(Proposition prop) {
		class CommandRemove implements Command {
			Proposition prop;

			@Override
			public void execute() {
				removePropositionCmd(prop);
			}
		}
		CommandRemove command = new CommandRemove();
		command.prop = prop;
		queueCommand(command);
	}

	private static void removePropositionCmd(Proposition prop) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				message("Error: " + caught.getMessage());
				dispatchCommand();
			}

			@Override
			public void onSuccess(Void result) {
				message("Server Reports Successful Proposition Delete");
				dispatchCommand();
			}
		};

		propositionService.removeProposition(prop.id, callback);
	}

	public static void updateProposition(Proposition prop) {
		class CommandUpdate implements Command {
			Proposition prop;

			@Override
			public void execute() {
				updatePropositionCmd(prop);
			}
		}
		CommandUpdate command = new CommandUpdate();
		command.prop = prop;

		queueCommand(command);
	}

	private static void updatePropositionCmd(Proposition prop) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				message("Error [updatePropositionCmd]: " + caught.getMessage());
				dispatchCommand();
			}

			@Override
			public void onSuccess(Void result) {
				message("Server Reports Successful Proposition Update");
				dispatchCommand();
			}
		};

		propositionService.updateProposition(prop.id, prop.getContent(),
				callback);
	}

	public static void addProposition(Proposition newProposition,
			Argument parentArgument, int position) {
		class CommandAdd implements Command {
			Proposition newProposition;
			Argument parentArgument;
			int position;

			@Override
			public void execute() {
				addPropositionCmd(newProposition, parentArgument, position);
			}

		}

		CommandAdd command = new CommandAdd();
		command.newProposition = newProposition;
		command.parentArgument = parentArgument;
		command.position = position;

		queueCommand(command);
	}

	private static void addPropositionCmd(Proposition newProposition,
			Argument parentArgument, int position) {

		class AddCallback implements AsyncCallback<Long> {
			Proposition newProposition;

			@Override
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				message("Error: " + details);
				dispatchCommand();
			}

			@Override
			public void onSuccess(Long result) {
				message("Server Reports Successful Proposition Add");
				newProposition.id = result;
				dispatchCommand();
			}
		}

		AddCallback addCallback = new AddCallback();
		addCallback.newProposition = newProposition;

		if (parentArgument != null)
			propositionService.addProposition(parentArgument.id, position,
					addCallback);
		else
			propositionService.addProposition(null, 0, addCallback);
	}
}
