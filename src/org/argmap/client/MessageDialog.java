package org.argmap.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MessageDialog extends DialogBox {

	/* user should call setText() to set title */
	public MessageDialog(String title, String content) {
		super();
		setGlassEnabled(true);
		setAnimationEnabled(true);

		setText(title);

		VerticalPanel dialogContents = new VerticalPanel();
		dialogContents.setWidth("40em");
		dialogContents.setSpacing(4);
		setWidget(dialogContents);

		HTML details = new HTML(content);
		dialogContents.add(details);

		Button closeButton = new Button("close", new ClickHandler() {
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		dialogContents.add(closeButton);
	}
}
