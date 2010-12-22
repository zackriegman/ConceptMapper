package org.argmap.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MessageDialog extends DialogBox {

	public MessageDialog(String title, String content) {
		this(title, content, "40em", "");
	}

	/*
	 * when set in "em" height seems to work as a minimum height, whereas width
	 * seems to work as a minimum and maximum width.
	 * 
	 * when set in "%" doesn't seem to work much at all...
	 */
	public MessageDialog(String title, String content, String width,
			String height) {
		super();
		setGlassEnabled(true);
		setAnimationEnabled(true);

		setText(title);

		VerticalPanel dialogContents = new VerticalPanel();
		dialogContents.setWidth(width);
		dialogContents.setHeight(height);
		dialogContents.setSpacing(4);
		setWidget(dialogContents);

		HTML details = new HTML(content);
		dialogContents.add(new ScrollPanel(details));

		Button closeButton = new Button("close", new ClickHandler() {
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		dialogContents.add(closeButton);
	}

	public void centerAndShow() {
		center();
		show();
	}
}
