package com.appspot.conceptmapper.client;

import com.google.gwt.core.client.EntryPoint;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ConceptMapper implements EntryPoint {

	private VerticalPanel mainPanel = new VerticalPanel();
	private Tree tree = new Tree();
	public static Label messageLabel = new Label();

	public void onModuleLoad() {

		// TODO: upload this version to appspot
		// TODO: implement breaking and merging proposition content by adding
		// and removing newlines
		// TODO: provide a way to add new top level propositions
		// TODO: put the thing in a frame, add helpful message along the side
		// TODO: implement proposition search
		// TODO: implement linking to existing propositions (i.e. propositions can belong to multiple arguments)

		// TODO: see if it works in different browsers
		// TODO: implement change message queue to keep things consistent

		// TODO: implement some basic database integrity checks (e.g. every argument belongs to an existing proposition...)
		// TODO: implement versioning
		// TODO: implement user accounts, email updates of changes, inviting friends
		// TODO: poll server every few seconds for server side changes (this has
		// to come after versioning I think)
		// TODO: give people a way to specify logical structure (first order predicate calculus structure of each proposition
		// TODO: integrate logic engine to verify validity of arguments

		ServerComm.init(messageLabel);
		tree.addItem(new PropositionView());

		messageLabel.setText("no message");

		// Assemble Main panel.
		mainPanel.add(messageLabel);
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
		// Window.alert( "pops up a window to user with message");
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
		PropositionView propView = new PropositionView(prop, argView);
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