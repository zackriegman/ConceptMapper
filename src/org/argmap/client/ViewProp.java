package org.argmap.client;

import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class ViewProp extends ViewNode {

	private static final int PROP_WIDTH_NUMBER = 41;
	private static final String PROP_WIDTH = "" + PROP_WIDTH_NUMBER + "em";

	protected final TextAreaAutoHeight textArea = new TextAreaAutoHeight(
			"ViewProp") {
		@Override
		public void onLoad() {
			super.onLoad();
			if (hasID() && isLoaded()) {
				getArgTree().onLoadedNodeAdd(ViewProp.this);
			}
		};

		@Override
		public void onUnload() {
			super.onUnload();
			if (hasID() && isLoaded()) {
				getArgTree().onLoadedNodeRemove(ViewProp.this);
			}
		}
	};
	// protected TextArea textArea = new TextArea();
	protected VerticalPanel mainPanel = new VerticalPanel();
	protected HorizontalPanel topPanel = new HorizontalPanel();
	protected FocusPanel focusPanel;
	public Proposition proposition;
	private Label negatedLabel = null;

	public ViewProp(Proposition proposition) {
		super();
		setNode(proposition);
		mainPanel.add(topPanel);
		topPanel.add(textArea);
		topPanel.setStylePrimaryName("propositionArea");
		textArea.setStylePrimaryName("propositionTextArea");
		textArea.addStyleName("nodeText");
		textArea.setWidth(PROP_WIDTH);
		// textArea.init();
		focusPanel = new FocusPanel(mainPanel);
		this.setWidget(focusPanel);
	}

	@Override
	public Widget getMainWidget() {
		return focusPanel;
	}

	public void setNodeLink(boolean link) {
	};

	@Override
	public void setNodeButNotTextAreaContent(Node node) {
		if (!hasID() && isLoaded() && isAttachedToTree()) {
			/*
			 * can't set node before testing hasID() (because then hasID() would
			 * always return true), but need it set before calling
			 * trackLoadedAdd(), and need it set regardless of this if() clause
			 * so I just set it here, and after the if() clause.
			 */
			proposition = (Proposition) node;
			getArgTree().onLoadedNodeAdd(this);
		}

		proposition = (Proposition) node;

		if (isLink()) {
			topPanel.addStyleName("linkedPropositionTextArea");
			textArea.addStyleName("linkedPropositionTextArea");
			setNodeLink(true);
		} else {
			topPanel.removeStyleName("linkedPropositionTextArea");
			textArea.removeStyleName("linkedPropositionTextArea");
			setNodeLink(false);
		}
	}

	public boolean isLink() {
		return proposition.linkCount > 1;
	}

	public boolean getNegated() {
		if (negatedLabel == null) {
			return false;
		} else {
			return true;
		}
	}

	public void setNegated(boolean negated) {
		if (negated == true) {
			if (negatedLabel == null) {
				negatedLabel = new Label("Not:");
				negatedLabel.addStyleName("negatedPropLabel");

				topPanel.insert(negatedLabel, 0);
			}
		} else {
			if (negatedLabel != null) {
				topPanel.remove(negatedLabel);
				negatedLabel = null;
			}
		}
	}

	@Override
	public String getDisplayText() {
		if (negatedLabel == null) {
			return getTextAreaContent();
		} else {
			return "Not: " + getTextAreaContent();
		}
	}

	@Override
	public void setNode(Node node) {
		setNodeButNotTextAreaContent(node);
		setContent(proposition.getContent());
	}

	@Override
	public void setNegation() {
		ViewNode parent = getParent();
		if (parent != null) {
			setNegated(((Argument) parent.getNode()).negatedChildIDs
					.contains(getNodeID()));
		}
	}

	@Override
	public Proposition getNode() {
		return proposition;
	}

	@Override
	public String toString() {
		return super.toString() + "; textArea:" + textArea.getText()
				+ "; prop:" + proposition;
	}

	@Override
	public Long getNodeID() {
		if (proposition != null) {
			return proposition.id;
		} else
			return null;
	}

	@Override
	public void setNodeID(Long id) {
		if (proposition == null) {
			proposition = new Proposition();
		}
		proposition.id = id;
	}

	public Argument parentArgument() {
		if (parentArgView() != null) {
			return parentArgView().argument;
		} else {
			return null;
		}
	}

	public ViewArgEdit parentArgView() {
		if (!isTopLevel())
			return ((ViewArgEdit) this.getParentItem());
		else
			return null;
	}

	@Override
	public ViewNode createViewNodeVerClone() {
		ViewPropVer cloneView = new ViewPropVer(proposition);
		cloneView.textArea.setText(textArea.getText());
		cloneView.setOpen(getModifiedState());
		cloneView.setLoaded(getModifiedState());
		cloneView.setNegated(getNegated());
		return cloneView;
	}

	public boolean isTopLevel() {
		return getParentItem() == null;
	}

	@Override
	public void haveFocus() {
		textArea.setFocus(true);
	}

	public void setContent(String content) {
		textArea.setText(content);
	}

	@Override
	public String getTextAreaContent() {
		return textArea.getText();
	}

	@Override
	public void setTextAreaContent(String content) {
		textArea.setText(content);
	}

	@Override
	public void setTextAreaCursorPosition(int index) {
		textArea.setCursorPos(index);

	}

	public Proposition getProposition() {
		return proposition;
	}

	public ViewArg getArgView(int index) {
		return (ViewArg) getChild(index);
	}

	@Override
	public void setAsCircularLink() {
		textArea.addStyleName("circularLink");
	}
}
