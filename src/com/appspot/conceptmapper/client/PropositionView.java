package com.appspot.conceptmapper.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PropositionView extends TreeItem implements ClickHandler,
		KeyDownHandler, FocusHandler, ChangeHandler {

	private static PropositionView lastPropositionWithFocus = null;
	
	private TextArea textArea = new TextAreaSloppyGrow();
	private Button proButton = new Button("For");
	private Button conButton = new Button("Against");
	private Proposition proposition;

	public ArgumentView parentArgView() {
		if (!isTopLevel())
			return ((ArgumentView) this.getParentItem());
		else
			return null;
	}

	public boolean isTopLevel() {
		return getParentItem() == null;
		// return getTree().equals( getParentItem() );
	}

	public PropositionView() {
		this(new Proposition());
	}

	public PropositionView(Proposition prop) {
		super();
		this.proposition = prop;
		setContent(proposition.getContent());
		VerticalPanel verticalPanel = new VerticalPanel();
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		verticalPanel.add(textArea);
		verticalPanel.add(horizontalPanel);
		horizontalPanel.add(proButton);
		horizontalPanel.add(conButton);
		this.setWidget(verticalPanel);

		proButton.addClickHandler(this);
		conButton.addClickHandler(this);

		textArea.addKeyDownHandler(this);
		textArea.addFocusHandler(this);
		textArea.addChangeHandler(this);
		//textArea.addBlurHandler(this);
		textArea.setStylePrimaryName("propositionTextArea");
		// proButton.addFocusHandler(this);
		// proButton.addBlurHandler( this);
		// conButton.addFocusHandler(this);
		// conButton.addBlurHandler( this );
		setState(true);
		proButton.setVisible(false);
		conButton.setVisible(false);
	}

	public void setContent(String content) {
		textArea.setText(content);
	}

	public Proposition getProposition() {
		return proposition;
	}

	public void haveFocus() {
		textArea.setFocus(true);
	}

	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource() == proButton) {
			addArgument(true);

		} else if (event.getSource() == conButton) {
			addArgument(false);
		}
	}

	public void addArgument(boolean pro) {
		ArgumentView newArgView = new ArgumentView(pro);
		PropositionView newPropView = new PropositionView();
		newArgView.addItem(newPropView);
		this.addItem(newArgView);
		newArgView.setState(true);
		this.setState(true);
		newPropView.textArea.setFocus(true);
		ServerComm.addArgument(pro, this.proposition, newArgView.argument,
				newPropView.proposition);
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		// TODO: use a lable or HTML for the text and only switch in an editor
		// when focused?

		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == textArea) {
			if (charCode == KeyCodes.KEY_ENTER && parentArgView() == null) {
				event.preventDefault();
			} else if (charCode == KeyCodes.KEY_ENTER
					&& parentArgView() != null) {
				addPropositionAfterThisOne();
				event.preventDefault();
			} else if (charCode == KeyCodes.KEY_BACKSPACE
					&& textArea.getCursorPos() == 0
					&& textArea.getSelectionLength() == 0) {
				removePropositionAndParentArgument();
				event.preventDefault();
			} else if (charCode == KeyCodes.KEY_DELETE
					&& textArea.getText().equals("")) {
				removePropositionAndParentArgument();
				event.preventDefault();
			} else if (charCode == KeyCodes.KEY_DELETE
					&& textArea.getCursorPos() == textArea.getText().length()
					&& textArea.getSelectionLength() == 0) {
				removeNextProposition();
				event.preventDefault();
			}
		}
	}

	public void removeNextProposition() {
		ArgumentView parentArgView = parentArgView();
		int thisIndex = parentArgView.getChildIndex(this);
		PropositionView nextPropView = ((PropositionView) parentArgView
				.getChild(thisIndex + 1));
		if (nextPropView != null && nextPropView.getChildCount() == 0) {
			int cursorPosition = textArea.getCursorPos();
			textArea.setText(textArea.getText()
					+ nextPropView.textArea.getText());
			textArea.setCursorPos(cursorPosition);
			ServerComm.removeProposition(nextPropView.proposition);
			nextPropView.remove();
		}

	}

	public void removePropositionAndParentArgument() {
		if (this.getChildCount() != 0) // cannot delete a proposition with
			// children...must delete children first
			return;

		if (isTopLevel()) { // don't worry about setting the focus or dealing
			// with subsequent or preceeding propositions or
			// parent arguments if this proposition is top leve.
			getTree().removeItem(this);
			ServerComm.removeProposition(this.proposition);
			return;
		}

		ArgumentView parentArgView = parentArgView();
		int thisIndex = parentArgView.getChildIndex(this);
		ConceptMapper.print("point c");
		if (thisIndex == 0) { // if this is the parent argument's first
			// proposition
			if (!textArea.getText().equals("")) // do nothing if textarea is not
				// empty, user must first delete
				// the text to delete a
				// proposition when not
				// combining propositions.
				return;
			ConceptMapper.print("point b");
			if (parentArgView.getChildCount() == 1) { // if this is the only
				// proposition of the
				// argument
				ConceptMapper.print("point a");
				((PropositionView) parentArgView().getParentItem()).textArea
						.setFocus(true); // set the focus on the parent
				// argument's proposition
				parentArgView().remove(); // remove the argument (which includes
				// the proposition) from the tree
			} else { // if there are other childrend
				((PropositionView) parentArgView().getChild(thisIndex + 1)).textArea
						.setFocus(true); // set focus on the next proposition
				parentArgView().removeItem(this); // just remove this
				// proposition
			}
		} else if (thisIndex != 0) { // if this is not the parent argument's
			// first proposition we want to combine
			// propositions
			PropositionView prePropView = ((PropositionView) parentArgView
					.getChild(thisIndex - 1));
			TextArea preTextArea = prePropView.textArea;
			int newCursorPosition = preTextArea.getText().length();
			String combinedText = preTextArea.getText() + textArea.getText();
			preTextArea.setText(combinedText);
			preTextArea.setFocus(true);
			preTextArea.setCursorPos(newCursorPosition);
			prePropView.proposition.setContent(combinedText);
			remove();
			ServerComm.updateProposition(prePropView.proposition);
		}
		ServerComm.removeProposition(this.proposition);
	}

	public void addPropositionAfterThisOne() {
		PropositionView newProposition = new PropositionView();
		int cursorPosition = textArea.getCursorPos();

		/*
		 * can't figure out how to insert an item at a specific point (instead
		 * items just get inserted as the last of the current TreeItem's
		 * children). So, instead, I'm removing all subsequent TreeItem
		 * children, then adding the new TreeItem (the new proposition) and then
		 * adding back all the subsequent tree items!
		 */

		// first remove all subsequent children
		int treePosition = parentArgView().getChildIndex(this);
		ConceptMapper.println("addPropAfterThisOne.treePosition:"+treePosition);
		Queue<TreeItem> removeQueue = new LinkedList<TreeItem>();
		TreeItem currentItem;
		while ((currentItem = parentArgView().getChild(treePosition + 1)) != null) {
			removeQueue.add(currentItem);
			parentArgView().removeItem(currentItem);
		}

		// then add the new one
		parentArgView().addItem(newProposition);

		// then add back the rest
		while (!removeQueue.isEmpty()) {
			TreeItem toRemove = removeQueue.poll();
			parentArgView().addItem(toRemove);
		}

		// then split the text between the current and new proposition
		String content = textArea.getText();
		String firstContent = content.substring(0, cursorPosition);
		String secondContent = content.substring(cursorPosition);

		textArea.setText(firstContent);
		newProposition.textArea.setText(secondContent);

		newProposition.textArea.setCursorPos(0);
		newProposition.textArea.setFocus(true);

		ServerComm.addProposition(newProposition.proposition,
				parentArgView().argument, treePosition + 1);
	}

	@Override
	public void onFocus(FocusEvent event) {
		Object source = event.getSource();
		if (source == textArea) {
			// if another Proposition's buttons are visible hide them
			if ( lastPropositionWithFocus != this
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

	/*
	@Override
	public void onBlur(BlurEvent event) {
		// must add this as a deferred command otherwise buttons disappear
		// before they are clicked...
		if (event.getSource() == textArea) {
			DeferredCommand.addCommand(new Command() {

				@Override
				public void execute() {
					proButton.setVisible(false);
					conButton.setVisible(false);

				}
			});
		}

	}
	*/		

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

			this.setCharacterWidth(width);
			setVisibleLines(1);

			this.addKeyUpHandler(new KeyUpHandler() {
				public void onKeyUp(KeyUpEvent event) {
					resize();
				}
			});
		}
	}

	@Override
	public void onChange(ChangeEvent event) {
		this.proposition.setContent(textArea.getText());
		ServerComm.updateProposition(this.proposition);
	}

}
