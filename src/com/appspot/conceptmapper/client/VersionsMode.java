package com.appspot.conceptmapper.client;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.appspot.conceptmapper.client.PropositionService.ArgTreeWithHistory;
import com.appspot.conceptmapper.client.PropositionService.PropTreeWithHistory;
import com.appspot.conceptmapper.client.ServerComm.LocalCallback;
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
				&& !item.getChild(0).getStyleName().equals("loadDummyProp")
				&& !item.getChild(0).getStyleName().equals("loadDummyArg")) {
			item.setState(true);
			for (int i = 0; i < item.getChildCount(); i++) {
				recursiveResetState(item.getChild(i));
			}
		}
	}
/*
	public void printPropRecursive(PropositionView propViewParent, int level) {
		GWT.log(spaces(level * 2) + "propID:" + propViewParent.proposition.id
				+ "; content:" + propViewParent.getContent());
		for (int i = 0; i < propViewParent.getChildCount(); i++) {
			ArgumentView arg = (ArgumentView) propViewParent.getChild(i);
			printArgRecursive( arg, level + 1 );
		}
	}
	
	public void printArgRecursive( ArgumentView arg, int level ){
		GWT.log(spaces(level * 2) + arg.getText() + "; id:"
				+ arg.argument.id);
		for (int j = 0; j < arg.getChildCount(); j++) {
			printPropRecursive((PropositionView) arg.getChild(j), level + 1);
		}
	}

	public String spaces(int spaces) {
		String string = "";
		for (int i = 0; i < spaces; i++) {
			string = string + " ";
		}
		return string;
	}
	*/

	public void displayVersions() {
		versionList.clear();
		versionList.addItem("Loading Revision History From Server...");
		if (treeClone != null) {
			remove(treeClone);
		}
		List<Proposition> props = new LinkedList<Proposition>();
		List<Argument> args = new LinkedList<Argument>();
		editMode.getOpenPropsAndArgs(props, args);
		ServerComm.getChanges(null, props, args,
				new ServerComm.LocalCallback<SortedMap<Date, Change>>() {

					@Override
					public void call(SortedMap<Date, Change> changes) {

						GWT.log("Got back " + changes.size() + " changes");

						Map<Long, PropositionView> propViewIndex = new HashMap<Long, PropositionView>();
						Map<Long, ArgumentView> argViewIndex = new HashMap<Long, ArgumentView>();

						treeClone = new Tree();
						treeClone.addCloseHandler(VersionsMode.this);
						treeClone.addOpenHandler(VersionsMode.this);
						/*
						 * TODO this could be done outside of the callback to
						 * reduce the user's wait time, but would need to ensure
						 * that it finishing before the callback proceeds. can
						 * the callback just keep calling something like wait()
						 * until it finds that the clone is finished?
						 */
						editMode.buildTreeCloneOfOpenNodesWithIndexes(
								treeClone, propViewIndex, argViewIndex);
						add(treeClone);

						mainTT = new TimeTraveler(changes, propViewIndex,
								argViewIndex, treeClone);

						loadVersionListFromTimeMachine();

						versionList.setSelectedIndex(0);

						onChange(null);
					}
				});
	}

	private void loadVersionListFromTimeMachine() {
		listBoxChangeHandlerRegistration.removeHandler();
		int selectedIndex = versionList.getSelectedIndex();
		String currentDate;
		if (selectedIndex >= 0) {
			currentDate = versionList.getValue(selectedIndex);
		} else {
			currentDate = null;
		}
		versionList.clear();

		List<Change> reverseList = mainTT.getChangeList();
		Collections.reverse(reverseList);
		int i = 0;
		int newSelectionIndex = 0;
		for (Change change : reverseList) {
			String timeString = "" + change.date.getTime();
			if (timeString.equals(currentDate)) {
				GWT.log("###########   Selecting Item:" + i);
				newSelectionIndex = i;
			}
			versionList.addItem("" + change.date + " [" + change.changeType
					+ "]", "" + timeString);
			i++;
		}

		versionList.setSelectedIndex(newSelectionIndex);

		listBoxChangeHandlerRegistration = versionList
				.addChangeHandler(VersionsMode.this);
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
				class Callback
						implements
						ServerComm.LocalCallback<PropTreeWithHistory> {
					PropositionView propView;

					@Override
					public void call(PropTreeWithHistory propTreeWithHistory) {
						mergeLoadedProposition(propView, propTreeWithHistory);
					}
				}
				Callback callback = new Callback();
				callback.propView = (PropositionView) treeItem;

				ServerComm.getPropositionCurrentVersionAndHistory(
						((PropositionView) treeItem).proposition, callback);
			} else if (child.getStyleName().equals("loadDummyArg")) {
				class Callback
						implements
						LocalCallback<ArgTreeWithHistory> {
					ArgumentView argView;

					@Override
					public void call(ArgTreeWithHistory argTreeWithHistory) {
						mergeLoadedArgument(argView, argTreeWithHistory);
					}
				}
				Callback callback = new Callback();
				callback.argView = (ArgumentView) treeItem;

				ServerComm.getArgumentCurrentVersionAndHistory(
						((ArgumentView) treeItem).argument, callback); 
			}
		}

	}

	public void mergeLoadedProposition(PropositionView propViewParent,
			PropTreeWithHistory propTreeWithHistory) {

		GWT.log("mergeLoadedPropositon: start");
		Map<Long, PropositionView> propViewIndex = new HashMap<Long, PropositionView>();
		Map<Long, ArgumentView> argViewIndex = new HashMap<Long, ArgumentView>();

		PropositionView propGraft = PropositionView
				.recursiveBuildPropositionView(propTreeWithHistory.proposition,
						false, propViewIndex, argViewIndex);

		GWT.log("propTree before timeTravel:");
		propGraft.printPropRecursive( 0);
		TimeTraveler timeTraveler = new TimeTraveler(
				propTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		timeTraveler.travelToDate(mainTT.getCurrentDate());
		GWT.log("propTree after timeTravel:");
		propGraft.printPropRecursive( 0);

		PropositionView view = mainTT.absorb(timeTraveler, propGraft);
		GWT.log("old propview after grafting:");
		view.printPropRecursive( 0);
		GWT.log("----------------");

		loadVersionListFromTimeMachine();

		// GWT.log("Prop tree transplant");
		// printPropRecursive(propTree, 0);
		GWT.log("changes");
		for (Change change : propTreeWithHistory.changes.values()) {
			GWT.log(change.toString());
		}
		GWT.log("mergeLoadedPropositon: start");
	}
	
	public void mergeLoadedArgument(ArgumentView argViewParent,
			ArgTreeWithHistory argTreeWithHistory) {

		GWT.log("mergeLoadedArgument: start");
		Map<Long, PropositionView> propViewIndex = new HashMap<Long, PropositionView>();
		Map<Long, ArgumentView> argViewIndex = new HashMap<Long, ArgumentView>();

		ArgumentView argGraft = PropositionView
				.recursiveBuildArgumentView(argTreeWithHistory.argument,
						false, propViewIndex, argViewIndex);

		GWT.log("propTree before timeTravel:");
		argGraft.printArgRecursive( 0);
		TimeTraveler timeTraveler = new TimeTraveler(
				argTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		timeTraveler.travelToDate(mainTT.getCurrentDate());
		GWT.log("argTree after timeTravel:");
		argGraft.printArgRecursive( 0);

		ArgumentView view = mainTT.absorb(timeTraveler, argGraft);
		GWT.log("old propview after grafting:");
		view.printArgRecursive( 0);
		GWT.log("----------------");

		loadVersionListFromTimeMachine();

		// GWT.log("Prop tree transplant");
		// printPropRecursive(propTree, 0);
		GWT.log("changes");
		for (Change change : argTreeWithHistory.changes.values()) {
			GWT.log(change.toString());
		}
		GWT.log("mergeLoadedPropositon: start");
	}

	@Override
	public void onChange(ChangeEvent event) {
		String millisecondStr = versionList.getValue(versionList
				.getSelectedIndex());
		mainTT.travelToDate(new Date(Long.parseLong(millisecondStr)));
		resetState(treeClone);
	}
}
