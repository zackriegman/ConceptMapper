package org.argmap.client;

import org.argmap.client.ArgMap.MessageType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;

public class ModeAdmin extends ResizeComposite implements ClickHandler {
	ArgMap argMap;
	private final Button clearDatastoreButton = new Button("Wipe Database");
	private final Button populateDatastoreButton = new Button("Populate Database");
	private final Button getPopulateDatastoreCountButton = new Button("Get Populate Database Count");

	private static ArgMapAdminServiceAsync argMapAdminService = GWT
			.create(ArgMapAdminService.class);

	public ModeAdmin(ArgMap argMap) {
		super();
		this.argMap = argMap;

		Label label = new Label("Please Select Administration Option:");
		
		clearDatastoreButton.addClickHandler(this);
		populateDatastoreButton.addClickHandler(this);
		getPopulateDatastoreCountButton.addClickHandler(this);

		FlowPanel flowPanel = new FlowPanel();
		flowPanel.add(label);
		flowPanel.add(clearDatastoreButton);
		flowPanel.add(populateDatastoreButton);
		flowPanel.add(getPopulateDatastoreCountButton);

		DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.EM);
		mainPanel.add(flowPanel);
		initWidget(mainPanel);
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() == clearDatastoreButton) {
			clearDatastore();
		} else if (event.getSource() == populateDatastoreButton ){
			populateDatastore();
		} else if (event.getSource() == getPopulateDatastoreCountButton ){
			getPopulateDatastoreCount();
		}
	}
	
	private void populateDatastore(){
		argMapAdminService.populateDatastore(new AsyncCallback<Void>() {

			@Override
			public void onSuccess(Void result) {
				ArgMap.messageTimed("Datastore Populated", MessageType.INFO);
			}

			@Override
			public void onFailure(Throwable caught) {
				ArgMap.messageTimed(
						"Failure: populateDatastore():" + caught.toString(),
						MessageType.ERROR);

			}
		});
	}
	
	private void getPopulateDatastoreCount(){
		argMapAdminService.getPopulateDatastoreCount(new AsyncCallback<Integer>() {

			@Override
			public void onSuccess(Integer count) {
				ArgMap.messageTimed("" + count + " random propositions/arguments created", MessageType.INFO);
			}

			@Override
			public void onFailure(Throwable caught) {
				ArgMap.messageTimed(
						"Failure: populateDatastore():" + caught.toString(),
						MessageType.ERROR);

			}
		});
	}

	private void clearDatastore() {
		argMapAdminService.clearDatastore(new AsyncCallback<Void>() {

			@Override
			public void onSuccess(Void result) {
				ArgMap.messageTimed("Datastore Cleared", MessageType.INFO);
			}

			@Override
			public void onFailure(Throwable caught) {
				ArgMap.messageTimed(
						"Failure: clearDatastore():" + caught.toString(),
						MessageType.ERROR);

			}
		});
	}
}
