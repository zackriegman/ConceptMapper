package org.argmap.client;

import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public abstract class ViewArg extends ViewNode {
	private static final String ARG_WIDTH = "48.5em";
	public Argument argument;
	protected Label label;
	protected TextBox textBox;
	protected FocusPanel focusPanel;
	protected HorizontalPanel horizontalPanel;

	public ViewArg(Argument arg) {
		super();
		initialize();
		setNode(arg);
	}

	public ViewArg(boolean pro) {
		super();
		initialize();
		Argument argument = new Argument();
		argument.pro = pro;
		setNode(argument);
	}

	public ViewArg() {
		super();
		initialize();
	}

	@Override
	public void setNode(Node node) {
		argument = (Argument) node;
		textBox.setText(argument.content);
		setPro(argument.pro);
	}

	@Override
	public Argument getNode() {
		return argument;
	}

	/*
	 * TODO: move this method from ViewArg and the equivalent from ViewProp into
	 * ViewArgVer and ViewPropVer.
	 */
	@Override
	public ViewNode createViewNodeVerClone() {
		ViewArgVer argView = new ViewArgVer(new Argument(argument));
		argView.setOpen(getModifiedState());
		argView.setLoaded(getModifiedState());
		return argView;
	}

	private final void initialize() {
		label = new Label();
		label.addStyleName("argLabel");
		textBox = new TextBox();
		textBox.setVisibleLength(Argument.MAX_LENGTH);
		textBox.setMaxLength(Argument.MAX_LENGTH);
		textBox.addStyleName("argTextBox");
		textBox.addStyleName("nodeText");
		horizontalPanel = new HorizontalPanel();
		horizontalPanel.setWidth(ARG_WIDTH);
		horizontalPanel.add(label);
		horizontalPanel.add(textBox);
		horizontalPanel.addStyleName("argBox");
		focusPanel = new FocusPanel(horizontalPanel);
		setWidget(focusPanel);
	}

	public void setPro(boolean pro) {
		if (argument != null) {
			argument.pro = pro;
		}
		if (pro) {
			label.setText("Argument For: ");
			horizontalPanel.addStyleName("proArg");
			textBox.addStyleName("proArg");
		} else {
			label.setText("Argument Against: ");
			horizontalPanel.addStyleName("conArg");
			textBox.addStyleName("conArg");
		}
	}

	@Override
	public Long getNodeID() {
		if (argument != null) {
			return argument.id;
		} else
			return null;
	}

	public void setArgTitle(String title) {
		textBox.setText(title);
	}

	public String getArgTitle() {
		return textBox.getText();
	}

	@Override
	public String toString() {
		return "text:" + getText() + "; arg:" + argument;
	}

	@Override
	public void setAsCircularLink() {
		horizontalPanel.removeStyleName("argBox");
		horizontalPanel.addStyleName("circularLink");
	}

}
