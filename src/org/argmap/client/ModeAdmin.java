package org.argmap.client;

import org.argmap.client.ArgMap.MessageType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Tree;

public class ModeAdmin extends ResizeComposite implements ClickHandler {
	ArgMap argMap;
	private final Button clearDatastoreButton = new Button("Wipe Database");
	private final Button populateDatastoreButton = new Button(
			"Populate Database");
	private final Button getPopulateDatastoreCountButton = new Button(
			"Get Populate Database Count");
	private final Button refreshEditModeOn = new Button("Turn Refresh On");
	private final Button refreshEditModeOff = new Button("Turn Refresh Off");
	private final Button emailOpenedTree = new Button("Email Opened Tree");
	private final Button showOpenedTree = new Button(
			"Show Copyable Text Version Of Opened Tree");

	private static ArgMapAdminServiceAsync argMapAdminService = GWT
			.create(ArgMapAdminService.class);

	public ModeAdmin(ArgMap argMap) {
		super();
		this.argMap = argMap;

		Label label = new Label("Please Select Administration Option:");

		clearDatastoreButton.addClickHandler(this);
		populateDatastoreButton.addClickHandler(this);
		getPopulateDatastoreCountButton.addClickHandler(this);
		refreshEditModeOn.addClickHandler(this);
		refreshEditModeOff.addClickHandler(this);
		emailOpenedTree.addClickHandler(this);
		showOpenedTree.addClickHandler(this);

		FlowPanel flowPanel = new FlowPanel();
		flowPanel.add(label);
		flowPanel.add(clearDatastoreButton);
		flowPanel.add(populateDatastoreButton);
		flowPanel.add(getPopulateDatastoreCountButton);
		flowPanel.add(refreshEditModeOn);
		flowPanel.add(refreshEditModeOff);
		flowPanel.add(emailOpenedTree);
		flowPanel.add(showOpenedTree);

		DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.EM);
		mainPanel.add(flowPanel);
		initWidget(mainPanel);
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() == clearDatastoreButton) {
			clearDatastore();
		} else if (event.getSource() == populateDatastoreButton) {
			populateDatastore();
		} else if (event.getSource() == getPopulateDatastoreCountButton) {
			getPopulateDatastoreCount();
		} else if (event.getSource() == refreshEditModeOn) {
			refreshEditMode(true);
		} else if (event.getSource() == refreshEditModeOff) {
			refreshEditMode(false);
		} else if (event.getSource() == emailOpenedTree) {
			emailOpenedTree();
		} else if (event.getSource() == showOpenedTree) {
			showOpenedTree();
		}
	}

	private void populateDatastore() {
		argMapAdminService.populateDatastore(new AsyncCallback<Void>() {

			@Override
			public void onSuccess(Void result) {
				ArgMap.messageTimed("Datastore Populating", MessageType.INFO);
			}

			@Override
			public void onFailure(Throwable caught) {
				ArgMap.messageTimed(
						"Failure: populateDatastore():" + caught.toString(),
						MessageType.ERROR);

			}
		});
	}

	private void getPopulateDatastoreCount() {
		argMapAdminService
				.getPopulateDatastoreCount(new AsyncCallback<Integer>() {

					@Override
					public void onSuccess(Integer count) {
						ArgMap.messageTimed("" + count
								+ " random propositions/arguments created",
								MessageType.INFO);
					}

					@Override
					public void onFailure(Throwable caught) {
						ArgMap.messageTimed("Failure: populateDatastore():"
								+ caught.toString(), MessageType.ERROR);

					}
				});
	}

	private void clearDatastore() {
		argMapAdminService.clearDatastore(new AsyncCallback<Void>() {

			@Override
			public void onSuccess(Void result) {
				ArgMap.messageTimed("Datastore Clearing", MessageType.INFO);
			}

			@Override
			public void onFailure(Throwable caught) {
				ArgMap.messageTimed(
						"Failure: clearDatastore():" + caught.toString(),
						MessageType.ERROR);

			}
		});
	}

	private void refreshEditMode(boolean on) {
		argMap.getModeEdit().updateTimer.setOn(on);
	}

	private void showOpenedTree() {
		Log.log("ma.sot", "GOT HERE");
		MessageDialog dialog = new MessageDialog("copyable argument tree",
				buildHTMLOfOpenedTree(), "", "");
		dialog.center();
		dialog.show();
	}

	private String buildHTMLOfOpenedTree() {
		Element root = DOM.createDiv();
		Tree tree = new Tree();
		argMap.getModeEdit().buildTreeCloneOfOpenNodes(tree);
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveBuildEmailHTML((ViewNode) tree.getItem(i), root);
		}
		return root.getInnerHTML();
	}

	private void emailOpenedTree() {
		argMapAdminService.emailFromToCurrentUser("coreason.org argument tree",
				buildHTMLOfOpenedTree(),
				"please view this message in an HTML capable email client",
				new AsyncCallback<Void>() {

					@Override
					public void onSuccess(Void result) {
						ArgMap.messageTimed("Message Sent", MessageType.INFO);
					}

					@Override
					public void onFailure(Throwable caught) {
						ArgMap.messageTimed("Failure: emailOpenedTree():"
								+ caught.toString(), MessageType.ERROR);
					}
				});
	}

	private void recursiveBuildEmailHTML(ViewNode viewNode, Element parent) {
		if (viewNode instanceof ViewDummyVer
				|| (viewNode instanceof ViewArg && !viewNode.isOpen())) {
			return;
		}

		Element element = DOM.createDiv();
		element.getStyle().setProperty("marginLeft", "40px");
		element.setInnerText("-- " + viewNode.getDisplayText());
		parent.appendChild(element);
		for (int i = 0; i < viewNode.getChildCount(); i++) {
			recursiveBuildEmailHTML(viewNode.getChild(i), element);
		}
	}
}
