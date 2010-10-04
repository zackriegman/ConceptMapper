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

/*
 * TODO ok, so the main approach i'm thinking of now is that the server
 * will be able to return a prop or arg as it existed (or didn't) at any
 * time. The returned prop (or arg) will include references to it's
 * children, which the client can build at with stubs saying
 * "loading from server", and when the prop or arg is opened the client
 * can send a request to the server, asking for the children at that
 * time (i.e. lazy loading the tree at the time). Of course, to make
 * that more seamless to the user, the server can send not only the
 * proposition but its children too, or perhaps even its children's
 * children (etc.) with each request.
 * 
 * The issue however is how to represent that to the user in terms of an
 * easily digestible change list. We want to show the user the changes
 * relevant to the part of the tree that he is viewing. As he opens more
 * sublevels however, more changes become apparent. We could update the
 * change list as sub levels are opened to reflect the changes that can
 * now been seen in the sublevels. However, what about changes that add
 * or delete nodes. As you move through the history those nodes will
 * appear and dispear. When one disapears do you remove the changes from
 * the changeList that had to do with that node? Probably not, that
 * would be disorienting. Instead you have to keep track of whether a
 * user opened or closed a node the last time he saw it... thats getting
 * too complicated
 * 
 * An alternative would be to show the user all the changes for the top
 * level proposition that he is currently navigating from, even for
 * nodes that are not visible on the screen and/or that are currently
 * closed, or even currently deleted, etc. That might be disorienting
 * however, because as the user clicks through the change list, he'll
 * see changes that dont' seem to change anything on screen.
 * 
 * I think the ideal solution might be the complicated one, but maybe it
 * doesn't make sense to try to implement the ideal.
 * 
 * maybe a relatively simple, perhaps temporary, until I can think of
 * something better, is to show changes for things that are open in the
 * edit screen, and open in the version screen. As someone opens or
 * closes a change in the version screen, the version list is updated,
 * to show just the versions for those nodes showing.
 * 
 * As they walk through the version list, when new nodes are added, they
 * are added automatically in an open position. And there changes are
 * immediately added.
 * 
 * Better yet, all the changes for the currently opened nodes are added
 * at the beginning including changes to any descendants of open nodes,
 * except those descendants that are all ready closed.
 * 
 * So lets walk through this plan. We have a tree with some open nodes
 * and some closed nodes in the edit window. The user clicks on versions
 * and goes to the version window where they see that tree, as it exists
 * in the edit window.
 * 
 * In the versions box they see all the versions for the currently
 * opened nodes. As they move back in time, nodes disappear at times
 * preceding their creation. The versions box does not change however to
 * reflect the disappearance of nodes unless a node is closed. Closing a
 * node will remove versions from the version list and opening a node
 * will add versions to the version list. But moving through time never
 * changes the version list by itself. Lets say you move back past a
 * deletion, and as a consequence a new node pops up. Should the node
 * pop up as opened (implying that it's changes are already part of the
 * change list) or should it pop up as closed (meaning the changes are
 * not part of the change list until it is explicitly opened). Either
 * way, we don't need to remember it's state if we pop back to the
 * beginning of the list before it existed, because the change list
 * itself remembers its state. I think as you move backwards in time
 * deletions should start visible, which means that you need to fetch
 * all the deleted descendants of a node when you fetch an opened node.
 * 
 * Steps to implementation: 1. fetch deleted descendants when fetching
 * an open node 2. add place holders to closed nodes, and handle
 * open/close events to automatically load children 3. add changes to
 * the version list when a node is opened 4. remove changes from the
 * version list when a node is closed
 */


//TODO: implement linking to existing propositions (i.e. propositions can belong to multiple arguments)
//TODO: weed changes list when a node is closed, expand when a node is opened (maybe to implement this, change how nodes are stored... maybe with a list of all their changes... which type of changes does a node need to know about...
//TODO: lazy load propositions (maybe a few layers deep in advance) instead of loading the entire tree

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
		modePanel.add( new ScrollPanel( versionsMode ), "Versions" );
		
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

	public String propositionToString(Proposition prop) {
		return "id:" + prop.id + "; content:" + prop.content + "; topLevel:"
				+ prop.topLevel;
	}
}