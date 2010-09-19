package com.appspot.conceptmapper.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;


// TODO: implement versioning
// TODO: put the thing in a frame, add helpful message along the side
// TODO: implement proposition search
// TODO: implement linking to existing propositions (i.e. propositions can belong to multiple arguments)
// TODO: maintain order of propositions? waiting to see if order gets screwed up

// TO DO: test in IE, chrome, etc.
// TO DO: implement user accounts, email updates of changes, inviting friends
// TO DO: poll server every few seconds for server side changes (this has
// to come after versioning I think)
// TO DO: implement some basic database integrity checks (e.g. every argument belongs to an existing proposition...)
// TO DO: give people a way to specify logical structure (first order predicate calculus structure of each proposition
// TO DO: add definitions (as a special kind of proposition?)
// TO DO: integrate logic engine to verify validity of arguments
// TO DO: implement change message queue to keep things consistent?  maybe not why bother with this unless its being a problem... a few propositions reappearing after they are deleted won't kill anybody...they can just delete them again...

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ConceptMapper implements EntryPoint {

	private VerticalPanel mainPanel = new VerticalPanel();
	private Tree tree;
	public static HTML messageArea = new HTML();

	public void onModuleLoad() {

		ServerComm.init();

		/**
		 * annoyingly, by default the Tree eats the arrow key events so they
		 * can't be used for moving in a text box. Setting a handler on the tree
		 * to keep the events from doing their default behavior or propagating
		 * doesn't seem to work.  I found this fix on stack overflow
		 */
		tree = new Tree() {
			@Override
			protected boolean isKeyboardNavigationEnabled(TreeItem inCurrentItem) {
				return false;
			}

			@Override
			public void onBrowserEvent(Event event) {
				int eventType = DOM.eventGetType(event);

				switch (eventType) {
				case Event.ONKEYDOWN:
				case Event.ONKEYPRESS:
				case Event.ONKEYUP:
					return;
				default:
					break;
				}

				super.onBrowserEvent(event);
			}
		};

		tree.setAnimationEnabled(false);

		Button addPropButton = new Button("Add A New Proposition");
		addPropButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				PropositionView newPropView = new PropositionView();

				// close the other tree items
				for (int i = 0; i < tree.getItemCount(); i++) {
					tree.getItem(i).setState(false);
				}

				tree.addItem(newPropView);
				newPropView.haveFocus();
				ServerComm
						.addProposition(newPropView.getProposition(), null, 0);
			}
		});

		// Assemble Main panel.
		// mainPanel.add(messageLabel);
		mainPanel.add(addPropButton);
		mainPanel.add(tree);

		// Associate the Main panel with the HTML host page.
		RootPanel.get("mappingWidget").add(mainPanel);

		ServerComm.fetchProps(new ServerComm.FetchPropsCallback() {

			@Override
			public void call(Proposition[] props) {
				for (Proposition prop : props) {
					tree.addItem(recursiveBuildPropositionView(prop, null));
				}
				openTree();
			}
		});

		VerticalPanel outputPanel = new VerticalPanel();
		outputPanel.add(messageArea);
		RootPanel.get("outputWidget").add(outputPanel);
		// Window.alert( "pops up a window to user with message");

		println("App Begin");
	}

	public static void println(String string) {
		print('\n' + string + "<br />");
	}

	public static void print(String string) {
		messageArea.setHTML(string + messageArea.getHTML());
	}

	private void openTree() {
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveOpenTreeItem(tree.getItem(i));
		}
	}

	private void recursiveOpenTreeItem(TreeItem item) {
		item.setState(true);
		for (int i = 0; i < item.getChildCount(); i++) {
			recursiveOpenTreeItem(item.getChild(i));
		}
	}

	private PropositionView recursiveBuildPropositionView(Proposition prop,
			ArgumentView argView) {
		PropositionView propView = new PropositionView(prop);
		for (Argument arg : prop.args) {
			propView.addItem(recursiveBuildArgumentView(arg));
		}
		return propView;
	}

	private ArgumentView recursiveBuildArgumentView(Argument arg) {
		ArgumentView argView = new ArgumentView(arg);
		for (Proposition prop : arg.props) {
			argView.addItem(recursiveBuildPropositionView(prop, argView));
		}
		return argView;
	}

}