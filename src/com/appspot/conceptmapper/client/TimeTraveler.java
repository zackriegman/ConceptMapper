package com.appspot.conceptmapper.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class TimeTraveler {
	private Tree tree;
	private Map<Long, PropositionView> propViewIndex;
	private Map<Long, ArgumentView> argViewIndex;
	private Date currentDate;
	/*
	 * these variables hold the information needed to move the tree forwards and
	 * backwards in time. changes holds the changes from the server which can be
	 * used to move backwards in time. As we move backwards in time, the
	 * information needed to redo various kinds of changes is saved in the
	 * various maps.
	 */
	// private List<Change> changes;
	private SortedMap<Date, Change> changes;
	private Map<Long, String> mapPropContent;
	private Map<Long, Boolean> mapArgPro;
	private Map<Long, Integer> mapPropIndex;
	private Map<Long, Long> mapPropID;
	
	public PropositionView absorb( TimeTraveler timeTraveler, PropositionView propGraft ){
		/*Long propID = propGraft.proposition.id;
		PropositionView oldPropView = propViewIndex.get( propID );
		propViewIndex.remove(propID);
		TreeItem parentItem = oldPropView.getParentItem();
		if( parentItem != null ){
			parentItem.getChildIndex(child)
		}
		*/
		
		Long propID = propGraft.proposition.id;
		PropositionView oldPropView = propViewIndex.get( propID );
		oldPropView.getChild(0).remove();
		while (propGraft.getChildCount() > 0) {
			GWT.log("transplanted argument" );
			TreeItem transplant = propGraft.getChild(0);
			transplant.remove();
			oldPropView.addItem(transplant);
		}
		timeTraveler.propViewIndex.remove(propID);
		
		propViewIndex.putAll(timeTraveler.propViewIndex);
		argViewIndex.putAll(timeTraveler.argViewIndex);
		changes.putAll(timeTraveler.changes);
		mapPropContent.putAll( timeTraveler.mapPropContent);
		mapArgPro.putAll( timeTraveler.mapArgPro);
		mapPropIndex.putAll( timeTraveler.mapPropIndex);
		mapPropID.putAll( timeTraveler.mapPropID);
		
		return oldPropView;
	}
	
	public ArgumentView absorb( TimeTraveler timeTraveler, ArgumentView argGraft ){
		/*Long propID = propGraft.proposition.id;
		PropositionView oldPropView = propViewIndex.get( propID );
		propViewIndex.remove(propID);
		TreeItem parentItem = oldPropView.getParentItem();
		if( parentItem != null ){
			parentItem.getChildIndex(child)
		}
		*/
		
		Long argID = argGraft.argument.id;
		ArgumentView oldArgView = argViewIndex.get( argID );
		oldArgView.getChild(0).remove();
		while (argGraft.getChildCount() > 0) {
			GWT.log("transplanted proposition" );
			TreeItem transplant = argGraft.getChild(0);
			transplant.remove();
			oldArgView.addItem(transplant);
		}
		timeTraveler.argViewIndex.remove(argID);
		
		propViewIndex.putAll(timeTraveler.propViewIndex);
		argViewIndex.putAll(timeTraveler.argViewIndex);
		changes.putAll(timeTraveler.changes);
		mapPropContent.putAll( timeTraveler.mapPropContent);
		mapArgPro.putAll( timeTraveler.mapArgPro);
		mapPropIndex.putAll( timeTraveler.mapPropIndex);
		mapPropID.putAll( timeTraveler.mapPropID);
		
		return oldArgView;
	}

	/*
	 * TimeTraveler will work with a null tree, but if it gets a top level
	 * proposition that needs to be added to the tree it will probably throw a
	 * null pointer exception
	 */
	public TimeTraveler(SortedMap<Date, Change> changes,
			Map<Long, PropositionView> propViewIndex,
			Map<Long, ArgumentView> argViewIndex, Tree tree) {
		this.changes = changes;
		this.propViewIndex = propViewIndex;
		this.argViewIndex = argViewIndex;
		this.tree = tree;

		mapPropContent = new HashMap<Long, String>();
		mapArgPro = new HashMap<Long, Boolean>();
		mapPropIndex = new HashMap<Long, Integer>();
		mapPropID = new HashMap<Long, Long>();

		currentDate = changes.lastKey();
	}

	public Date getCurrentDate() {
		return currentDate;
	}
	
	public List<Change> getChangeList(){
		return new ArrayList<Change>(changes.values());
	}

	public void travelToDate(Date newDate) {
		
		if (newDate.before(currentDate)) {
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
			 */
			/*
			 * this was really simple and efficient, but until gwt supports
			 * navigable map it won't work...
			 * moveTreeBackwards(changes.subMap(newDate, false, currentDate,
			 * true).descendingMap().values());
			 */
			GWT.log("traveling back to date:" + newDate);
			List<Change> reverseList = valuesSubset(changes, newDate, currentDate);
			Collections.reverse( reverseList );
			moveTreeBackwards( reverseList );
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
			GWT.log("traveling forward to date:" + newDate);
			moveTreeForwards(valuesSubset(changes, currentDate, newDate));
		}
		currentDate = newDate;
	}

	/*
	 * I was using NavigableMap, and it was really elegant and simple and
	 * includes it's own subMap function... but it is not supported in GWT. So
	 * I'm using this ugly innefficient piece of shit instead, and crossing my
	 * fingers that the next GWT release includes a NavigableMap (which seems
	 * sort of essential if you ask me).
	 * 
	 * Anyway this simply returns a list of changes that fall within the range
	 * date1, non-inclusive, to date2, inclusive.
	 */
	private List<Change> valuesSubset(SortedMap<Date, Change> map, Date date1,
			Date date2) {
		List<Change> list = new ArrayList<Change>(map.values());
		int start = 0;
		int end = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).date.after(date1)) {
				start = i;
				break;
			}
		}
		for (int i = start; i < list.size(); i++) {
			if (list.get(i).date.equals(date2)) {
				end = i + 1;
				break;
			} else if (list.get(i).date.after(date2)) {
				end = i;
				break;
			}
		}
		return list.subList(start, end);
	}

	private void moveTreeForwards(List<Change> changesToProcess) {
		GWT.log("----re-doing changes----");
		for (Change change : changesToProcess) {
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
					argView.insertPropositionViewAt(
							mapPropIndex.get(change.id), propView);

				} else {
					tree.addItem(propView);
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
				ArgumentView argView = new ArgumentView(mapArgPro
						.get(change.id));
				argView.argument.id = change.argID;

				PropositionView newPropView = new PropositionView(false);
				newPropView.proposition.id = mapPropID.get(change.id);
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
				 * as someone browses.
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
	private void moveTreeBackwards(List<Change> changesToProcess) {
		GWT.log("----undoing changes----");
		for (Change change : changesToProcess) {
			GWT.log("processing: " + change.changeType);
			switch (change.changeType) {
			case PROP_DELETION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				// println("change: " + change.toString());
				PropositionView deletedPropView = new PropositionView(false);
				propViewIndex.put(change.propID, deletedPropView);
				deletedPropView.setContent(change.propContent);
				deletedPropView.proposition.id = change.propID;
				argView.insertPropositionViewAt(change.argPropIndex,
						deletedPropView);

				break;
			}
			case PROP_ADDITION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = propViewIndex.get(change.propID);
				if (argView != null) {
					mapPropIndex
							.put(change.id, argView.getChildIndex(propView));
					argView.removeItem(propView);
				} else {
					tree.removeItem(propView);
				}
				propViewIndex.remove(change.propID);
				break;
			}
			case PROP_MODIFICATION: {
				PropositionView propView = propViewIndex.get(change.propID);
				mapPropContent.put(change.id, propView.getContent());
				propView.setContent(change.propContent);

				break;
			}
			case ARG_ADDITION: {
				// printPropRecursive(propView, 0);
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = (PropositionView) argView
						.getChild(0);
				mapArgPro.put(change.id, argView.argument.pro);
				/*
				 * TODO hmmm... this is more complicated isn't it? what about
				 * dummy nodes need to think about what happens when we add and
				 * delete nodes with loadDummys... need to make sure that the
				 * loadDummy get puts back...
				 */
				mapPropID.put(change.id, propView.proposition.id);
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
}
