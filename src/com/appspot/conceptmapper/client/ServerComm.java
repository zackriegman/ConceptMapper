package com.appspot.conceptmapper.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

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
				String details = caught.getMessage();
				message("Error: " + details);
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
	
	public static void addArgument(boolean pro, Proposition parentProp,
			Argument newArg, Proposition newProp) {
		class CommandAdd implements Command {
			boolean pro;
			Proposition parentProp;
			Argument newArg;
			Proposition newProp;
			
			@Override
			public void execute() {
				addArgumentCmd( pro, parentProp, newArg, newProp );
			}
		}
		CommandAdd command = new CommandAdd();
		command.pro = pro;
		command.parentProp = parentProp;
		command.newArg = newArg;
		command.newProp = newProp;
		queueCommand( command );
	}

	public static void addArgumentCmd(boolean pro, Proposition parentProp,
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
		class CommandRemove implements Command{
			Proposition prop;

			@Override
			public void execute() {
				removePropositionCmd( prop );
			}
		}
		CommandRemove command = new CommandRemove();
		command.prop = prop;
		queueCommand(command);
	}

	public static void removePropositionCmd(Proposition prop) {
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
				updatePropositionCmd( prop );
			}
		}
		CommandUpdate command = new CommandUpdate();
		command.prop = prop;
		
		queueCommand( command );
	}

	public static void updatePropositionCmd(Proposition prop) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				message("Error: " + caught.getMessage());
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
				addPropositionCmd( newProposition, parentArgument, position);
			}
			
		}
		
		CommandAdd command = new CommandAdd();
		command.newProposition = newProposition;
		command.parentArgument = parentArgument;
		command.position = position;
		
		queueCommand( command );
	}

	public static void addPropositionCmd(Proposition newProposition,
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
