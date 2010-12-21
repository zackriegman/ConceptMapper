package org.argmap.client;

import java.util.Collection;
import java.util.List;

import org.argmap.client.ArgMapService.NodeChangesMaps;

public class ViewDummyVer extends ViewNode implements ViewNodeVer {
	Long nodeID;

	public ViewDummyVer(Long nodeID) {
		super();
		this.nodeID = nodeID;
		setText("loading from server...");
	}

	@Override
	public List<ViewChange> getViewChangeList() {
		assert false;
		return null;
	}

	@Override
	public List<ViewChange> getViewChangeHideList() {
		assert false;
		return null;
	}

	@Override
	public Collection<ViewNodeVer> getDeletedViewList() {
		assert false;
		return null;
	}

	@Override
	public ViewNodeVer getChildViewNode(int i) {
		assert false;
		return null;
	}

	@Override
	public boolean isOpen() {
		assert false;
		return false;
	}

	@Override
	public void setOpen(boolean open) {
		assert false;
	}

	/* TODO: this is the only implemented method... explain why that is... */
	@Override
	public Long getNodeID() {
		return nodeID;
	}

	@Override
	public ViewNode createViewNodeVerClone() {
		assert false;
		return null;
	}

	@Override
	public NodeChanges chooseNodeChanges(NodeChangesMaps changesMaps) {

		return null;
	}

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public void setLoaded(boolean isLoaded) {
		assert false;
	}

	/*
	 * @Override public ViewNodeVer createDeletedDummyView(Long id) { assert
	 * false; return null; }
	 */

	@Override
	public ViewNode createChild() {
		assert false;
		return null;
	}

	@Override
	public void setNode(Node node) {
		assert false;

	}

	@Override
	public void addDeletedItem(ViewNodeVer viewNodeVer) {
		assert false;

	}

	@Override
	public ViewNodeVer createChild(Node node) {
		assert false;
		return null;
	}

	@Override
	public void clearDeletedViews() {
		assert false;
	}

	@Override
	public ViewNodeVer createChild(Long nodeID) {
		assert false;
		return null;
	}

	@Override
	public Node getNode() {
		assert false;
		return null;
	}

	@Override
	public String toString() {
		return "[DUMMY - ID: " + nodeID + "]";
	}

	@Override
	public void setAsCircularLink() {
		assert false;
	}

	@Override
	public String getTextAreaContent() {
		assert false;
		return null;
	}

	@Override
	public void setNodeButNotTextAreaContent(Node node) {
		assert false;
	}

	@Override
	public void setTextAreaContent(String content) {
		assert false;
	}

	@Override
	public String getDisplayText() {
		assert false;
		return null;
	}

	@Override
	public void haveFocus() {
		assert false;
	}

	@Override
	public void setTextAreaCursorPosition(int index) {
		assert false;
	}

	@Override
	public Long getChangeIDOnClose() {
		assert false;
		return null;
	}

	@Override
	public void setChangeIDOnClose(Long changeIDOnClose) {
		assert false;
	}
}
