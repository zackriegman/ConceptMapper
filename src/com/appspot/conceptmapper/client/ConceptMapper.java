package com.appspot.conceptmapper.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

// TODO: modify move forward and backward functions so that they work with subtrees
// TODO: implement, geting the subtree with the version info, reverting to current version in the versionsmode, and integrating into the tree
// TODO: fix: can delete an empty proposition in VersionsMode
// TODO at least in IE can delete root level proposition without first deleting its text
// TODO: "loading from server" place holder for nodes with children, and then lazy load
// TODO: provide a way to see deleted top level nodes
// TODO: client should print to System.out or GWT.debug, get ride of extra panel, replace message panel
// TODO: if the message queue gets backed up (more than 5?) give user a message that there is trouble saving changes to server, and changes may be lost
// TODO: implement versioning
// TODO: put the thing in a frame, add helpful message along the side
// TODO: implement proposition search
// TODO: implement linking to existing propositions (i.e. propositions can belong to multiple arguments)
// TODO: maintain order of propositions? waiting to see if order gets screwed up

// TO DO: test in IE, chrome, etc.
// TO DO:  upload to appengine, and add an example argument (for instance my argument about legalizing unauthorized access)
// TO DO: implement user accounts, email updates of changes, inviting friends
// TO DO: poll server every few seconds for server side changes (this has
// to come after versioning I think)
// TO DO: implement some basic database integrity checks (e.g. every argument belongs to an existing proposition...)
// TO DO: lazy load propositions (maybe a few layers deep in advance) instead of loading the entire tree
// TO DO: give people a way to specify logical structure (first order predicate calculus structure of each proposition
// TO DO: add definitions (as a special kind of proposition?)
// TO DO: integrate logic engine to verify validity of arguments

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ConceptMapper implements EntryPoint {

	private HorizontalPanel mainPanel = new HorizontalPanel();
	private VerticalPanel modePanel = new VerticalPanel();
	private HorizontalPanel buttonPanel = new HorizontalPanel();
	private EditMode editMode = new EditMode();
	private VersionsMode versionsMode;
	private static HTML messageArea = new HTML();
	
	private Button seeRevisionsButton = new Button("See Previous Revisions");
	private Button backToEditingButton = new Button("Go Back to Editing");

	public void onModuleLoad() {
		modePanel.add( buttonPanel );
		modePanel.add( editMode );
		versionsMode = new VersionsMode( editMode );
		modePanel.add( versionsMode );
		
		versionsMode.setVisible( false );

		
		seeRevisionsButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				versionsMode.displayVersions();
				editMode.setVisible(false);
				versionsMode.setVisible(true);
				seeRevisionsButton.setVisible(false);
				backToEditingButton.setVisible(true);
			}
		});
		
		backToEditingButton.setVisible(false);
		backToEditingButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				versionsMode.setVisible(false);
				editMode.setVisible(true);
				seeRevisionsButton.setVisible(true);
				backToEditingButton.setVisible(false);
			}
		});

		buttonPanel.add(seeRevisionsButton);
		buttonPanel.add(backToEditingButton);
		
		
		mainPanel.add(modePanel);
		mainPanel.add(messageArea);

		// Associate the Main panel with the HTML host page.
		RootPanel.get("mappingWidget").add(mainPanel);


		// Window.alert( "pops up a window to user with message");

		println("App Begin");
	}

	public String propositionToString(Proposition prop) {
		return "id:" + prop.id + "; content:" + prop.content + "; topLevel:"
				+ prop.topLevel;
	}



	public static void println(String string) {
		print('\n' + string + "<br />");
	}

	public static void print(String string) {
		messageArea.setHTML(string + messageArea.getHTML());
	}



}