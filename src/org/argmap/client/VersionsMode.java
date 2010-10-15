package org.argmap.client;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodesWithHistory;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class VersionsMode extends ResizeComposite implements
		CloseHandler<TreeItem>, OpenHandler<TreeItem>, ChangeHandler {

	private ListBox versionList = new ListBox();
	private EditMode editMode;
	private Tree treeClone = null;
	private TimeTraveler mainTT; // TODO DELETE this line
	private TimeMachine mainTM;
	SortedMultiMap<Date, ViewChange> timeMachineMap;
	private FlowPanel treePanel = new FlowPanel();
	private final int LIST_WIDTH = 20;

	// SplitLayoutPanel mainPanel;

	private HandlerRegistration listBoxChangeHandlerRegistration;
	private HandlerRegistration treeOpenHandlerRegistration;
	private HandlerRegistration treeCloseHandlerRegistration;

	public VersionsMode(EditMode editModePair) {
		super();
		this.editMode = editModePair;
		DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.EM);
		// mainPanel = new SplitLayoutPanel();

		// add(versionList);
		mainPanel.addWest(versionList, LIST_WIDTH);
		mainPanel.add(new ScrollPanel(treePanel));
		// mainPanel.add(new Label("HEELO"));

		versionList.setVisibleItemCount(2);
		versionList.setWidth(LIST_WIDTH + "em");

		listBoxChangeHandlerRegistration = versionList.addChangeHandler(this);
		initWidget(mainPanel);
	}

	public void resetState(Tree tree) {
		treeOpenHandlerRegistration.removeHandler();
		treeCloseHandlerRegistration.removeHandler();
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveResetState(tree.getItem(i));
		}
		treeClone.addOpenHandler(this);
		treeClone.addCloseHandler(this);
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

	public void displayVersions_DELETE_ME() {
		versionList.clear();
		versionList.addItem("Loading Revision History From Server...");
		if (treeClone != null) {
			treePanel.remove(treeClone);
		}
		List<Proposition> props = new LinkedList<Proposition>();
		List<Argument> args = new LinkedList<Argument>();
		editMode.getOpenPropsAndArgs(props, args);
		ServerComm.getRevisions(null, props, args,
				new ServerComm.LocalCallback<SortedMap<Date, Change>>() {

					@Override
					public void call(SortedMap<Date, Change> changes) {

						GWT.log("Got back " + changes.size() + " changes");

						Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
						Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

						treeClone = new Tree();
						treeCloseHandlerRegistration = treeClone
								.addCloseHandler(VersionsMode.this);
						treeOpenHandlerRegistration = treeClone
								.addOpenHandler(VersionsMode.this);
						/*
						 * TODO this could be done outside of the callback to
						 * reduce the user's wait time, but would need to ensure
						 * that it finishing before the callback proceeds. can
						 * the callback just keep calling something like wait()
						 * until it finds that the clone is finished?
						 */
						/*
						 * preventing the new method from compiling:
						 * editMode.buildTreeCloneOfOpenNodesWithIndexes(
						 * treeClone, propViewIndex, argViewIndex);
						 */
						treePanel.add(treeClone);

						mainTT = new TimeTraveler(changes, propViewIndex,
								argViewIndex, treeClone);

						loadVersionListFromTimeMachine();

						versionList.setSelectedIndex(0);

						onChange(null);
					}
				});
	}

	public void displayVersions() {
		versionList.clear();
		versionList.addItem("Loading Revision History From Server...");
		if (treeClone != null) {
			treePanel.remove(treeClone);
		}
		List<Proposition> props = new LinkedList<Proposition>();
		List<Argument> args = new LinkedList<Argument>();
		editMode.getOpenPropsAndArgs(props, args);
		ServerComm.getChanges(props, args,
				new ServerComm.LocalCallback<NodeChangesMaps>() {

					@Override
					public void call(NodeChangesMaps changesMaps) {
						ArgMap.logStart("vm.dv.c");
						ArgMap.log("vm.dv.c", "Got back these changes:\n"
								+ changesMaps.toString());

						treeClone = new Tree();
						treeCloseHandlerRegistration = treeClone
								.addCloseHandler(VersionsMode.this);
						treeOpenHandlerRegistration = treeClone
								.addOpenHandler(VersionsMode.this);
						/*
						 * TODO this could be done outside of the callback to
						 * reduce the user's wait time, but would need to ensure
						 * that it finishing before the callback proceeds. can
						 * the callback just keep calling something like wait()
						 * until it finds that the clone is finished?
						 */
						editMode.buildTreeCloneOfOpenNodesWithIndexes(treeClone);

						treePanel.add(treeClone);

						timeMachineMap = prepTreeWithDeletedNodesAndChangseAndBuildTimeMachineMap(
								treeClone, changesMaps);

						mainTM = new TimeMachine(timeMachineMap, treeClone);

						loadVersionListFromTimeMachine();

						versionList.setSelectedIndex(0);

						onChange(null);
						resetState(treeClone);
						ArgMap.logEnd("vm.dv.c");
					}
				});

	}

	private SortedMultiMap<Date, ViewChange> prepTreeWithDeletedNodesAndChangseAndBuildTimeMachineMap(
			Tree treeClone, NodeChangesMaps changesMaps) {
		SortedMultiMap<Date, ViewChange> timeMachineMap = new SortedMultiMap<Date, ViewChange>();
		for (int i = 0; i < treeClone.getItemCount(); i++) {
			recursivePrepAndBuild((ViewPropVer) treeClone.getItem(i),
					timeMachineMap, changesMaps);
		}
		return timeMachineMap;
	}

	public void recursivePrepAndBuild(ViewPropVer viewProp,
			SortedMultiMap<Date, ViewChange> timeMachineMap,
			NodeChangesMaps changesMaps) {
		NodeChanges<Proposition> nodeChanges = changesMaps.propChanges
				.get(viewProp.getNodeID());
		for (Long id : nodeChanges.deletedChildIDs) {
			ViewArgVer deletedView = viewProp.createDeletedView(id);
			recursivePrepAndBuild(deletedView, timeMachineMap, changesMaps);
		}
		for (int i = 0; i < viewProp.getChildCount(); i++) {
			// TODO how to get rid of this cast?
			/* TODO what about dummy nodes!!!! this should create a cast error */
			recursivePrepAndBuild((ViewArgVer) viewProp.getArgView(i),
					timeMachineMap, changesMaps);
		}
		for (Change change : nodeChanges.changes) {
			ViewChange viewChange = new ViewChange();
			viewChange.change = change;
			viewChange.viewNode = viewProp;
			viewProp.viewChanges.add(viewChange);
			timeMachineMap.put(change.date, viewChange);
		}
	}

	public void recursivePrepAndBuild(ViewArgVer viewArg,
			SortedMultiMap<Date, ViewChange> timeMachineMap,
			NodeChangesMaps changesMaps) {
		NodeChanges<Argument> nodeChanges = changesMaps.argChanges.get(viewArg
				.getNodeID());
		for (Long id : nodeChanges.deletedChildIDs) {
			ViewPropVer deletedView = viewArg.createDeletedView(id);
			recursivePrepAndBuild(deletedView, timeMachineMap, changesMaps);
		}
		for (int i = 0; i < viewArg.getChildCount(); i++) {
			// TODO how to get rid of this cast?
			recursivePrepAndBuild((ViewPropVer) viewArg.getPropView(i),
					timeMachineMap, changesMaps);
		}
		for (Change change : nodeChanges.changes) {
			ViewChange viewChange = new ViewChange();
			viewChange.change = change;
			viewChange.viewNode = viewArg;
			viewArg.viewChanges.add(viewChange);
			timeMachineMap.put(change.date, viewChange);
		}
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
		ArgMap.logStart("vm.lvlftm");
		DateTimeFormat dateFormat = DateTimeFormat
				.getFormat("yyyy MMM d kk:mm:ss:SSS");

		List<Change> reverseList = mainTM.getChangeList();
		Collections.reverse(reverseList);
		int i = 0;
		int newSelectionIndex = 0;
		for (Change change : reverseList) {
			String timeString = "" + change.date.getTime();
			if (timeString.equals(currentDate)) {
				ArgMap.log("vm.lvlftm", "###########   Selecting Item:" + i
						+ "; timeString:" + timeString + "; currentDate:"
						+ currentDate);
				newSelectionIndex = i;
			}
			versionList.addItem("" + dateFormat.format(change.date) + " ["
					+ change.changeType + "]", "" + timeString);
			ArgMap.log("vm.lvlftm", "\n" + change);
			i++;
		}
		ArgMap.logEnd("vm.lvlftm");
		versionList.setSelectedIndex(newSelectionIndex);

		listBoxChangeHandlerRegistration = versionList
				.addChangeHandler(VersionsMode.this);
	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		ArgMap.logStart("vm.oo");
		ViewNodeVer viewNodeVer = (ViewNodeVer) event.getTarget();
		ArgMap.log("vm.oo", "Adding View Changes: ");
		recursiveAddViewChanges(viewNodeVer);
		loadVersionListFromTimeMachine();
		ArgMap.logEnd("vm.oo");
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
		ArgMap.logStart("vm.oc");
		ViewNodeVer viewNodeVer = (ViewNodeVer) event.getTarget();
		ArgMap.log("vm.oc", "Removing View Changes: ");
		recursiveRemoveViewChanges(viewNodeVer);
		loadVersionListFromTimeMachine();
		ArgMap.logEnd("vm.oc");
	}

	public void recursiveRemoveViewChanges(ViewNodeVer viewNodeVer) {
		for (ViewChange viewChange : viewNodeVer.getViewChangeList()) {
			ArgMap.logln("vm.oc", "nodeID: " + viewNodeVer.getNodeID()
					+ " viewChange: " + viewChange);
			timeMachineMap.remove(viewChange.change.date, viewChange);
		}
		ArgMap.logln("vm.oc", "Node: " + viewNodeVer.getNodeID() + "; State: "
				+ viewNodeVer.getState());
		/*
		 * TODO this doesn't work because this function is called after the
		 * first node's state is changed!!!!!!!!!!!!!!!!!!!!!! so it never
		 * gets past the first node
		 */
		if (viewNodeVer.getState()) {
			for (int i = 0; i < viewNodeVer.getChildCount(); i++) {
				recursiveRemoveViewChanges(viewNodeVer.getChildViewNodeVer(i));
			}
			for (ViewNodeVer deletedView : viewNodeVer.getDeletedViewList()) {
				recursiveRemoveViewChanges(deletedView);
			}
		}
	}

	public void recursiveAddViewChanges(ViewNodeVer viewNodeVer) {
		for (ViewChange viewChange : viewNodeVer.getViewChangeList()) {
			ArgMap.logln("vm.oo", "nodeID: " + viewNodeVer.getNodeID()
					+ " viewChange: " + viewChange);
			timeMachineMap.put(viewChange.change.date, viewChange);
		}
		ArgMap.logln("vm.oo", "Node: " + viewNodeVer.getNodeID() + "; State: "
				+ viewNodeVer.getState());
		if (viewNodeVer.getState()) {
			for (int i = 0; i < viewNodeVer.getChildCount(); i++) {
				recursiveAddViewChanges(viewNodeVer.getChildViewNodeVer(i));
			}
			for (ViewNodeVer deletedView : viewNodeVer.getDeletedViewList()) {
				recursiveAddViewChanges(deletedView);
			}
		}
	}

	public void onClose_DELETE_ME(CloseEvent<TreeItem> event) {
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0) {
			if (treeItem.getChild(0).getStyleName().equals("loadDummyProp")) {
				// ServerComm.getPropositionCurrentVersionAndHistory(prop,
				// localCallback)
				GWT.log("VersionsMode.onClose[loadDummyProp]:  NOT REMOVING -- METHOD NOT IMPLEMENTED!");
			} else if (treeItem.getChild(0).getStyleName()
					.equals("loadDummyArg")) {
				GWT.log("VersionsMode.onClose[loadDummyArg]:  NOT REMOVING -- METHOD NOT IMPLEMENTED!");
			}
		}

	}

	public void onOpen_DELETE_ME(OpenEvent<TreeItem> event) {
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0) {
			TreeItem child = treeItem.getChild(0);
			if (child.getStyleName().equals("loadDummyProp")) {
				class Callback implements
						ServerComm.LocalCallback<NodesWithHistory> {
					ViewPropEdit propView;

					@Override
					public void call(NodesWithHistory propTreeWithHistory) {
						mergeLoadedProposition(propView.proposition,
								propTreeWithHistory);
					}
				}
				Callback callback = new Callback();
				callback.propView = (ViewPropEdit) treeItem;

				ServerComm.getPropositionCurrentVersionAndHistory(
						((ViewPropEdit) treeItem).proposition, callback);
			} else if (child.getStyleName().equals("loadDummyArg")) {
				class Callback implements LocalCallback<NodesWithHistory> {
					ViewArgVer argView;

					@Override
					public void call(NodesWithHistory argTreeWithHistory) {
						mergeLoadedArgument(argView.argument,
								argTreeWithHistory);
					}
				}
				Callback callback = new Callback();
				callback.argView = (ViewArgVer) treeItem;

				ServerComm.getArgumentCurrentVersionAndHistory(
						((ViewArgVer) treeItem).argument, callback);
			}
		}

	}

	public void mergeLoadedProposition(Proposition proposition,
			NodesWithHistory propTreeWithHistory) {

		ArgMap.logStart("vm.mlp");
		Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
		Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

		ViewPropVer propGraft = ViewProp.recursiveBuildPropositionView(
				proposition, propTreeWithHistory.nodes, propViewIndex,
				argViewIndex, ViewPropVer.FACTORY, ViewArgVer.FACTORY);

		ArgMap.log("vm.mlp", "propTree before timeTravel:");
		propGraft.logNodeRecursive(0, "vm.mlp");
		TimeTraveler timeTraveler = new TimeTraveler(
				propTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		timeTraveler.travelToDate(mainTT.getCurrentDate());
		ArgMap.log("vm.mlp", "propTree after timeTravel:");
		propGraft.logNodeRecursive(0, "vm.mlp");

		ViewPropVer view = mainTT.absorb(timeTraveler, propGraft);
		ArgMap.log("vm.mlp", "old propview after grafting:");
		view.logNodeRecursive(0, "vm.mlp");
		ArgMap.log("vm.mlp", "----------------");

		loadVersionListFromTimeMachine();

		// GWT.log("Prop tree transplant");
		// printPropRecursive(propTree, 0);
		ArgMap.log("vm.mlp", "changes");
		for (Change change : propTreeWithHistory.changes.values()) {
			GWT.log(change.toString());
		}
		ArgMap.log("vm.mlp", "mergeLoadedPropositon: start");
	}

	public void mergeLoadedArgument(Argument argument,
			NodesWithHistory argTreeWithHistory) {

		ArgMap.logStart("vm.mla");
		Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
		Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

		ViewArgVer argGraft = ViewArg.recursiveBuildArgumentView(argument,
				argTreeWithHistory.nodes, propViewIndex, argViewIndex,
				ViewPropVer.FACTORY, ViewArgVer.FACTORY);

		ArgMap.log("vm.mla", "propTree before timeTravel:");
		argGraft.logNodeRecursive(0, "vm.mla");
		TimeTraveler timeTraveler = new TimeTraveler(
				argTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		timeTraveler.travelToDate(mainTT.getCurrentDate());
		ArgMap.log("vm.mla", "argTree after timeTravel:");
		argGraft.logNodeRecursive(0, "vm.mla");

		ViewArgVer view = mainTT.absorb(timeTraveler, argGraft);
		ArgMap.log("vm.mla", "old propview after grafting:");
		view.logNodeRecursive(0, "vm.mla");
		ArgMap.log("vm.mla", "----------------");

		loadVersionListFromTimeMachine();

		// GWT.log("Prop tree transplant");
		// printPropRecursive(propTree, 0);
		ArgMap.log("vm.mla", "changes");
		for (Change change : argTreeWithHistory.changes.values()) {
			GWT.log(change.toString());
		}
		ArgMap.logEnd("vm.mla");
	}

	@Override
	public void onChange(ChangeEvent event) {
		String millisecondStr = versionList.getValue(versionList
				.getSelectedIndex());
		mainTM.travelToDate(new Date(Long.parseLong(millisecondStr)));
	}
}
