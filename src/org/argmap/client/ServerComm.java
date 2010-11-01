package org.argmap.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodeWithChanges;
import org.argmap.client.ArgMapService.PropsAndArgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/* for remote calls that change data on the server (i.e. adding/removing/modifying 
 * propositions or arguments this class queues calls so that they arrive to the 
 * server in order, one after another.  This avoids inconsistent states, where, for instance
 * the server receives a request to delete a proposition before it has finished adding
 * the proposition.  For remote calls that don't change data however, I don't bother with
 * queuing the remote calls.
 */
public class ServerComm {
	private static ArgMapServiceAsync argMapService = GWT
			.create(ArgMapService.class);

	private static Queue<Command> commandQueue = new LinkedList<Command>();
	private static boolean callInProgress = false;

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
		public final void onFailure(Throwable caught) {
			ArgMap.message("Server Error: " + caught.getMessage(), MessageType.ERROR,
					10);
			throw new RuntimeException(caught);
		}

		@Override
		public final void onSuccess(T result) {

			if (localCallback != null) {
				localCallback.call(result);
			}
			if (successMessage != null) {
				ArgMap.message(successMessage, MessageType.INFO, 2);
			}
		}
	}

	public static void logException(Throwable e) {
		argMapService.logClientException(e.toString(),
				new AsyncCallback<Void>() {

					@Override
					public void onSuccess(Void result) {
						// Window.alert("Succesfully Logged Bug On Server");

					}

					@Override
					public void onFailure(Throwable caught) {
						Window.alert("Failed To Log Bug On Server: "
								+ caught.toString());

					}
				});
	}

	private static abstract class ServerCallbackWithDispatch<T> implements
			AsyncCallback<T> {
		String successMessage;

		public ServerCallbackWithDispatch(String successMessage) {
			this.successMessage = successMessage;
		}

		@Override
		public final void onFailure(Throwable caught) {
			dispatchCommand();
			ArgMap.message("Server Error: " + caught.getMessage(), MessageType.ERROR,
					10);
			// GWT.log(caught.getMessage());
			// caught.printStackTrace();
			/* trying this to get the exception printed in the GWT.log */
			throw new RuntimeException(caught);
		}

		@Override
		public final void onSuccess(T result) {

			/*
			 * this must come before dispatchCommand() otherwise the client
			 * might send a request to add a proposition to an argument before
			 * the argument has been assigned an id by the return call from the
			 * server...
			 */
			doOnSuccess(result);
			dispatchCommand();
			if (successMessage != null) {
				ArgMap.message(successMessage, MessageType.INFO, 2);
			}

		}

		public abstract void doOnSuccess(T result);
	}

	public static void getRootProps(int depthLimit,
			LocalCallback<PropsAndArgs> localCallback) {
		argMapService.getPropsAndArgs(depthLimit,
				new ServerCallback<PropsAndArgs>(localCallback,
						"Server Reports Success Fetching Props"));
	}

	public static void getNodesChildren(List<Long> nodeIDs, int depth,
			LocalCallback<Nodes> localCallback) {
		argMapService.getNodesChildren(nodeIDs, depth,
				new ServerCallback<Nodes>(localCallback,
						"Server Reports Success" + "Fetching Props"));
	}

	public static void getLoginInfo(LocalCallback<LoginInfo> localCallback) {
		String requestURI;
		if( GWT.isProdMode() ){
			requestURI = GWT.getHostPageBaseURL();
		} else {
			requestURI = "http://127.0.0.1:8888/ArgMap.html?gwt.codesvr=127.0.0.1:9997";
		}
		argMapService.getLoginInfo(requestURI,
				new ServerCallback<LoginInfo>(localCallback,
						"Server Reports Success" + "Getting Login Info"));
	}

	public static void getChanges(List<Proposition> props, List<Argument> args,
			LocalCallback<NodeChangesMaps> localCallback) {
		List<Long> propIDs = new LinkedList<Long>();
		for (Proposition prop : props) {
			propIDs.add(prop.id);
		}
		List<Long> argIDs = new LinkedList<Long>();
		for (Argument arg : args) {
			argIDs.add(arg.id);
		}
		argMapService.getChanges(propIDs, argIDs,
				new ServerCallback<NodeChangesMaps>(localCallback,
						"Server Reports Success Getting Changes"));
	}

	public static void getPropsWithChanges(List<Long> propIDs,
			LocalCallback<Map<Long, NodeWithChanges>> localCallback) {
		argMapService
				.getPropsWithChanges(
						propIDs,
						new ServerCallback<Map<Long, NodeWithChanges>>(
								localCallback,
								"Server Reports Success Fetching Proposition With Changes"));
	}

	public static void getArgsWithChanges(List<Long> argIDs,
			LocalCallback<Map<Long, NodeWithChanges>> localCallback) {
		argMapService
				.getArgsWithChanges(
						argIDs,
						new ServerCallback<Map<Long, NodeWithChanges>>(
								localCallback,
								"Server Reports Success Fetching Proposition With Changes"));
	}

	public static void searchProps(String string, Argument filterArg,
			Proposition filterProp, LocalCallback<PropsAndArgs> localCallback) {
		Long argID = filterArg == null ? null : filterArg.id;
		Long propID = filterProp == null ? null : filterProp.id;

		argMapService.searchProps(string, argID, propID,
				new ServerCallback<PropsAndArgs>(localCallback, null));
	}

	public static void addArg(boolean pro, Proposition parentProp,
			Argument newArg) {
		class CommandAdd extends ServerCallbackWithDispatch<Long> implements
				Command {
			boolean pro;
			Proposition parentProp;
			Argument newArg;

			// Proposition newProp;

			CommandAdd(String message) {
				super(message);
			}

			@Override
			public void execute() {
				argMapService.addArg(parentProp.id, pro, this);
			}

			@Override
			public void doOnSuccess(Long result) {
				// newProp.id = result.propIDs.get(0);
				newArg.id = result;
			}
		}
		CommandAdd command = new CommandAdd(
				"Server Reports Successful Argument Add");
		command.pro = pro;
		command.parentProp = parentProp;
		command.newArg = newArg;
		// command.newProp = newProp;
		queueCommand(command);
	}

	public static void deleteProp(Proposition prop) {
		class CommandRemove extends ServerCallbackWithDispatch<Void> implements
				Command {
			Proposition prop;

			public CommandRemove(String message) {
				super(message);
			}

			@Override
			public void execute() {
				argMapService.deleteProp(prop.id, this);
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

	public static void deleteArg(Argument arg) {
		class CommandRemove extends ServerCallbackWithDispatch<Void> implements
				Command {
			Argument arg;

			public CommandRemove(String message) {
				super(message);
			}

			@Override
			public void execute() {
				argMapService.deleteArg(arg.id, this);
			}

			@Override
			public void doOnSuccess(Void result) {
			}
		}
		CommandRemove command = new CommandRemove(
				"Server Reports Successful Argument Delete");
		command.arg = arg;
		queueCommand(command);
	}

	public static void unlinkProp(Argument parentArg, Proposition unlinkProp) {
		class CommandUnlink extends ServerCallbackWithDispatch<Void> implements
				Command {
			Proposition unlinkProp;
			Argument parentArg;

			public CommandUnlink(String message) {
				super(message);
			}

			@Override
			public void execute() {
				argMapService.unlinkProp(parentArg.id, unlinkProp.id, this);
			}

			@Override
			public void doOnSuccess(Void result) {
			}
		}
		CommandUnlink command = new CommandUnlink(
				"Server Reports Successful Proposition Unlink");
		command.unlinkProp = unlinkProp;
		command.parentArg = parentArg;
		queueCommand(command);
	}

	public static void updateArg(Argument arg) {
		class CommandUpdate extends ServerCallbackWithDispatch<Void> implements
				Command {
			Argument arg;

			public CommandUpdate(String message) {
				super(message);
			}

			@Override
			public void execute() {
				argMapService.updateArg(arg.id, arg.content, this);
			}

			@Override
			public void doOnSuccess(Void result) {
			}
		}
		CommandUpdate command = new CommandUpdate(
				"Server Reports Successful Argument Update");
		command.arg = arg;

		queueCommand(command);
	}

	public static void updateProp(Proposition prop) {
		class CommandUpdate extends ServerCallbackWithDispatch<Void> implements
				Command {
			Proposition prop;

			public CommandUpdate(String message) {
				super(message);
			}

			@Override
			public void execute() {
				argMapService.updateProp(prop.id, prop.getContent(), this);
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

	public static void addProp(Proposition newProposition,
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
					argMapService.addProp(parentArgument.id, position,
							newProposition.content, this);
				else
					argMapService
							.addProp(null, 0, newProposition.content, this);
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

	public static void replaceWithLinkAndGet(Argument parentArg,
			Proposition linkProp, Proposition removeProp,
			LocalCallback<Nodes> localCallback) {
		class CommandLink extends ServerCallbackWithDispatch<Nodes> implements
				Command {
			Long parentArgID;
			Long linkPropID;
			Long removePropID;
			LocalCallback<Nodes> localCallback;

			public CommandLink(String message) {
				super(message);
			}

			@Override
			public void execute() {
				argMapService.replaceWithLinkAndGet(parentArgID, linkPropID,
						removePropID, this);
			}

			@Override
			public void doOnSuccess(Nodes result) {
				localCallback.call(result);
			}
		}

		CommandLink command = new CommandLink(
				"Server Reports Success Getting Prop Tree");
		command.parentArgID = parentArg.id;
		command.linkPropID = linkProp.id;
		command.removePropID = removeProp.id;
		command.localCallback = localCallback;

		queueCommand(command);
	}
}
