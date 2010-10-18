package org.argmap.client;

import java.util.Collection;
import java.util.Date;
import java.util.List;

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
}
