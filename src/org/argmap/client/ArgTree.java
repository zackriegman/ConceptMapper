package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class ArgTree extends Tree {
	private final List<OpenHandler<TreeItem>> openHandlerList = new ArrayList<OpenHandler<TreeItem>>();
	private final List<CloseHandler<TreeItem>> closeHandlerList = new ArrayList<CloseHandler<TreeItem>>();
	private final List<HandlerRegistration> openHandlerRegistrationList = new ArrayList<HandlerRegistration>();
	private final List<HandlerRegistration> closeHandlerRegistrationList = new ArrayList<HandlerRegistration>();

	public ArgTree() {
		super();

		addOpenHandlerTracked(new OpenHandler<TreeItem>() {

			@Override
			public void onOpen(OpenEvent<TreeItem> event) {
				updateStateOpen(event);
			}
		});

		addCloseHandlerTracked(new CloseHandler<TreeItem>() {

			@Override
			public void onClose(CloseEvent<TreeItem> event) {
				updateStateClose(event);
			}
		});
	}

	private final void updateStateClose(CloseEvent<TreeItem> event) {
		Object target = event.getTarget();
		if (target instanceof ViewNode && !(target instanceof ViewDummyVer)) {
			((ViewNode) target).setOpen(false);
		}
	}

	private final void updateStateOpen(OpenEvent<TreeItem> event) {
		Object target = event.getTarget();
		if (target instanceof ViewNode && !(target instanceof ViewDummyVer)) {
			((ViewNode) target).setOpen(true);
		}
	}

	public void resetState() {
		setStateHandlersOff();

		for (int i = 0; i < getItemCount(); i++) {
			recursiveResetState((ViewNode) getItem(i));
		}

		setStateHandlersOn();
	}

	public void setStateHandlersOff() {
		for (HandlerRegistration registration : openHandlerRegistrationList) {
			registration.removeHandler();
		}
		for (HandlerRegistration registration : closeHandlerRegistrationList) {
			registration.removeHandler();
		}
	}

	public void setStateHandlersOn() {
		for (OpenHandler<TreeItem> handler : openHandlerList) {
			addOpenHandler(handler);
		}
		for (CloseHandler<TreeItem> handler : closeHandlerList) {
			addCloseHandler(handler);
		}
	}

	public HandlerRegistration addOpenHandlerTracked(
			OpenHandler<TreeItem> handler) {
		openHandlerList.add(handler);
		HandlerRegistration registration = addOpenHandler(handler);
		openHandlerRegistrationList.add(registration);
		return registration;
	}

	public HandlerRegistration addCloseHandlerTracked(
			CloseHandler<TreeItem> handler) {
		closeHandlerList.add(handler);
		HandlerRegistration registration = addCloseHandler(handler);
		closeHandlerRegistrationList.add(registration);
		return registration;
	}

	/*
	 * for some reason the attach handler of ViewProps.TextAreaGrow is being
	 * called before the text area has a scroll height reflecting its actual
	 * height when the TreeItem has no children. When the tree item does have
	 * children the scroll height reflects the actual height upon attachment and
	 * the size is determined appropriately. I've tried putting the resize even
	 * in a deferred command but that doesn't make any difference. Now I'm
	 * trying manually resizing upon an open event.
	 */
	public void recursiveResetState(ViewNode item) {
		/*
		 * if this item has children, and the first child is not a dummy node
		 * place holder loading message
		 */
		if (item.getChildCount() > 0
				&& !(item.getChild(0) instanceof ViewDummyVer)) {
			if (item.getState() != item.isOpen()) {
				item.setState(item.isOpen());

			}
			for (int i = 0; i < item.getChildCount(); i++) {
				ViewNode child = item.getChild(i);
				recursiveResetState(child);
				// if (child instanceof ViewProp) {
				// ((ViewProp) child).resize();
				// }
			}
		}
	}

	/*
	 * solves the annoying problem where the tree scrolls to a previously
	 * selected node when opening a new node thereby often putting the new node
	 * out of view.
	 */
	@Override
	public void onBrowserEvent(Event event) {
		int eventType = DOM.eventGetType(event);

		switch (eventType) {
		case Event.ONCLICK:
			Element e = DOM.eventGetTarget(event);
			if (e.getTagName().equals("IMG")) {
				return;
			}
		}

		super.onBrowserEvent(event);
	}

	public ViewNode getViewNode(int index) {
		return (ViewNode) getItem(index);
	}

	public void logTree(Log log) {
		if (Log.on) {
			for (int i = 0; i < getItemCount(); i++) {
				getViewNode(i).logNodeRecursive(0, log, true);
			}
		}
	}

	// public ViewNode getLastVisibleNodeInAncestorPath(ViewNode viewNode) {
	// ViewNode parent = viewNode.getParent();
	// if (parent == null) {
	// return viewNode;
	// } else {
	// ViewNode lastVisibleAncestor = getLastVisibleNodeInAncestorPath(parent);
	// if (lastVisibleAncestor == viewNode.getParent()
	// && lastVisibleAncestor.isOpen()) {
	// return viewNode;
	// } else {
	// return lastVisibleAncestor;
	// }
	// }
	// }
	//
	// private class Stack<T> extends ArrayList<T> {
	// public void push(T object) {
	// add(object);
	// }
	//
	// public T pop() {
	// return get(size() - 1);
	// }
	// }
	//
	// public ViewNode getVisiblyPreceedingNode(ViewNode viewNode) {
	// ViewNode parent = viewNode.getParent();
	// ViewNode candidate = parent;
	// ViewNode preCandidate = viewNode;
	// while (parent != null) {
	// if (!parent.isOpen()) {
	// preCandidate = candidate;
	// candidate = parent;
	// }
	// parent = parent.getParent();
	// }
	// }
	//
	// /*
	// * hmmm... these methods won't actually work to make sure that the node is
	// * visible because the parent node might be open even while an ancestor is
	// * not, thereby make it invisible. So maybe I should simplify these
	// methods
	// * and make them less general, and use the assumption that the parent is
	// * open and visible, because it must be if the user is deleting a
	// child....
	// */
	// /*
	// * get the node that visually precedes this one given according to what
	// the
	// * user sees on his screen (i.e. the node directly above this one on the
	// * screen).
	// */
	// public ViewNode getVisiblyPreceedingNode_DELETE_ME(ViewNode viewNode) {
	// ViewNode relative = viewNode.getParent();
	// ViewNode previous = viewNode;
	// while (relative != null && !relative.isOpen()) {
	// previous = relative;
	// relative = relative.getParent();
	// }
	//
	// /* relative will equal null where previous was a root node */
	// if (relative == null) {
	// relative = previous.getPreceedingSibling();
	// if (relative == null) {
	// return null;
	// }
	// }
	// /*
	// * where relative is parent we want to get preceeding sibling of
	// * viewNode instead of last child of relative
	// */
	// else if (relative == viewNode.getParent()) {
	// ViewNode preceedingSibling = viewNode.getPreceedingSibling();
	// if (preceedingSibling == null) {
	// return relative;
	// } else {
	// relative = preceedingSibling;
	// }
	// }
	//
	// while (relative.isOpen() && relative.getChildCount() != 0) {
	// relative = relative.getChild(relative.getChildCount() - 1);
	// }
	// return relative;
	// }
	//
	// /*
	// * get the node that visually follows the one given according to what the
	// * user sees on his screen, but that is not a child of the given node
	// (i.e.
	// * the node that would be directly below this one on the screen if this
	// node
	// * was closed.).
	// */
	// public ViewNode getVisiblyFollowingNodeNotInSubTree_DELETE_ME(
	// ViewNode viewNode) {
	// if (viewNode.getParent().isOpen()
	// && viewNode.getFollowingSibling() != null) {
	// return viewNode.getFollowingSibling();
	// }
	//
	// ViewNode relative = viewNode.getParent();
	// ViewNode previous = viewNode;
	// while (relative != null && !relative.isOpen()) {
	// previous = relative;
	// relative = relative.getParent();
	// }
	//
	// /* relative will equal null where previous was a root node */
	// if (relative == null) {
	// relative = previous.getFollowingSibling();
	// if (relative == null) {
	// return null;
	// }
	// }
	//
	// while (relative.isOpen() && relative.getChildCount() != 0) {
	// relative = relative.getChild(0);
	// }
	// return relative;
	// }

	/*
	 * ViewNode, ViewArg and ViewProp make calls to these methods when a node is
	 * loaded/unloaded so these empty methods need to be here instead of in
	 * ModeEditTree, even though they are only used in ModeEditTree. They are
	 * overriden in ModeEditTree to track the additions/deletions of nodes.
	 * ViewNode calls these methods upon a ViewNode being loaded/unloaded when
	 * it is already attached to the tree and already has an ID. ViewProp and
	 * ViewArg call these methods when a upon setNode() if the node is already
	 * loaded and already attached to the tree, or upon being attached to the
	 * tree if the node is already loaded and already has an ID. In otherwords
	 * we are trying to track all the Nodes for which three things are true: it
	 * isLoaded(), it hasID(), and it isAttachedToTree(). (It's easier to test
	 * for those conditions at each of the points where a change happens instead
	 * of here because some of the conditions have to be tested for before
	 * calling here, for instance isAttachedToTree() must be called before
	 * calling these methods because if a node is not attached to the tree it
	 * won't have a reference here.
	 */
	public void onLoadedNodeAdd(ViewNode node) {
	}

	public void onLoadedNodeRemove(ViewNode node) {
	}
}
