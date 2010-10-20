package org.argmap.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;

//TODO: onopen if not loaded yet, load the node, zoom it, append it...

//TODO: deployed app has problems... nodes disapear and errors when adding and deleting nodes

/*TODO: if you close a node in versions mode sometimes you can never open it again because it will 
 * never have children again given the dates available to click on.  For instance if you add a node, 
 * add a child, delete the child, and delete the node, all consecutively, and then go to a time when 
 * the node has children and close it, the node will never have children again because it doesn't have
 * children when it is created or when it is destroyed... and there are no visible events in between...
 * this should be a low priority fix, but it might take some ui creativity...
 */

//TODO: look at backwards/forwards of linking/unlinking ... I'm not sure I ever finished writing that code
//TODO: test versioning of unlinking
//TODO: test/fix versioning of a single empty proposition
//TODO: undeleting links does not restore their yellow color and linking color seems to be broken...
//TODO: lazy load closed items in versions mode
//TODO: prevent circular linking from crashing program...

//TODO: linking: original linked item should also change color immediately upon first linking
//TODO: linking: how will client automatically update link changes...

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
	private static List<String> messageList = new ArrayList<String>();
	private EditMode editMode = new EditMode();
	private VersionsMode versionsMode;
	private static Map<String, StringBuilder> logs = new HashMap<String, StringBuilder>();
	private static Map<String, Boolean> logsImmediatePrint = new HashMap<String, Boolean>();
	private static Map<String, Integer> logsCurrentIndent = new HashMap<String, Integer>();

	public void onModuleLoad() {
		try {
			modePanel.add(editMode, "Edit");
			versionsMode = new VersionsMode(editMode);
			modePanel.add(versionsMode, "Versions");

			modePanel.addSelectionHandler(new SelectionHandler<Integer>() {

				@Override
				public void onSelection(SelectionEvent<Integer> event) {
					try {
						if (modePanel.getSelectedIndex() == 1) {
							versionsMode.displayVersions();
						}
					} catch (Exception e) {
						ServerComm.handleClientException(e);
					}

				}
			});

			HTML htmlTitle = new HTML(
					"<div class=\"title\">coreason.org</div>"
							+ "<div class=\"subTitle\">...mass collaborative reasoning about everything...</div>");
			// htmlTitle.setStylePrimaryName("titleLabel");
			// messageArea.setStylePrimaryName("messageArea");
			mainPanel.addNorth(htmlTitle, 4);
			mainPanel.addNorth(messageArea, 2);
			mainPanel.add(modePanel);

			RootLayoutPanel rp = RootLayoutPanel.get();
			rp.add(mainPanel);

			// Window.alert( "pops up a window to user with message");

			message("App Begin", MessageType.INFO);
		} catch (Exception e) {
			ServerComm.handleClientException(e);
		}
	}

	public enum MessageType {
		ERROR, INFO;
	}

	public static void message(String string, MessageType type) {
		message(string, type, 5);
	}

	public static void message(String string, MessageType type,
			int displaySeconds) {

		String cssLabel;
		if (type == MessageType.ERROR) {
			cssLabel = "errorMessage";
		} else {
			cssLabel = "infoMessage";
		}
		String message = "<span class=\"" + cssLabel
				+ "\">&nbsp;&nbsp;&nbsp;&nbsp;" + string
				+ "&nbsp;&nbsp;&nbsp;&nbsp;</span>";
		messageList.add(message);

		refreshMessageList();

		MessageTimer messageTimer = new MessageTimer();
		messageTimer.message = message;
		messageTimer.schedule(displaySeconds * 1000);
	}

	public static void refreshMessageList() {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"messageArea\">");
		for (int i = 0; i < messageList.size(); i++) {
			sb.append(messageList.get(i));
			if (i < messageList.size() - 1) {
				sb.append("&nbsp;&nbsp;-&nbsp;&nbsp;");
			}
		}
		sb.append("</div>");

		messageArea.setHTML(sb.toString());
	}

	private static class MessageTimer extends Timer {
		String message;

		@Override
		public void run() {
			try {
				messageList.remove(message);
				refreshMessageList();
			} catch (Exception e) {
				ServerComm.handleClientException(e);
			}
		}

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

	public static void logStart(String logName) {
		logStart(logName, false);
	}

	/*
	 * at some point I might want to modify this to accept a boolean indicating
	 * whether to print the log or not so that I can easily flip the particular
	 * log on or off depending on whether I need it...
	 */
	public static void logStart(String logName, boolean immediatePrint) {
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
		logsImmediatePrint.put(logName, immediatePrint);
		logsCurrentIndent.put(logName, 0);

	}

	public static void logEnd(String logName) {
		if (logName == null)
			return;
		StringBuilder log = logs.remove(logName);
		if (logsImmediatePrint.remove(logName)) {
			return;
		}
		logsCurrentIndent.remove(logName);
		GWT.log(log.toString());
	}

	public static void log(String logName, String string) {
		if (logName == null)
			return;
		if (logsImmediatePrint.get(logName)) {
			GWT.log(logName + ": " + string);
			return;
		}
		logs.get(logName).append(string);
	}

	public static void logln(String logName, String string) {
		if (logName == null)
			return;
		if (logsImmediatePrint.get(logName)) {
			log(logName, string);
		} else {
			log(logName, "\n" + spaces(logsCurrentIndent.get(logName) * 2)
					+ string);
		}
	}

	public static void logIndent(String logName) {
		if (logName == null)
			return;
		Integer current = logsCurrentIndent.get(logName);
		logsCurrentIndent.put(logName, current++);
	}

	public static void logUnindent(String logName) {
		if (logName == null)
			return;
		Integer current = logsCurrentIndent.get(logName);
		logsCurrentIndent.put(logName, current--);
	}
}