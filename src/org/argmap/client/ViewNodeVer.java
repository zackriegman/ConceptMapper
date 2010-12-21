package org.argmap.client;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.argmap.client.ArgMapService.NodeChangesMaps;

import com.google.gwt.user.client.ui.TreeItem;

public interface ViewNodeVer {
	public List<ViewChange> getViewChangeList();

	public List<ViewChange> getViewChangeHideList();

	public Collection<ViewNodeVer> getDeletedViewList();

	public void clearDeletedViews();

	public void addDeletedItem(ViewNodeVer viewNodeVer);

	public int getChildCount();

	public ViewNodeVer getChildViewNode(int i);

	public void addItem(TreeItem item);

	public ViewNodeVer createChild(Node node);

	public ViewNodeVer createChild(Long nodeID);

	public ViewNodeVer createDummyChild(Long nodeID);

	public void remove();

	public boolean getState();

	public Long getNodeID();

	public Long getChangeIDOnClose();

	public void setChangeIDOnClose(Long changeIDOnClose);

	public boolean isOpen();

	public void setOpen(boolean open);

	public NodeChanges chooseNodeChanges(NodeChangesMaps changesMaps);

	public boolean isLoaded();

	public void setLoaded(boolean isLoaded);

	public ViewNode getOldestAncestor();

	public class CombinedViewIterator implements Iterator<ViewNodeVer>,
			Iterable<ViewNodeVer> {
		private ViewNodeVer viewNodeVer;
		private int existingIndex = 0;
		private Iterator<ViewNodeVer> deletedCollectionIterator;

		public CombinedViewIterator(ViewNodeVer viewNodeVer) {
			this.viewNodeVer = viewNodeVer;
			deletedCollectionIterator = viewNodeVer.getDeletedViewList()
					.iterator();
		}

		@Override
		public boolean hasNext() {
			if (existingIndex < viewNodeVer.getChildCount()) {
				return true;
			} else {
				return deletedCollectionIterator.hasNext();
			}
		}

		@Override
		public ViewNodeVer next() {
			if (existingIndex < viewNodeVer.getChildCount()) {
				existingIndex++;
				return viewNodeVer.getChildViewNode(existingIndex - 1);
			} else {
				return deletedCollectionIterator.next();
			}
		}

		@Override
		public void remove() {
			/*
			 * not implemented yet... (maybe never) so don't call this...
			 */
			assert false;

		}

		@Override
		public Iterator<ViewNodeVer> iterator() {
			return this;
		}
	}
}
