package org.argmap.client;

import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class ViewProp extends ViewNode {

	private static final int PROP_WIDTH_NUMBER = 47;
	private static final String PROP_WIDTH = "" + PROP_WIDTH_NUMBER + "em";

	protected final TextAreaAutoHeight textArea = new TextAreaAutoHeight() {
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

	public ViewProp(Proposition proposition) {
		super();
		setNode(proposition);
		mainPanel.add(topPanel);
		topPanel.add(textArea);
		textArea.setStylePrimaryName("propositionTextArea");
		textArea.addStyleName("nodeText");
		textArea.setWidth(PROP_WIDTH);
		// textArea.init();
		focusPanel = new FocusPanel(mainPanel);
		this.setWidget(focusPanel);
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

		if (proposition.linkCount <= 1) {
			textArea.removeStyleName("linkedPropositionTextArea");
			setNodeLink(false);
		} else if (proposition.linkCount > 1) {
			textArea.addStyleName("linkedPropositionTextArea");
			setNodeLink(true);
		}
	}

	@Override
	public void setNode(Node node) {
		setNodeButNotTextAreaContent(node);
		setContent(proposition.getContent());
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
		return cloneView;
	}

	public boolean isTopLevel() {
		return getParentItem() == null;
		// return getTree().equals( getParentItem() );
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

	public Proposition getProposition() {
		return proposition;
	}

	public ViewArg getArgView(int index) {
		return (ViewArg) getChild(index);
	}

	public void resize() {
		textArea.resize();
	}

	@Override
	public void setAsCircularLink() {
		textArea.addStyleName("circularLink");
	}
}
