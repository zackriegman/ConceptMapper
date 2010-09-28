package com.appspot.conceptmapper.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class VersionsMode extends HorizontalPanel implements
		CloseHandler<TreeItem>, OpenHandler<TreeItem>, ChangeHandler {

	private ListBox versionList = new ListBox();
	private EditMode editMode;
	private Tree treeClone = null;
	Map<Long, PropositionView> propViewIndex;
	Map<Long, ArgumentView> argViewIndex;
	private List<Change> changes;
	private ChangeHandler listBoxChangeHandler;
	private HandlerRegistration listBoxChangeHandlerRegistration;

	public VersionsMode(EditMode editModePair) {
		this.editMode = editModePair;

		add(versionList);
		versionList.setVisibleItemCount(30);
		versionList.setWidth("25em");

		listBoxChangeHandlerRegistration = versionList
				.addChangeHandler(this);
	}

	public void resetState(Tree tree) {
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveResetState(tree.getItem(i));
		}
	}

	public void recursiveResetState(TreeItem item) {
		/*
		 * if this item has children, and the first child is not a dummy node
		 * place holder loading message
		 */
		if (item.getChildCount() > 0
				&& !item.getChild(0).getStyleName().equals("loadDummy")) {
			item.setState(true);
			for (int i = 0; i < item.getChildCount(); i++) {
				recursiveResetState(item.getChild(i));
			}
		}
	}

	public void modifyTreeToVersion() {
		int index = versionList.getSelectedIndex();
		// TODO shouldn't this be index + 1, rather than index? but
		// indexOutOfBoundsException is thrown... and it seems to work the way
		// it is...
		GWT.log("----reverting tree----");
		for (int i = 0; i < index; i++) {
			Change change = changes.get(i);
			GWT.log("processing: " + change.changeType);
			switch (change.changeType) {
			case PROP_DELETION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				// println("change: " + change.toString());
				PropositionView deletedPropView = new PropositionView(false);
				propViewIndex.put(change.propID, deletedPropView);
				deletedPropView.setContent(change.propContent);
				deletedPropView.proposition.id = change.propID;
				println("propView:" + deletedPropView + "; argView:" + argView
						+ "; argPropIndex:" + change.argPropIndex);
				argView.insertPropositionViewAt(change.argPropIndex,
						deletedPropView);

				break;
			}
			case PROP_ADDITION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = propViewIndex.get(change.propID);
				if (argView != null) {
					argView.removeItem(propView);
				} else
					treeClone.removeItem(propView);
				break;
			}
			case PROP_MODIFICATION: {
				PropositionView propView = propViewIndex.get(change.propID);
				propView.setContent(change.propContent);
				break;
			}
			case ARG_ADDITION: {
				PropositionView propView = propViewIndex.get(change.propID);
				printPropRecursive(propView, 0);
				ArgumentView argView = argViewIndex.get(change.argID);
				argView.remove();
				// propView.removeItem(argView);
				println("propView:" + propView + "; argView:" + argView);
				break;
			}
			case ARG_DELETION: {
				PropositionView propView = propViewIndex.get(change.propID);
				ArgumentView argView = new ArgumentView(change.argPro);
				argView.argument.id = change.argID;
				propView.addItem(argView);
				argViewIndex.put(change.argID, argView);
				break;
			}
			case PROP_UNLINK: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = propViewIndex.get(change.propID);
				argView.removeItem(propView);
				break;
			}
			case PROP_LINK: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = propViewIndex.get(change.propID);
				/*
				 * TODO need to think through linking and unlinking in more
				 * detail. What I have here will not be enough. Specifically the
				 * server probably doesn't currently send the linked
				 * proposition's content which will be important for the client
				 * to display when reviewing revisions. Furthermore, with all
				 * the other operations, a given change will only add a node.
				 * But here the change will add hundreds of nodes, the children
				 * of the linked proposition. Probably the way to show that is
				 * simply to add the proposition, and then lazy load the tree
				 * and someone browses.
				 */
				argView.insertPropositionViewAt(change.argPropIndex, propView);
				break;
			}
			}

		}
	}

	public void printPropRecursive(PropositionView propViewParent, int level) {
		for (int i = 0; i < level; i++)
			print("  ");
		print("propID:" + propViewParent.proposition.id + "; "
				+ propViewParent.getContent());
		print("\n");
		print("\nargCount:" + propViewParent.getChildCount());
		for (int i = 0; i < propViewParent.getChildCount(); i++) {
			ArgumentView arg = (ArgumentView) propViewParent.getChild(i);
			for (int j = 0; j < level + 1; j++)
				print("  ");
			print(arg.getText());
			print("" + arg.argument.id);
			print("\n");
			for (int j = 0; j < arg.getChildCount(); j++) {
				printPropRecursive((PropositionView) arg.getChild(j), level + 2);
			}
		}
	}

	public void println(String string) {
		print(string + '\n');
	}

	public void print(String string) {
		System.out.print("C:" + string);
	}

	public void displayVersions() {
		List<Proposition> props = new LinkedList<Proposition>();
		List<Argument> args = new LinkedList<Argument>();
		editMode.getOpenPropsAndArgs(props, args);
		ServerComm.getChanges(null, props, args,
				new ServerComm.GetChangesCallback() {

					@Override
					public void call(List<Change> chngs) {
						changes = chngs;
						println("Got back " + changes.size() + " changes");
						listBoxChangeHandlerRegistration.removeHandler();
						versionList.clear();

						if (treeClone != null) {
							remove(treeClone);
							treeClone = null;
						}
						for (Change change : changes) {
							versionList.addItem("" + change.date + " ["
									+ change.changeType + "]", "" + change.id);
							println("\ndisplayVersions -- " + change.toString());
						}
						versionList.addChangeHandler(listBoxChangeHandler);
						versionList.setSelectedIndex(0);
						listBoxChangeHandler.onChange(null);
					}
				});
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
		// TODO Auto-generated method stub
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0
				&& treeItem.getChild(0).getStyleName().equals("loadDummy")) {
			println("VersionsMode.onClose:  NOT REMOVING -- METHOD NOT IMPLEMENTED!");
		}

	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0
				&& treeItem.getChild(0).getStyleName().equals("loadDummy")) {
			println("VersionsMode.onOpen:  NOT LOADING -- METHOD NOT IMPLEMENTED!");
		}

	}

	@Override
	public void onChange(ChangeEvent event) {
		buildFreshTreeClone();
		modifyTreeToVersion();
		add(treeClone);
		resetState(treeClone);
	}
	
	public void buildFreshTreeClone(){
		if (treeClone != null) {
			remove(treeClone);
		}
		treeClone = new Tree();
		treeClone.addCloseHandler(VersionsMode.this);
		treeClone.addOpenHandler(VersionsMode.this);
		propViewIndex = new HashMap<Long, PropositionView>();
		argViewIndex = new HashMap<Long, ArgumentView>();
		editMode.buildTreeCloneOfOpenNodesWithIndexes(treeClone, propViewIndex,
				argViewIndex);
	}
}
