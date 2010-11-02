package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.layout.client.Layout.Alignment;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;

//TODO: read more about: http://domino.research.ibm.com/cambridge/research.nsf/0/66fb7b9f526da69c852570fa00753e93?OpenDocument
//TODO: continue research on "collaborative reasoning" and other possible similar projects (argument mapping)

//TODO: prevent circular linking from crashing program...
//TODO: suggests use of proposition for which the argument is for/against!?!
//TODO: fix linking of root level nodes automatically incorporating the node into another tree...(and therefore not color the node appropriately)
//TODO: provide a way to see deleted top level nodes
//TODO: try running speed tracer
//TODO: fix slow searches (break them up?  use a different approach all together?)

//TODO: comment the hell out of versions mode!!!!!!! someday I'll need to change it...

//TODO: implement user accounts, email updates of changes, inviting friends
//TODO: implement proposition strength voting, and scoring algorithm
//TODO: poll server every few seconds for server side changes (this has to come after versioning I think)
//TODO: add helpful message along the side (tips box)

//TODO: batch open icon not visible/clickable on props that reach right screen edge
/*TODO: a proposition tree begins at time A.  At time C a pre-existing node is linked into the proposition tree.
 * The user browses to a time B between times A and C.  At that time the linked node is not present.
 * However the linked node is open in the tree(as a deleted node, so its changes are displayed in the change list).
 * The result is that there are changes that the user can scroll past that have no apparent effect on the tree
 * very confusing... so only a the changes post dating the linked nodes linking should be added to the change list?
 */
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
//TODO: linking: original linked item should also change color immediately upon first linking
//TODO: linking: how will client automatically update link changes...
//TODO: figure out how to make server log more than warn and severe while in hosted mode...

//TODO: setup backups on home computer
/*TODO: implement locking when modifying a parent and deleting/adding nodes to make sure that two simultaneous
 * changes (modifying the parent or deleteing/adding two different child nodes) do not clobber each other
 * and result in inconsistent datastore state. */
//TODO: highlight last change node after time travel
//TODO: think about merging of duplicate propositions (both possibly already having arguments)
//TODO: fine tune UI for arguments... see implementation notes
//TODO: get rid of irrelevant update calls (e.g. clearing update before delete)
//TODO: still an issue?: what are all the phantom propositions that show up on an empty search?
//TODO: decide how to get rid of repetative code (for propositions and args), maybe make them both subclasses of a Node class?
//TODO: if the message queue gets backed up (more than 5?) give user a message that there is trouble saving changes to server, and changes may be lost
//TODO: maintain order of propositions? waiting to see if order gets screwed up
//TODO: IE problems: (1) VersionsMode doesn't seem to work (2) can delete root level proposition without first deleting its text
//TODO: figure out how to have logging code compiled out like the GWT logger framework supposedly does

// TO DO: test in IE8, chrome, safari, opera, etc.
// TO DO: upload to appengine, and add an example argument (for instance my argument about legalizing unauthorized access)
// TO DO: implement some basic database integrity checks (e.g. every argument belongs to an existing proposition...)
// TO DO: give people a way to specify logical structure (first order predicate calculus structure of each proposition
// TO DO: add definitions (as a special kind of proposition?)
// TO DO: integrate logic engine to verify validity of arguments

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ArgMap implements EntryPoint, UncaughtExceptionHandler {

	private final DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.EM);
	private final TabLayoutPanel modePanel = new TabLayoutPanel(1.5,
			Style.Unit.EM);
	private final HorizontalPanel loginPanel = new HorizontalPanel();

	private static HTML messageArea = new HTML();
	private static List<String> messageList = new ArrayList<String>();
	private final ModeEdit editMode = new ModeEdit(this);
	private ModeVersions versionsMode;

	public void onModuleLoad() {

		GWT.setUncaughtExceptionHandler(this);
		modePanel.add(editMode, "Find And Collaborate");
		versionsMode = new ModeVersions(editMode);

		modePanel.addSelectionHandler(new SelectionHandler<Integer>() {

			@Override
			public void onSelection(SelectionEvent<Integer> event) {
				if (modePanel.getWidget(modePanel.getSelectedIndex()) == versionsMode) {
					versionsMode.displayVersions();
				}
			}
		});

		HTML htmlTitle = new HTML(
				"<div class=\"title\">coreason.org</div>"
						+ "<div class=\"subTitle\">...mass collaborative reasoning about everything...</div>");

		LayoutPanel bannerPanel = new LayoutPanel();
		bannerPanel.add(htmlTitle);
		bannerPanel.add(loginPanel);
		loginPanel.addStyleName("loginPanel");
		bannerPanel.setWidgetHorizontalPosition(loginPanel, Alignment.END);
		mainPanel.addNorth(bannerPanel, 4);
		mainPanel.addNorth(messageArea, 2);
		mainPanel.add(modePanel);

		RootLayoutPanel rp = RootLayoutPanel.get();
		rp.add(mainPanel);

		message("App Begin", MessageType.INFO);
		getLoginInfo();

	}

	private void getLoginInfo() {
		ServerComm.getLoginInfo(new ServerComm.LocalCallback<LoginInfo>() {

			@Override
			public void call(LoginInfo loginInfo) {
				if (Log.on) {
					Log log = Log.getLog("am.oml");
					log.logln(loginInfo.email);
					log.logln(loginInfo.nickName);
					log.logln(loginInfo.firstName);
					log.logln(loginInfo.lastName);
					log.logln("" + loginInfo.loggedIn);
					log.logln(loginInfo.logInURL);
					log.logln(loginInfo.logOutURL);
					log.logln(GWT.HOSTED_MODE_PERMUTATION_STRONG_NAME);
					log.logln(GWT.getHostPageBaseURL());
					log.logln(GWT.getModuleBaseURL());
					log.logln(GWT.getModuleName());
					log.logln(GWT.getPermutationStrongName());
					log.finish();
				}
				if (loginInfo.loggedIn) {
					Anchor signOutLink = new Anchor("Sign out");
					signOutLink.addStyleName("loginText");
					signOutLink.setHref(loginInfo.logOutURL);
					Label nickName = new Label(loginInfo.nickName + " |");
					nickName.addStyleName("loginText");
					nickName.addStyleName("email");
					loginPanel.add(nickName);
					loginPanel.add(signOutLink);
					if (loginInfo.isAdmin ) {
						modePanel.add(new ModeAdmin(ArgMap.this), "Admin");
					}
				} else {
					Anchor signInLink = new Anchor("Sign in");
					signInLink.addStyleName("loginText");
					signInLink.setHref(loginInfo.logInURL);
					loginPanel.add(signInLink);
				}
			}
		});
	}

	public void showVersions() {
		if (!versionsIsDisplayed()) {
			modePanel.insert(versionsMode, "History", 1);
		}
	}

	public void hideVersions() {
		if (versionsIsDisplayed()) {
			modePanel.remove(versionsMode);
		}
	}

	private boolean versionsIsDisplayed() {
		if (modePanel.getWidgetIndex(versionsMode) == -1) {
			return false;
		} else {
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
			messageList.remove(message);
			refreshMessageList();
		}
	}

	@Override
	public void onUncaughtException(Throwable e) {
		try {
			ArgMap.message("EXCEPTION CAUGHT ON CLIENT", MessageType.ERROR, 10);
			Window.alert("Exception: " + e.toString());
			ServerComm.logException(e);
		} catch (Exception handlerException) {
		}
		GWT.log("Uncaught Exception", e);
		if (Log.on) Log.finishOpenLogs();
	}
}