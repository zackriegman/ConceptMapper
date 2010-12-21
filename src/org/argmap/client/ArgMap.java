package org.argmap.client;

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
import com.google.gwt.user.client.ui.Widget;

/*
 * open root prop 'testing add' one level.  go to versions. go back in time one step. 
 * open up link children:  they are empty even though they should have text...
 * 
 * but if you open up reasoning for so the link show, and rather than open up the link at that point, you go to the point
 * in time of the re-link, and then you open of the link, and then you go back to the point in time of the original link, the
 * the children are shown correctly.
 * 
 * actually, if you do it the first way, so that the children are shown empty when they should have text, and then
 * you go forward in time to the point where the children have been deleted and different children have been added, and then you
 * go backwards in time back to where they should have text, they will at that point have text.
 * 
 * doesn't seem to be limited to links.  deleted nodes in general sometimes do not seem to be restored with their text.  See 'simple tree',
 * goto to versions mode with tree closed, open one level, goto latest change where first arg has child, open arg, child should have text but
 * doesn't.
 * 
 * OK, I have a theory about what is going on here.  When a node is opened (or when it is loaded) the
 * program zooms the node's children to the right time.  However (!) a child that was deleted has 
 * no content until the undelete is processed.  The undelete change lives in the parent that was just opened,
 * not in the child.  However, the program zooms the children of the open node, based on the children's
 * changes.  When I used to generate an extra modification change for each deletion, there was a modification
 * that belonged to the child that would give the child node its content at the time of deletion.  But
 * when I eliminated that modification (because it clutters up the change list without serving a purpose)
 * the deleted nodes were left contentless.  One simple way to address this would be to populate nodes
 * with their content at the time of deletion when they are added to the tree, based on their parent's delete
 * event.
 * 
 * Make sure to also think about other info contained in delete events such as negation for props and pro/con for args?
 * 
 * ok, so I decided that the best approach is to make dummy nodes actual nodes that merely display
 * "loading from server", and remove the ViewDummyVer class.  I've done this.
 * 
 * Now I have to remove the code that tests a node to make sure that it is not a dummy before before operating on it
 * when moving the tree backwards.  That way the negated, pro/con, content and all other info will already be loaded
 * into the dummy nodes.
 * 
 * I'll also want to convert dummy nodes into real nodes, thereby preserving the info that will not be loaded into them,
 * instead of replacing them with real nodes.
 * 
 * And while I'm at it, I'll want to make ModeVersions.mergeLoadedNode() use the integrated iterator in ViewNodeVer to clean things up
 * a little bit.
 * 
 * And I'll also want to transition ModeEdit to the new dummy node approach.
 */

/*
 * get rid of ViewDummyVer
 */
/*
 * when a root proposition is first added, program suggest using it instead (as a link) of itself!
 */
/* TODO: ModeVersions: for some reason child nodes of a deleted and re-added link are not showing up in the first attachement of the node
 * so you have to browse to a later point in time, and open up the link, and the go back in time, in order to open up the link node.  
 * (Not sure if the problem is confined to links or not.)
 */
/*TODO: client throws an exception when removing a link and re-adding it (at least if you make a single change to the link in the interim
 * I haven't tested other scenarios yet)
 */
/*TODO: what happens in ModeVersions if I first link to a node as negated, then delete it, then link to it as non-negated and delete that
 * (or vice-versa)?  Nodes are stores by ID in the deletedNodes list. Since its stored in a hashmap, storing the same deleted node twice will
 * result in only one copy.  Will that work for storing the history?  One thing I want to work out below is only storing the history for a
 * link that is relevant to the time that it exists in the tree so there aren't a lot of extra changes in the change list.  Will having
 * only one copy complicate that?  If so maybe I could save in a list or a hashmap keyed on date instead of ID (or ID and date, or something)...
 * 
 * Regardless, as far as negation is concerned, if there is only one copy, I can't depend on it to have the right negated value when
 * it is re-added to the tree for the first time.
 */
