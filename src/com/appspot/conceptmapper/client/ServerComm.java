package com.appspot.conceptmapper.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

public class ServerComm {
	private static PropositionServiceAsync propositionService = GWT
			.create(PropositionService.class);
	private static Label message;

	public static void init(Label label) {
		message = label;
		//TODO: comment out test function
		test();
	}

	private static void message(String string) {
		message.setText(string);
		message.setVisible(true);
	}

	private static void test() {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				message("Error: " + details);
			}

			@Override
			public void onSuccess(Void result) {
				message("Server Reports Success.");
			}
		};

		propositionService.test(callback);
	}
	
	public interface FetchPropsCallback {
		public void call( Proposition[] props);
	}
	
	public static void fetchProps( FetchPropsCallback fetchCallback ){
		class ThisCallback implements AsyncCallback<Proposition[]> {
			public FetchPropsCallback fetch;
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				message("Error: " + details);
			}

			@Override
			public void onSuccess(Proposition[] result) {
				message("Server Reports Success Fetching Props");
				fetch.call( result );
			}
		};
		
		ThisCallback callback = new ThisCallback();
		callback.fetch = fetchCallback;

		propositionService.getAllProps( callback );
	}

	public static void addArgument(boolean pro, Proposition parentProp,
			Argument newArg, Proposition newProp) {
		class AddCallback implements AsyncCallback<Argument> {
			Proposition newProposition;
			Argument newArgument;

			@Override
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				message("Error: " + details);
			}

			@Override
			public void onSuccess(Argument result) {
				message("Server Reports Successful Argument Add");
				newProposition.id = result.props.get(0).id;
				newArgument.id = result.id;
			}
		}

		AddCallback addCallback = new AddCallback();
		addCallback.newProposition = newProp;
		addCallback.newArgument = newArg;

		propositionService.addArgument(parentProp.id, pro, addCallback);
	}

	public static void removeProposition(Proposition prop) {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				message("Error: " + caught.getMessage());
			}

			@Override
			public void onSuccess(Void result) {
				message("Server Reports Successful Proposition Delete");
			}
		};

		propositionService.removeProposition(prop.id, callback);
	}
	
	public static void updateProposition( Proposition prop ){
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				message("Error: " + caught.getMessage());
			}

			@Override
			public void onSuccess(Void result) {
				message("Server Reports Successful Proposition Update");
			}
		};

		propositionService.updateProposition(prop.id, prop.getContent(), callback);
	}

	public static void addProposition(Proposition newProposition,
			Argument parentArgument, int position) {
		// TODO: notice that with this approach if someone adds and then deletes
		// a proposition, the delete could conceivably return first, with an
		// error because it could not find the new proposition, and the add
		// could return second, never to be deleted. Perhaps it would be better
		// to implement a queue and only dispatch events after the previous one
		// succeeded. this would keep things consistent. If the queue grew too
		// long (say more than 5 or six requests) the user could be notified
		// that the there was trouble contacting the server and they should
		// refresh the page to avoid losing work.

		class AddCallback implements AsyncCallback<Long> {
			Proposition newProposition;

			@Override
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				message("Error: " + details);

			}

			@Override
			public void onSuccess(Long result) {
				message("Server Reports Successful Proposition Add");
				newProposition.id = result;

			}
		}

		AddCallback addCallback = new AddCallback();
		addCallback.newProposition = newProposition;

		propositionService.addProposition(parentArgument.id, position,
				addCallback);
	}
	
	private void testMakePropChanges() {
		// create an argument hiearchy
		Proposition rootProp = new Proposition("rootProp");
		rootProp.topLevel = true;
		Proposition topProp;
		Proposition subProp;
		Argument topArg = new Argument();
		rootProp.args.add(topArg);
		Argument subArg;
		for (int i = 0; i < 3; i++) {
			topProp = new Proposition("topProp" + i);
			topProp.topLevel = false;
			rootProp.args.get(0).props.add(topProp);
			for (int j = 0; j < 3; j++) {
				subArg = new Argument();
				topProp.args.add(subArg);
				subProp = new Proposition("subProp" + j);
				subProp.topLevel = false;
				subArg.props.add(subProp);
			}
		}

		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				message("Error: " + caught.getMessage());
			}

			@Override
			public void onSuccess(Void result) {
				message("Server Reports Successfull Make Changes.");
			}
		};
		Proposition[] newProps = new Proposition[1];
		newProps[0]= rootProp;
		propositionService.makePropChanges(newProps, null, null, null, null,
				callback);
	}
	
	/*
	private void testGetProposition() {
		AsyncCallback<Proposition[]> callback = new AsyncCallback<Proposition[]>() {
			public void onFailure(Throwable caught) {
				message("Error: " + caught.getMessage());
			}

			public void onSuccess(Proposition[] result) {
				for (int i = 0; i < result.length; i++) {
					tree.addItem(new PropositionView(result[i], null));
				}
			}
		};

		// Make the call to the stock price service.
		propositionService.getRootPropositions(callback);
	}
	*/
}
