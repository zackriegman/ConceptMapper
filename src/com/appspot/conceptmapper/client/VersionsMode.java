package com.appspot.conceptmapper.client;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import com.appspot.conceptmapper.client.PropositionService.PropTreeWithHistory;
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
	private TimeTraveler mainTT;

	private HandlerRegistration listBoxChangeHandlerRegistration;

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

	public void printPropRecursive(PropositionView propViewParent, int level) {
		GWT.log(spaces(level * 2) + "propID:" + propViewParent.proposition.id
				+ "; content:" + propViewParent.getContent());
		for (int i = 0; i < propViewParent.getChildCount(); i++) {
			ArgumentView arg = (ArgumentView) propViewParent.getChild(i);
			GWT.log(spaces((level + 1) * 2) + arg.getText() + "; id:"
					+ arg.argument.id);
			for (int j = 0; j < arg.getChildCount(); j++) {
				printPropRecursive((PropositionView) arg.getChild(j), level + 2);
			}
		}
	}

	public String spaces(int spaces) {
		String string = "";
		for (int i = 0; i < spaces; i++) {
			string = string + " ";
		}
		return string;
	}

	public void displayVersions() {
		List<Proposition> props = new LinkedList<Proposition>();
		List<Argument> args = new LinkedList<Argument>();
		editMode.getOpenPropsAndArgs(props, args);
		ServerComm.getChanges(null, props, args,
				new ServerComm.GetChangesCallback() {

					@Override
					public void call(NavigableMap<Date, Change> changes) {

						GWT.log("Got back " + changes.size() + " changes");
						listBoxChangeHandlerRegistration.removeHandler();

						versionList.clear();

						Map<Long, PropositionView> propViewIndex = new HashMap<Long, PropositionView>();
						Map<Long, ArgumentView> argViewIndex = new HashMap<Long, ArgumentView>();
						buildFreshTreeClone(propViewIndex, argViewIndex);

						mainTT = new TimeTraveler(changes, propViewIndex,
								argViewIndex, treeClone);
						/*
						 * if (treeClone != null) { remove(treeClone); treeClone
						 * = null; }
						 */
						for (Change change : changes.values()) {
							versionList.addItem("" + change.date.getTime() + " ["
									+ change.changeType + "]", "" + change.date);
							// println("\ndisplayVersions -- " +
							// change.toString());
						}
						listBoxChangeHandlerRegistration = versionList
								.addChangeHandler(VersionsMode.this);
						versionList.setSelectedIndex(0);
						onChange(null);
					}
				});
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0) {
			if (treeItem.getChild(0).getStyleName().equals("loadDummyProp")) {
				// ServerComm.getPropositionCurrentVersionAndHistory(prop,
				// localCallback)
				GWT
						.log("VersionsMode.onClose[loadDummyProp]:  NOT REMOVING -- METHOD NOT IMPLEMENTED!");
			} else if (treeItem.getChild(0).getStyleName().equals(
					"loadDummyArg")) {
				GWT
						.log("VersionsMode.onClose[loadDummyArg]:  NOT REMOVING -- METHOD NOT IMPLEMENTED!");
			}
		}

	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0) {
			TreeItem child = treeItem.getChild(0);
			if (child.getStyleName().equals("loadDummyProp")) {
				ServerComm
						.getPropositionCurrentVersionAndHistory(
								((PropositionView) treeItem).proposition,
								new ServerComm.GetPropositionCurrentVersionAndHistoryCallback() {

									@Override
									public void call(
											PropTreeWithHistory propTreeWithHistory) {
										mergeLoadedProposition(propTreeWithHistory);
									}
								});
			} else if (child.getStyleName().equals("loadDummyArg")) {
				GWT
						.log("VersionsMode.onOpen[loadDummyArg]:  NOT ADDING -- METHOD NOT IMPLEMENTED!");
			}
		}

	}

	public void mergeLoadedProposition(PropTreeWithHistory propTreeWithHistory) {

		Map<Long, PropositionView> propViewIndex = new HashMap<Long, PropositionView>();
		Map<Long, ArgumentView> argViewIndex = new HashMap<Long, ArgumentView>();

		PropositionView propTree = PropositionView
				.recursiveBuildPropositionView(propTreeWithHistory.proposition,
						false, propViewIndex, argViewIndex);

		TimeTraveler timeTraveler = new TimeTraveler(
				propTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * TODO hmmm... maybe time travelers need to work on dates instead...
		 * that might be more adaptable than trying to figure out at this point
		 * what the right change index is...
		 */
		timeTraveler.travelToDate(mainTT.getCurrentDate());
		

		printPropRecursive(propTree, 0);
		for (Change change : propTreeWithHistory.changes.values()) {
			GWT.log(change.toString());
		}
	}

	@Override
	public void onChange(ChangeEvent event) {
		String millisecondStr = versionList.getValue(versionList.getSelectedIndex());
		mainTT.travelToDate( new Date(Long.parseLong(millisecondStr)) );
		resetState(treeClone);
	}

	public void buildFreshTreeClone(Map<Long, PropositionView> propViewIndex,
			Map<Long, ArgumentView> argViewIndex) {
		if (treeClone != null) {
			remove(treeClone);
		}
		treeClone = new Tree();
		treeClone.addCloseHandler(VersionsMode.this);
		treeClone.addOpenHandler(VersionsMode.this);
		editMode.buildTreeCloneOfOpenNodesWithIndexes(treeClone, propViewIndex,
				argViewIndex);
		add(treeClone);
	}
}
