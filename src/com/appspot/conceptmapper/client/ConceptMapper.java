package com.appspot.conceptmapper.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;

//TODO: implement versioning of links/unlinks and test
//TODO: if you have two links to a node open in VersionsMode only one of them gets updated as you travel through the tree
//TODO: weed changes list when a node is closed, expand when a node is opened (maybe to implement this, change how nodes are stored... maybe with a list of all their changes... which type of changes does a node need to know about...
//TODO: lazy load propositions (maybe a few layers deep in advance) instead of loading the entire tree
//TODO: what are all the phantom propositions that show up on an empty search?

//TODO: think about merging of duplicate propositions (both possibly already having arguments)
//TODO: add helpful message along the side (tips box)
//TODO: provide a way to see deleted top level nodes
//TODO: implement user accounts, email updates of changes, inviting friends
//TODO: implement proposition strength voting, and scoring algorithm
//TODO: allow arguments to have titles

//TODO: trim whitespace before checking to see if update has actually changed something
//TODO: highlight last change node after time travel
//TODO: get rid of irrelevant update operations (e.g. clearing update before delete)
//TODO: decide how to get rid of repetative code (for propositions and args), maybe make them both subclasses of a Node class?

//TODO: if the message queue gets backed up (more than 5?) give user a message that there is trouble saving changes to server, and changes may be lost
//TODO: maintain order of propositions? waiting to see if order gets screwed up
//TODO: IE problems: (1) VersionsMode doesn't seem to work (2) can delete root level proposition without first deleting its text

// TO DO: test in IE, chrome, safari, opera, etc.
// TO DO: upload to appengine, and add an example argument (for instance my argument about legalizing unauthorized access)
// TO DO: poll server every few seconds for server side changes (this has to come after versioning I think)
// TO DO: implement some basic database integrity checks (e.g. every argument belongs to an existing proposition...)
// TO DO: give people a way to specify logical structure (first order predicate calculus structure of each proposition
// TO DO: add definitions (as a special kind of proposition?)
// TO DO: integrate logic engine to verify validity of arguments

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ConceptMapper implements EntryPoint {

	private DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.EM);
	private TabLayoutPanel modePanel = new TabLayoutPanel(1.5, Style.Unit.EM);

	private static HTML messageArea = new HTML();
	private EditMode editMode = new EditMode();
	private VersionsMode versionsMode;
	


	public void onModuleLoad() {
		modePanel.add( editMode, "Edit" );
		versionsMode = new VersionsMode( editMode );
		modePanel.add( versionsMode, "Versions" );
		
		modePanel.addSelectionHandler(new SelectionHandler<Integer>() {
			
			@Override
			public void onSelection(SelectionEvent<Integer> event) {
				if( modePanel.getSelectedIndex() == 1 ){
					versionsMode.displayVersions();
				}
				
			}
		});
		

		Label titleLabel = new Label("ConceptMapper Prototype");
		titleLabel.setStylePrimaryName("titleLabel");
		//messageArea.setStylePrimaryName("messageLabel");
		mainPanel.addNorth( titleLabel, 5);
		mainPanel.addNorth( messageArea, 2);
		mainPanel.add( modePanel );
		
		
		
		RootLayoutPanel rp = RootLayoutPanel.get();
	    rp.add(mainPanel);

		// Window.alert( "pops up a window to user with message");

		message("App Begin");
	}
	
	public static void message( String string ){
		messageArea.setHTML("<div class=\"messageLabel\">" + string + "</div>");
	}
	
	public static String spaces(int spaces) {
		String string = "";
		for (int i = 0; i < spaces; i++) {
			string = string + " ";
		}
		return string;
	}
}