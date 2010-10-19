package org.argmap.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	private ScrollPanel treePanel = new ScrollPanel();
	private final int LIST_WIDTH = 20;

	// SplitLayoutPanel mainPanel;

	private HandlerRegistration listBoxChangeHandlerRegistration;
	private HandlerRegistration treeOpenHandlerRegistration;
	private HandlerRegistration treeCloseHandlerRegistration;

	/*
	 * variables related to moving the tree forwards and backwards in time
	 */
	private Map<ViewChange, String> mapPropContent;
	private Map<ViewChange, String> mapArgTitle;
	private Map<ViewChange, Integer> mapPropIndex;
	private Map<ViewChange, Integer> mapArgIndex;
	// private TimeTraveler mainTT; // TODO DELETE this line
	// private TimeMachine mainTM;
	private SortedMultiMap<Date, ViewChange> timeMachineMap;
	private Date currentDate;

	public VersionsMode(EditMode editModePair) {
		super();
		this.editMode = editModePair;
		DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.EM);
		// mainPanel = new SplitLayoutPanel();

		// add(versionList);
		mainPanel.addWest(versionList, LIST_WIDTH);
		mainPanel.add(treePanel);
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
			item.setState(((ViewNodeVer)item).isOpen());
			for (int i = 0; i < item.getChildCount(); i++) {
				recursiveResetState(item.getChild(i));
			}
		}
	}

	public void recursiveResetState_DELETE_ME(TreeItem item) {
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

						mapPropContent = new HashMap<ViewChange, String>();
						mapArgTitle = new HashMap<ViewChange, String>();
						mapArgIndex = new HashMap<ViewChange, Integer>();
						mapPropIndex = new HashMap<ViewChange, Integer>();
						currentDate = timeMachineMap.lastKey();
						// mainTM = new TimeMachine(timeMachineMap, treeClone);

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
		Long currentDate = this.currentDate.getTime();
		versionList.clear();
		ArgMap.logStart("vm.lvlftm");
		DateTimeFormat dateFormat = DateTimeFormat
				.getFormat("yyyy MMM d kk:mm:ss:SSS");

		List<Change> reverseList = getChangeList();
		Collections.reverse(reverseList);
		int i = 0;
		int newSelectionIndex = -1;
		for (Change change : reverseList) {
			Long changeTime = change.date.getTime();
			int comparison = currentDate.compareTo(changeTime);
			if (comparison == 0) {
				ArgMap.log("vm.lvlftm", "###########   Selecting Item:" + i
						+ "; changeTime:" + changeTime + "; currentDate:"
						+ currentDate);
				newSelectionIndex = i;
			} else if (comparison > 0 && newSelectionIndex == -1) {
				ArgMap.log("vm.lvlftm", "###########   Selecting Item:" + i
						+ " - 1; before item with changeTime:" + changeTime
						+ "; currentDate:" + currentDate);
				newSelectionIndex = i - 1;
			}
			versionList.addItem("" + dateFormat.format(change.date) + " ["
					+ change.changeType + "]", "" + changeTime);
			ArgMap.log("vm.lvlftm", "\n" + change);
			i++;
		}
		ArgMap.logEnd("vm.lvlftm");
		versionList.setSelectedIndex(newSelectionIndex);

		listBoxChangeHandlerRegistration = versionList
				.addChangeHandler(VersionsMode.this);
	}

	public List<Change> getChangeList() {
		List<ViewChange> viewChangeList = timeMachineMap.firstValues();
		List<Change> returnList = new ArrayList<Change>(viewChangeList.size());
		for (ViewChange viewChange : viewChangeList) {
			if (!viewChange.hidden)
				returnList.add(viewChange.change);
		}

		/*
		 * if the oldest change is hidden we need to include it anyway,
		 * otherwise there is no way to replay that change, and potentially no
		 * way to open up the closed node (because the open/close icon will not
		 * appear for a node with no children) whose closure resulted in hiding
		 * the change... and thus to way to unhide the change...
		 */
		ViewChange oldestChange = viewChangeList.get(viewChangeList.size() - 1);
		if (oldestChange.hidden) {
			returnList.add(oldestChange.change);
		}
		return returnList;
	}

	@SuppressWarnings("unused")
	private void logTree(String logName) {
		for (int i = 0; i < treeClone.getItemCount(); i++) {
			ViewNode viewNode = (ViewNode) treeClone.getItem(i);
			viewNode.logNodeRecursive(2, logName, false);
		}
	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		ArgMap.logStart("vm.oo");
		ViewNodeVer viewNodeVer = (ViewNodeVer) event.getTarget();
		ArgMap.log("vm.oo", "Adding View Changes: ");
		SortedMultiMap<Date, ViewChange> subTreeChanges = new SortedMultiMap<Date, ViewChange>();
		recursiveGetViewChanges(viewNodeVer, subTreeChanges, true, "vm.oo");
		travelFromDateToDate(viewNodeVer.getClosedDate(), currentDate,
				subTreeChanges);
		timeMachineMap.putAll(subTreeChanges);

		for (ViewChange viewChange : viewNodeVer.getViewChangeAddRemoveList()) {
			viewChange.hidden = false;
		}

		loadVersionListFromTimeMachine();
		viewNodeVer.setOpen(true);
		ArgMap.logEnd("vm.oo");
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
		ArgMap.logStart("vm.oc");
		ViewNodeVer viewNodeVer = (ViewNodeVer) event.getTarget();
		viewNodeVer.setClosedDate(currentDate);
		ArgMap.log("vm.oc", "Removing View Changes: ");
		SortedMultiMap<Date, ViewChange> subTreeChanges = new SortedMultiMap<Date, ViewChange>();
		recursiveGetViewChanges(viewNodeVer, subTreeChanges, true, "vm.oc");
		timeMachineMap.removeAll(subTreeChanges);

		for (ViewChange viewChange : viewNodeVer.getViewChangeAddRemoveList()) {
			viewChange.hidden = true;
		}

		loadVersionListFromTimeMachine();
		viewNodeVer.setOpen(false);
		ArgMap.logEnd("vm.oc");
	}

	public void recursiveGetViewChanges(ViewNodeVer viewNodeVer,
			SortedMultiMap<Date, ViewChange> forAddOrRemove, boolean firstNode,
			String logName) {
		/*
		 * if it is the first node then we don't want to remove any changes, as 
		 * add/remove changes are merely hidden, and modification changes are
		 * left entirely intact.
		 */
		if (!firstNode) {
			ArgMap.logln(logName, "nodeID: " + viewNodeVer.getNodeID()
					+ "; State: " + viewNodeVer.isOpen());
			for (ViewChange viewChange : viewNodeVer.getViewChangeList()) {
				ArgMap.logln(logName, "  viewChange: " + viewChange);
				forAddOrRemove.put(viewChange.change.date, viewChange);
			}
		}

		/*
		 * we want to process all the children of the first node regardless of
		 * whether or not it is open, because we are handling the first node's
		 * open/close event here. Also, all the changes for a closed child node
		 * will not have been removed (rather the non-modification changes are
		 * merely hidden and the modification changes are left entirely intact),
		 * so all the changes for all the children of the firstNode must be
		 * processed.
		 */
		if (firstNode || viewNodeVer.isOpen()) {
			for (int i = 0; i < viewNodeVer.getChildCount(); i++) {
				ViewNodeVer childView = viewNodeVer.getChildViewNodeVer(i);
				recursiveGetViewChanges(childView, forAddOrRemove, false,
						logName);
			}
			for (ViewNodeVer deletedView : viewNodeVer.getDeletedViewList()) {
				recursiveGetViewChanges(deletedView, forAddOrRemove, false,
						logName);
			}
		}
	}

	public void recursiveGetViewChanges_DELETE_ME_NOW(ViewNodeVer viewNodeVer,
			SortedMultiMap<Date, ViewChange> forAddOrRemove,
			boolean skipStateCheck, String logName) {

		for (int i = 0; i < viewNodeVer.getChildCount(); i++) {
			ViewNodeVer childView = viewNodeVer.getChildViewNodeVer(i);
			ArgMap.logln(logName, "Node: " + childView.getNodeID()
					+ "; State: " + childView.isOpen());
			if (childView.isOpen()) {
				for (ViewChange viewChange : childView.getViewChangeList()) {
					ArgMap.logln(logName, "nodeID: " + childView.getNodeID()
							+ " viewChange: " + viewChange);
					forAddOrRemove.put(viewChange.change.date, viewChange);
				}
				recursiveGetViewChanges(childView, forAddOrRemove, false,
						logName);
			}
		}
		for (ViewNodeVer deletedView : viewNodeVer.getDeletedViewList()) {
			ArgMap.logln(logName, "Node: " + deletedView.getNodeID()
					+ "; State: " + deletedView.isOpen());
			if (deletedView.isOpen()) {
				for (ViewChange viewChange : deletedView.getViewChangeList()) {
					ArgMap.logln(logName, "nodeID: " + deletedView.getNodeID()
							+ " viewChange: " + viewChange);
					forAddOrRemove.put(viewChange.change.date, viewChange);
				}
				recursiveGetViewChanges(deletedView, forAddOrRemove, false,
						logName);
			}
		}
	}

	/*
	 * public void recursiveAddViewChanges(ViewNodeVer viewNodeVer, boolean
	 * skipStateCheck) { for (ViewChange viewChange :
	 * viewNodeVer.getViewChangeList()) { ArgMap.logln("vm.oo", "nodeID: " +
	 * viewNodeVer.getNodeID() + " viewChange: " + viewChange);
	 * timeMachineMap.put(viewChange.change.date, viewChange); }
	 * ArgMap.logln("vm.oo", "Node: " + viewNodeVer.getNodeID() + "; State: " +
	 * viewNodeVer.getState()); if (viewNodeVer.getState() || skipStateCheck) {
	 * for (int i = 0; i < viewNodeVer.getChildCount(); i++) {
	 * recursiveAddViewChanges(viewNodeVer.getChildViewNodeVer(i), false); } for
	 * (ViewNodeVer deletedView : viewNodeVer.getDeletedViewList()) {
	 * recursiveAddViewChanges(deletedView, false); } } }
	 */

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
						mergeLoadedProposition_DELETE_ME(propView.proposition,
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
						mergeLoadedArgument_DELETE_ME(argView.argument,
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

	public void mergeLoadedProposition_DELETE_ME(Proposition proposition,
			NodesWithHistory propTreeWithHistory) {

		ArgMap.logStart("vm.mlp");
		Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
		Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

		ViewPropVer propGraft = ViewProp.recursiveBuildPropositionView(
				proposition, propTreeWithHistory.nodes, propViewIndex,
				argViewIndex, ViewPropVer.FACTORY, ViewArgVer.FACTORY);

		ArgMap.log("vm.mlp", "propTree before timeTravel:");
		propGraft.logNodeRecursive(0, "vm.mlp", true);
		//TimeTraveler timeTraveler = new TimeTraveler(propTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		// timeTraveler.travelToDate(mainTT.getCurrentDate());
		ArgMap.log("vm.mlp", "propTree after timeTravel:");
		propGraft.logNodeRecursive(0, "vm.mlp", true);

		// ViewPropVer view = mainTT.absorb(timeTraveler, propGraft);
		ArgMap.log("vm.mlp", "old propview after grafting:");
		// view.logNodeRecursive(0, "vm.mlp");
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

	public void mergeLoadedArgument_DELETE_ME(Argument argument,
			NodesWithHistory argTreeWithHistory) {

		ArgMap.logStart("vm.mla");
		Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
		Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

		ViewArgVer argGraft = ViewArg.recursiveBuildArgumentView(argument,
				argTreeWithHistory.nodes, propViewIndex, argViewIndex,
				ViewPropVer.FACTORY, ViewArgVer.FACTORY);

		ArgMap.log("vm.mla", "propTree before timeTravel:");
		argGraft.logNodeRecursive(0, "vm.mla", true);
		//TimeTraveler timeTraveler = new TimeTraveler(argTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		// timeTraveler.travelToDate(mainTT.getCurrentDate());
		ArgMap.log("vm.mla", "argTree after timeTravel:");
		argGraft.logNodeRecursive(0, "vm.mla", true);

		// ViewArgVer view = mainTT.absorb(timeTraveler, argGraft);
		ArgMap.log("vm.mla", "old propview after grafting:");
		// view.logNodeRecursive(0, "vm.mla");
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
		Date destinationDate = new Date(Long.parseLong(millisecondStr));
		travelFromDateToDate(currentDate,
				destinationDate, timeMachineMap);
		
		//TODO: if multiple copies of the same linked proposition are showing how do we know which one to make visible?
		
		treePanel.ensureVisible(timeMachineMap.get(destinationDate).get(0).viewNode);
	}

	public void travelFromDateToDate(Date currentDate, Date newDate,
			SortedMultiMap<Date, ViewChange> changes) {
		ArgMap.logStart("tm.ttd", false);
		if (newDate.before(currentDate)) {
			ArgMap.log("tm.ttd", "traveling back to date:" + newDate);
			/*
			 * here newDate is the date that the user clicked on, and is
			 * highlighted. Therefore we do not want to process newDate, because
			 * doing so would move the tree to a time before the date
			 * highlighted by the user. But the user wants the tree at the date
			 * highlighted, not before the date highlighted.
			 * 
			 * Similarly, currentDate is the date that the user had previously
			 * highlighted, and it has not been processed. So we do want to
			 * process the change associated with currentDate.
			 * 
			 * Therefore we want a list of dates to process that ranges from
			 * newDate (in the past) to currentDate (in the future) and includes
			 * currentDate but not newDate. We then want to reverse the order so
			 * we are undoing the newest changes first.
			 * 
			 * NOTE: this *was* really simple and efficient [i.e.
			 * moveTreeBackwards(changes.subMap(newDate, false, currentDate,
			 * true).descendingMap().values())], but until gwt supports
			 * navigable map we have to put the selected changes in a list and
			 * reverse them... ;
			 */
			List<List<ViewChange>> reverseList = changes.valuesSublist(newDate,
					false, currentDate, true);
			Collections.reverse(reverseList);
			moveTreeBackwards(reverseList);
		} else if (newDate.after(currentDate)) {
			/*
			 * the current tree shows the tree after the change highlighted was
			 * made. currentDate corresponds to the change higlighted. To move
			 * forward in time we do not want to include the change associated
			 * with the currentDate. We do however want to include the change
			 * associated with newDate.
			 * 
			 * Therefore we need to get a map that extends from currentDate to
			 * newDate, not including currentDate, but including newDate.
			 */
			/*
			 * this was really simple and efficient, but until gwt supports
			 * navigable map it won't work...
			 * moveTreeForwards(changes.subMap(currentDate, false, newDate,
			 * true).values());
			 */
			ArgMap.log("tm.ttd", "traveling forward to date:" + newDate);
			moveTreeForwards(changes.valuesSublist(currentDate, false, newDate,
					true));
		}
		this.currentDate = newDate;
		resetState(treeClone);
		ArgMap.logEnd("tm.ttd");
	}

	private void moveTreeForwards(Collection<List<ViewChange>> changesToProcess) {
		ArgMap.logln("tm.ttd", "----re-doing changes----");
		for (List<ViewChange> changeList : changesToProcess) {
			for (ViewChange vC : changeList) {
				ArgMap.logln("tm.ttd", "processing: " + vC.change);
				switch (vC.change.changeType) {
				case PROP_DELETION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??
					argView.removeAndSaveChildView(vC.change.propID);
					break;
				}
				case PROP_ADDITION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??

					argView.reviveDeletedView(vC.change.propID,
							mapPropIndex.get(vC));
					break;
				}
				case PROP_MODIFICATION: {
					/*
					 * NOTE: the same content might be put in the mapPropContent
					 * multiple times if there are multiple links to the same
					 * node, but it shouldn't be a problem, because the change
					 * for each link should be processed one right after the
					 * other, without a chance for the content to be changed by
					 * a different change...
					 */
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					propView.setContent(mapPropContent.get(vC));
					break;
				}
				case ARG_ADDITION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					propView.reviveDeletedView(vC.change.argID,
							mapArgIndex.get(vC));
					break;
				}
				case ARG_DELETION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					propView.removeAndSaveChildView(vC.change.argID);
					break;
				}
				case ARG_MODIFICATION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					argView.setArgTitle(mapArgTitle.get(vC));
					break;
				}
				case PROP_UNLINK: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					argView.removeAndSaveChildView(vC.change.propID);
					/*
					 * reverse this: ArgumentView argView =
					 * argViewIndex.get(change.argID); PropositionView propView
					 * = propViewIndex.get(change.propID);
					 * argView.removeItem(propView);
					 */
					break;
				}
				case PROP_LINK: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??
					argView.reviveDeletedView(vC.change.propID,
							mapPropIndex.get(vC));

					/*
					 * TODO need to think through linking and unlinking in more
					 * detail. What I have here will not be enough. Specifically
					 * the server probably doesn't currently send the linked
					 * proposition's content which will be important for the
					 * client to display when reviewing revisions. Furthermore,
					 * with all the other operations, a given change will only
					 * add a node. But here the change will add hundreds of
					 * nodes, the children of the linked proposition. Probably
					 * the way to show that is simply to add the proposition,
					 * and then lazy load the tree as someone browses.
					 */
					/*
					 * reverse this: ArgumentView argView =
					 * argViewIndex.get(change.argID); PropositionView propView
					 * = propViewIndex.get(change.propID);
					 * argView.insertPropositionViewAt(change.argPropIndex,
					 * propView);
					 */
					break;
				}
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
	private void moveTreeBackwards(Collection<List<ViewChange>> changesToProcess) {
		ArgMap.logln("tm.ttd", "----undoing changes----");
		for (List<ViewChange> changeList : changesToProcess) {
			for (ViewChange vC : changeList) {
				ArgMap.logln("tm.ttd", "processing: " + vC.change);
				switch (vC.change.changeType) {
				case PROP_DELETION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??
					argView.reviveDeletedView(vC.change.propID,
							vC.change.argPropIndex);
					ViewProp viewPropVer = argView
							.getPropView(vC.change.argPropIndex);
					viewPropVer.proposition.content = vC.change.content;
					break;
				}
				case PROP_ADDITION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??
					int index = argView.indexOfChildWithID(vC.change.propID);
					mapPropIndex.put(vC, index);
					argView.removeAndSaveChildView(vC.change.propID);
					break;
				}
				case PROP_MODIFICATION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					/*
					 * NOTE: the same content might be put in the mapPropContent
					 * multiple times if there are multiple links to the same
					 * node, but it shouldn't be a problem, because the change
					 * for each link should be processed one right after the
					 * other, without a chance for the content to be changed by
					 * a different change...
					 */
					mapPropContent.put(vC, propView.getContent());
					propView.setContent(vC.change.content);
					break;
				}
				case ARG_ADDITION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					int index = propView.indexOfChildWithID(vC.change.argID);
					mapArgIndex.put(vC, index);
					propView.removeAndSaveChildView(vC.change.argID);
					break;
				}
				case ARG_DELETION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					propView.reviveDeletedView(vC.change.argID,
							vC.change.argPropIndex);
					ViewArg viewArgVer = propView
							.getArgView(vC.change.argPropIndex);
					viewArgVer.argument.pro = vC.change.argPro;
					viewArgVer.setArgTitle(vC.change.content);
					break;
				}
				case ARG_MODIFICATION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					/* NOTE: see not regarding arg modification */
					mapArgTitle.put(vC, argView.getArgTitle());
					argView.setArgTitle(vC.change.content);
					break;
				}
				case PROP_UNLINK: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??
					argView.reviveDeletedView(vC.change.propID,
							vC.change.argPropIndex);

					/*
					 * hmmm... linking might be a problem... how do we know if
					 * we need to remove the node from the index... it might be
					 * referenced elsewhere. Perhaps more importantly... a
					 * treeItem probably cannot be anchored to two different
					 * places in a a tree (look into this...but I think you can
					 * request a treeItem's parent, which wouldn't work if it
					 * can be anchored in two places). In that case we would
					 * need two propView objects to represent the linked
					 * proposition in two different places. But how will that
					 * work with the index? I guess we could look it up by
					 * argID, and then search the argument for children with
					 * that ID?
					 * 
					 * ViewArgVer argView = argViewIndex.get(change.argID);
					 * ViewPropVer propView = propViewIndex.get(change.propID);
					 * argView.removeItem(propView);
					 */
					break;
				}
				case PROP_LINK: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					int index = argView.indexOfChildWithID(vC.change.propID);
					mapPropIndex.put(vC, index);
					argView.removeAndSaveChildView(vC.change.propID);

					/*
					 * ViewArgVer argView = argViewIndex.get(change.argID);
					 * ViewPropVer propView = propViewIndex.get(change.propID);
					 * 
					 * TODO need to think through linking and unlinking in more
					 * detail. What I have here will not be enough. Specifically
					 * the server probably doesn't currently send the linked
					 * proposition's content which will be important for the
					 * client to display when reviewing revisions. Furthermore,
					 * with all the other operations, a given change will only
					 * add a node. But here the change will add hundreds of
					 * nodes, the children of the linked proposition. Probably
					 * the way to show that is simply to add the proposition,
					 * and then lazy load the tree and someone browses.
					 * 
					 * argView.insertPropositionViewAt(change.argPropIndex,
					 * propView);
					 */
					break;
				}
				}
			}
		}
	}
}
