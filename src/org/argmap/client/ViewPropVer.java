package org.argmap.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argmap.client.ArgMapService.NodeChangesMaps;



public class ViewPropVer extends ViewProp implements ViewNodeVer {
	public List<ViewChange> viewChanges = new ArrayList<ViewChange>();

	public Map<Long, ViewNodeVer> deletedViews = new HashMap<Long, ViewNodeVer>();
	public Date closedDate;
	
	public Date getClosedDate(){
		return closedDate;
	}
	
	public void setClosedDate( Date closedDate ){
		this.closedDate = closedDate;
	}

	public ViewPropVer(Proposition proposition) {
		super(proposition);
		textArea.setReadOnly(true);
	}
	
	public ViewPropVer(){
		this( new Long(-1) );
	}

	public ViewPropVer( Long id ) {
		this(new Proposition(id));
		textArea.setReadOnly(true);
	}

	public static ViewPropVer cloneViewEdit_DELETE_ME(ViewPropEdit viewEdit) {
		ViewPropVer cloneView = new ViewPropVer(viewEdit.proposition);
		cloneView.textArea.setText(viewEdit.textArea.getText());
		cloneView.setState(viewEdit.getState());
		return cloneView;
	}

	public void removeAndSaveChildView(Long id) {
		ViewNodeVer viewNode = (ViewNodeVer) removeChildWithID(id);
		deletedViews.put(id, viewNode);
	}

	public void reviveDeletedView(Long id, int index) {
		//GWT.log("reviveDeletedView( " + id + ", " + index + " );" );
		ViewNode viewNode = (ViewNode) deletedViews.remove(id);
		assert viewNode != null ;
		insertItem(index, viewNode);
	}
	

//	public ViewArgVer createDeletedView(Long id) {
//		/*
//		 * this view is just empty; can set to false, because real value will be
//		 * set before it is used when the time machine goes back in time...
//		 */
//		Argument argument = new Argument();
//		argument.id = id;
//		ViewArgVer deletedView = new ViewArgVer(argument);
//		deletedView.setState(true);
//		deletedViews.put(id, deletedView );
//		return deletedView;
//	}
	
	public ViewArgVer createChild( Long id ){
		Argument argument = new Argument();
		argument.id = id;
		ViewArgVer viewArgVer = new ViewArgVer(argument);
		return viewArgVer;
	}
	
	@Override
	public ViewNode createChild(){
		return new ViewArgVer();
	}

	@Override
	public ViewNodeVer createChild(Node node) {
		return new ViewArgVer( (Argument) node );
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
			case ARG_ADDITION:
			case ARG_DELETION:
				list.add(viewChange);
				break;
			case PROP_ADDITION:
			case PROP_DELETION:
			case PROP_LINK:
			case PROP_UNLINK:
			case ARG_MODIFICATION:
				assert false;
				break;
			case PROP_MODIFICATION:
				//do nothing
				break;
			}
		}
		return list;
	}

	public ViewNodeVer getChildViewNode( int i ){
		return (ViewNodeVer) getChild(i);
	}
	
	public Collection<ViewNodeVer> getDeletedViewList(){
		return deletedViews.values();
	}
	
	@Override
	public NodeChanges chooseNodeChanges(NodeChangesMaps changesMaps) {
		return changesMaps.propChanges.get(getNodeID());
	}

	@Override
	public void clearDeletedViews() {
		deletedViews.clear();
	}
}

