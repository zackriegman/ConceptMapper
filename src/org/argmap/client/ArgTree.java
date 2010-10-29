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

	public void recursiveResetState(ViewNode item) {
		/*
		 * if this item has children, and the first child is not a dummy node
		 * place holder loading message
		 */
		if (item.getChildCount() > 0
				&& !(item.getChild(0) instanceof ViewDummyVer)) {
			if (item.getState() != item.isOpen) {
				item.setState(item.isOpen());
			}
			for (int i = 0; i < item.getChildCount(); i++) {
				recursiveResetState(item.getChildView(i));
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

}
