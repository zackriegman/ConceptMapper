package org.argmap.client;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.RunAsyncCallback;
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

/*TODO: fix exceptions when opening circular links in versions mode and continue testings version mode's handling of circular linking*/
//TODO: fix linking of root level nodes automatically incorporating the node into another tree...(and therefore not color the node appropriately)
//TODO: provide a way to see deleted top level nodes
/*TODO: move changes from propID/argID to parentID/childID (this will make querying more efficient:  want
 * all the changes having to do with a particular node?  Just query on parentID with that node's ID.
 * Hmmm... what about prop adds where the proposition has content?  Anyway, the idea being, that when
 * a node is updated we store its id in the parentID.  So when we query on parentID we get all the additions to,
 * deletions from, and content modifications of, the node and nothing else.  Right now querying on propID
 * gives not only additions to, deletions from, and content modifications of, the prop, it also gives additions of,
 * links of, and deletions of the prop, which in my current paradigm for handling changes, I don't need.
 */

//TODO: implement proposition strength voting, and scoring algorithm
//TODO: add helpful message along the side (tips box)
//TODO: implement user accounts, email updates of changes, inviting friends
//TODO: poll server every few seconds for server side changes (this has to come after versioning I think)

/*TODO: a proposition tree begins at time A.  At time C a pre-existing node is linked into the proposition tree.
 * The user browses to a time B between times A and C.  At that time the linked node is not present.
 * However the linked node is open in the tree(as a deleted node, so its changes are displayed in the change list).
 * The result is that there are changes that the user can scroll past that have no apparent effect on the tree
 * very confusing... so only a the changes post dating the linked nodes linking should be added to the change list?
 */
//TODO: comment the hell out of versions mode!!!!!!! someday I'll need to change it...
//TODO: try running speed tracer

/* TODO: deploy with big database (~40 nodes) and test speed of search */
//TODO: read more about: http://domino.research.ibm.com/cambridge/research.nsf/0/66fb7b9f526da69c852570fa00753e93?OpenDocument
//TODO: continue research on "collaborative reasoning" and other possible similar projects (argument mapping)

//TODO: batch open icon not visible/clickable on props that reach right screen edge

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
//TODO: highlight last change node after time travel
//TODO: think about merging of duplicate propositions (both possibly already having arguments)
//TODO: fine tune UI for arguments... see implementation notes
//TODO: get rid of irrelevant update calls (e.g. clearing update before delete)
//TODO: still an issue?: what are all the phantom propositions that show up on an empty search?
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

	private ModeEdit editMode;

	private DockLayoutPanel mainPanel;
	private TabLayoutPanel modePanel;
	private HorizontalPanel loginPanel;

	private HTML messageArea;
	private MultiMap<String, Message> messageMap;
	private ModeVersions versionsMode;
	private static ArgMap argMap;

	public void onModuleLoad() {
		argMap = this;
		messageArea = new HTML();
		messageMap = new MultiMap<String, Message>();
		editMode = new ModeEdit(this);
		
		GWT.runAsync(new RunAsyncCallback() {
			
			@Override
			public void onSuccess() {
				mainPanel = new DockLayoutPanel(Style.Unit.EM);
				modePanel = new TabLayoutPanel(1.8, Style.Unit.EM);
				loginPanel = new HorizontalPanel();

				GWT.setUncaughtExceptionHandler(ArgMap.this);
				modePanel.add(editMode, "Find And Collaborate");

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

				getLoginInfo();

				
			}
			
			@Override
			public void onFailure(Throwable reason) {
				ArgMap.messageTimed("Code download failed", MessageType.ERROR);
				Log.log("am.oml", "Code download failed" + reason.toString());
				
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
					Anchor signOutLink = new Anchor("Sign out");
					signOutLink.addStyleName("loginText");
					signOutLink.setHref(loginInfo.logOutURL);
					Label nickName = new Label(loginInfo.nickName + " |");
					nickName.addStyleName("loginText");
					nickName.addStyleName("email");
					loginPanel.add(nickName);
					loginPanel.add(signOutLink);
					if (loginInfo.isAdmin) {
						GWT.runAsync(new RunAsyncCallback() {

							@Override
							public void onSuccess() {
								modePanel.add(new ModeAdmin(ArgMap.this),
										"Admin");
							}

							@Override
							public void onFailure(Throwable reason) {
								ArgMap.messageTimed("Code download failed",
										MessageType.ERROR);
								Log.log("am.sv.a.of", "Code download failed"
										+ reason.toString());
							}
						});
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
			GWT.runAsync(new RunAsyncCallback() {

				@Override
				public void onSuccess() {
					if (versionsMode == null) {
						versionsMode = new ModeVersions(editMode);
					}
					modePanel.insert(versionsMode, "History", 1);
				}

				@Override
				public void onFailure(Throwable reason) {
					ArgMap.messageTimed("Code download failed",
							MessageType.ERROR);
					Log.log("am.sv.a.of",
							"Code download failed" + reason.toString());
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
			sb.append("\">" + sp + sp );

			sb.append(messageList.get(0).content);
			if (messageList.size() > 1) {
				sb.append(sp + "(" + messageList.size() + ")");
			}
			sb.append(sp + sp  + "</span>" + sp + sp);
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
}