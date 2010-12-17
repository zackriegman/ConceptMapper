package org.argmap.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodeChangesMapsAndRootChanges;
import org.argmap.client.ArgMapService.NodeWithChanges;
import org.argmap.client.Change.ChangeType;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class ModeVersions extends ResizeComposite implements
		CloseHandler<TreeItem>, OpenHandler<TreeItem>, ChangeHandler {
	/*
	 * This class contains the widget and logic for a program mode that allows
	 * the user to browse versions of an argument tree at different points in
	 * time based on history stored in the form of Change objects which are
	 * generated and stored on the server after every change.
	 * 
	 * The user is presented with a change list on the left, and a tree on the
	 * right that corresponds to the state of the tree at the date selected in
	 * change list.
	 * 
	 * As users open and close ViewNodes in the tree, the change lists is
	 * updated to only show changes relevant to opened nodes, or nodes that
	 * would be opened if they had not been deleted at the point in time
	 * currently selected.
	 * 
	 * When the user selects a new change from the change list the program
	 * applies the intermediate changes to the tree to move the tree either
	 * forwards or backwards in time till it is the same as it was at the time
	 * that the user selected.
	 * 
	 * In order to do this the program keeps a list of all the relevant changes,
	 * in the timeMachineMap field, ordered by the date of the change. Each date
	 * corresponds to a single change, but may correspond to multiple ViewNodes
	 * because the same Node can exist in multiple points in a tree if the Node
	 * has been linked to by different Arguments within a tree. Thus the
	 * timeMachineMap is a SortedMultiMap, sorted and keyed by the date of each
	 * change, with a list of ViewNodes that represent the Node changed by the
	 * Change object.
	 * 
	 * The timeMachineMap contains changes only of nodes that are currently
	 * visible or those that would be visible, except that they have been
	 * deleted or have yet to be added at the current point in time. When the
	 * user closes or opens a node the timeMachineMap is updated.
	 * 
	 * When a user closes a ViewNode all the changes for all the descendants of
	 * that ViewNode are removed from the timeMachineMap. The add/remove changes
	 * for the closed ViewNode are NOT removed from the map however but are
	 * instead marked as 'hidden' (and the textual modifications of the close
	 * ViewNode are left in the map and left un-hidden). Such changes are either
	 * hidden from the user or displayed as dashed lines, whereas the un-hidden
	 * changes have their dates displayed for the user. Hidden changes are kept
	 * in the change list so that as the program moves the tree either forwards
	 * or backwards in time, closed nodes can have their children added/removed
	 * and their open/close icons can be updated to reflect whether they have
	 * children at that point in time or not. In general it is not necessary to
	 * show hidden changes to the user at all, they are processed by the program
	 * silently, and the open/close icon is updated as the user traverses the
	 * tree. However, in some cases, the open/close icon would never be visible
	 * because there are no intervening changes between the time when all of a
	 * Node's children are added and then deleted. In those cases, the hidden
	 * change must be displayed to the user so that the user has a way to select
	 * a moment in time when the Node had children, so that the user can open
	 * the node and browse the children. In those cases, a hidden ViewChange is
	 * displayed in the change list as a dashed line.
	 * 
	 * When a user opens a ViewNode all the changes for all the descendants that
	 * are themselves open are added to the timeMachineMap, and the ViewNode's
	 * changes are marked as un-hidden.
	 * 
	 * That is the general idea. To make all that work, each node keeps track
	 * not only of its current children but also of children that have yet to be
	 * added or that have been deleted at the current time. This allows the
	 * program to collect changes not only from existing nodes but also from
	 * non-existing nodes. This is important because we want to show the changes
	 * for non-existing nodes that are open. For instance, we want to show the
	 * change in the change list that corresponds to adding a node, as well as
	 * the change that corresponds to editing its text. But if the node's parent
	 * is close, we do not want to show those changes, because then when the
	 * user clicks on the change, he/she will see no apparent effect which will
	 * be confusing.
	 * 
	 * Further notes about how and why things work.
	 * 
	 * 1. When (1) an arg links to a prop; (2) prop is unlinked from the arg (3)
	 * prop is modified or subtree is modified; (4) arg re-links to the node.
	 * What happens? At any given time the user should see the correct version
	 * of the link and its subtree, because all the relevant ViewChanges are
	 * kept in the timeMachineMap and processed when moving forwards or
	 * backwards regardless of whether the linked prop is currently part of the
	 * existing tree or is rather being held in the deleted nodes list of the
	 * arg. So it seems to work.
	 */

	private final ListBox versionList = new ListBox();
	private final ModeEdit editMode;
	private ArgTree treeClone = null;
	private final ScrollPanel treePanel = new ScrollPanel();
	private final int LIST_WIDTH = 20;

	// SplitLayoutPanel mainPanel;

	private HandlerRegistration listBoxChangeHandlerRegistration;

	/*
	 * variables related to moving the tree forwards and backwards in time. Each
	 * map holds some information that is lost from the tree when moving
	 * backwards in time that will be needed to later move forwards in time. For
	 * instance when undoing a prop modification, the current prop content is
	 * lost and replaced with the previous prop content that is stored in the
	 * Change object. Because the Change only stores enough information to move
	 * backwards, it does not store the current prop content needed to move back
	 * to this time. Therefore that information is saved for later use in
	 * mapPropContent.
	 */
	private Map<ViewChange, String> mapPropContent;
	private Map<ViewChange, String> mapArgTitle;
	private Map<ViewChange, Integer> mapPropIndex;
	private Map<ViewChange, Integer> mapArgIndex;

	/*
	 * A ViewChange contains a pointer to a ViewNodeVer and a Change object, as
	 * well as a flag indicated whether the ViewChange is 'hidden' or not. The
	 * timeMachineMap keeps track of each change, and each ViewNode for which
	 * the change is applicable. It is sorted by the date of the change. This
	 * allows the program to replay (or undo) changes sequentially, and if there
	 * is more than one copy of the ViewNodeVer that needs to be updated, to
	 * have a link to each one.
	 */
	private SortedMultiMap<Date, ViewChange> timeMachineMap;

	/*
	 * The Date represented by the current tree state. After modifying the tree
	 * to reflect a tree state at a certain time this is updated to reflect what
	 * time, so the program knows the date of the current tree state.
	 */
	private Date currentDate;

	public ModeVersions(ModeEdit editModePair) {
		super();

		/************************************
		 * setup the history browsing panel *
		 ************************************/
		this.editMode = editModePair;
		DockLayoutPanel historyPanel = new DockLayoutPanel(Unit.EM);

		historyPanel.addWest(versionList, LIST_WIDTH);
		historyPanel.add(treePanel);

		versionList.setVisibleItemCount(20);
		versionList.setWidth(LIST_WIDTH + "em");

		listBoxChangeHandlerRegistration = versionList.addChangeHandler(this);

		/**************************
		 * setup the button panel *
		 **************************/
		DockLayoutPanel buttonPanel = new DockLayoutPanel(Unit.EM);
		final ToggleButton deletedRootPropsToggle = new ToggleButton(
				"browse deleted root propositions instead");
		deletedRootPropsToggle.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (deletedRootPropsToggle.isDown()) {
					displayDeletedRootPropsVersions();
				} else {
					displayVersions();
				}

			}
		});
		deletedRootPropsToggle.addStyleName("deletedRootPropsToggle");

		FlowPanel buttonsFlow = new FlowPanel();
		buttonsFlow.addStyleName("buttonsFlow");
		buttonsFlow.add(deletedRootPropsToggle);

		buttonPanel.addEast(buttonsFlow, 20);
		buttonPanel.addStyleName("searchBoxPanel");

		/****************************
		 * setup the overall layout *
		 ****************************/

		DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.EM);
		mainPanel.addNorth(buttonPanel, 2.7);
		mainPanel.add(historyPanel);

		initWidget(mainPanel);
	}

	/*
	 * this method takes care of down-loading the changes from the server and
	 * setting up a clone of the edit tree to work with.
	 */
	public void displayVersions() {
		versionList.clear();
		versionList.addItem("Loading Revision History From Server...");
		if (treeClone != null) {
			treePanel.remove(treeClone);
		}
		List<Proposition> props = new ArrayList<Proposition>();
		List<Argument> args = new ArrayList<Argument>();
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
						 * TODO this could be done outside of the call back to
						 * reduce the user's wait time, but would need to ensure
						 * that it finishing before the call back proceeds. can
						 * the call back just keep calling something like wait()
						 * until it finds that the clone is finished?
						 */
						editMode.buildTreeCloneOfOpenNodes(treeClone);

						treePanel.add(treeClone);

						timeMachineMap = prepTreeWithDeletedNodesAndChangesAndBuildTimeMachineMap(
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

						/*
						 * TODO what is this onChange() called for? get rid of
						 * it?
						 */
						onChange(null);
						treeClone.resetState();
						logTreeWithChanges();
						log.finish();
					}
				});

	}

	/*
	 * this method takes care of down-loading the changes from the server and
	 * setting up a clone of the edit tree to work with.
	 */
	public void displayDeletedRootPropsVersions() {
		versionList.clear();
		versionList.addItem("Loading Revision History From Server...");
		if (treeClone != null) {
			treePanel.remove(treeClone);
		}

		ServerComm
				.getChangesForDeletedRootProps(new ServerComm.LocalCallback<NodeChangesMapsAndRootChanges>() {

					@Override
					public void call(NodeChangesMapsAndRootChanges changes) {

						treeClone = new ArgTree();
						treeClone.addCloseHandlerTracked(ModeVersions.this);
						treeClone.addOpenHandlerTracked(ModeVersions.this);

						treePanel.add(treeClone);

						/*
						 * I think this is already done in the lines immediately
						 * following? Remove this todo?
						 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
						 * TODO: must setup the tree with root nodes returned
						 * from server
						 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
						 */
						for (Change change : changes.rootChanges) {
							Proposition proposition = new Proposition();
							proposition.content = change.oldContent;
							proposition.linkCount = change.propLinkCount;
							proposition.id = change.propID;
							ViewPropVer viewProp = new ViewPropVer(proposition);
							treeClone.addItem(viewProp);
						}

						timeMachineMap = prepTreeWithDeletedNodesAndChangesAndBuildTimeMachineMap(
								treeClone, changes.nodeChangesMaps);

						mapPropContent = new HashMap<ViewChange, String>();
						mapArgTitle = new HashMap<ViewChange, String>();
						mapArgIndex = new HashMap<ViewChange, Integer>();
						mapPropIndex = new HashMap<ViewChange, Integer>();
						currentDate = timeMachineMap.lastKey();
						// mainTM = new TimeMachine(timeMachineMap, treeClone);

						loadVersionListFromTimeMachine();

						versionList.setSelectedIndex(0);

						// onChange(null);
						treeClone.resetState();
					}
				});

	}

	/*
	 * This method takes the tree cloned from edit mode and adds empty child
	 * nodes to a parent's deleted nodes list (so that when going back in time
	 * the deleted nodes can be revived by appending them to the tree). For each
	 * node, deleted and not deleted, this method loads the node with its
	 * changes. This allows the program to know which changes need to be trimmed
	 * from the change list shown to the viewer, when a node is opened or closed
	 * (by walking through the sub tree of the opened/closed node and asking
	 * each subtree node what changes should be added/removed from the change
	 * list).
	 * 
	 * This method also builds the timeMachineMap which is a sorted multimap,
	 * sorted by the date of changes, that is used when time traveling to keep
	 * track of each change that must be made to the tree (with a single Change
	 * sometimes requiring multiple changes to the tree if it is a change to a
	 * linked subtree).
	 */
	private SortedMultiMap<Date, ViewChange> prepTreeWithDeletedNodesAndChangesAndBuildTimeMachineMap(
			Tree treeClone, NodeChangesMaps changesMaps) {
		Log log = Log.getLog("vm.ptwdnacab");
		SortedMultiMap<Date, ViewChange> timeMachineMap = new SortedMultiMap<Date, ViewChange>();
		for (int i = 0; i < treeClone.getItemCount(); i++) {
			ViewPropVer viewPropVer = (ViewPropVer) treeClone.getItem(i);
			recursivePrepAndBuild(viewPropVer, timeMachineMap, changesMaps, log);
			recursiveHideLinkedSubtreesDuringUnlinkedPeriods(viewPropVer,
					new TimePeriods());
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
			/*
			 * first setup the deleted nodes, these will not be in the tree
			 * cloned from EditMode, so these nodes need to be created or (in
			 * the case of unlinked propositions) added from the info sent from
			 * the server. The deleted nodes are added and kept track of because
			 * the changes list must be updated depending on which nodes are
			 * open in order to make for intuitive browsing of nodes. This means
			 * that each node needs to know who its children are, including its
			 * deleted children, so that it can add/remove changes from the
			 * changes list shown to the user.
			 */
			for (Long id : nodeChanges.deletedChildIDs) {

				/*
				 * if the deletedChild is actually an unlinked proposition then
				 * we create an unloaded proposition (with dummy child nodes)
				 * and do not recurse on the children. this is necessary because
				 * unlike a deleted proposition/argument an unlinked proposition
				 * may have been unlinked while still having children which
				 * means that when we undo the unlinking we need to show the
				 * children at the time of unlinking.
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
			/*
			 * after processing deleted nodes we process nodes that currently
			 * exist in the tree.
			 */
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

		/*
		 * this is the line that actually loads up the changes for the node and
		 * adds it to the timeMachineMap. (Whereas the lines above are creating
		 * the tree and crawling through it.)
		 */
		loadChangesIntoNodeAndMap(viewNode, nodeChanges.changes, timeMachineMap);

		log.unindent();
	}

	private void loadChangesIntoNodeAndMap(ViewNodeVer viewNode,
			List<Change> changes,
			SortedMultiMap<Date, ViewChange> timeMachineMap) {
		for (Change change : changes) {
			/*
			 * for each Change create a ViewChange mapping the Change to the
			 * ViewNode
			 */
			ViewChange viewChange = new ViewChange();
			viewChange.change = change;
			viewChange.viewNode = viewNode;

			/*
			 * load the ViewChange into the ViewNode so that the Node can
			 * quickly/easily generate a list of Changes to be hidden/shown
			 * based on whether the ViewNode is closed/open
			 */
			viewNode.getViewChangeList().add(viewChange);

			/*
			 * load the ViewChange into the timeMachineMap so that when moving
			 * forward or backwards in time there is an quick/easy way to know
			 * which ViewNode needs to be updated.
			 */
			timeMachineMap.put(change.date, viewChange);
		}

		/*
		 * if the ViewNode is not open that means that all of its add/remove
		 * Changes should be hidden from the user so that there are not changes
		 * in the Change list which don't seem to have any effect on the visible
		 * tree (which would be confusing)...
		 */
		if (!viewNode.isOpen()) {
			for (ViewChange viewChange : viewNode.getViewChangeHideList()) {
				viewChange.hidden = true;
			}
		}
	}

	/*
	 * TODO still need to make alwaysHidden Changes actually hidden when
	 * building the change list.
	 * 
	 * TODO this just does the initial hide... need to redo this every time the
	 * user opens and loads unloaded children for the first time... right?
	 * 
	 * TODO explain what this function does in a comment here
	 */
	private void recursiveHideLinkedSubtreesDuringUnlinkedPeriods(
			ViewNodeVer viewNodeVer, TimePeriods hidePeriods) {
		if (viewNodeVer instanceof ViewDummyVer) {
			return;
		}

		List<ViewChange> viewChangeList = viewNodeVer.getViewChangeList();

		// hide all changes of this node that occur within a period in list
		for (ViewChange viewChange : viewChangeList) {
			if (hidePeriods.covers(viewChange.change.date)) {
				viewChange.alwaysHidden = true;
			}
		}

		if (viewNodeVer instanceof ViewArgVer) {
			// sort the node's viewChangeList
			Collections.sort(viewChangeList, ViewChange.DATE_COMPARATOR);

			/*
			 * start building more restrictive hidePeriods for the children that
			 * need them
			 */
			Map<Long, TimePeriods> newPeriods = new HashMap<Long, TimePeriods>();
			Map<Long, ViewChange> lastChanges = new HashMap<Long, ViewChange>();
			for (ViewChange viewChange : viewChangeList) {
				Long propID = viewChange.change.propID;
				if (viewChange.change.changeType == ChangeType.PROP_LINK) {

					TimePeriods newPeriod = newPeriods.get(propID);
					if (newPeriod == null) {
						newPeriod = hidePeriods.copy();
						newPeriods.put(propID, newPeriod);
					}

					ViewChange lastViewChange = lastChanges.get(propID);
					if (lastViewChange != null
							&& lastViewChange.change.changeType == ChangeType.PROP_UNLINK) {
						newPeriod.addPeriod(lastViewChange.change.date,
								viewChange.change.date);
					} else {
						newPeriod.addPeriod(TimePeriods.THIRTY_YEARS_AGO,
								viewChange.change.date);
					}

					lastChanges.put(propID, viewChange);
				} else if (viewChange.change.changeType == ChangeType.PROP_UNLINK) {
					lastChanges.put(propID, viewChange);
				}
			}

			// for each child node
			for (ViewNodeVer child : new ViewNodeVer.CombinedViewIterator(
					viewNodeVer)) {
				Long propID = child.getNodeID();

				/*
				 * finish building a more restrictive hide period if this child
				 * needs it
				 */
				ViewChange lastViewChange = lastChanges.get(propID);
				if (lastViewChange != null
						&& lastViewChange.change.changeType == ChangeType.PROP_UNLINK) {
					TimePeriods newPeriod = newPeriods.get(propID);
					if (newPeriod == null) {
						newPeriod = hidePeriods.copy();
						newPeriods.put(propID, newPeriod);
					}
					/*
					 * we got here because lastChanges for propID was never set
					 * to a Change of type PROP_LINK, which means we had a
					 * PROP_UNLINK with no following link, which means that all
					 * subsequent changes should be hidden. So we add a hide
					 * period from the last change to 100 years from now.
					 */
					newPeriod.addPeriod(lastViewChange.change.date,
							TimePeriods.ONE_HUNDRED_YEARS_FROM_NOW);
				}

				if (newPeriods.containsKey(propID)) {
					recursiveHideLinkedSubtreesDuringUnlinkedPeriods(child,
							newPeriods.get(propID));
				} else {
					recursiveHideLinkedSubtreesDuringUnlinkedPeriods(child,
							hidePeriods);
				}
			}
		} else {
			for (ViewNodeVer child : new ViewNodeVer.CombinedViewIterator(
					viewNodeVer)) {
				recursiveHideLinkedSubtreesDuringUnlinkedPeriods(child,
						hidePeriods);
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

	/*
	 * called in the call back in onOpen(); when a node is opened the program
	 * asks the server for the node and when it gets a response it uses this
	 * method to merge the node into the tree. The basic idea is that closed
	 * nodes initially have dummy nodes for children (corresponding to both
	 * existing and non-existing (i.e. deleted or not yet added) nodes for the
	 * current point in time), and when the node is opened, the client converts
	 * the dummy nodes into real nodes with actual content which the user can
	 * view.
	 */
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
		 * this line must come before travelFromDateToDate() because that method
		 * resets the tree to open of added nodes that should be open. But this
		 * line must come after recursiveGetViewChanges, I think, because that
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
	 * this function gets all the changes of all the descendants in an unbroken
	 * chain of open decesdents from the ViewNodeVer passed to it. It does not
	 * return the changes of the ViewNodeVer actually passed to it as those
	 * changes are dealt with differently (hidden instead of removed from the
	 * change list). It also does not return the changes of closed descendants
	 * (and their subtrees) because those changes have already been remove or do
	 * not need to be added upon the closing/opening of the ViewNodeVer passed.
	 * (Dummy nodes have no changes so it skips dummy nodes when collecting
	 * changes).
	 */
	public void recursiveGetViewChanges(ViewNodeVer viewNodeVer,
			SortedMultiMap<Date, ViewChange> forAddOrRemove, boolean firstNode,
			Log log) {
		/*
		 * if it is the first node then we don't want to remove any changes, as
		 * add/remove changes are merely hidden, and modification changes are
		 * left entirely intact. Otherwise we want to add this nodes changes.
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
		 * must check to make sure a change exists for destinationDate because
		 * destination date may be a place holder to allow the user to walk past
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
					 * Remove this note? Now that changes are keyed to vC seems
					 * like this note is no longer relevant... NOTE: the same
					 * content might be put in the mapPropContent multiple times
					 * if there are multiple links to the same node, but it
					 * shouldn't be a problem, because the change for each link
					 * should be processed one right after the other, without a
					 * chance for the content to be changed by a different
					 * change...
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

					ViewNode viewNode = argView
							.getChild(vC.change.argPropIndex);
					if (viewNode instanceof ViewPropVer) {
						((ViewPropVer) viewNode)
								.setNegated(vC.change.propNegated);
					}
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
						((ViewPropVer) viewNode)
								.setNegated(vC.change.propNegated);
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
