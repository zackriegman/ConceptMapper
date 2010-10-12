package com.appspot.conceptmapper.client;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class ViewProp extends ViewNode {

	protected TextArea textArea = new TextAreaSloppyGrow();
	protected VerticalPanel mainPanel = new VerticalPanel();
	public Proposition proposition;
	
	public ViewProp(Proposition proposition){
		super();
		this.proposition = proposition;
		setContent(proposition.getContent());
		mainPanel.add(textArea);
		this.setWidget(mainPanel);
		
		if (proposition.linkCount <= 1) {
			textArea.setStylePrimaryName("propositionTextArea");
		} else if (proposition.linkCount > 1) {
			textArea.setStylePrimaryName("linkedPropositionTextArea");
		}
	}
	
	public String toString() {
		return "textArea:" + textArea.getText() + "; id:" + proposition.id;
	}
	
	public Long getNodeID(){
		return proposition.id;
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

	public ViewArgEdit getArgView(int index) {
		return (ViewArgEdit) getChild(index);
	}
	
	private static class TextAreaSloppyGrow extends TextArea {
		public TextAreaSloppyGrow() {
			this(80);
		}

		public void setText(String text) {
			super.setText(text);
			resize();
		}

		private void resize() {
			int widthInCharacters = getCharacterWidth();
			int length = getText().length();

			int lineEstimate = length / widthInCharacters;
			if (lineEstimate < 1) {
				lineEstimate = 1;
			}
			// ConceptMapper.print("onKeyPress: line estimate = " + lineEstimate
			// );
			setVisibleLines(lineEstimate);
		}

		public TextAreaSloppyGrow(int width) {
			super();

			//this.setCharacterWidth(width);
			this.setWidth("50em");
			setVisibleLines(1);

			this.addKeyUpHandler(new KeyUpHandler() {
				public void onKeyUp(KeyUpEvent event) {
					resize();
				}
			});
		}
	}
	
	public void printPropRecursive(int level) {
		GWT.log(ConceptMapper.spaces(level * 2) + "propID:" + proposition.id
				+ "; content:" + getContent());
		for (int i = 0; i < getChildCount(); i++) {
			ViewArgEdit arg = getArgView(i);
			arg.printArgRecursive(level + 1);
		}
	}
}
