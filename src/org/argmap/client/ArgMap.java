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


//TODO: read more about: http://domino.research.ibm.com/cambridge/research.nsf/0/66fb7b9f526da69c852570fa00753e93?OpenDocument
//TODO: continue research on "collaborative reasoning" and other possible similar projects (argument mapping)

//TODO: deleted argument's text is not restored in versions mode...
/*TODO: if you close a node in versions mode sometimes you can never open it again (in that versions
 * session) because it will 
 * never have children again given the dates available to click on.  For instance if you add a node, 
 * add a child, delete the child, and delete the node, all consecutively, and then go to a time when 
 * the node has children and close it, the node will never have children again because it doesn't have
 * children when it is created or when it is destroyed... and there are no visible events in between...
 * this should be a low priority fix, but it might take some ui creativity... ui creativity should be minimal
 * just add a placeholder element like "-----" for instance... but need to think about how to detect when
 * this is necessary and what date to assign the placeholder...
 * 
 * Actually this should be somewhat higher priority because if the parent node wasn't visible in the edit tree
 * (because its parent was close) and you browse to it in a previous version, the deleted branch will
 * never be visible...
 */
//TODO: batch open icon not visible/clickable on props that reach right screen edge
//TODO: test in chrome, safari, ie8, opera,

//TODO: prevent circular linking from crashing program...
//TODO: fix linking of root level nodes automatically incorporating the node into another tree...(and therefore not color the node appropriately)
//TODO: provide a way to see deleted top level nodes

//TODO: comment the hell out of versions mode!!!!!!! someday I'll need to change it...

//TODO: implement user accounts, email updates of changes, inviting friends
//TODO: implement proposition strength voting, and scoring algorithm
//TODO: poll server every few seconds for server side changes (this has to come after versioning I think)
//TODO: add helpful message along the side (tips box)

//TODO: figure out CSS inheritance
/*TODO: undoing unlinks does not restore the link's yellow color if the link *currently* is not  
 * linked to by more than one argument because the server sends the current proposition which
 * indicates a link count of 1.  This could be addressed by having a proposition's link/unlink
 * events also be owned by that proposition in VersionsMode, and hiding them probably (because
 * they may be interesting only in regards to the color of the node because the unlink/link
 * events may be with regards to arguments that are not currently opened) and treating
 * them as only coloring events when moving forwards and backwards (thus in some cases there
 * would be two link/unlink ViewChanges, one for the proposition whose link count is being updated
 * and one for the argument which needs to either display or hide the linked proposition).  The forwards/backwards
 * methods could distinguish between links/unlinks that were to be treated only as coloring events versus
 * ones that would be treated as displaying/removing the linked proposition based on whether the ViewNodeVer
 * object contained in the ViewChange object was a ViewPropVer or a ViewArgVer.  If it's a ViewArgVer
 * it would display/remove the linked proposition.  If it's a ViewPropVer it would simply update the link
 * count and change the color from yellow to white if it falls below 2, and vice versa.  This would require a
 * change on the server to return the coloring events for a proposition, and would require changes on the client
 * to insert the coloring events into the timeMachineMap and so forth.
 */
/*TODO: version mode should only display toplevel propositions that have been interacted with.
 * Displaying open top level propositions is not enough because a toplevel node might
 * have had child nodes that have been deleted and we want to be able to walk through that history.
 * Furthermore, we want to be able to see the modifications of the node.  This isn't exactly ideal.
 * The search feature should return all nodes... not just linked nodes.  What if the node has no
 * children.  How does the user indicated that he wants to view it (since it won't have an
 * open tree icon).  He could click inside it and make it dirty, but that is not intuitive to the
 * user.  This relates to the general question of how to conceive of toplevelness of a node.
 * Rather than toplevelness a node should be tagged with a category, and we would have tagged
 * and untagged nodes.  Tagged ones would show up when browsing, and might have priority when
 * searching, but would otherwise be the same. Hmmm... I don't think this is high priority
 * right now.  Showing open toplevel propositions will get the job done for the time being...
 * [Also note that attempting to version a single empty proposition results in a null pointer
 * exception... so if I decide not to prevent that in the UI (for instance if I decide not to only display
 * toplevel propositions in version mode) then this needs to be fixed.]
 */
//TODO: linking: original linked item should also change color immediately upon first linking
//TODO: linking: how will client automatically update link changes...

//TODO: setup backups on home computer
//TODO: redo loggin framework to match server (and maybe put logging statements in assert statements so they are eliminated?
/*TODO: implement locking when modifying a parent and deleting/adding nodes to make sure that two simultaneous
 * changes (modifying the parent or deleteing/adding two different child nodes) do not clobber each other
 * and result in inconsistent datastore state. */
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

	private final DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.EM);
	private final TabLayoutPanel modePanel = new TabLayoutPanel(1.5, Style.Unit.EM);

	private static HTML messageArea = new HTML();
	private static List<String> messageList = new ArrayList<String>();
	private final EditMode editMode = new EditMode( this );
	private VersionsMode versionsMode;
	private static Map<String, StringBuilder> logs = new HashMap<String, StringBuilder>();
	private static Map<String, Boolean> logsImmediatePrint = new HashMap<String, Boolean>();
	private static Map<String, Integer> logsCurrentIndent = new HashMap<String, Integer>();

	public void onModuleLoad() {
		try {
			modePanel.add(editMode, "Find And Collaborate");
			versionsMode = new VersionsMode(editMode);

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
			// htmlTitle.addStyleName("titleLabel");
			// messageArea.addStyleName("messageArea");
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
	
	public void showVersions(){
		if( ! versionsIsDisplayed() ){
		modePanel.add(versionsMode, "History");
		}
	}
	
	public void hideVersions(){
		if( versionsIsDisplayed() ){
		modePanel.remove(versionsMode);
		}
	}
	
	private boolean versionsIsDisplayed(){
		if( modePanel.getWidgetIndex( versionsMode ) == -1 ){
			return false;
		}
		else{
			return true;
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
		String indent = spaces(logsCurrentIndent.get(logName) * 4);
		
		if (logsImmediatePrint.get(logName)) {
			log(logName,  indent + string);
		} else {
			log(logName, "\n" + indent + string);
		}
	}

	public static void logIndent(String logName) {
		if (logName == null)
			return;
		Integer current = logsCurrentIndent.get(logName);
		logsCurrentIndent.put(logName, current+1);
	}

	public static void logUnindent(String logName) {
		if (logName == null)
			return;
		Integer current = logsCurrentIndent.get(logName);
		logsCurrentIndent.put(logName, current-1);
	}
}