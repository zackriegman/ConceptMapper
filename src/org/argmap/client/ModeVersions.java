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
	 * currently selected. (The idea is that if a user closes a part of the tree
	 * that means he does not want to see changes having to do with that part of
	 * the tree; this is also important because when the user clicks on the
	 * change, if the tree is closed he/she won't see any change in the tree
	 * which will be confusing.)
	 * 
	 * When the user selects a new change from the change list the program
	 * applies the intermediate changes to the tree to move the tree either
	 * forwards or backwards in time till it is the same as it was at the time
	 * that the user selected.
	 * 
	 * In order to do this the program keeps a list of all the relevant changes,
	 * in the timeMachineMap field, ordered by the id of the change. Ids are set
	 * monotonically incrementing on the server, so every change an id that is
	 * greater than the id of a change that happened earlier. Each id
	 * corresponds to a single change, but may correspond to multiple ViewNodes
	 * because the same Proposition can exist in multiple points in a tree if
	 * the Proposition has been linked to by different Arguments within a tree.
	 * Thus the timeMachineMap is a SortedMultiMap, sorted and keyed by the id
	 * of each change, with a list of ViewNodes that represent the Node changed
	 * by the Change object.
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
	 * 
	 * 2. When a Proposition is linked to an argument, unlinked, edited (or its
	 * descendants are edited), and relinked, there is a potential problem. The
	 * changes for the linked proposition's subtree during the time when the
	 * link was not attached to the tree will be visible, but will not
	 * correspond to any visible change. This will be confusing to the user. To
	 * avoid this the program keeps track of windows of time when a subtree's
	 * changes should not be visible (because a link has been removed from a
	 * tree) and marks those changes as 'alwaysHidden'. These changes are never
	 * shown. Note however, that it is important that the changes exist on the
	 * client so that when a link is added, removed, edited, and re-added, those
	 * edits show up on the re-add.
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
	 * well as a flag indicating whether the ViewChange is 'hidden' or not. The
	 * timeMachineMap keeps track of each change, and each ViewNode for which
	 * the change is applicable. It is sorted by the id of the change. This
	 * allows the program to replay (or undo) changes sequentially, and if there
	 * is more than one copy of the ViewNodeVer that needs to be updated, to
	 * have a link to each one.
	 */
	private SortedMultiMap<Long, ViewChange> timeMachineMap;

	/*
	 * The 'id' of the change represented by the current tree state. After
	 * modifying the tree to reflect a tree state up to a certain change this is
	 * updated to reflect what change, so the program knows what change to start
	 * from when performing further time transformations.
	 */
	private Long currentChangeID;

	/*
	 * the date at the time ModeVersions starts displaying versions for the
	 * current tree (used for formating dates in the change list (e.g. don't
	 * bother displaying the year for changes in the current year, the month for
	 * changes in the current month...)
	 */
	private static Date now = new Date();

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

						now = new Date();
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
						currentChangeID = timeMachineMap.lastKey();
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
						now = new Date();

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
						currentChangeID = timeMachineMap.lastKey();
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
	 * sorted by the id of changes, that is used when time traveling to keep
	 * track of each change that must be made to the tree (with a single Change
	 * sometimes requiring multiple changes to the tree if it is a change to a
	 * linked subtree).
	 */
	private SortedMultiMap<Long, ViewChange> prepTreeWithDeletedNodesAndChangesAndBuildTimeMachineMap(
			Tree treeClone, NodeChangesMaps changesMaps) {
		Log log = Log.getLog("vm.ptwdnacab");
		SortedMultiMap<Long, ViewChange> timeMachineMap = new SortedMultiMap<Long, ViewChange>();
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
			SortedMultiMap<Long, ViewChange> timeMachineMap,
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
		}
		/*
		 * for nodes that are not open there are only dummy nodes. For nodes
		 * that currently exist (are not deleted) the dummy nodes are already
		 * created when ModeEdit cloned the tree. For deleted nodes we have to
		 * create the dummy nodes now.
		 */
		else {
			for (Long id : nodeChanges.deletedChildIDs) {
				viewNode.addDeletedItem(viewNode.createDummyChild(id));
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
			SortedMultiMap<Long, ViewChange> timeMachineMap) {
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
			timeMachineMap.put(change.id, viewChange);
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

	private TimePeriods getOrMakePeriods(Map<Long, TimePeriods> newPeriods,
			TimePeriods baseTimePeriods, Long propID) {
		TimePeriods newPeriod = newPeriods.get(propID);
		if (newPeriod == null) {
			newPeriod = baseTimePeriods.copy();
			newPeriods.put(propID, newPeriod);
		}
		return newPeriod;
	}

	/*
	 * TODO this just does the initial hide... need to redo this every time the
	 * user opens and loads unloaded children for the first time... right?
	 * 
	 * Subtrees might have changes listed for periods which they are not
	 * attached to the tree, which can be confusing and distracting for a user
	 * trying to browse a trees changes. This happens because a tree can link to
	 * a pre-existing proposition. That link will incorporate a subtree, and all
	 * the changes of that subtree, into the larger tree. Some of the changes
	 * associated with the subtree will predate the linking. Thus, when a user
	 * is browsing through the changes, some of the changes will correspond to
	 * events that can not be seen in the tree, unless those irrelevant events
	 * are hidden. This method walks through the tree, and hides changes for
	 * times when they are not attached to the tree that the user is browsing.
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

		/*
		 * if this is an argument, we need to build new hide periods for any
		 * linked children
		 */
		if (viewNodeVer instanceof ViewArgVer) {
			/*
			 * Start building more restrictive hidePeriods for the children that
			 * need them. There are two possibilities for the first link to a
			 * child. A child could be created and then unlinked or linked and
			 * then unlinked. In the case where the child is created, that child
			 * can have no changes before its creation, so we don't need to have
			 * a pre creation hide period (and note that although the child
			 * might have linked descendants that existed well before its
			 * creation date, the changes associated with those links will be
			 * hidden based on when the link was created, so no hide periods
			 * needs to be created based on the date of the child's creation).
			 * In the case where a child is linked as its first link to the
			 * parent, it might have changes that predate the linking and
			 * therefore a hidePeriod needs to be created for all changes that
			 * predate the linking. Next, it's possible for a node to be
			 * unlinked and then re-linked later, in which case we need to hide
			 * all the changes between unlinking and re-linking. And finally, we
			 * need to change all of the changes to a child after it has been
			 * unlinked. In summary we must hide all changes before the first
			 * link, between each unlink-link pair, and after the last unlink.
			 * The two for() loops below should do that.
			 */
			/*
			 * sort the node's viewChangeList so because the for loops depend on
			 * the list to be in date order.
			 */
			Collections.sort(viewChangeList, ViewChange.DATE_COMPARATOR);
			Map<Long, TimePeriods> newPeriods = new HashMap<Long, TimePeriods>();
			Map<Long, ViewChange> lastUnlinks = new HashMap<Long, ViewChange>();
			for (ViewChange viewChange : viewChangeList) {
				Long propID = viewChange.change.propID;
				if (viewChange.change.changeType == ChangeType.PROP_LINK) {

					TimePeriods newPeriod = getOrMakePeriods(newPeriods,
							hidePeriods, propID);

					ViewChange lastUnlink = lastUnlinks.remove(propID);
					if (lastUnlink != null) {
						newPeriod.addPeriod(lastUnlink.change.date,
								viewChange.change.date);
					} else {
						newPeriod.addPeriod(TimePeriods.THIRTY_YEARS_AGO,
								viewChange.change.date);
					}

				} else if (viewChange.change.changeType == ChangeType.PROP_UNLINK) {
					lastUnlinks.put(propID, viewChange);
				}
			}
			/*
			 * finish building more restrictive hide period for children that
			 * need it
			 */
			for (ViewChange viewChange : lastUnlinks.values()) {
				TimePeriods newPeriod = getOrMakePeriods(newPeriods,
						hidePeriods, viewChange.change.propID);

				/*
				 * we got here because there was a PROP_UNLINK which wasn't
				 * removed from the map, which means we had a PROP_UNLINK with
				 * no following link, which means that all subsequent changes
				 * should be hidden. So we add a hide period from the last
				 * change to 100 years from now (tomorrow would be enough, but
				 * just to be safe...).
				 */
				newPeriod.addPeriod(viewChange.change.date,
						TimePeriods.ONE_HUNDRED_YEARS_FROM_NOW);

			}

			/*
			 * for each child node call this recursive function with either a
			 * newly construction TimePeriods defining hide periods, or, if no
			 * new hide periods was constructed for the child node, just pass on
			 * the same hidePeriods that was used for this node.
			 */
			for (ViewNodeVer child : new ViewNodeVer.CombinedViewIterator(
					viewNodeVer)) {
				Long propID = child.getNodeID();

				if (newPeriods.containsKey(propID)) {
					recursiveHideLinkedSubtreesDuringUnlinkedPeriods(child,
							newPeriods.get(propID));
				} else {
					recursiveHideLinkedSubtreesDuringUnlinkedPeriods(child,
							hidePeriods);
				}
			}
		}
		/*
		 * if this is not an argument, it cannot have linked children, and thus
		 * we can call the recursive function without building new hide periods
		 * for linked children.
		 */
		else {
			for (ViewNodeVer child : new ViewNodeVer.CombinedViewIterator(
					viewNodeVer)) {
				recursiveHideLinkedSubtreesDuringUnlinkedPeriods(child,
						hidePeriods);
			}
		}
	}

	/* setup the date formats for the list */
	private static final DateTimeFormat dateFormatYear = DateTimeFormat
			.getFormat("hh:mm a, MMM d, yyyy [ss:SSS]");
	private static final DateTimeFormat dateFormatMonth = DateTimeFormat
			.getFormat("hh:mm a, MMM d [ss:SSS]");
	private static final DateTimeFormat dateFormatDay = DateTimeFormat
			.getFormat("hh:mm a, EEEE [ss:SSS]");
	private static final DateTimeFormat dateFormatTime = DateTimeFormat
			.getFormat("hh:mm a [ss:SSS]");

	private static final long SIX_DAYS_IN_MILLIS = 1000 * 60 * 60 * 24 * 6;

	@SuppressWarnings("deprecation")
	private String formatDate(Date date) {
		if (now.getYear() != date.getYear()) {
			return dateFormatYear.format(date);
		} else if (date.getDay() != now.getDay()
				&& date.getTime() < now.getTime() + SIX_DAYS_IN_MILLIS) {
			return dateFormatDay.format(date);
		} else if (now.getMonth() != date.getMonth()) {
			return dateFormatMonth.format(date);
		} else {
			return dateFormatTime.format(date);
		}

	}

	private void loadVersionListFromTimeMachine() {
		/* remove the handler so it doesn't fire events while we are working */
		listBoxChangeHandlerRegistration.removeHandler();

		/*
		 * get the date of the tree at the time it is currently displayed at in
		 * milliseconds
		 */
		Long currentChangeID = this.currentChangeID;

		/* clear the list */
		versionList.clear();
		Log log = Log.getLog("vm.lvlftm");

		/* build the list */
		List<ViewChange> reverseList = getChangeList();

		/* reverse the list so youngest changes come first */
		Collections.reverse(reverseList);
		int i = 0;
		int newSelectionIndex = -1;
		for (ViewChange viewChange : reverseList) {
			Change change = viewChange.change;

			/* figure out which item should be selected in the list */
			Long changeID = change.id;
			int comparison = currentChangeID.compareTo(changeID);
			/* if we have a change at the exact same date, we use that */
			if (comparison == 0) {
				log.log("###########   Selecting Item:" + i + "; changeID:"
						+ changeID + "; currentID:" + currentChangeID);
				newSelectionIndex = i;
			}
			/*
			 * if we reach a date that is younger than the current date of the
			 * tree before we have reached a change with the same date (this
			 * could happen for instance if the branch containing the node with
			 * the currently selected change is closed, and that change has
			 * become invisible), then we use the next change instead
			 */
			else if (comparison > 0 && newSelectionIndex == -1) {
				log.log("###########   Selecting Item:" + i
						+ " - 1; before item with changeID:" + changeID
						+ "; currentID:" + currentChangeID);
				newSelectionIndex = i - 1;
			}

			/*
			 * add the item, either with the date of the change or as place
			 * holder for hidden changes
			 */
			if (!viewChange.hidden) {
				versionList.addItem("" + formatDate(change.date) + " ["
						+ change.changeType + "]", "" + changeID);
			} else {
				versionList.addItem("----------------------------------------",
						"" + changeID);
			}
			log.log("\n" + change);
			i++;
		}

		/* add an item younger than all items so that first change can be shown */
		Long youngestID = timeMachineMap.get(timeMachineMap.firstKey()).get(0).change.id;
		versionList.addItem("----------------------------------------", ""
				+ (youngestID - 1));

		versionList.setSelectedIndex(newSelectionIndex);

		listBoxChangeHandlerRegistration = versionList
				.addChangeHandler(ModeVersions.this);
		log.finish();
	}

	public List<ViewChange> getChangeList() {
		Log log = Log.getLog("mv.gcl");

		/*
		 * build a list of ViewChanges, with one change per date, which includes
		 * only changes not marked alwaysHidden.
		 */
		Collection<List<ViewChange>> mapValues = timeMachineMap.values();
		List<ViewChange> viewChangeList = new ArrayList<ViewChange>(
				mapValues.size());
		log.log("all changes in change list");
		for (List<ViewChange> viewChanges : mapValues) {
			log.logln(viewChanges, "\n");
			for (ViewChange viewChange : viewChanges) {
				if (!viewChange.alwaysHidden) {
					viewChangeList.add(viewChange);
					break;
				}
			}
		}

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
		 * way to open up a closed node (because the open/close icon will not
		 * appear for a node with no children) whose closure resulted in hiding
		 * the change... and thus no way to unhide the change...
		 */
		ViewChange oldestChange = viewChangeList.get(viewChangeList.size() - 1);
		if (oldestChange.hidden) {
			returnList.add(oldestChange);
		}
		log.finish();
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
			SortedMultiMap<Long, ViewChange> viewChanges) {

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
				ViewNode childDummy = (ViewNode) child
						.createDummyChild(childDummyID);
				child.addItem(childDummy);
			}
		}

		/*
		 * for both deleted and undeleted nodes, we need to create dummies for
		 * the deleted children.
		 */
		for (Long deletedID : deletedGrandChildIDs) {
			ViewNodeVer childDummy = child.createDummyChild(deletedID);
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
	 * 
	 * Why have the dummy nodes? The dummy nodes are manipulated as if they were
	 * real nodes as the tree moves through time. So, for instance, they are
	 * moved from the deleted list to the existing list, so when we load the
	 * real nodes from the server we know which ones to create as deleted, and
	 * which ones to create a existing.
	 * 
	 * The dummy nodes will also keep track of other information that exists in
	 * the parents change history but that is relevant to reconstructing the
	 * children. For instance, when a node is deleted, the information needed to
	 * reconstruct the initial version of it is in the deletion event which is
	 * in the parents change history. Arg deletions have pro/con flag, and
	 * content, and prop deletions have negation flag, and content.
	 */
	public void mergeLoadedNodes(ViewNodeVer viewNodeVer,
			Map<Long, NodeWithChanges> nodesWithChanges) {

		SortedMultiMap<Long, ViewChange> viewChanges = new SortedMultiMap<Long, ViewChange>();

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

		recursiveHideLinkedSubtreesDuringUnlinkedPeriods(
				(ViewNodeVer) viewNodeVer.getOldestAncestor(),
				new TimePeriods());

		zoomToCurrentChangeAndReloadChangeList(viewNodeVer, viewChanges,
				viewChanges.lastKey());
		// zoomToCurrentChangeAndReloadChangeList(viewNodeVer, viewChanges,
		// Long.MAX_VALUE);
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
			SortedMultiMap<Long, ViewChange> subTreeChanges = new SortedMultiMap<Long, ViewChange>();
			recursiveGetViewChanges(viewNodeVer, subTreeChanges, true, log);

			zoomToCurrentChangeAndReloadChangeList(viewNodeVer, subTreeChanges,
					viewNodeVer.getChangeIDOnClose());

		}
		log.finish();
		logTreeWithChanges();
	}

	public void zoomToCurrentChangeAndReloadChangeList(ViewNodeVer viewNodeVer,
			SortedMultiMap<Long, ViewChange> subTreeChanges, Long startChangeID) {

		viewNodeVer.setOpen(true);

		travelFromChangeToChange(startChangeID, currentChangeID, subTreeChanges);

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
		viewNodeVer.setChangeIDOnClose(currentChangeID);
		log.log("Removing View Changes: ");
		SortedMultiMap<Long, ViewChange> subTreeChanges = new SortedMultiMap<Long, ViewChange>();
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
			SortedMultiMap<Long, ViewChange> forAddOrRemove, boolean firstNode,
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
				forAddOrRemove.put(viewChange.change.id, viewChange);
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

		String changeIDString = versionList.getValue(versionList
				.getSelectedIndex());
		Long destinationChangeID = Long.parseLong(changeIDString);
		travelFromChangeToChange(currentChangeID, destinationChangeID,
				timeMachineMap);

		// TODO: if multiple copies of the same linked proposition are
		// showing
		// how do we know which one to make visible?

		/*
		 * must check to make sure a change exists for destinationDate because
		 * destination date may be a place holder to allow the user to walk past
		 * the earliest change.
		 */
		if (timeMachineMap.get(destinationChangeID) != null) {
			treePanel.ensureVisible((ViewNode) timeMachineMap.get(
					destinationChangeID).get(0).viewNode);
		}

	}

	public void travelFromChangeToChange(Long currentChangeID,
			Long newChangeID, SortedMultiMap<Long, ViewChange> changes) {
		Log log = Log.getLog("tm.ttd");
		if (newChangeID < currentChangeID) {
			log.log("traveling back to Change.id:" + newChangeID
					+ "; from Change.id:" + currentChangeID);
			/*
			 * here newChangeID is the ID of teh change that the user clicked
			 * on, and is highlighted. Therefore we do not want to process
			 * newDate, because doing so would move the tree to a time before
			 * the date highlighted by the user. But the user wants the tree at
			 * the date highlighted, not before the date highlighted.
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
			List<List<ViewChange>> reverseList = changes.valuesSublist(
					newChangeID, false, currentChangeID, true);
			Collections.reverse(reverseList);
			moveTreeBackwards(reverseList, log);
		} else if (newChangeID > currentChangeID) {
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
			log.log("traveling forward to Change.id:" + newChangeID
					+ "; from Change.id:" + currentChangeID);
			moveTreeForwards(changes.valuesSublist(currentChangeID, false,
					newChangeID, true), log);
		}
		log.logln("ALL CHANGES IN PASSED MAP:");
		log.logln(changes.firstValues());
		this.currentChangeID = newChangeID;
		treeClone.resetState();
		log.finish();
	}

	private void moveTreeForwards(
			Collection<List<ViewChange>> changesToProcess, Log log) {
		log.logln("----re-doing changes----");
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
						log.logln("SETTING CONTENT ON UNDO OF PROP DELETION:"
								+ vC.change.oldContent);
						((ViewPropVer) viewNode)
								.setContent(vC.change.oldContent);
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
