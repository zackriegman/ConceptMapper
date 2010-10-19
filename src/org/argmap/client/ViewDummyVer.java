package org.argmap.client;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.argmap.client.ArgMapService.NodeChangesMaps;

public class ViewDummyVer extends ViewNode implements ViewNodeVer {
	Long nodeID;
	
	public ViewDummyVer(Long nodeID ){
		super();
		this.nodeID = nodeID;
		setText("loading from server...");
	}

	@Override
	public List<ViewChange> getViewChangeList() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public List<ViewChange> getViewChangeAddRemoveList() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public Collection<ViewNodeVer> getDeletedViewList() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public ViewNodeVer getChildViewNodeVer(int i) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public Date getClosedDate() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public void setClosedDate(Date closedDate) {
		// TODO Auto-generated method stub
		assert false;
		
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	@Override
	public void setOpen(boolean open) {
		// TODO Auto-generated method stub
		assert false;
		
	}

	@Override
	public Long getNodeID() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public ViewNode createViewNodeVerClone() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public NodeChanges chooseNodeChanges(NodeChangesMaps changesMaps) {
		// TODO Auto-generated method stub
		
		return null;
	}

	@Override
	public ViewNodeVer createDeletedView(Long id) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	@Override
	public boolean isLoaded() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	@Override
	public void setLoaded(boolean isLoaded) {
		// TODO Auto-generated method stub
		assert false;
	}

	

}