//TODO: test opening negated links in versions mode (probably won't work!!!)
//TODO: fix versions mode formating of links
//TODO: versioning root node with no modifications/adds throws exception because of empty change list
//TODO: do some basic testing of versioning of deleted top level nodes
/*TODO: fix exceptions when opening circular links in versions mode and continue testings version mode's handling of circular linking*/
/*TODO: track down exceptions in ModeVersion (unrelated to circular linking)*/
/*TODO: versions mode: update link coloring depending on whether a proposition is currently a link?
 * to do this properly it seems like for each proposition, I need to included in the changes sent to the client
 * unlinks which lower the link count to less than or equal to one, and links which raise the link
 * count above one. (usually link events aren't considered events of the proposition being unlinked, 
 * but rather of the argument link/unlinking to the proposition.)
 * Then on the client those changes would be handled by forwards/backwards function.  Maybe the function
 * would notice whether the ViewNode associated with the change was an argument or a proposition.  If it was a 
 * proposition it would update the props link status.  If it was an argument it would just do what it currently does.
 * This way the system would work for propositions whose link coloring is changed by a linking/unlinking not within
 * the tree being viewed by the user...
 */
//TODO: comment the hell out of versions mode!!!!!!! someday I'll need to change it...

//TODO: add 'real' example arguments for demonstration (for instance my argument about legalizing unauthorized access)

//TODO: implement reversions and batch reversions
//TODO: implement email updates of changes
//TODO: implement email invitations to participate in an particular argument
//TODO: implement facebook integration?  see [http://code.google.com/p/batchfb/] and the facebook API
//TODO: implement scoring algorithm
//TODO: compose more/edit help tips

//TODO: often times conflict dialog is not displayed and edits are silently overwritten...
//TODO: rating an item seems to scroll it out of view!
//TODO: deleting an items sometimes causes view to scroll to seemingly unrelated place...
//TODO: the text of linked propositions and their children does not automatically update when edited elsewhere within the same window... (at least for links added in the current session)
//TODO: cleanup deprecated stuff from SDK upgrade (at the moment there is no documentation about this so I'll have to wait)
//TODO: fix side bar border formatting on Chrome

//TODO: research semenatic reasoning (OWL, etc.)

//TODO: try running speed tracer
//TODO: make batch open icon visible, and open tree a few layers deep regardless of whether mouse over node is already loaded
//TODO: batch open icon not visible/clickable on props that reach right screen edge
/*TODO: ModeVersions:  undoing unlinks does not restore the link's yellow color if the link *currently* is not  
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
//TODO: figure out how to make server log more than info, warn and severe while in hosted mode...

/*TODO: move changes from propID/argID to parentID/childID (this will make querying more efficient:  want
 * all the changes having to do with a particular node?  Just query on parentID with that node's ID.
 * Hmmm... what about prop adds where the proposition has content?  Anyway, the idea being, that when
 * a node is updated we store its id in the parentID.  So when we query on parentID we get all the additions to,
 * deletions from, and content modifications of, the node and nothing else.  Right now querying on propID
 * gives not only additions to, deletions from, and content modifications of, the prop, it also gives additions of,
 * links of, and deletions of the prop, which in my current paradigm for handling changes, I don't need.
 */
/* TODO: deploy with big database (~40 root nodes) and test speed of search */
//TODO: read more about: http://domino.research.ibm.com/cambridge/research.nsf/0/66fb7b9f526da69c852570fa00753e93?OpenDocument
//TODO: continue research on "collaborative reasoning" and other possible similar projects (argument mapping)

/*TODO: what to do with ratings of deleted propositions?  might be worth keeping them around for a while in case the 
 * proposition is undeleted (but of course the facility to undelete a proposition does not yet exists).  eventually
 * I'll probably want to clean them up...
 */
