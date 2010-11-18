package org.argmap.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.TreeItem;

public abstract class ViewNode extends TreeItem {
	public abstract Long getNodeID();

	private boolean isOpen = true;

	/*
	 * a ViewNode is defined to be loaded when it's children are attached, and
	 * are real nodes with actual content, rather than dummy nodes (with a
	 * "loading from server" message)
	 */
	private boolean isLoaded = false;
	private List<Long> ancestorIDs;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("isOpen:");
		sb.append(isOpen);
		sb.append("; isLoaded:");
		sb.append(isLoaded);
		return sb.toString();
	}

	public abstract ViewNode createViewNodeVerClone();

	public ViewNode removeChildWithID(Long id) {
		int index = indexOfChildWithID(id);
		if (index >= 0) {
			return removeChildAt(index);
		} else {
			throw new RuntimeException("cannot remove node with id " + id
					+ " because it is not a child node");
		}
	}

	public ViewNode removeChildAt(int index) {
		ViewNode child = getChild(index);
		recursiveCallOnRemoveLoadedNode(child);
		child.remove();
		return child;
	}

	private void recursiveCallOnRemoveLoadedNode(ViewNode viewNode) {
		if (viewNode.isLoaded() && getArgTree() != null) {
			getArgTree().onRemovedLoadedNode(viewNode);
			for (int i = 0; i < viewNode.getChildCount(); i++) {
				recursiveCallOnRemoveLoadedNode(viewNode.getChild(i));
			}
		}
	}

	@Override
	public void insertItem(int beforeIndex, TreeItem item) {
		super.insertItem(beforeIndex, item);
		recursiveCallOnAddLoadedNode((ViewNode) item);
	}

	@Override
	public void addItem(TreeItem item) {
		super.addItem(item);
		recursiveCallOnAddLoadedNode((ViewNode) item);
	}

	public void recursiveCallOnAddLoadedNode(ViewNode viewNode) {
		if (viewNode.isLoaded() && getArgTree() != null) {
			getArgTree().onAddLoadedNode(viewNode);
			for (int i = 0; i < viewNode.getChildCount(); i++) {
				recursiveCallOnAddLoadedNode(viewNode.getChild(i));
			}
		}
	}

	@Override
	public void remove() {
		recursiveCallOnRemoveLoadedNode(this);
		super.remove();
	}

	@Override
	public void removeItems() {
		for (int i = 0; i < getChildCount(); i++) {
			recursiveCallOnRemoveLoadedNode(getChild(i));
		}
		super.removeItems();
	}

	@Override
	public void removeItem(TreeItem item) {
		recursiveCallOnRemoveLoadedNode((ViewNode) item);
		super.removeItem(item);
	}

	public int indexOfChildWithID(Long id) {
		for (int i = 0; i < getChildCount(); i++) {
			ViewNode child = getChild(i);
			if (id.equals(child.getNodeID())) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public ViewNode getChild(int index) {
		return (ViewNode) super.getChild(index);
	}

	public ViewNode getParent() {
		return (ViewNode) getParentItem();
	}

	public void logNodeRecursive(int level, Log log,
			boolean includeChildrenOfClosedNodes) {
		if (Log.on) {
			log.logln(Log.spaces(level * 5) + toString() + "; hashCode:"
					+ hashCode());
			if (includeChildrenOfClosedNodes || getState()) {
				for (int i = 0; i < getChildCount(); i++) {
					ViewNode node = getChild(i);
					node.logNodeRecursive(level + 1, log,
							includeChildrenOfClosedNodes);
				}
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

	public abstract void setNode(Node node);

	public abstract Node getNode();

	public abstract void setAsCircularLink();

	/*
	 * As far as I can tell this is currently only used in EditMode.
	 */
	public void recursiveBuildViewNode(Node node, Map<Long, Node> nodes,
			int openDepth) {
		setNode(node);
		boolean circularLink = linkExistsInAncestorPath(node.id);
		if (circularLink) {
			getParent().setAsCircularLink();
		}
		for (Long nodeID : node.childIDs) {
			Node childNode = nodes.get(nodeID);
			if (childNode != null && !circularLink) {
				ViewNode childView = createChild();
				if (openDepth <= 1) {
					childView.setOpen(false);
				}
				addItem(childView);
				childView.recursiveBuildViewNode(childNode, nodes,
						openDepth - 1);
				setLoaded(true);
			} else if (circularLink) {
				setAsCircularLink();
			} else {
				addItem(new ViewDummyVer(nodeID));
				isOpen = false;
			}
		}
		if (node.childIDs.size() == 0) {
			setLoaded(true);
		}
	}

	public boolean linkExistsInAncestorPath(Long id) {
		ViewNode parent = (ViewNode) getParentItem();
		while (parent != null) {
			if (id.equals(parent.getNodeID())) {
				return true;
			}
			parent = (ViewNode) parent.getParentItem();
		}
		return false;
	}

	public List<Long> getAncestorIDs() {
		if (ancestorIDs != null) {
			return ancestorIDs;
		}
		ancestorIDs = new ArrayList<Long>();
		ViewNode parent = this;
		while (parent != null) {
			ancestorIDs.add(parent.getNodeID());
			parent = (ViewNode) parent.getParentItem();
		}
		return ancestorIDs;
	}

	public List<Long> getChildIDs() {
		List<Long> childIDs = new ArrayList<Long>(getChildCount());
		for (int i = 0; i < getChildCount(); i++) {
			childIDs.add(getChild(i).getNodeID());
		}
		return childIDs;
	}

	/*
	 * isOpen differs from getState() in at least one important way. When it is
	 * being used it will return true regardless of whether it has children if
	 * the last time and Open/Close event left it open. This is important in
	 * VersionsMode where open nodes frequently have children added/deleted and
	 * need to remember that they are open or closed. However, I don't think
	 * it's needed for EditMode so it should probably be moved to ViewNodeVer
	 * implementations. But actually it is needed because ArgTree.resetState()
	 * method uses the isOpen() to decided whether to leave the variable open or
	 * closed. Maybe extracting that functionality into ArgTree is silly as it
	 * turns out. VersionsMode and EditMode seem to have different needs in
	 * regard to managing the open/closed state of nodes. VersionsMode needs to
	 * remember whether a node is open or closed even when it has not children,
	 * and reset state is what syncs up that memory with what is displayed on
	 * screen.
	 * 
	 * Edit mode on the other hand simply needs to keep unloaded nodes closed,
	 * which can be handled when they are created and appended to the tree.
	 * 
	 * I guess I'll keep this around for now because I feel like the TreeItem
	 * state variables are a little flaky, for instance they if you set the
	 * state before appending a node it doesn't seem to remember what you set.
	 */
	public boolean isOpen() {
		return isOpen;
	}

	public void setOpen(boolean open) {
		this.isOpen = open;
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public void setLoaded(boolean isLoaded) {
		if (getArgTree() != null && getNodeID() != null) {
			if (isLoaded && this.isLoaded == false) {
				getArgTree().trackLoadedAdd(this);
			} else if (isLoaded == false && this.isLoaded) {
				getArgTree().trackLoadedRemove(this);
			}
		}
		if (isLoaded == true && this.isLoaded == false && getArgTree() != null) {
			getArgTree().onNodeIsLoaded(this);

		}
		this.isLoaded = isLoaded;
	}

	public boolean hasID() {
		if (getNodeID() != null) {
			return true;
		} else {
			return false;
		}
	}

	public ArgTree getArgTree() {
		return (ArgTree) super.getTree();
	}
}
