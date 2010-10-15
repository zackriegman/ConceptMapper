package org.argmap.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.user.client.ui.TreeItem;

public abstract class ViewNode extends TreeItem {
	public abstract Long getNodeID();

	public ViewNode removeChildView(Long id) {
		int index = indexOfChildWithID(id);
		if (index >= 0) {
			return removeChildViewAt( index );
		} else {
			throw new RuntimeException("cannot remove node with id " + id
					+ " because it is not a child node");
		}
	}

	public ViewNode removeChildViewAt(int index) {
		ViewNode child = (ViewNode)getChild(index);
		child.remove();
		return child;
	}

	public int indexOfChildWithID(Long id) {
		for (int i = 0; i < getChildCount(); i++) {
			ViewNode child = (ViewNode) getChild(i);
			if (id.equals(child.getNodeID())) {
				return i;
			}
		}
		return -1;
	}
	
	public ViewNode getChildView(int index ){
		return (ViewNode)getChild(index);
	}

	public void insertChildViewAt(int index, ViewNode viewNode) {
		/*
		 * can't figure out how to insert an item at a specific point (instead
		 * items just get inserted as the last of the current TreeItem's
		 * children). So, instead, I'm removing all subsequent TreeItem
		 * children, then adding the new TreeItem (the new proposition) and then
		 * adding back all the subsequent tree items!
		 */

		// first remove all subsequent children
		Queue<TreeItem> removeQueue = new LinkedList<TreeItem>();
		TreeItem currentItem;
		while ((currentItem = getChild(index)) != null) {
			removeQueue.add(currentItem);
			removeItem(currentItem);
		}

		// then add the new one
		addItem(viewNode);

		// then add back the rest
		while (!removeQueue.isEmpty()) {
			TreeItem toRemove = removeQueue.poll();
			addItem(toRemove);
		}
	}
	
	public void logNodeRecursive(int level, String logName ) {
		ArgMap.logln(logName, ArgMap.spaces(level * 2) + toString() + "; hashCode:" + hashCode()  );
		for (int i = 0; i < getChildCount(); i++) {
			ViewNode node = getChildView(i);
			node.logNodeRecursive(level + 1, logName);
		}
	}
}
