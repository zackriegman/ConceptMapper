package org.argmap.client;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.ForwardChanges;
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
		ArgMap.Message message;
		String successMessage;
		String startMessage;

		public ServerCallback(LocalCallback<T> localCallback,
				String startMessage, String successMessage) {
			this.localCallback = localCallback;
			this.successMessage = successMessage;
			this.startMessage = startMessage;
			message = ArgMap.getMessage();
			messageForStart(startMessage, message);
		}

		@Override
		public final void onFailure(Throwable caught) {
			messageForFailure(startMessage, message);
		}

		@Override
		public final void onSuccess(T result) {

			if (localCallback != null) {
				localCallback.call(result);
			}
			messageForSuccess(successMessage, message);
		}
	}

	private static void messageForStart(String startMessage,
			ArgMap.Message message) {
		if (startMessage != null) {
			message.setMessage(startMessage, MessageType.INFO);
			message.display();
		}
	}

	private static void messageForFailure(String startMessage,
			ArgMap.Message message) {
		if (startMessage != null) {
			message.setMessage("Server error while " + startMessage,
					MessageType.ERROR);
		} else {
			message.setMessage("Server error", MessageType.ERROR);
		}
		message.display();
		message.hideAfter(15000);
		// Window.alert("Exception: " + caught.toString());
	}

	private static void messageForSuccess(String successMessage,
			ArgMap.Message message) {
		if (successMessage != null) {
			message.setMessage(successMessage);
			message.display();
			message.hideAfter(1000);
		} else {
			message.hide();
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
			AsyncCallback<T>, Command {
		ArgMap.Message message;
		String successMessage;
		String startMessage;

		public ServerCallbackWithDispatch(String startMessage,
				String successMessage) {
			this.startMessage = startMessage;
			this.successMessage = successMessage;
			message = ArgMap.getMessage();
			messageForStart(startMessage, message);
		}

		@Override
		public final void onFailure(Throwable caught) {
			dispatchCommand();
			messageForFailure(startMessage, message);
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
			messageForSuccess(successMessage, message);
		}

		public void doOnSuccess(T result) {
		};
	}

	// private static abstract class ServerCallbackWithDispatch<T> implements
	// AsyncCallback<T>, Command {
	// String message;
	//
	// public ServerCallbackWithDispatch(String successMessage) {
	// this.message = successMessage;
	// }
	//
	// @Override
	// public final void onFailure(Throwable caught) {
	// dispatchCommand();
	// handleFailure(message, caught);
	// }
	//
	// @Override
	// public final void onSuccess(T result) {
	//
	// /*
	// * this must come before dispatchCommand() otherwise the client
	// * might send a request to add a proposition to an argument before
	// * the argument has been assigned an id by the return call from the
	// * server...
	// */
	// doOnSuccess(result);
	// dispatchCommand();
	// if (message != null) {
	// handleSuccess(message);
	// }
	//
	// }
	//
	// public void doOnSuccess(T result) {
	// };
	// }

	public static void getRootProps(int depthLimit,
			LocalCallback<PropsAndArgs> localCallback) {
		argMapService.getPropsAndArgs(depthLimit,
				new ServerCallback<PropsAndArgs>(localCallback, "loading...",
						"finished loading"));
	}

	public static void getNodesChildren(List<Long> nodeIDs, int depth,
			LocalCallback<Nodes> localCallback) {
		argMapService.getNodesChildren(nodeIDs, depth,
				new ServerCallback<Nodes>(localCallback, "pre-loading...",
						"finished pre-loading"));
	}

	public static void getLoginInfo(LocalCallback<LoginInfo> localCallback) {
		String requestURI;
		if (GWT.isProdMode()) {
			requestURI = GWT.getHostPageBaseURL();
		} else {
			requestURI = "http://127.0.0.1:8888/ArgMap.html?gwt.codesvr=127.0.0.1:9997";
		}
		argMapService.getLoginInfo(requestURI, new ServerCallback<LoginInfo>(
				localCallback, "authenticating...", null));
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
						"loading history...", "history loaded"));
	}

	public static void getPropsWithChanges(List<Long> propIDs,
			LocalCallback<Map<Long, NodeWithChanges>> localCallback) {
		argMapService.getPropsWithChanges(propIDs,
				new ServerCallback<Map<Long, NodeWithChanges>>(localCallback,
						"loading history...", "history loaded"));
	}

	public static void getArgsWithChanges(List<Long> argIDs,
			LocalCallback<Map<Long, NodeWithChanges>> localCallback) {
		argMapService.getArgsWithChanges(argIDs,
				new ServerCallback<Map<Long, NodeWithChanges>>(localCallback,
						"loading history...", "history loaded"));
	}

	public static void searchProps(String searchString, String searchName,
			int resultLimit, List<Long> filterNodeIDs,
			LocalCallback<PropsAndArgs> localCallback) {
		argMapService.searchProps(searchString, searchName, resultLimit,
				filterNodeIDs, new ServerCallback<PropsAndArgs>(localCallback,
						null, null));
	}

	public static void continueSearchProps(String searchName,
			LocalCallback<PropsAndArgs> localCallback) {
		argMapService.continueSearchProps(searchName,
				new ServerCallback<PropsAndArgs>(localCallback, null, null));
	}

	public static void addArg(final boolean pro, final Proposition parentProp,
			final Argument newArg) {
		queueCommand(new ServerCallbackWithDispatch<Long>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.addArg(parentProp.id, pro, this);
			}

			@Override
			public void doOnSuccess(Long result) {
				newArg.id = result;
			}
		});
	}

	public static void deleteProp(final Proposition prop) {
		queueCommand(new ServerCallbackWithDispatch<Void>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.deleteProp(prop.id, this);
			}
		});
	}

	public static void deleteArg(final Argument arg) {
		queueCommand(new ServerCallbackWithDispatch<Void>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.deleteArg(arg.id, this);
			}
		});
	}

	public static void unlinkProp(final Argument parentArg,
			final Proposition unlinkProp) {
		queueCommand(new ServerCallbackWithDispatch<Void>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.unlinkProp(parentArg.id, unlinkProp.id, this);
			}
		});
	}

	public static void updateArg(final Argument arg) {
		queueCommand(new ServerCallbackWithDispatch<Void>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.updateArg(arg.id, arg.content, this);
			}
		});
	}

	public static void updateProp(final Proposition prop) {
		queueCommand(new ServerCallbackWithDispatch<Void>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.updateProp(prop.id, prop.getContent(), this);
			}
		});
	}

	public static void addProp(final Proposition newProposition,
			final Argument parentArgument, final int position) {
		queueCommand(new ServerCallbackWithDispatch<Long>("saving...", "saved") {
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
		});
	}

	public static void replaceWithLinkAndGet(final Argument parentArg,
			final Proposition linkProp, final Proposition removeProp,
			final LocalCallback<Nodes> localCallback) {
		queueCommand(new ServerCallbackWithDispatch<Nodes>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.replaceWithLinkAndGet(parentArg.id, linkProp.id,
						removeProp.id, this);
			}

			@Override
			public void doOnSuccess(Nodes result) {
				localCallback.call(result);
			}
		});
	}

	public static void getNewChanges(Date date, Set<Long> propIDs,
			Set<Long> argIDs, LocalCallback<ForwardChanges> localCallback) {
		argMapService.getNewChanges(date, propIDs, argIDs,
				new ServerCallback<ArgMapService.ForwardChanges>(localCallback, "refreshing...", "refreshed") );
	}
}
