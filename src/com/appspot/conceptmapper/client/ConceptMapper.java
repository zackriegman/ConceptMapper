package com.appspot.conceptmapper.client;


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ConceptMapper implements EntryPoint {

	private VerticalPanel mainPanel = new VerticalPanel();
	private Tree tree = new Tree();
	private Label messageLabel = new Label();

	private PropositionServiceAsync propositionService = GWT
			.create(PropositionService.class);

	public void onModuleLoad() {
		// TODO: make server persistent data update according to local changes
		// TODO: implement breaking and merging proposition content by adding
		// and removing newlines

		tree.addItem(new PropositionView());

		messageLabel.setText("no message");

		// Assemble Main panel.
		mainPanel.add(messageLabel);
		mainPanel.add(tree);

		// Associate the Main panel with the HTML host page.
		RootPanel.get("mappingWidget").add(mainPanel);

		messageLabel.setText( "testingMakePropChanges()");
		//testMakePropChanges();
		// testAddProposition();
		// testDeleteProposition();
		fetchPropsAndBuildTree();
	}
	
	private void fetchPropsAndBuildTree(){
		AsyncCallback<Proposition[]> callback = new AsyncCallback<Proposition[]>() {
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				messageLabel.setText("Error: " + details);
				messageLabel.setVisible(true);
			}

			@Override
			public void onSuccess(Proposition[] result) {
				messageLabel.setText("Server Reports Success.");
				for( Proposition prop : result )
					tree.addItem( recursiveBuildPropositionView( prop ) );
			}
		};

		propositionService.getAllProps( callback );
	}
	
	private PropositionView recursiveBuildPropositionView( Proposition prop ){
		PropositionView propView = new PropositionView( prop );
		for( Argument arg : prop.args){
			propView.addItem( recursiveBuildArgumentView( arg ) );
		}
		return propView;
	}
	
	private ArgumentView recursiveBuildArgumentView( Argument arg ){
		ArgumentView argView = new ArgumentView( arg );
		for( Proposition prop : arg.props ){
			argView.addItem( recursiveBuildPropositionView( prop ));
		}
		return argView;
	}

	private void testMakePropChanges() {
		// create an argument hiearchy
		Proposition rootProp = new Proposition("rootProp");
		Proposition topProp;
		Proposition subProp;
		Argument topArg = new Argument();
		rootProp.args.add(topArg);
		Argument subArg;
		for (int i = 0; i < 3; i++) {
			topProp = new Proposition("topProp" + i);
			rootProp.args.get(0).props.add(topProp);
			for (int j = 0; j < 3; j++) {
				subArg = new Argument();
				topProp.args.add(subArg);
				subProp = new Proposition("subProp" + j);
				subArg.props.add(subProp);
			}
		}

		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				String details = caught.getMessage();
				messageLabel.setText("Error: " + details);
				messageLabel.setVisible(true);
			}

			@Override
			public void onSuccess(Void result) {
				messageLabel.setText("Server Reports Success.");
			}
		};
		Proposition[] newProps = new Proposition[1];
		newProps[0]= rootProp;
		propositionService.makePropChanges(newProps, null, null, null, null,
				callback);
	}

	private void testDeleteProposition() {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				// If the stock code is in the list of delisted codes, display
				// an error message.
				String details = caught.getMessage();
				messageLabel.setText("Error: " + details);
				messageLabel.setVisible(true);
			}

			@Override
			public void onSuccess(Void result) {
				messageLabel.setText("Deleted Proposition");
			}
		};

		propositionService.deleteProposition(
				((PropositionView) tree.getItem(1)).getProposition(), callback);
	}

	private void testAddProposition() {
		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				// If the stock code is in the list of delisted codes, display
				// an error message.
				String details = caught.getMessage();
				messageLabel.setText("Error: " + details);
				messageLabel.setVisible(true);
			}

			@Override
			public void onSuccess(Void result) {
				testGetProposition();
			}
		};

		propositionService.addRootProposition(new Proposition(
				"Round Trip w/ Persistence!!"), callback);
	}

	private void testGetProposition() {
		AsyncCallback<Proposition[]> callback = new AsyncCallback<Proposition[]>() {
			public void onFailure(Throwable caught) {
				// If the stock code is in the list of delisted codes, display
				// an error message.
				String details = caught.getMessage();
				messageLabel.setText("Error: " + details);
				messageLabel.setVisible(true);
			}

			public void onSuccess(Proposition[] result) {
				for (int i = 0; i < result.length; i++) {
					tree.addItem(new PropositionView(result[i]));
				}
			}
		};

		// Make the call to the stock price service.
		propositionService.getRootPropositions(callback);
	}
}