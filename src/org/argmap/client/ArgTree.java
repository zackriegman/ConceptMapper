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
				if (child instanceof ViewProp) {
					((ViewProp) child).resize();
				}
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

	/*
	 * the current implementation of clear() in GWT is to call remove on each
	 * item, so we don't need a special method for clear() (and having one
	 * causes problems because nodes are removed multiple times causing MultiMap
	 * to throw an exception
	 */
	// @Override
	// public void clear() {
	// onRemoveAllLoadedNodes();
	// super.clear();
	// }

	// @Override
	// public void removeItems() {
	// onRemoveAllLoadedNodes();
	// super.removeItems();
	// }

	@Override
	public void addItem(TreeItem item) {
		super.addItem(item);
		onAddLoadedNode((ViewNode) item);
	}

	@Override
	public void insertItem(int index, TreeItem item) {
		super.insertItem(index, item);
		onAddLoadedNode((ViewNode) item);
	}

	@Override
	public void removeItem(TreeItem item) {
		onRemoveAllLoadedNodes();
		super.removeItem(item);
	}

	/*
	 * these methods are called by a ViewNode whenever a loaded Node is attached
	 * or removed from the ArgTree or whenever an attached Node becomes loaded.
	 * Edit modes uses them to keep track of loaded nodes.
	 */
	public void onRemovedLoadedNode(ViewNode node) {
	}

	public void onAddLoadedNode(ViewNode node) {
	}

	public void onNodeIsLoaded(ViewNode node) {
	}

	public void onRemoveAllLoadedNodes() {
	}
}
