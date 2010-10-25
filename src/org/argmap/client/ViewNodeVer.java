package org.argmap.client;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.argmap.client.ArgMapService.NodeChangesMaps;

import com.google.gwt.user.client.ui.TreeItem;

public interface ViewNodeVer {
	public List<ViewChange> getViewChangeList();
	public List<ViewChange> getViewChangeHideList();
	
	public Collection<ViewNodeVer> getDeletedViewList();
	public void clearDeletedViews();
	public void addDeletedItem( ViewNodeVer viewNodeVer );
	
	public int getChildCount();
	public ViewNodeVer getChildViewNode( int i );
	public void addItem( TreeItem item );
	public ViewNodeVer createChild( Node node );
	public ViewNodeVer createChild( Long nodeID );
	public void remove();
	
	public boolean getState();
	public Long getNodeID();
	public Date getClosedDate();
	public void setClosedDate( Date closedDate );
	public boolean isOpen();
	public void setOpen( boolean open );
	public NodeChanges chooseNodeChanges( NodeChangesMaps changesMaps );
	
	public boolean isLoaded();
	public void setLoaded( boolean isLoaded );
	
}
