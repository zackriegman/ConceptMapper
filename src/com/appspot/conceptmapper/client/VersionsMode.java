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

	private int changesIndex;
	private HandlerRegistration listBoxChangeHandlerRegistration;

	/*
	 * these variables hold the information need to move the tree forwards and
	 * backwards in time. changes holds the changes from the server which can be
	 * used to move backwards int time. As we move backwards in time, the
	 * information need to redo various kinds of changes is saved in the various
	 * maps.
	 */
	private List<Change> changes;
	private Map<Long, String> mapPropContent;
	private Map<Long, Boolean> mapArgPro;
	private Map<Long, Integer> mapPropIndex;
	private Map<Long, Long> mapPropID;

	public VersionsMode(EditMode editModePair) {
		this.editMode = editModePair;

		add(versionList);
		versionList.setVisibleItemCount(30);
		versionList.setWidth("25em");

		listBoxChangeHandlerRegistration = versionList.addChangeHandler(this);
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
		int newIndex = versionList.getSelectedIndex();
		if (newIndex > changesIndex) {
			moveTreeBackwards(newIndex);
		} else if (newIndex < changesIndex) {
			moveTreeForwards(newIndex);
		}
		changesIndex = newIndex;
	}

	public void moveTreeForwards(int newIndex) {
		// TODO shouldn't this be index + 1, rather than index? but
		// indexOutOfBoundsException is thrown... and it seems to work the way
		// it is...
		GWT.log("----re-doing changes----");
		for (int i = changesIndex - 1; i >= newIndex; i--) {
			Change change = changes.get(i);
			GWT.log("processing: " + change.changeType);
			switch (change.changeType) {
			case PROP_DELETION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propViewToDelete = propViewIndex
						.get(change.propID);
				propViewIndex.remove(change.propID);
				argView.removeItem(propViewToDelete);
				break;
			}
			case PROP_ADDITION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = new PropositionView(false);
				if (argView != null) {
					argView.insertPropositionViewAt(mapPropIndex.get( change.id ),
							propView);

				} else {
					treeClone.addItem(propView);
				}
				propViewIndex.put(change.propID, propView);
				break;
			}
			case PROP_MODIFICATION: {
				PropositionView propView = propViewIndex.get(change.propID);
				propView.setContent(mapPropContent.get(change.id));
				break;
			}
			case ARG_ADDITION: {
				PropositionView oldPropView = propViewIndex.get(change.propID);
				ArgumentView argView = new ArgumentView(mapArgPro.get(change.id));
				argView.argument.id = change.argID;
				
				PropositionView newPropView = new PropositionView(false);
				newPropView.proposition.id = mapPropID.get( change.id );
				argView.addItem(newPropView);
				oldPropView.addItem(argView);
				
				argViewIndex.put(change.argID, argView);
				propViewIndex.put(newPropView.proposition.id, newPropView);
				break;

				/*
				 * PropositionView propView = propViewIndex.get(change.propID);
				 * ArgumentView argView = new ArgumentView(change.argPro);
				 * argView.argument.id = change.argID;
				 * propView.addItem(argView); argViewIndex.put(change.argID,
				 * argView); break;
				 */
			}
			case ARG_DELETION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				argView.remove();
				argViewIndex.remove(change.argID);
				break;
			}
			case PROP_UNLINK: {
				/*
				 * reverse this: ArgumentView argView =
				 * argViewIndex.get(change.argID); PropositionView propView =
				 * propViewIndex.get(change.propID);
				 * argView.removeItem(propView);
				 */
				break;
			}
			case PROP_LINK: {
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
				/*
				 * reverse this: ArgumentView argView =
				 * argViewIndex.get(change.argID); PropositionView propView =
				 * propViewIndex.get(change.propID);
				 * argView.insertPropositionViewAt(change.argPropIndex,
				 * propView);
				 */
				break;
			}
			}
		}

	}

	/*
	 * this function moves the tree backwards in time by taking each change and
	 * undoing its effect on the tree. In the process of doing it is also saves
	 * information that will be needed by moveTreeForwards() in order to re-do
	 * changes undone by this function. For changes of type PROP_MODIFICATION,
	 * this function will save newer propContent, for changes of type
	 * PROP_ADDITION, this function will record the position of the proposition
	 * within the argument, for changes of type ARG_ADDITION, this function will
	 * record whether the argument is pro or con. This allows the program to
	 * move back and forth along the change list arbitrarily.
	 */
	public void moveTreeBackwards(int newIndex) {
		// TODO shouldn't this be index + 1, rather than index? but
		// indexOutOfBoundsException is thrown... and it seems to work the way
		// it is...
		GWT.log("----undoing changes----");
		for (int i = changesIndex; i < newIndex; i++) {
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
					mapPropIndex.put(change.id, argView.getChildIndex(propView));
					argView.removeItem(propView);
				} else {
					treeClone.removeItem(propView);
				}
				propViewIndex.remove(change.propID);
				break;
			}
			case PROP_MODIFICATION: {
				PropositionView propView = propViewIndex.get(change.propID);
				mapPropContent.put( change.id, propView.getContent());
				propView.setContent(change.propContent);
				
				break;
			}
			case ARG_ADDITION: {
				// printPropRecursive(propView, 0);
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = (PropositionView)argView.getChild(0);
				mapArgPro.put( change.id, argView.argument.pro);
				/*TODO hmmm... this is more complicated isn't it?  what about dummy nodes
				 * need to think about what happens when we add and delete nodes with
				 * loadDummys... need to make sure that the loadDummy get puts back...
				 */
				mapPropID.put( change.id, propView.proposition.id);
				argView.remove();
				argViewIndex.remove(change.argID);
				// propView.removeItem(argView);
				// println("propView:" + propView + "; argView:" + argView);
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
				/*
				 * hmmm... linking might be a problem... how do we know if we
				 * need to remove the node from the index... it might be
				 * referenced elsewhere. Perhaps more importantly... a treeItem
				 * probably cannot be anchored to two different places in a a
				 * tree (look into this...but I think you can request a
				 * treeItem's parent, which wouldn't work if it can be anchored
				 * in two places). In that case we would need two propView
				 * objects to represent the linked proposition in two different
				 * places. But how will that work with the index? I guess we
				 * could look it up by argID, and then search the argument for
				 * children with that ID?
				 */
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
						
						mapPropContent = new HashMap<Long, String>();
						mapArgPro = new HashMap<Long, Boolean>();
						mapPropIndex = new HashMap<Long, Integer>();
						mapPropID = new HashMap<Long, Long>();
						
						versionList.clear();

						buildFreshTreeClone();
						/*
						 * if (treeClone != null) { remove(treeClone); treeClone
						 * = null; }
						 */
						for (Change change : changes) {
							versionList.addItem("" + change.date + " ["
									+ change.changeType + "]", "" + change.id);
							// println("\ndisplayVersions -- " +
							// change.toString());
						}
						listBoxChangeHandlerRegistration = versionList
								.addChangeHandler(VersionsMode.this);
						versionList.setSelectedIndex(0);
						changesIndex = 0;
						onChange(null);
					}
				});
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
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

		// buildFreshTreeClone();
		modifyTreeToVersion();

		resetState(treeClone);
	}

	public void buildFreshTreeClone() {
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
		add(treeClone);
	}
}
