package org.argmap.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.Tree;

public class TimeMachine {
	private Tree tree;

	private Date currentDate;
	/*
	 * these variables hold the information needed to move the tree forwards and
	 * backwards in time. changes holds the changes from the server which can be
	 * used to move backwards in time. As we move backwards in time, the
	 * information needed to redo various kinds of changes is saved in the
	 * various maps.
	 */
	private SortedMultiMap<Date, ViewChange> changes;
	private Map<Long, String> mapPropContent;
	private Map<Long, String> mapArgTitle;
	private Map<Long, Integer> mapPropIndex;
	private Map<Long, Integer> mapArgIndex;

	/*
	public ViewPropVer absorb(TimeMachine timeTraveler, ViewPropVer propGraft) {
		
		Long propID = propGraft.proposition.id;
		ViewPropVer oldPropView = propViewIndex.get(propID);
		oldPropView.getChild(0).remove();
		while (propGraft.getChildCount() > 0) {
			GWT.log("transplanted argument");
			TreeItem transplant = propGraft.getChild(0);
			transplant.remove();
			oldPropView.addItem(transplant);
		}
		timeTraveler.propViewIndex.remove(propID);

		changes.putAll(timeTraveler.changes);
		mapPropContent.putAll(timeTraveler.mapPropContent);
		mapArgTitle.putAll(timeTraveler.mapArgTitle);
		mapPropIndex.putAll(timeTraveler.mapPropIndex);
		mapArgIndex.putAll(timeTraveler.mapArgIndex);

		return oldPropView;
	}

	public ViewArgVer absorb(TimeMachine timeTraveler, ViewArgVer argGraft) {

		Long argID = argGraft.argument.id;
		ViewArgVer oldArgView = argViewIndex.get(argID);
		oldArgView.getChild(0).remove();
		while (argGraft.getChildCount() > 0) {
			GWT.log("transplanted proposition");
			TreeItem transplant = argGraft.getChild(0);
			transplant.remove();
			oldArgView.addItem(transplant);
		}
		timeTraveler.argViewIndex.remove(argID);

		changes.putAll(timeTraveler.changes);
		mapPropContent.putAll(timeTraveler.mapPropContent);
		mapArgTitle.putAll(timeTraveler.mapArgTitle);
		mapPropIndex.putAll(timeTraveler.mapPropIndex);
		mapArgIndex.putAll(timeTraveler.mapArgIndex);

		return oldArgView;
	}
	*/

	/*
	 * TimeTraveler will work with a null tree, but if it gets a top level
	 * proposition that needs to be added to the tree it will probably throw a
	 * null pointer exception
	 */
	public TimeMachine(SortedMultiMap<Date, ViewChange> changes, Tree tree) {
		this.changes = changes;
		this.tree = tree;

		mapPropContent = new HashMap<Long, String>();
		mapArgTitle = new HashMap<Long, String>();
		mapArgIndex = new HashMap<Long, Integer>();
		mapPropIndex = new HashMap<Long, Integer>();

		currentDate = changes.lastKey();
	}

	public Date getCurrentDate() {
		return currentDate;
	}

	public List<Change> getChangeList() {
		List<ViewChange> viewChangeList = changes.firstValues();
		List<Change> returnList = new ArrayList<Change>(viewChangeList.size());
		for (ViewChange viewChange : viewChangeList) {
			returnList.add(viewChange.change);
		}
		return returnList;
	}

	public void travelToDate(Date newDate) {
		ArgMap.logStart("TM.TTD");
		if (newDate.before(currentDate)) {
			ArgMap.log("TM.TTD", "traveling back to date:" + newDate);
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
			ArgMap.log("TM.TTD", "traveling forward to date:" + newDate);
			moveTreeForwards(changes.valuesSublist(currentDate, false, newDate,
					true));
		}
		currentDate = newDate;
		ArgMap.logEnd("TM.TTD");
	}

	private void moveTreeForwards(Collection<List<ViewChange>> changesToProcess) {
		ArgMap.logln("TM.TTD", "----re-doing changes----");
		for (List<ViewChange> changeList : changesToProcess) {
			for (ViewChange vC : changeList) {
				ArgMap.logln("TM.TTD", "processing: " + vC.change );
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
							mapPropIndex.get(vC.change.id));
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
					propView.setContent(mapPropContent.get(vC.change.id));
					break;
				}
				case ARG_ADDITION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					propView.reviveDeletedView(vC.change.argID,
							mapArgIndex.get(vC.change.id));
					break;
				}
				case ARG_DELETION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					propView.removeAndSaveChildView(vC.change.argID);
					break;
				}
				case ARG_MODIFICATION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					argView.setArgTitle(mapArgTitle.get(vC.change.id));
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
							mapPropIndex.get(vC.change.id));

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
		ArgMap.logln("TM.TTD", "----undoing changes----");
		for (List<ViewChange> changeList : changesToProcess) {
			for (ViewChange vC : changeList) {
				ArgMap.logln("TM.TTD", "processing: " + vC.change );
				switch (vC.change.changeType) {
				case PROP_DELETION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??
					argView.reviveDeletedView(vC.change.propID,
							vC.change.argPropIndex);
					ViewProp viewPropVer = argView.getPropView(vC.change.argPropIndex);
					viewPropVer.proposition.content = vC.change.content;
					break;
				}
				case PROP_ADDITION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					// TODO: root prop additions? who keeps the add/remove of
					// the prop??!!??
					int index = argView.indexOfChildWithID(vC.change.propID);
					mapPropIndex.put(vC.change.id, index);
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
					mapPropContent.put(vC.change.id, propView.getContent());
					propView.setContent(vC.change.content);
					break;
				}
				case ARG_ADDITION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					int index = propView.indexOfChildWithID(vC.change.argID);
					mapArgIndex.put(vC.change.id, index);
					propView.removeAndSaveChildView(vC.change.argID);
					break;
				}
				case ARG_DELETION: {
					ViewPropVer propView = (ViewPropVer) vC.viewNode;
					propView.reviveDeletedView(vC.change.argID,
							vC.change.argPropIndex);
					ViewArg viewArgVer = propView.getArgView(vC.change.argPropIndex);
					viewArgVer.argument.pro = vC.change.argPro;
					viewArgVer.setArgTitle(vC.change.content);
					break;
				}
				case ARG_MODIFICATION: {
					ViewArgVer argView = (ViewArgVer) vC.viewNode;
					/* NOTE: see not regarding arg modification */
					mapArgTitle.put(vC.change.id, argView.getArgTitle());
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
					mapPropIndex.put(vC.change.id, index);
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
