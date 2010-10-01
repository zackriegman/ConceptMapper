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
		ConceptMapper.println(string);
	}

	public interface FetchPropsCallback {
		public void call(Proposition[] props);
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

	public static void fetchProps(FetchPropsCallback fetchCallback) {
		class ThisCallback implements AsyncCallback<Proposition[]> {
			public FetchPropsCallback fetch;

			public void onFailure(Throwable caught) {
				message("Error: " + caught.getMessage());
			}

			@Override
			public void onSuccess(Proposition[] result) {
				message("Server Reports Success Fetching Props");
				fetch.call(result);
			}
		}
		;

		ThisCallback callback = new ThisCallback();
		callback.fetch = fetchCallback;

		propositionService.getAllProps(callback);
	}

	public interface GetChangesCallback {
		public void call(SortedMap<Date, Change> changes);
	}

	public static void getChanges(Change change, List<Proposition> props,
			List<Argument> args, GetChangesCallback localCallback) {

		class ThisCallback implements AsyncCallback<SortedMap<Date, Change>> {

			GetChangesCallback getChangesCallback;

			@Override
			public void onSuccess(SortedMap<Date, Change> result) {
				message("Server Reports Success Fetching Changes");
				getChangesCallback.call(result);
			}

			@Override
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
				// message("Error: " + s.toString() );
			}
		}

		Long changeID = (change == null) ? null : change.id;
		ThisCallback serverCallback = new ThisCallback();
		serverCallback.getChangesCallback = localCallback;
		List<Long> propIDs = new LinkedList<Long>();
		for (Proposition prop : props) {
			propIDs.add(prop.id);
		}
		List<Long> argIDs = new LinkedList<Long>();
		for (Argument arg : args) {
			argIDs.add(arg.id);
		}

		propositionService.getRevisions(changeID, propIDs, argIDs,
				serverCallback);
	}

	public interface GetPropositionCurrentVersionAndHistoryCallback {
		public void call(PropTreeWithHistory propTreeWithHistory);
	}

	public static void getPropositionCurrentVersionAndHistory(Proposition prop,
			GetPropositionCurrentVersionAndHistoryCallback localCallback) {
		class ServerCallback implements AsyncCallback<PropTreeWithHistory> {
			GetPropositionCurrentVersionAndHistoryCallback localCallback;

			@Override
			public void onSuccess(PropTreeWithHistory result) {
				message("Server Reports Success Fetching Proposition and History");
				localCallback.call(result);

			}

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				caught.printStackTrace();
			}
		}
		ServerCallback serverCallback = new ServerCallback();
		serverCallback.localCallback = localCallback;
		propositionService.getPropositionCurrentVersionAndHistory(prop.id,
				serverCallback);
	}

	public interface GetArgumentCurrentVersionAndHistoryCallback {
		public void call(ArgTreeWithHistory argTreeWithHistory);
	}

	public static void getArgumentCurrentVersionAndHistory(Argument arg,
			GetArgumentCurrentVersionAndHistoryCallback localCallback) {
		class ServerCallback implements AsyncCallback<ArgTreeWithHistory> {
			GetArgumentCurrentVersionAndHistoryCallback localCallback;

			@Override
			public void onSuccess(ArgTreeWithHistory result) {
				message("Server Reports Success Fetching Argument and History");
				localCallback.call(result);
			}

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				caught.printStackTrace();
			}
		}
		ServerCallback serverCallback = new ServerCallback();
		serverCallback.localCallback = localCallback;
		propositionService.getArgumentCurrentVersionAndHistory(arg.id,
				serverCallback);
	}

	public interface SearchPropositionsCallback {
		public void call(List<Proposition> propMatches);
	}

	public static void searchPropositions(String string, Proposition prop,
			SearchPropositionsCallback localCallback) {
		class ServerCallback implements AsyncCallback<List<Proposition>> {
			SearchPropositionsCallback localCallback;

			@Override
			public void onSuccess(List<Proposition> result) {
				//message("Server Reports Success Searching");
				localCallback.call(result);
			}

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				caught.printStackTrace();
			}
		}
		ServerCallback serverCallback = new ServerCallback();
		serverCallback.localCallback = localCallback;
		Long id = null;
		if( prop != null ){
			id = prop.id;
		}
		
		propositionService.searchPropositions( string, id, serverCallback );
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
