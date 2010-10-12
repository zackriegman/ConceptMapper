package com.appspot.conceptmapper.client;

import com.google.gwt.user.client.ui.TreeItem;

public abstract class ViewNode extends TreeItem {
	public abstract Long getNodeID();
	
	public ViewNode removeChildView( Long id ){
		for(int i = 0; i<getChildCount(); i++){
			ViewNode child = (ViewNode)getChild(i);
			if( id == child.getNodeID() ){
				child.remove();
				return child;
			}
		}
		throw new RuntimeException("cannot remove node with id " + id + " because it is not a child node");
	}
}
