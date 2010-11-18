package org.argmap.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class ViewProp extends ViewNode {

	private static final int PROP_WIDTH_NUMBER = 47;
	private static final String PROP_WIDTH = "" + PROP_WIDTH_NUMBER + "em";

	protected final TextAreaAutoHeight textArea = new TextAreaAutoHeight() {
		@Override
		public void onLoad() {
			super.onLoad();
			if (getNodeID() != null) {
				getArgTree().trackLoadedAdd(ViewProp.this);
			}
			Log.log("vp.taah.on", "onLoad:" + proposition.id);
		};

		@Override
		public void onUnload() {
			super.onUnload();
			Log.log("vp.taah.on", "onUnload:" + proposition.id);
			getArgTree().trackLoadedRemove(ViewProp.this);
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
	public void setNode(Node node) {
		if (getNodeID() == null && isLoaded() && getArgTree() != null) {
			proposition = (Proposition) node;
			getArgTree().trackLoadedAdd(this);
		}
		proposition = (Proposition) node;
		setContent(proposition.getContent());

		if (proposition.linkCount <= 1) {
			textArea.removeStyleName("linkedPropositionTextArea");
			setNodeLink(false);
		} else if (proposition.linkCount > 1) {
			textArea.addStyleName("linkedPropositionTextArea");
			setNodeLink(true);
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

	public String getContent() {
		return textArea.getText();
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

	protected static class TextAreaAutoResize extends TextArea {
		private final Element element;

		@Override
		public void setText(String text) {
			super.setText(text);
			if (isAttached()) {
				resize();
			}
		}

		// tried to copy this link but ran into problem that I can't get padding
		// as
		// pixels and I can't do math with pixel results in EM
		// https://github.com/jaz303/jquery-grab-bag/blob/f1a3cc1e86cbb248bcb41391d6eff115b1be6d89/javascripts/jquery.autogrow-textarea.js
		private void resize() {
			Element shadow = DOM.createDiv();
			Element areaElement = getElement();

			// Style style = areaElement.getStyle();
			// GWT.log( style.getWidth() + " ||| " +
			// areaElement.getAttribute("width") + "]]" );
			int areaWidth = Integer.parseInt(areaElement.getAttribute("width"));
			// int areaWidth = areaElement.getClientWidth();

			int areaPaddingLeft = Integer.parseInt(areaElement
					.getAttribute("paddingLeft"));
			int areaPaddingRight = Integer.parseInt(areaElement
					.getAttribute("paddingRight"));
			int ghostWidth = areaWidth - areaPaddingLeft - areaPaddingRight;

			DOM.setStyleAttribute(shadow, "position", "absolute");
			DOM.setStyleAttribute(shadow, "top", "-1000");
			DOM.setStyleAttribute(shadow, "left", "-1000");
			DOM.setStyleAttribute(shadow, "width", "" + ghostWidth);
			DOM.setStyleAttribute(shadow, "fontSize",
					areaElement.getAttribute("fontSize"));
			DOM.setStyleAttribute(shadow, "fontFamily",
					areaElement.getAttribute("fontFamily"));
			DOM.setStyleAttribute(shadow, "lineHeight",
					areaElement.getAttribute("lineHeight"));
			DOM.setStyleAttribute(shadow, "resize",
					areaElement.getAttribute("none"));

			shadow.setInnerText(getText());
			com.google.gwt.dom.client.Element body = Document.get().getBody();
			body.appendChild(shadow);
			areaElement.setAttribute("height", shadow.getAttribute("height"));
		}

		public TextAreaAutoResize() {
			element = getElement();
			setWidth(PROP_WIDTH);
			DOM.setStyleAttribute(element, "overflow", "hidden");
			addKeyUpHandler(new KeyUpHandler() {

				@Override
				public void onKeyUp(KeyUpEvent event) {
					resize();
				}
			});

			addAttachHandler(new AttachEvent.Handler() {

				@Override
				public void onAttachOrDetach(AttachEvent event) {
					if (event.isAttached()) {
						resize();
					}
				}
			});
		}
	}

	protected static class TextAreaGrow extends TextArea {
		private final Element element;

		@Override
		public void setText(String text) {
			super.setText(text);
			if (isAttached()) {
				resize();
			}
		}

		private void resize() {
			DOM.setStyleAttribute(element, "height", "1em");
			DOM.setStyleAttribute(element, "height",
					"" + element.getScrollHeight() + "px");
		}

		public TextAreaGrow() {
			element = getElement();
			setWidth(PROP_WIDTH);
			DOM.setStyleAttribute(element, "overflow", "hidden");
			addKeyUpHandler(new KeyUpHandler() {

				@Override
				public void onKeyUp(KeyUpEvent event) {
					resize();
				}
			});

			addAttachHandler(new AttachEvent.Handler() {

				@Override
				public void onAttachOrDetach(AttachEvent event) {
					if (isAttached()) {
						resize();
					}
				}
			});
		}
	}

	@Override
	public void setAsCircularLink() {
		textArea.addStyleName("circularLink");
	}
}
