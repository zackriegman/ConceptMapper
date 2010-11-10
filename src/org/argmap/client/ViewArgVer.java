package org.argmap.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argmap.client.ArgMapService.NodeChangesMaps;


public class ViewArgVer extends ViewArg implements ViewNodeVer {
	public List<ViewChange> viewChanges = new ArrayList<ViewChange>();
	public Map<Long, ViewNodeVer> deletedViews = new HashMap<Long, ViewNodeVer>();
	public Date closedDate;
	
	public Date getClosedDate(){
		return closedDate;
	}
	
	public void setClosedDate( Date closedDate ){
		this.closedDate = closedDate;
	}
	
	public ViewArgVer(){
		super();
		textBox.setReadOnly(true);
	}

	public ViewArgVer(boolean pro) {
		super(pro);
		textBox.setReadOnly(true);
	}

	public ViewArgVer(Argument arg) {
		super(arg);
		textBox.setReadOnly(true);
	}

	public void removeAndSaveChildView(Long id) {
		ViewNodeVer viewNode = (ViewNodeVer) removeChildWithID(id);
		deletedViews.put(id, viewNode);
	}

	public void reviveDeletedView(Long id, int index) {
		ViewNode viewNode = (ViewNode) deletedViews.remove(id);
		assert viewNode != null ;
		insertItem(index, viewNode);
	}

//	public ViewPropVer createDeletedView(Long id) {
//		/*
//		 * this view is just empty; can set to false, because real value will be
//		 * set before it is used when the time machine goes back in time...
//		 */
//		ViewPropVer deletedView = new ViewPropVer(id);
//		deletedView.setState(true);
//		deletedViews.put(id, deletedView);
//		return deletedView;
//	}
	
	@Override
	public ViewNode createChild(){
		return new ViewPropVer();
	}
	
	@Override
	public ViewNodeVer createChild(Node node) {
		return new ViewPropVer( (Proposition) node );
	}
	
	@Override
	public ViewNodeVer createChild(Long nodeID) {
		return new ViewPropVer( nodeID );
	}
	
	public void addDeletedItem( ViewNodeVer viewNodeVer ){
		assert viewNodeVer.getNodeID() != null;
		deletedViews.put( viewNodeVer.getNodeID(), viewNodeVer );
		
	}
	
	/*
	@Override
	public ViewNodeVer createDeletedDummyView(Long id) {
		ViewDummyVer deletedView = new ViewDummyVer(id);
		deletedViews.put(id, deletedView );
		return deletedView;
	}
	*/

	public List<ViewChange> getViewChangeList() {
		return viewChanges;
	}
	
	public List<ViewChange> getViewChangeHideList(){
		List<ViewChange> list = new ArrayList<ViewChange>();
		for( ViewChange viewChange : viewChanges ){
			switch( viewChange.change.changeType ){
			case PROP_ADDITION:
			case PROP_DELETION:
			case PROP_LINK:
			case PROP_UNLINK:
				list.add(viewChange);
				break;
			case PROP_MODIFICATION:
			case ARG_ADDITION:
			case ARG_DELETION:
				assert false;
				break;
			case ARG_MODIFICATION:
				//do nothing
				break;
			}
		}
		return list;
	}

	public ViewNodeVer getChildViewNode(int i) {
		return (ViewNodeVer) getChild(i);
	}
	
	public Collection<ViewNodeVer> getDeletedViewList(){
		return deletedViews.values();
	}

	@Override
	public NodeChanges chooseNodeChanges(NodeChangesMaps changesMaps) {
		return changesMaps.argChanges.get(getNodeID());
	}

	@Override
	public void clearDeletedViews() {
		deletedViews.clear();
		
	}

}
