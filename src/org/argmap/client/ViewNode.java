package org.argmap.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.user.client.ui.TreeItem;

public abstract class ViewNode extends TreeItem {
	public abstract Long getNodeID();
	
	public boolean isOpen = true;
	private boolean isLoaded = true;

	public abstract ViewNode createViewNodeVerClone();

	public ViewNode removeChildView(Long id) {
		int index = indexOfChildWithID(id);
		if (index >= 0) {
			return removeChildViewAt(index);
		} else {
			throw new RuntimeException("cannot remove node with id " + id
					+ " because it is not a child node");
		}
	}

	public ViewNode removeChildViewAt(int index) {
		ViewNode child = (ViewNode) getChild(index);
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

	public ViewNode getChildView(int index) {
		return (ViewNode) getChild(index);
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

	public void logNodeRecursive(int level, String logName,
			boolean includeChildrenOfClosedNodes) {
		ArgMap.logln(logName, ArgMap.spaces(level * 2) + toString()
				+ "; hashCode:" + hashCode());
		if (includeChildrenOfClosedNodes || getState()) {
			for (int i = 0; i < getChildCount(); i++) {
				ViewNode node = getChildView(i);
				node.logNodeRecursive(level + 1, logName,
						includeChildrenOfClosedNodes);
			}
		}
	}

	public boolean getModifiedState() {
		if (getState() || getChildCount() == 0) {
			return true;
		} else {
			return false;
		}
	}

	public abstract ViewNode createChild();

	public abstract Node getChildNodeFromNodeList(Long nodeID, Nodes nodes);

	public abstract void setNode(Node node);
	public abstract Node getNode();

	/*
	 * As far as I can tell this is currently only used in EditMode.
	 */
	public void recursiveBuildViewNode(Node node, Nodes nodes) {
		setNode(node);
		for (Long nodeID : node.childIDs) {
			Node childNode = getChildNodeFromNodeList(nodeID, nodes);
			if (childNode != null) {
				ViewNode childView = createChild();
				addItem(childView);
				childView.recursiveBuildViewNode(childNode, nodes);
			}
			else {
				addItem( new ViewDummyVer(nodeID) );
				isLoaded = false;
				isOpen = false;
			}
		}
	}
	
	/*
	 * isOpen differs from getState() in at least one important way.
	 * When it is being used it will return true regardless of whether it has children
	 * if the last time and Open/Close event left it open.  This is important
	 * in VersionsMode where open nodes frequently have children added/deleted
	 * and need to remember that they are open or closed.  However, I don't think
	 * it's needed for EditMode so it should probably be moved to ViewNodeVer implementations.
	 * But actually it is needed because ArgTree.resetState() method uses the isOpen()
	 * to decided whether to leave the variable open or closed.  Maybe extracting that functionality
	 * into ArgTree is silly as it turns out.  VersionsMode and EditMode seem to have different
	 * needs in regard to managing the open/closed state of nodes.  VersionsMode needs to remember
	 * whether a node is open or closed even when it has not children, and reset state is what
	 * syncs up that memory with what is displayed on screen.
	 * 
	 * Edit mode on the other hand simply needs to keep unloaded nodes closed, which can be handled
	 * when they are created and appended to the tree.
	 * 
	 * I guess I'll keep this around for now because I feel like the TreeItem state variables are a little
	 * flaky, for instance they if you set the state before appending a node it doesn't seem to remember
	 * what you set.
	 */
	public boolean isOpen(){
		return isOpen;
	}
	
	public void setOpen( boolean open ){
		this.isOpen = open;
	}

	public boolean isLoaded(){
		return isLoaded;
	}
	
	public void setLoaded( boolean isLoaded ){
		this.isLoaded = isLoaded;
	}
}
