package org.argmap.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class ViewArg extends ViewNode {
	private static final String PRO_LABEL_WIDTH = "7.5em";
	private static final String CON_LABEL_WIDTH = "9.3em";
	private static final String PRO_TEXT_BOX_WIDTH = "34.2em";
	private static final String CON_TEXT_BOX_WIDTH = "32.4em";
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
		setNodeButNotTextAreaContent(node);
		textBox.setText(argument.content);
	}

	@Override
	public void setNodeButNotTextAreaContent(Node node) {
		if (!hasID() && isLoaded() && isAttachedToTree()) {
			/*
			 * can't set node before testing for hasID() (because then hasID()
			 * will always return true), but need it set before calling
			 * trackLoadedAdd(), and need it set regardless of this if() clause
			 * so I just set it here, and after the if() clause.
			 */
			argument = (Argument) node;
			getArgTree().onLoadedNodeAdd(this);
		}
		argument = (Argument) node;
		setPro(argument.pro);
	}

	@Override
	public Argument getNode() {
		return argument;
	}

	@Override
	public String getTextAreaContent() {
		return textBox.getText();
	}

	@Override
	public String getDisplayText() {
		return label.getText() + getTextAreaContent();
	}

	@Override
	public void haveFocus() {
		textBox.setFocus(true);
	}

	@Override
	public void setTextAreaContent(String content) {
		textBox.setText(content);
	}

	@Override
	public void setTextAreaCursorPosition(int index) {
		textBox.setCursorPos(index);
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

	@Override
	public ViewProp createDummyChild(Long childID) {
		ViewProp child = (ViewProp) super.createDummyChild(childID);
		Log.log("va.cdc", "creating child with id: " + childID
				+ "; and negated:" + argument.negatedChildIDs.contains(childID));
		child.setNegated(argument.negatedChildIDs.contains(childID));
		return child;
	}

	private final void initialize() {
		label = new Label();
		label.addStyleName("argLabel");
		textBox = new TextBox() {
			@Override
			protected void onLoad() {
				super.onLoad();
				if (hasID() && isLoaded()) {
					getArgTree().onLoadedNodeAdd(ViewArg.this);
				}
			};

			@Override
			public void onUnload() {
				super.onUnload();
				if (hasID() && isLoaded()) {
					getArgTree().onLoadedNodeRemove(ViewArg.this);
				}
			}
		};
		textBox.setVisibleLength(80);
		textBox.setMaxLength(Argument.MAX_LENGTH);
		textBox.addStyleName("argTextBox");
		textBox.addStyleName("nodeText");
		textBox.getElement().getStyle().setMarginRight(1, Unit.EM);
		horizontalPanel = new HorizontalPanel();
		horizontalPanel.add(label);
		horizontalPanel.add(textBox);
		horizontalPanel.addStyleName("argBox");
		focusPanel = new FocusPanel(horizontalPanel);
		setWidget(focusPanel);
	}

	@Override
	public Widget getMainWidget() {
		return focusPanel;
	}

	public void setPro(boolean pro) {
		if (argument != null) {
			argument.pro = pro;
		}
		if (pro) {
			label.setText("Reasoning For: ");
			label.setWidth(PRO_LABEL_WIDTH);
			horizontalPanel.addStyleName("proArg");
			textBox.addStyleName("proArg");
			textBox.setWidth(PRO_TEXT_BOX_WIDTH);
		} else {
			label.setText("Reasoning Against: ");
			label.setWidth(CON_LABEL_WIDTH);
			horizontalPanel.addStyleName("conArg");
			textBox.addStyleName("conArg");
			textBox.setWidth(CON_TEXT_BOX_WIDTH);
		}
	}

	@Override
	public Long getNodeID() {
		if (argument != null) {
			return argument.id;
		} else {
			return null;
		}
	}

	@Override
	public void setNodeID(Long id) {
		if (argument == null) {
			argument = new Argument();
		}
		argument.id = id;
	}

	public void setArgTitle(String title) {
		textBox.setText(title);
	}

	public String getArgTitle() {
		return textBox.getText();
	}

	@Override
	public String toString() {
		return super.toString() + "; text:" + getText() + "; arg:" + argument;
	}

	@Override
	public void setAsCircularLink() {
		horizontalPanel.removeStyleName("argBox");
		horizontalPanel.addStyleName("circularLink");
	}

}