/* TODO: make a TextAreaAutoHeight implementation for IE with deferred binding? */
/* TODO: fix TextAreaAutoHeight Chrome problem where some of the root props don't resize fulls and leaves the text clipped... */

//TODO: setup backups on home computer
//TODO: highlight last change node after time travel
//TODO: think about merging of duplicate propositions (both possibly already having arguments)
//TODO: fine tune UI for arguments... see implementation notes
//TODO: get rid of irrelevant update calls (e.g. clearing update before delete)
//TODO: still an issue?: what are all the phantom propositions that show up on an empty search?
//TODO: if the message queue gets backed up (more than 5?) give user a message that there is trouble saving changes to server, and changes may be lost
//TODO: maintain order of propositions? waiting to see if order gets screwed up
//TODO: IE problems: (1) VersionsMode doesn't seem to work (2) can delete root level proposition without first deleting its text
//TODO: figure out how to have logging code compiled out like the GWT logger framework supposedly does
/* TODO live updates: I might want to have a way to remove deleted search
 * results so that the user doesn't start editing a deleted item. This
 * should be easy, just supply the server with a list of root items, and
 * return add/remove changes for them...
 */

// TO DO: test in IE8, chrome, safari, opera, etc.
// TO DO: implement some basic database integrity checks (e.g. every argument belongs to an existing proposition...)
// TO DO: give people a way to specify logical structure (first order predicate calculus structure of each proposition
// TO DO: add definitions (as a special kind of proposition?)
// TO DO: integrate logic engine to verify validity of arguments

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ArgMap implements EntryPoint, UncaughtExceptionHandler,
		SelectionHandler<Integer> {

	private ModeEdit editMode;

	private DockLayoutPanel mainPanel;
	private TabLayoutPanel modePanel;
	private HorizontalPanel loginPanel;

	private HTML messageArea;
	private MultiMap<String, Message> messageMap;
	private ModeVersions versionsMode;
	private static ArgMap argMap;
	private boolean loggedIn;

	public void onModuleLoad() {
		argMap = this;
		messageArea = new HTML();
		messageMap = new MultiMap<String, Message>();
		editMode = new ModeEdit(this);

		GWT.runAsync(new AsyncRunCallback() {

			@Override
			public void onSuccess() {
				mainPanel = new DockLayoutPanel(Style.Unit.EM);
				modePanel = new TabLayoutPanel(1.8, Style.Unit.EM);
				loginPanel = new HorizontalPanel();

				GWT.setUncaughtExceptionHandler(ArgMap.this);
				modePanel.add(editMode, "Find And Collaborate");

				modePanel.addSelectionHandler(ArgMap.this);

				HTML htmlTitle = new HTML(
						"<div class=\"title\">coreason.org</div>"
								+ "<div class=\"subTitle\">...mass collaborative reasoning about everything...</div>");

				LayoutPanel bannerPanel = new LayoutPanel();
				bannerPanel.add(htmlTitle);
				bannerPanel.add(loginPanel);
				loginPanel.addStyleName("loginPanel");
				bannerPanel.setWidgetHorizontalPosition(loginPanel,
						Alignment.END);
				mainPanel.addNorth(bannerPanel, 4);
				mainPanel.addNorth(messageArea, 2);
				mainPanel.add(modePanel);

				RootLayoutPanel rp = RootLayoutPanel.get();
				rp.add(mainPanel);

				getLoginInfo();

			}
		});
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
					loggedIn = true;
					Anchor signOutLink = new Anchor("Sign out");
					signOutLink.addStyleName("loginText");
					signOutLink.setHref(loginInfo.logOutURL);
					Label nickName = new Label(loginInfo.nickName + " |");
					nickName.addStyleName("loginText");
					nickName.addStyleName("email");
					loginPanel.add(nickName);
					loginPanel.add(signOutLink);
					if (loginInfo.isAdmin) {
						GWT.runAsync(new AsyncRunCallback() {

							@Override
							public void onSuccess() {
								modePanel.add(new ModeAdmin(ArgMap.this),
										"Admin");
							}
						});
					}
				} else {
					loggedIn = false;
					Anchor signInLink = new Anchor("Sign in");
					signInLink.addStyleName("loginText");
					signInLink.setHref(loginInfo.logInURL);
					loginPanel.add(signInLink);
				}
			}
		});
	}

	public static boolean loggedIn() {
		return argMap.loggedIn;
	}

	public void showVersions() {
		if (!versionsIsDisplayed()) {
			GWT.runAsync(new AsyncRunCallback() {

				@Override
				public void onSuccess() {
					if (versionsMode == null) {
						versionsMode = new ModeVersions(editMode);
					}
					modePanel.insert(versionsMode, "History", 1);
				}
			});
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

	public class Message {
		private MessageType type;
		private String content;
		private boolean displayed = false;
		private MessageTimer timer;

		public void setMessage(String content, MessageType type) {
			if (displayed) {
				messageMap.remove(this.content + this.type, this);
				messageMap.put(content + type, this);
				this.type = type;
				this.content = content;
				refreshMessageList();
			} else {
				this.type = type;
				this.content = content;
			}
		}

		public void setMessage(String newContent) {
			assert type != null;
			setMessage(newContent, type);
		}

		public void display() {
			if (!displayed) {
				messageMap.put(content + type, this);
				refreshMessageList();
				displayed = true;
			}
		}

		public void hide() {
			if (displayed) {
				messageMap.remove(content + type, this);
				refreshMessageList();
				displayed = false;
			}
		}

		public void hideAfter(int millis) {
			if (timer == null) {
				timer = new MessageTimer();
				timer.message = this;
			}
			timer.schedule(millis);
		}
	}

	public enum MessageType {
		ERROR, INFO;
	}

	public static Message messageTimed(String string, MessageType type) {
		return messageTimed(string, type, 5);
	}

	public static Message messageTimed(String string, MessageType type,
			int displaySeconds) {
		Message message = message(string, type);
		message.hideAfter(displaySeconds * 1000);
		return message;
	}

	public static Message getMessage() {
		return argMap.new Message();
	}

	public static Message message(String string, MessageType type) {
		Message message = argMap.new Message();
		message.setMessage(string, type);
		message.display();
		return message;
	}

	public static void refreshMessageList() {
		String sp = "&nbsp;";
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"messageArea\">");
		for (List<Message> messageList : argMap.messageMap.values()) {
			sb.append("<span class=\"");
			if (messageList.get(0).type == MessageType.ERROR) {
				sb.append("errorMessage");
			} else {
				sb.append("infoMessage");
			}
			sb.append("\">" + sp + sp);

			sb.append(messageList.get(0).content);
			if (messageList.size() > 1) {
				sb.append(sp + "(" + messageList.size() + ")");
			}
			sb.append(sp + sp + "</span>" + sp + sp);
		}
		sb.append("</div>");

		argMap.messageArea.setHTML(sb.toString());
	}

	private static class MessageTimer extends Timer {
		Message message;

		@Override
		public void run() {
			message.hide();
			refreshMessageList();
		}
	}

	@Override
	public void onUncaughtException(Throwable e) {
		try {
			ArgMap.messageTimed("EXCEPTION CAUGHT ON CLIENT",
					MessageType.ERROR, 10);
			Window.alert("Exception: " + e.toString());
			ServerComm.logException(e);
		} catch (Exception handlerException) {
		}
		GWT.log("Uncaught Exception", e);
		if (Log.on) Log.finishOpenLogs();
	}

	public ModeEdit getModeEdit() {
		return editMode;
	}

	@Override
	public void onSelection(SelectionEvent<Integer> event) {
		Widget widget = modePanel.getWidget(modePanel.getSelectedIndex());
		if (widget == versionsMode) {
			versionsMode.displayVersions();
		}
	}

}