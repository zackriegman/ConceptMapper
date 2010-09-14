package com.appspot.conceptmapper.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PropositionView extends TreeItem implements ClickHandler,
		KeyPressHandler, FocusHandler {

	private static PropositionView lastPropositionWithFocus = null;
	private TextAreaSloppyGrow textArea = new TextAreaSloppyGrow();
	private Button proButton = new Button("For");
	private Button conButton = new Button("Against");
	private TreeItem parentArgument;
	private Proposition proposition;

	public PropositionView(TreeItem parentArgument) {
		this();
		this.parentArgument = parentArgument;
	}
	
	public PropositionView( Proposition proposition ){
		this();
		this.proposition = proposition;
		setContent( proposition.getContent() );
	}

	public PropositionView() {
		super();
		VerticalPanel verticalPanel = new VerticalPanel();
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		verticalPanel.add(textArea);
		verticalPanel.add(horizontalPanel);
		horizontalPanel.add(proButton);
		horizontalPanel.add(conButton);
		this.setWidget(verticalPanel);

		proButton.addClickHandler(this);
		conButton.addClickHandler(this);

		textArea.addKeyPressHandler(this);
		textArea.addFocusHandler(this);
		// textArea.addBlurHandler(this);
		proButton.addFocusHandler(this);
		// proButton.addBlurHandler( this);
		conButton.addFocusHandler(this);
		// conButton.addBlurHandler( this );
		setState(true);
	}
	
	public void setContent( String content ){
		textArea.setText( content );
	}
	
	public Proposition getProposition(){
		return proposition;
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() == proButton) {
			addArgument("Argument For");

		} else if (event.getSource() == conButton) {
			addArgument("Argument Against");
		}
	}

	public void addArgument(String labelText) {
		TreeItem argumentTreeItem = new TreeItem(labelText);
		PropositionView newProposition = new PropositionView(argumentTreeItem);
		argumentTreeItem.addItem(newProposition);
		this.addItem(argumentTreeItem);
		argumentTreeItem.setState(true);
		this.setState(true);
		newProposition.textArea.setFocus(true);
	}

	@Override
	public void onKeyPress(KeyPressEvent event) {
		char charCode = event.getCharCode();
		Object source = event.getSource();
		String textAreaContent = textArea.getText();
		if (source == textArea && (charCode == '\n' || charCode == '\r')
				&& parentArgument != null) {
			PropositionView newProposition = new PropositionView(parentArgument);

			/*
			 * can't figure out how to insert an item at a specific point
			 * (instead items just get inserted as the last of the current
			 * TreeItem's children). So, instead, I'm removing all subsequent
			 * TreeItem children, then adding the new TreeItem (the new
			 * proposition) and then adding back all the subsequent tree items!
			 */

			// first remove subsequent children
			int treePosition = parentArgument.getChildIndex(this);
			Queue<TreeItem> removeQueue = new LinkedList<TreeItem>();
			TreeItem currentItem;
			while ((currentItem = parentArgument.getChild(treePosition + 1)) != null) {
				removeQueue.add(currentItem);
				parentArgument.removeItem(currentItem);
			}

			// then add the new one
			parentArgument.addItem(newProposition);

			// then add back the rest
			while (!removeQueue.isEmpty()) {
				TreeItem toRemove = removeQueue.poll();
				parentArgument.addItem(toRemove);
			}

			newProposition.textArea.setFocus(true);

			// Window.alert( "splitting proposition for your convenience");
		} else if (source == textArea && (charCode == '\b')
				&& parentArgument != null && textAreaContent.equals("")) {
			if (parentArgument.getChildCount() > 1) {
				parentArgument.removeItem(this);
			} else {
				parentArgument.remove();
			}
		}
	}

	@Override
	public void onFocus(FocusEvent event) {
		Object source = event.getSource();
		if (source == textArea) {
			// if another Proposition's buttons are visible hide them
			if (lastPropositionWithFocus != this
					&& lastPropositionWithFocus != null) {
				lastPropositionWithFocus.proButton.setVisible(false);
				lastPropositionWithFocus.conButton.setVisible(false);
			}
			// make this proposition's button's visible
			proButton.setVisible(true);
			conButton.setVisible(true);
			lastPropositionWithFocus = this;
		}
	}

	private static class TextAreaSloppyGrow extends TextArea {
		public TextAreaSloppyGrow() {
			this(80);
		}

		public TextAreaSloppyGrow(int width) {
			super();

			this.setCharacterWidth(width);
			setVisibleLines(1);

			this.addKeyPressHandler(new KeyPressHandler() {
				public void onKeyPress(KeyPressEvent event) {
					TextArea source = (TextArea) event.getSource();
					int widthInCharacters = source.getCharacterWidth();
					String text = source.getText();
					int length = text.length();

					int newLineCount = 0;
					for (int i = 0; i < length; i++) {
						if (text.charAt(i) == '\n') {
							newLineCount++;
						}
					}
					char charCode = event.getCharCode();
					if (charCode == '\n' || charCode == '\r') {
						newLineCount++;
					}

					int lineEstimate = length / widthInCharacters
							+ newLineCount;
					if (lineEstimate < 1) {
						lineEstimate = 1;
					}
					source.setVisibleLines(lineEstimate);
					// source.setVisibleLines( 30 );
				}
			});
		}
	}
}
