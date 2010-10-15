package org.argmap.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;

//TODO: handle open and close nodes in VersionsMode:  when opening, need to zoom node to same time as rest of tree
//TODO: undeleting links does not restory their yellow color
//TODO: unlinking last prop in an arg deletes the arg on the server but not on the client, resulting in strange behavor
//TODO: weed changes list when a node is closed, expand when a node is opened (maybe to implement this, change how nodes are stored... maybe with a list of all their changes... which type of changes does a node need to know about...
//TODO: lazy load closed items in versions mode
//TODO: prevent circular linking from crashing program...

//TODO: lazy load in editmode (maybe a few layers deep in advance) instead of loading the entire tree
//TODO: provide a way to see deleted top level nodes
//TODO: add helpful message along the side (tips box)
//TODO: poll server every few seconds for server side changes (this has to come after versioning I think)
//TODO: implement user accounts, email updates of changes, inviting friends
//TODO: implement proposition strength voting, and scoring algorithm

//TODO: highlight last change node after time travel
//TODO: think about merging of duplicate propositions (both possibly already having arguments)
//TODO: fine tune UI for arguments... see implementation notes
//TODO: what is this?:   get rid of irrelevant update operations (e.g. clearing update before delete)
//TODO: still an issue?: what are all the phantom propositions that show up on an empty search?
//TODO: decide how to get rid of repetative code (for propositions and args), maybe make them both subclasses of a Node class?
//TODO: if the message queue gets backed up (more than 5?) give user a message that there is trouble saving changes to server, and changes may be lost
//TODO: maintain order of propositions? waiting to see if order gets screwed up
//TODO: IE problems: (1) VersionsMode doesn't seem to work (2) can delete root level proposition without first deleting its text

// TO DO: test in IE, chrome, safari, opera, etc.
// TO DO: upload to appengine, and add an example argument (for instance my argument about legalizing unauthorized access)
// TO DO: implement some basic database integrity checks (e.g. every argument belongs to an existing proposition...)
// TO DO: give people a way to specify logical structure (first order predicate calculus structure of each proposition
// TO DO: add definitions (as a special kind of proposition?)
// TO DO: integrate logic engine to verify validity of arguments

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ArgMap implements EntryPoint {

	private DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.EM);
	private TabLayoutPanel modePanel = new TabLayoutPanel(1.5, Style.Unit.EM);

	private static HTML messageArea = new HTML();
	private EditMode editMode = new EditMode();
	private VersionsMode versionsMode;
	private static Map<String, StringBuilder> logs = new HashMap<String, StringBuilder>();

	public void onModuleLoad() {
		modePanel.add(editMode, "Edit");
		versionsMode = new VersionsMode(editMode);
		modePanel.add(versionsMode, "Versions");

		modePanel.addSelectionHandler(new SelectionHandler<Integer>() {

			@Override
			public void onSelection(SelectionEvent<Integer> event) {
				if (modePanel.getSelectedIndex() == 1) {
					versionsMode.displayVersions();
				}

			}
		});

		Label titleLabel = new Label("ArgMap Prototype");
		titleLabel.setStylePrimaryName("titleLabel");
		// messageArea.setStylePrimaryName("messageLabel");
		mainPanel.addNorth(titleLabel, 5);
		mainPanel.addNorth(messageArea, 2);
		mainPanel.add(modePanel);

		RootLayoutPanel rp = RootLayoutPanel.get();
		rp.add(mainPanel);

		// Window.alert( "pops up a window to user with message");

		message("App Begin");
	}

	public static void message(String string) {
		messageArea.setHTML("<div class=\"messageLabel\">" + string + "</div>");
	}

	public static String spaces(int spaces) {
		String string = "";
		for (int i = 0; i < spaces; i++) {
			string = string + " ";
		}
		return string;
	}

	public static void logNull(String string, Object... vars) {
		string += ": ";
		for (int i = 0; i < vars.length; i++) {
			if (vars[i] == null) {
				string += i + " ";
			}
		}
		GWT.log(string);
	}

	/*
	 * at some point I might want to modify this to accept a boolean indicating
	 * whether to print the log or not so that I can easily flip the particular
	 * log on or off depending on whether I need it...
	 */
	public static void logStart(String logName) {
		if (logName == null)
			return;
		/*
		 * asserts that logStart is only called once per logName before calling
		 * logEnd otherwise log messages could be lost... or mixed with
		 * irrelevant logs
		 */
		assert (logs.get(logName) == null);
		StringBuilder log = new StringBuilder();
		log.append(logName + ": ");
		logs.put(logName, log);

	}

	public static void logEnd(String logName) {
		if (logName == null)
			return;
		GWT.log(logs.remove(logName).toString());
	}

	public static void log(String logName, String string) {
		if (logName == null)
			return;
		logs.get(logName).append(string);
	}

	public static void logln(String logName, String string) {
		if (logName == null)
			return;
		log(logName, "\n" + string);
	}
}