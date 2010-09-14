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

	private PropositionServiceAsync propositionService = GWT.create(PropositionService.class);
	
	public void onModuleLoad() {

		tree.addItem(new PropositionView());

		messageLabel.setText("no message");

		// Assemble Main panel.
		mainPanel.add(messageLabel);
		mainPanel.add(tree);

		// Associate the Main panel with the HTML host page.
		RootPanel.get("mappingWidget").add(mainPanel);
		
		
		testAddProposition();
	}
	
	private void testAddProposition(){
		AsyncCallback<Void> addCallback = new AsyncCallback<Void>() {
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
		
		propositionService.addRootProposition( new Proposition("Round Trip w/ Persistence!!"), addCallback );
	}
	
	private  void testGetProposition(){
		AsyncCallback<Proposition[]> getCallback = new AsyncCallback<Proposition[]>() {
			public void onFailure(Throwable caught) {
				// If the stock code is in the list of delisted codes, display
				// an error message.
				String details = caught.getMessage();
				messageLabel.setText("Error: " + details);
				messageLabel.setVisible(true);
			}

			public void onSuccess(Proposition[] result) {
				for( int i = 0; i < result.length; i++ ){
					PropositionView prop = new PropositionView();
					prop.setContent( result[ i ].getContent() );
					tree.addItem( prop );
				}
			}
		};

		// Make the call to the stock price service.
		propositionService.getRootPropositions( getCallback );
	}
}