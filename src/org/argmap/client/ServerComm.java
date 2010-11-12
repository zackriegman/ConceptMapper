package org.argmap.client;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.DateAndChildIDs;
import org.argmap.client.ArgMapService.ForwardChanges;
import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodeInfo;
import org.argmap.client.ArgMapService.NodeWithChanges;
import org.argmap.client.ArgMapService.NodesAndNode;
import org.argmap.client.ArgMapService.PartialTrees;
import org.argmap.client.ArgMapService.PartialTrees_DELETE_ME;

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

		public abstract void doOnSuccess(T result);
	}

	public static void getRootProps(int depthLimit,
			LocalCallback<PartialTrees_DELETE_ME> localCallback) {
		argMapService.getRootProps(depthLimit,
				new ServerCallback<PartialTrees_DELETE_ME>(localCallback,
						"loading...", "finished loading"));
	}

	public static void getNodesChildren(List<Long> nodeIDs, int depth,
			LocalCallback<Map<Long, Node>> localCallback) {
		argMapService.getNodesChildren(nodeIDs, depth,
				new ServerCallback<Map<Long, Node>>(localCallback,
						"pre-loading...", "finished pre-loading"));
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

	public static void getNewChanges_DELETE_ME(Date date, Set<Long> propIDs,
			Set<Long> argIDs, LocalCallback<ForwardChanges> localCallback) {
		argMapService.getNewChanges_DELETE_ME(date, propIDs, argIDs,
				new ServerCallback<ArgMapService.ForwardChanges>(localCallback,
						"refreshing...", "refreshed"));
	}

	public static void getUpdates(Map<Long, DateAndChildIDs> propsInfo,
			Map<Long, DateAndChildIDs> argsInfo,
			LocalCallback<PartialTrees> localCallback) {
		argMapService.getUpToDateNodes(propsInfo, argsInfo,
				new ServerCallback<PartialTrees>(localCallback,
						"refreshing...", "refreshed"));
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
			LocalCallback<PartialTrees_DELETE_ME> localCallback) {
		argMapService.searchProps(searchString, searchName, resultLimit,
				filterNodeIDs, new ServerCallback<PartialTrees_DELETE_ME>(
						localCallback, null, null));
	}

	public static void continueSearchProps(String searchName,
			LocalCallback<PartialTrees_DELETE_ME> localCallback) {
		argMapService.continueSearchProps(searchName,
				new ServerCallback<PartialTrees_DELETE_ME>(localCallback, null,
						null));
	}
	
	
	public static interface LocalDispatchCallback{
		public Node getNode();
		public void updateNode( Map<Long, Node> nodes );
	}
	
	public static interface LocalDispatchAddCallback extends LocalDispatchCallback {
		public void setAddedNode( Node node );
	}

	public static void addArg(final boolean pro,
			final LocalDispatchAddCallback localCallback) {
		queueCommand(new ServerCallbackWithDispatch<NodesAndNode>("saving...",
				"saved") {
			@Override
			public void execute() {
				argMapService.addArg(new NodeInfo(localCallback.getNode()), pro, this);
			}

			@Override
			public void doOnSuccess(NodesAndNode result) {
				localCallback.setAddedNode(result.node);
				localCallback.updateNode(result.nodes);
			}
		});
	}

	public static void deleteProp(final LocalDispatchCallback localCallback) {
		queueCommand(new ServerCallbackWithDispatch<Map<Long, Node>>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.deleteProp(localCallback.getNode().id, this);
			}
			
			@Override
			public void doOnSuccess(Map<Long, Node> nodes) {
				localCallback.updateNode( nodes );
			}
		});
	}

	public static void deleteArg(final LocalDispatchCallback localCallback) {
		queueCommand(new ServerCallbackWithDispatch<Map<Long, Node>>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.deleteArg(localCallback.getNode().id, this);
			}
			
			@Override
			public void doOnSuccess(Map<Long, Node> nodes){
				localCallback.updateNode( nodes );
			}
		});
	}
	
	public static interface LocalDispatchUnlinkCallback extends LocalDispatchCallback{
		public Node getChildNode();
		public void updateChildNode( Map<Long, Node> nodes );
	}

	public static void unlinkProp(final LocalDispatchUnlinkCallback localCallback) {
		queueCommand(new ServerCallbackWithDispatch<Map<Long, Node>>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.unlinkProp(new NodeInfo(localCallback.getNode()),
						new NodeInfo(localCallback.getChildNode()), this);
			}
			
			@Override
			public void doOnSuccess(Map<Long, Node> nodes){
				localCallback.updateNode( nodes );
				localCallback.updateChildNode( nodes );
			}
		});
	}
	
	

	public static void updateArg(final LocalDispatchCallback localCallback, final String content) {
		queueCommand(new ServerCallbackWithDispatch<Map<Long, Node>>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.updateArg(new NodeInfo(localCallback.getNode()), content, this);
			}
			
			@Override
			public void doOnSuccess(Map<Long, Node> nodes ){
				localCallback.updateNode( nodes );
			}
		});
	}

	public static void updateProp(final LocalDispatchCallback localCallback, final String content) {
		queueCommand(new ServerCallbackWithDispatch<Map<Long, Node>>("saving...", "saved") {
			@Override
			public void execute() {
				argMapService.updateProp(new NodeInfo(localCallback.getNode()), content, this);
			}
			
			@Override
			public void doOnSuccess(Map<Long, Node> nodes ){
				localCallback.updateNode( nodes );
			}
		});
	}
	
	public static void addProp(final LocalDispatchAddCallback localCallback, final int position, final String content) {
		queueCommand(new ServerCallbackWithDispatch<NodesAndNode>("saving...",
				"saved") {
			@Override
			public void execute() {
				if (localCallback.getNode() != null)
					argMapService.addProp(new NodeInfo(localCallback.getNode()), position,
							content, this);
				else
					argMapService
							.addProp(null, 0, content, this);
			}

			@Override
			public void doOnSuccess(NodesAndNode result) {
				localCallback.setAddedNode(result.node);
				localCallback.updateNode(result.nodes);
			}
		});
	}
	
	public interface LocalReplaceWithLinkCallback {
		public Node getParentNode();
		public Node getChildToRemove();
		public Node getLinkChild();
		public void updateParentNode( Map<Long, Node> nodes );
		public void updateLinkedNode( Map<Long, Node> nodes );
	}

	public static void replaceWithLinkAndGet(final LocalReplaceWithLinkCallback localCallback) {
		queueCommand(new ServerCallbackWithDispatch<Map<Long, Node>>(
				"saving...", "saved") {
			@Override
			public void execute() {
				argMapService.replaceWithLinkAndGet(new NodeInfo(localCallback.getParentNode()), new NodeInfo(localCallback.getLinkChild()),
						localCallback.getChildToRemove().id, this);
			}

			@Override
			public void doOnSuccess(Map<Long, Node> nodes) {
				localCallback.updateParentNode( nodes );
				localCallback.updateLinkedNode( nodes );
			}
		});
	}
}
