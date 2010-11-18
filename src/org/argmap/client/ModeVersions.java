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
import org.argmap.client.ArgMapService.NodeWithChanges;

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

public class ModeVersions extends ResizeComposite implements
		CloseHandler<TreeItem>, OpenHandler<TreeItem>, ChangeHandler {

	private final ListBox versionList = new ListBox();
	private final ModeEdit editMode;
	private ArgTree treeClone = null;
	private final ScrollPanel treePanel = new ScrollPanel();
	private final int LIST_WIDTH = 20;

	// SplitLayoutPanel mainPanel;

	private HandlerRegistration listBoxChangeHandlerRegistration;

	/*
	 * variables related to moving the tree forwards and backwards in time
	 */
	private Map<ViewChange, String> mapPropContent;
	private Map<ViewChange, String> mapArgTitle;
	private Map<ViewChange, Integer> mapPropIndex;
	private Map<ViewChange, Integer> mapArgIndex;
	private SortedMultiMap<Date, ViewChange> timeMachineMap;
	private Date currentDate;

	public ModeVersions(ModeEdit editModePair) {
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
						Log log = Log.getLog("vm.dv.c");
						if (Log.on)
							log.log("Got back these changes:\n"
									+ changesMaps.toString());

						treeClone = new ArgTree();
						treeClone.addCloseHandlerTracked(ModeVersions.this);
						treeClone.addOpenHandlerTracked(ModeVersions.this);
						/*
						 * TODO this could be done outside of the callback to
						 * reduce the user's wait time, but would need to ensure
						 * that it finishing before the callback proceeds. can
						 * the callback just keep calling something like wait()
						 * until it finds that the clone is finished?
						 */
						editMode.buildTreeCloneOfOpenNodes(treeClone);

						treePanel.add(treeClone);

						timeMachineMap = prepTreeWithDeletedNodesAndChangseAndBuildTimeMachineMap(
								treeClone, changesMaps);

						if (Log.on) {
							Log treeLog = Log.getLog("vm.dv.tree");
							treeClone.logTree(log);
							treeLog.finish();
						}

						mapPropContent = new HashMap<ViewChange, String>();
						mapArgTitle = new HashMap<ViewChange, String>();
						mapArgIndex = new HashMap<ViewChange, Integer>();
						mapPropIndex = new HashMap<ViewChange, Integer>();
						currentDate = timeMachineMap.lastKey();
						// mainTM = new TimeMachine(timeMachineMap, treeClone);

						loadVersionListFromTimeMachine();

						versionList.setSelectedIndex(0);

						onChange(null);
						treeClone.resetState();
						logTreeWithChanges();
						log.finish();
					}
				});

	}

	private SortedMultiMap<Date, ViewChange> prepTreeWithDeletedNodesAndChangseAndBuildTimeMachineMap(
			Tree treeClone, NodeChangesMaps changesMaps) {
		Log log = Log.getLog("vm.ptwdnacab");
		SortedMultiMap<Date, ViewChange> timeMachineMap = new SortedMultiMap<Date, ViewChange>();
		for (int i = 0; i < treeClone.getItemCount(); i++) {
			recursivePrepAndBuild((ViewPropVer) treeClone.getItem(i),
					timeMachineMap, changesMaps, log);
		}
		log.finish();
		return timeMachineMap;
	}

	public void recursivePrepAndBuild(ViewNodeVer viewNode,
			SortedMultiMap<Date, ViewChange> timeMachineMap,
			NodeChangesMaps changesMaps, Log log) {
		log.indent();
		NodeChanges nodeChanges = viewNode.chooseNodeChanges(changesMaps);
		if (viewNode.isOpen()) {
			for (Long id : nodeChanges.deletedChildIDs) {

				/*
				 * if the deletedChild is actually an unlinked proposition then
				 * we create an unloaded proposition (with dummy child nodes)
				 * and do not recurse on the children. this is necessary because
				 * unlike a deleted proposition/argument and unlinked
				 * proposition may have been unlinked while still having
				 * children which means that when we undo the unlinking we need
				 * to show the children at the time of unlinking.
				 */
				if (viewNode instanceof ViewArgVer
						&& changesMaps.unlinkedLinks.containsKey(id)) {
					NodeChanges linkNodeChanges = changesMaps.propChanges
							.get(id);
					Node node = changesMaps.unlinkedLinks.get(id);
					ViewNodeVer deletedView = createChildWithDummiesAndLoadChanges(
							viewNode, node, id, linkNodeChanges, timeMachineMap);
					viewNode.addDeletedItem(deletedView);
				}

				/*
				 * if we are not dealing with an unlinked proposition then we
				 * create a child node and recurse on its children
				 */
				else {
					ViewNodeVer deletedView = viewNode.createChild(id);
					viewNode.addDeletedItem(deletedView);
					recursivePrepAndBuild(deletedView, timeMachineMap,
							changesMaps, log);
				}
			}
			for (int i = 0; i < viewNode.getChildCount(); i++) {
				ViewNodeVer child = viewNode.getChildViewNode(i);
				recursivePrepAndBuild(child, timeMachineMap, changesMaps, log);
			}
		} else {
			for (Long id : nodeChanges.deletedChildIDs) {
				viewNode.addDeletedItem(new ViewDummyVer(id));
			}
			for (int i = 0; i < viewNode.getChildCount(); i++) {
				/*
				 * TODO: remove the creation of dummies from
				 * editmode.clonetree() and create them here for clarity... but
				 * then how do we know how many are needed and what their ids
				 * are?
				 */
			}
		}

		loadChangesIntoNodeAndMap(viewNode, nodeChanges.changes, timeMachineMap);

		log.unindent();
	}

	private void loadChangesIntoNodeAndMap(ViewNodeVer viewNode,
			List<Change> changes,
			SortedMultiMap<Date, ViewChange> timeMachineMap) {
		for (Change change : changes) {
			ViewChange viewChange = new ViewChange();
			viewChange.change = change;
			viewChange.viewNode = viewNode;
			viewNode.getViewChangeList().add(viewChange);
			timeMachineMap.put(change.date, viewChange);
		}
		if (!viewNode.isOpen()) {
			for (ViewChange viewChange : viewNode.getViewChangeHideList()) {
				viewChange.hidden = true;
			}
		}
	}

	private void loadVersionListFromTimeMachine() {
		listBoxChangeHandlerRegistration.removeHandler();
		Long currentDate = this.currentDate.getTime();
		versionList.clear();
		Log log = Log.getLog("vm.lvlftm");
		DateTimeFormat dateFormat = DateTimeFormat
				.getFormat("yyyy MMM d kk:mm:ss:SSS");

		List<ViewChange> reverseList = getChangeList();
		Collections.reverse(reverseList);
		int i = 0;
		int newSelectionIndex = -1;
		for (ViewChange viewChange : reverseList) {
			Change change = viewChange.change;
			Long changeTime = change.date.getTime();
			int comparison = currentDate.compareTo(changeTime);
			if (comparison == 0) {
				log.log("###########   Selecting Item:" + i + "; changeTime:"
						+ changeTime + "; currentDate:" + currentDate);
				newSelectionIndex = i;
			} else if (comparison > 0 && newSelectionIndex == -1) {
				log.log("###########   Selecting Item:" + i
						+ " - 1; before item with changeTime:" + changeTime
						+ "; currentDate:" + currentDate);
				newSelectionIndex = i - 1;
			}
			if (!viewChange.hidden) {
				versionList.addItem("" + dateFormat.format(change.date) + " ["
						+ change.changeType + "]", "" + changeTime);
			} else {
				versionList.addItem("----------------------------------------",
						"" + changeTime);
			}
			log.log("\n" + change);
			i++;
		}

		/* add an item older than all items so that last change can be shown */
		Date oldest = timeMachineMap.get(timeMachineMap.firstKey()).get(0).change.date;
		versionList.addItem("----------------------------------------", ""
				+ (oldest.getTime() - 1000));

		versionList.setSelectedIndex(newSelectionIndex);

		listBoxChangeHandlerRegistration = versionList
				.addChangeHandler(ModeVersions.this);
		log.finish();
	}

	public List<ViewChange> getChangeList() {
		List<ViewChange> viewChangeList = timeMachineMap.firstValues();
		List<ViewChange> returnList = new ArrayList<ViewChange>(
				viewChangeList.size());

		/*
		 * this for loop adds all the non-hidden changes and also adds hidden
		 * add changes that are contained in the same block of consecutive
		 * hidden changes as a corresponding delete change (but only one per
		 * parent so if the parent had a few different add/deletes pairs of
		 * children within the same block only one is included). This change
		 * acts as a place holder so that at some point in the change history
		 * the parent has children, so the open branch icon appears and the user
		 * can browse through the parent's children and their changes.
		 */
		Map<Long, ViewChange> addedInCurrentHideBlock = new HashMap<Long, ViewChange>();
		List<Long> handledParent = new ArrayList<Long>();
		for (ViewChange viewChange : viewChangeList) {
			Change change = viewChange.change;
			if (!viewChange.hidden) {
				returnList.add(viewChange);
				addedInCurrentHideBlock.clear();
				handledParent.clear();
			} else if (change.isAddition()) {
				addedInCurrentHideBlock.put(change.getAddedID(), viewChange);
			} else if (change.isDeletion()) {
				if (addedInCurrentHideBlock.containsKey(change.getDeletedID())) {
					if (!handledParent.contains(change.getParentID())) {
						handledParent.add(change.getParentID());
						returnList.add(addedInCurrentHideBlock.remove(change
								.getDeletedID()));
					}
				}
			}
		}

		/*
		 * if the oldest change is hidden we need to include it anyway,
		 * otherwise there is no way to replay that change, and potentially no
		 * way to open up the closed node (because the open/close icon will not
		 * appear for a node with no children) whose closure resulted in hiding
		 * the change... and thus no way to unhide the change...
		 */
		ViewChange oldestChange = viewChangeList.get(viewChangeList.size() - 1);
		if (oldestChange.hidden) {
			returnList.add(oldestChange);
		}
		return returnList;
	}

	@SuppressWarnings("unused")
	private void logTree(Log log) {
		for (int i = 0; i < treeClone.getItemCount(); i++) {
			ViewNode viewNode = (ViewNode) treeClone.getItem(i);
			viewNode.logNodeRecursive(2, log, false);
		}
	}

	public ViewNodeVer createChildWithDummiesAndLoadChanges(
			ViewNodeVer parentView, Node childNode, Long childID,
			NodeChanges childChanges,
			SortedMultiMap<Date, ViewChange> viewChanges) {

		ViewNodeVer child = createChildWithDummies(parentView, childNode,
				childID, childChanges.deletedChildIDs);

		loadChangesIntoNodeAndMap(child, childChanges.changes, viewChanges);

		return child;
	}

	public ViewNodeVer createChildWithDummies(ViewNodeVer parentView,
			Node childNode, Long childID, List<Long> deletedGrandChildIDs) {
		ViewNodeVer child;

		/*
		 * childNode will be null where the child is currently deleted and
		 * therefore the server does not have a currently existing copy of the
		 * child node. In that case the child node needs to be regenerated
		 * entirely from the change history, and a blank node can be used to
		 * start with.
		 */
		if (childNode == null) {
			child = parentView.createChild(childID);
		} else {
			child = parentView.createChild(childNode);
			/*
			 * only an undeleted node will have existing children to process
			 * because a node can only be deleted after its children are
			 * deleted. Therefore the following for loop creating dummies for
			 * existing children is only need when childNode does not equal
			 * null.
			 */
			for (Long childDummyID : childNode.childIDs) {
				ViewNode childDummy = new ViewDummyVer(childDummyID);
				child.addItem(childDummy);
			}
		}

		/*
		 * for both deleted and undeleted nodes, we need to create dummies for
		 * the deleted children.
		 */
		for (Long deletedID : deletedGrandChildIDs) {
			ViewNodeVer childDummy = new ViewDummyVer(deletedID);
			child.addDeletedItem(childDummy);
		}

		child.setLoaded(false);
		child.setOpen(false);
		return child;
	}

	public void mergeLoadedNodes(ViewNodeVer viewNodeVer,
			Map<Long, NodeWithChanges> nodesWithChanges) {

		SortedMultiMap<Date, ViewChange> viewChanges = new SortedMultiMap<Date, ViewChange>();

		/*
		 * for the deleted views: get the dummys to convert to reals
		 */
		List<ViewNodeVer> deletedViewList = new ArrayList<ViewNodeVer>(
				viewNodeVer.getDeletedViewList());
		/* remove the dummys */
		viewNodeVer.clearDeletedViews();
		/* for each dummy create a real and append it to the node */
		for (ViewNodeVer deletedView : deletedViewList) {
			Long deletedID = deletedView.getNodeID();
			NodeWithChanges nodeWithChanges = nodesWithChanges.get(deletedID);
			viewNodeVer.addDeletedItem(createChildWithDummiesAndLoadChanges(
					viewNodeVer, nodeWithChanges.node, deletedID,
					nodeWithChanges.nodeChanges, viewChanges));
		}

		/*
		 * for the existing views: get the dummys to convert to reals while
		 * removing dummys
		 */
		List<Long> dummyIDs = new ArrayList<Long>();
		while (viewNodeVer.getChildCount() > 0) {
			ViewNodeVer child = viewNodeVer.getChildViewNode(0);
			dummyIDs.add(child.getNodeID());
			child.remove();
		}
		/* for each dummy create a real and append it to the node */
		for (Long id : dummyIDs) {
			NodeWithChanges nodeWithChanges = nodesWithChanges.get(id);
			viewNodeVer
					.addItem((ViewNode) createChildWithDummiesAndLoadChanges(
							viewNodeVer, nodeWithChanges.node, id,
							nodeWithChanges.nodeChanges, viewChanges));
		}

		viewNodeVer.setLoaded(true);

		zoomToCurrentDateAndReloadChangeList(viewNodeVer, viewChanges,
				viewChanges.lastKey());
	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		Log log = Log.getLog("vm.oo");
		final ViewNodeVer viewNodeVer = (ViewNodeVer) event.getTarget();
		if (!viewNodeVer.isLoaded()) {

			/*
			 * make a list of all the child dummy ids, both existing and deleted
			 */
			List<Long> childIDs = new ArrayList<Long>();
			for (ViewNodeVer deletedView : viewNodeVer.getDeletedViewList()) {
				childIDs.add(deletedView.getNodeID());
			}
			for (int i = 0; i < viewNodeVer.getChildCount(); i++) {
				childIDs.add(viewNodeVer.getChildViewNode(i).getNodeID());
			}

			/* a class to hold the call back method */
			class Callback implements
					ServerComm.LocalCallback<Map<Long, NodeWithChanges>> {
				@Override
				public void call(Map<Long, NodeWithChanges> nodesWithChanges) {
					mergeLoadedNodes(viewNodeVer, nodesWithChanges);
				}
			}

			/*
			 * request the nodes and changes for each of the ids collected above
			 * from the server. If the node being opened is a proposition
			 * request a list of child arguments and vice-versa.
			 */
			if (viewNodeVer instanceof ViewArgVer) {
				ServerComm.getPropsWithChanges(childIDs, new Callback());
			} else if (viewNodeVer instanceof ViewPropVer) {
				ServerComm.getArgsWithChanges(childIDs, new Callback());
			}
		} else {

			log.log("Adding View Changes: ");
			SortedMultiMap<Date, ViewChange> subTreeChanges = new SortedMultiMap<Date, ViewChange>();
			recursiveGetViewChanges(viewNodeVer, subTreeChanges, true, log);

			zoomToCurrentDateAndReloadChangeList(viewNodeVer, subTreeChanges,
					viewNodeVer.getClosedDate());

		}
		log.finish();
		logTreeWithChanges();
	}

	public void zoomToCurrentDateAndReloadChangeList(ViewNodeVer viewNodeVer,
			SortedMultiMap<Date, ViewChange> subTreeChanges, Date startDate) {
		/*
		 * this line must come before travelFromDateToDate() becuase that method
		 * resets the tree to open of added nodes that should be open. But this
		 * line must come after recursiveGetViewChanges I think because that
		 * method depends on the opened node not yet have a positive isOpen()
		 * value... actually that doesn't seem to be true anymore...so this
		 * could probably be moved to the beginning.
		 */
		viewNodeVer.setOpen(true);

		travelFromDateToDate(startDate, currentDate, subTreeChanges);

		timeMachineMap.putAll(subTreeChanges);

		for (ViewChange viewChange : viewNodeVer.getViewChangeHideList()) {
			viewChange.hidden = false;
		}

		loadVersionListFromTimeMachine();
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {

		Log log = Log.getLog("vm.oc");
		ViewNodeVer viewNodeVer = (ViewNodeVer) event.getTarget();
		viewNodeVer.setClosedDate(currentDate);
		log.log("Removing View Changes: ");
		SortedMultiMap<Date, ViewChange> subTreeChanges = new SortedMultiMap<Date, ViewChange>();
		recursiveGetViewChanges(viewNodeVer, subTreeChanges, true, log);
		timeMachineMap.removeAll(subTreeChanges);

		for (ViewChange viewChange : viewNodeVer.getViewChangeHideList()) {
			viewChange.hidden = true;
		}

		loadVersionListFromTimeMachine();
		// viewNodeVer.setOpen(false);
		log.finish();

	}

	/*
	 * this function gets all the changes of all the open descendants of the
	 * ViewNodeVer passed to it. It does not return the changes of the
	 * ViewNodeVer actually passed to it as those changes are dealt with
	 * different (hidden instead of removed from the change list). It also does
	 * not return the changes of closed descendants because those changes have
	 * already been remove or do not need to be added upon the closing/opening
	 * of the ViewNodeVer passed. (Dummy nodes have no changes so it skips dummy
	 * nodes when collecting changes).
	 */
	public void recursiveGetViewChanges(ViewNodeVer viewNodeVer,
			SortedMultiMap<Date, ViewChange> forAddOrRemove, boolean firstNode,
			Log log) {
		/*
		 * if it is the first node then we don't want to remove any changes, as
		 * add/remove changes are merely hidden, and modification changes are
		 * left entirely intact.
		 */
		if (!firstNode) {
			log.log("nodeID: " + viewNodeVer.getNodeID() + "; State: "
					+ viewNodeVer.isOpen());
			for (ViewChange viewChange : viewNodeVer.getViewChangeList()) {
				log.log("  viewChange: " + viewChange);
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
				ViewNodeVer childView = viewNodeVer.getChildViewNode(i);
				if (!(childView instanceof ViewDummyVer)) {
					recursiveGetViewChanges(childView, forAddOrRemove, false,
							log);
				}
			}
			for (ViewNodeVer deletedView : viewNodeVer.getDeletedViewList()) {
				if (!(deletedView instanceof ViewDummyVer)) {
					recursiveGetViewChanges(deletedView, forAddOrRemove, false,
							log);
				}
			}
		}
	}

	@Override
	public void onChange(ChangeEvent event) {

		String millisecondStr = versionList.getValue(versionList
				.getSelectedIndex());
		Date destinationDate = new Date(Long.parseLong(millisecondStr));
		travelFromDateToDate(currentDate, destinationDate, timeMachineMap);

		// TODO: if multiple copies of the same linked proposition are
		// showing
		// how do we know which one to make visible?

		/*
		 * must check to make sure a change exists for destinationDate becuase
		 * destination date may be a placeholder to allow the user to walk past
		 * the earliest change.
		 */
		if (timeMachineMap.get(destinationDate) != null) {
			treePanel.ensureVisible((ViewNode) timeMachineMap.get(
					destinationDate).get(0).viewNode);
		}

	}

	public void travelFromDateToDate(Date currentDate, Date newDate,
			SortedMultiMap<Date, ViewChange> changes) {
		Log log = Log.getLog("tm.ttd");
		if (newDate.before(currentDate)) {
			log.log("traveling back to date:" + newDate);
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
			moveTreeBackwards(reverseList, log);
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
			log.log("traveling forward to date:" + newDate);
			moveTreeForwards(
					changes.valuesSublist(currentDate, false, newDate, true),
					log);
		}
		this.currentDate = newDate;
		treeClone.resetState();
		log.finish();
	}

	private void moveTreeForwards(
			Collection<List<ViewChange>> changesToProcess, Log log) {
		log.log("----re-doing changes----");
		for (List<ViewChange> changeList : changesToProcess) {
			for (ViewChange vC : changeList) {
				log.log("processing: " + vC.change);
				switch (vC.change.changeType) {
				case PROP_UNLINK:
				case PROP_DELETION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					argView.removeAndSaveChildView(vC.change.propID);
					break;
				}
				case PROP_LINK:
				case PROP_ADDITION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
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
	private void moveTreeBackwards(
			Collection<List<ViewChange>> changesToProcess, Log log) {
		log.logln("----undoing changes----");
		for (List<ViewChange> changeList : changesToProcess) {
			for (ViewChange vC : changeList) {
				log.logln("processing: " + vC.change);
				switch (vC.change.changeType) {
				case PROP_UNLINK: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					argView.reviveDeletedView(vC.change.propID,
							vC.change.argPropIndex);
					break;
				}
				case PROP_DELETION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					argView.reviveDeletedView(vC.change.propID,
							vC.change.argPropIndex);
					ViewNode viewNode = argView
							.getChild(vC.change.argPropIndex);
					/*
					 * need to check before cast because this might be a dummy
					 * node (in which case we don't need to set the content
					 * anyway...)
					 */
					if (viewNode instanceof ViewPropVer) {
						((ViewPropVer) viewNode).proposition.content = vC.change.oldContent;
					}
					break;
				}
				case PROP_LINK:
				case PROP_ADDITION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
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
					mapPropContent.put(vC, propView.getTextAreaContent());
					propView.setContent(vC.change.oldContent);
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
					ViewNode deletedView = propView
							.getChild(vC.change.argPropIndex);
					/*
					 * need to check before cast because this might be a dummy
					 * node (in which case we don't need to set the content
					 * anyway...)
					 */
					if (deletedView instanceof ViewArgVer) {
						ViewArg viewArgVer = (ViewArg) deletedView;
						viewArgVer.setPro(vC.change.argPro);
						viewArgVer.setArgTitle(vC.change.oldContent);
					}
					break;
				}
				case ARG_MODIFICATION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					/* NOTE: see not regarding arg modification */
					mapArgTitle.put(vC, argView.getArgTitle());
					argView.setArgTitle(vC.change.oldContent);
					break;
				}
				}
			}
		}
	}

	public void logTreeWithChanges() {
		Log log = Log.getLog("vm.ltwc");
		for (int i = 0; i < treeClone.getItemCount(); i++) {
			recursiveLogTreeWithChanges((ViewNodeVer) treeClone.getItem(i), log);
		}
		log.finish();
	}

	public void recursiveLogTreeWithChanges(ViewNodeVer viewNodeVer, Log log) {
		log.indent();
		log.logln("node:" + viewNodeVer.toString());
		if (!(viewNodeVer instanceof ViewDummyVer)) {
			log.logln("changes:");
			log.indent();
			for (ViewChange change : viewNodeVer.getViewChangeList()) {
				log.logln(change.toString());
			}
			log.unindent();
			log.logln("children:");
			for (int i = 0; i < viewNodeVer.getChildCount(); i++) {
				recursiveLogTreeWithChanges(viewNodeVer.getChildViewNode(i),
						log);
			}
			log.logln("deleted children:");
			for (ViewNodeVer delChild : viewNodeVer.getDeletedViewList()) {
				recursiveLogTreeWithChanges(delChild, log);
			}
		}
		log.unindent();
	}
}
