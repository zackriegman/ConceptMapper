package org.argmap.client;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.argmap.client.ArgMapService.NodeChangesMaps;

import com.google.gwt.user.client.ui.TreeItem;

public interface ViewNodeVer {
	public List<ViewChange> getViewChangeList();
	public List<ViewChange> getViewChangeAddRemoveList();
	public Collection<ViewNodeVer> getDeletedViewList();
	public int getChildCount();
	public ViewNodeVer getChildViewNodeVer( int i );
	public boolean getState();
	public Long getNodeID();
	public Date getClosedDate();
	public void setClosedDate( Date closedDate );
	public boolean isOpen();
	public void setOpen( boolean open );
	public NodeChanges chooseNodeChanges( NodeChangesMaps changesMaps );
	public ViewNodeVer createDeletedView(Long id);
	public ViewNodeVer createDeletedDummyView( Long id );
	public boolean isLoaded();
	public void setLoaded( boolean isLoaded );
	public void addItem( TreeItem item );
}
