package org.argmap.client;

import org.argmap.client.ModeEdit.EditModeTree;

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
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;

public class ViewPropEdit extends ViewProp implements ClickHandler,
		KeyDownHandler, KeyUpHandler, FocusHandler, ChangeHandler,
		MouseOverHandler, MouseOutHandler {

	private static ViewPropEdit lastPropositionWithFocus = null;
	private final Button proButton;
	private final Button conButton;
	private final Button expandButton;
	private Button linkRemoveButton;
	private Button linkEditButton;
	private final HorizontalPanel buttonsPanel;

	boolean deleted = false;

	public static ViewPropEdit getLastPropositionWithFocus() {
		return lastPropositionWithFocus;
	}

	public ViewPropEdit() {
		this(new Proposition());
	}

	public ViewPropEdit(Proposition prop) {
		super(prop);

		buttonsPanel = new HorizontalPanel();
		mainPanel.add(buttonsPanel);
		proButton = new Button("For");
		conButton = new Button("Against");
		buttonsPanel.add(proButton);
		buttonsPanel.add(conButton);

		proButton.addClickHandler(this);
		proButton.setStylePrimaryName("button");
		conButton.addClickHandler(this);
		conButton.setStylePrimaryName("button");

		proButton.setVisible(false);
		conButton.setVisible(false);

		expandButton = new Button("+");
		expandButton.addClickHandler(this);
		expandButton.setVisible(false);
		expandButton.setStylePrimaryName("expandButton");
		// expandButton.setStylePrimaryName("button");

		if (proposition.linkCount > 1) {
			setNodeLink(true);
		} else {
			setNodeLink(false);
		}

		textArea.addKeyDownHandler(this);
		textArea.addKeyUpHandler(this);
		textArea.addFocusHandler(this);
		textArea.addChangeHandler(this);
		focusPanel.addFocusHandler(this);
		focusPanel.addMouseOverHandler(this);
		focusPanel.addMouseOutHandler(this);
		setState(true);
	}

	@Override
	public void setNodeLink(boolean link) {
		if (link && linkRemoveButton == null) {
			linkRemoveButton = new Button("Unlink");
			linkEditButton = new Button("Edit");
			buttonsPanel.add(linkRemoveButton);
			buttonsPanel.add(linkEditButton);
			linkRemoveButton.addClickHandler(this);
			linkEditButton.addClickHandler(this);
			linkRemoveButton.setStylePrimaryName("button");
			linkEditButton.setStylePrimaryName("button");
			linkRemoveButton.setVisible(false);
			linkEditButton.setVisible(false);
			textArea.setReadOnly(true);
		} else if (!link && linkRemoveButton != null) {
			buttonsPanel.remove(linkRemoveButton);
			buttonsPanel.remove(linkEditButton);
			linkRemoveButton = null;
			linkEditButton = null;
			textArea.setReadOnly(false);
		}
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
		} else if (event.getSource() == linkRemoveButton) {
			ServerComm.unlinkProp(parentArgument(), proposition);

			/*
			 * note: remove() must come last, otherwise parentArgumet() == null
			 */
			remove();
		} else if (event.getSource() == linkEditButton) {
			textArea.setReadOnly(false);
		} else if (event.getSource() == expandButton) {
			expandButton.setVisible(false);
			getEditMode().loadFromServer(this, 10, 10);
			setOpen(true);
			getEditModeTree().resetState();
		}
	}

	public void addArgument(boolean pro) {
		ViewArgEdit newArgView = new ViewArgEdit(pro);
		ViewPropEdit newPropView = new ViewPropEdit();
		newArgView.addItem(newPropView);
		this.addItem(newArgView);
		newArgView.setOpen(true);
		this.setOpen(true);
		getEditModeTree().resetState();
		newPropView.haveFocus();
		ServerComm.addArg(pro, this.proposition, newArgView.argument);
		ServerComm.addProp(newPropView.proposition, newArgView.argument, 0);
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == textArea) {
			if (charCode == KeyCodes.KEY_ENTER && parentArgView() == null) {
				onChange(null);
				addArgument(true);
				event.preventDefault();
			} else if (charCode == KeyCodes.KEY_ENTER
					&& parentArgView() != null) {
				onChange(null);
				addProposition();
				event.preventDefault();
			}
			/* only do the following key actions if this is not a link */
			else if (proposition.linkCount <= 1) {
				if (charCode == KeyCodes.KEY_BACKSPACE
						&& textArea.getCursorPos() == 0
						&& textArea.getSelectionLength() == 0) {
					removePropositionAndMaybeParentArgument();
					event.preventDefault();
				} else if (charCode == KeyCodes.KEY_DELETE
						&& textArea.getText().equals("")) {
					removePropositionAndMaybeParentArgument();
					event.preventDefault();
				} else if (charCode == KeyCodes.KEY_DELETE
						&& textArea.getCursorPos() == textArea.getText()
								.length() && textArea.getSelectionLength() == 0) {
					removeNextProposition();
					event.preventDefault();
				}
			}
		}
	}

	public void removeNextProposition() {
		ViewArgEdit parentArgView = parentArgView();
		int thisIndex = parentArgView.getChildIndex(this);
		ViewPropEdit nextPropView = ((ViewPropEdit) parentArgView
				.getChild(thisIndex + 1));
		if (nextPropView != null && nextPropView.getChildCount() == 0
				&& nextPropView.proposition.linkCount <= 1) {
			int cursorPosition = textArea.getCursorPos();
			textArea.setText(textArea.getText()
					+ nextPropView.textArea.getText());
			textArea.setCursorPos(cursorPosition);
			ServerComm.deleteProp(nextPropView.proposition);
			nextPropView.remove();
		}

	}

	public void removePropositionAndMaybeParentArgument() {
		if (this.getChildCount() != 0) // cannot delete a proposition with
			// children...must delete children first
			return;

		if (isTopLevel()) { // don't worry about setting the focus or dealing
			// with subsequent or preceeding propositions or
			// parent arguments if this proposition is top level.
			getTree().removeItem(this);
			ServerComm.deleteProp(this.proposition);
			deleted = true;
			return;
		}

		boolean parentArgViewRemoved = false;
		ViewArgEdit parentArgView = parentArgView();
		int thisIndex = parentArgView.getChildIndex(this);

		/* if this is the parent argument's first proposition */
		if (thisIndex == 0) {
			/*
			 * do nothing if textarea is not empty, user must first delete the
			 * text to delete a proposition when not combining propositions.
			 */
			if (!textArea.getText().equals("")) return;

			/* if this is the only proposition of the argument */
			if (parentArgView.getChildCount() == 1) {
				/* set the focus on the parent argument's proposition */
				((ViewPropEdit) parentArgView.getParentItem()).textArea
						.setFocus(true);

				/*
				 * remove the argument (which includes the proposition) from the
				 * tree
				 */
				parentArgView.remove();
				parentArgViewRemoved = true;
			}
			/* if there are other children */
			else {
				/* set focus on the next proposition */
				((ViewPropEdit) parentArgView.getChild(thisIndex + 1)).textArea
						.setFocus(true);
				/* just remove this proposition */
				parentArgView.removeItem(this);
			}
			/*
			 * if this is not the parent argument's first proposition we want to
			 * combine propositions
			 */
		} else if (thisIndex != 0) {
			ViewPropEdit prePropView = ((ViewPropEdit) parentArgView
					.getChild(thisIndex - 1));

			/*
			 * cannot combine contents with a preceeding proposition that is a
			 * link
			 */
			if (prePropView.proposition.linkCount > 1) {
				return;
			}
			TextArea preTextArea = prePropView.textArea;
			int newCursorPosition = preTextArea.getText().length();
			String combinedText = preTextArea.getText() + textArea.getText();
			preTextArea.setText(combinedText);
			preTextArea.setFocus(true);
			preTextArea.setCursorPos(newCursorPosition);
			prePropView.proposition.setContent(combinedText);
			remove();
			prePropView.updatePropOnServerIfChanged();
		}
		ServerComm.deleteProp(this.proposition);

		deleted = true;

		/*
		 * this must come after the ServerComm.removeProposition() otherwise it
		 * will fail on the server because the argumentView will still have
		 * children... (also the deleteProposition() probably wouldn't work out
		 * to well...)
		 */
		if (parentArgViewRemoved == true) {
			ServerComm.deleteArg(parentArgView.argument);
		}
	}

	public void addProposition() {
		int cursorPosition = textArea.getCursorPos();
		String content = textArea.getText();

		/*
		 * for linked props do not permit splitting the prop; only permit adding
		 * new empty props before or after
		 */
		if (proposition.linkCount > 1 && cursorPosition != 0
				&& cursorPosition != content.length()) {
			return;
		}

		int treePosition = parentArgView().getChildIndex(this);
		ViewPropEdit newPropView = new ViewPropEdit();
		if (cursorPosition == 0) {
			parentArgView().insertChildViewAt(treePosition, newPropView);
			ServerComm.addProp(newPropView.proposition,
					parentArgView().argument, treePosition);
		} else {
			parentArgView().insertChildViewAt(treePosition + 1, newPropView);

			// then split the text between the current and new proposition
			content = textArea.getText();
			String firstContent = content.substring(0, cursorPosition);
			String secondContent = content.substring(cursorPosition);

			textArea.setText(firstContent);
			proposition.setContent(firstContent);

			newPropView.textArea.setText(secondContent);
			newPropView.proposition.setContent(secondContent);

			newPropView.textArea.setCursorPos(0);
			newPropView.textArea.setFocus(true);

			ServerComm.addProp(newPropView.proposition,
					parentArgView().argument, treePosition + 1);
			updatePropOnServerIfChanged();
		}

	}

	public void onFocus(FocusEvent event) {
		Object source = event.getSource();
		if (source == textArea) {
			updateButtons();
			getEditMode().sideSearchTimer.setViewProp(this);
			getEditMode().sideSearch( this );
		}
	}


	private ModeEdit getEditMode() {
		return ((EditModeTree) getTree()).getEditMode();
	}

	private EditModeTree getEditModeTree() {
		return (EditModeTree) getTree();
	}

	/*
	 * TODO: delete this method not using this while I test out having the
	 * buttons appear on mouseover events...
	 */
	private void updateButtons() {
		// if another Proposition's buttons are visible hide them
		if (lastPropositionWithFocus != this
				&& lastPropositionWithFocus != null) {
			lastPropositionWithFocus.proButton.setVisible(false);
			lastPropositionWithFocus.conButton.setVisible(false);
			lastPropositionWithFocus.expandButton.setVisible(false);
			if (lastPropositionWithFocus.linkEditButton != null) {
				lastPropositionWithFocus.linkEditButton.setVisible(false);
				lastPropositionWithFocus.linkRemoveButton.setVisible(false);
			}
		}
		// make this proposition's button's visible
		proButton.setVisible(true);
		conButton.setVisible(true);
		expandButton.setVisible(true);
		// expandButton.setEnabled( false );
		if (linkEditButton != null) {
			linkEditButton.setVisible(true);
			linkRemoveButton.setVisible(true);
		}
		lastPropositionWithFocus = this;
	}

	@Override
	public void onChange(ChangeEvent event) {
		updatePropOnServerIfChanged();
		if (proposition.linkCount > 1) {
			textArea.setReadOnly(true);
		}

	}

	private void updatePropOnServerIfChanged() {
		String trimmedTextAreaContent = textArea.getText() == null ? ""
				: textArea.getText().trim();
		String trimmedPropositionContent = proposition.getContent() == null ? ""
				: proposition.getContent().trim();
		if (!trimmedPropositionContent.equals(trimmedTextAreaContent)) {
			this.proposition.setContent(trimmedTextAreaContent);
			ServerComm.updateProp(this.proposition);
		}
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == textArea) {
			getEditMode().sideSearchTimer.keyPress( charCode );
		}
	}

	@Override
	public ViewNode createChild() {
		return new ViewArgEdit();
	}

	@Override
	public void onMouseOut(MouseOutEvent event) {
		topPanel.remove(expandButton);

		// proButton.setVisible(false);
		// conButton.setVisible(false);
		// expandButton.setVisible(false);
		// if (linkEditButton != null) {
		// linkEditButton.setVisible(false);
		// linkRemoveButton.setVisible(false);
		// }
	}

	@Override
	public void onMouseOver(MouseOverEvent event) {

		if (!isLoaded()) {
			// topPanel.add(expandButton, 580, 2);
			topPanel.add(expandButton);
			expandButton.setVisible(true);
		}

		// proButton.setVisible(true);
		// conButton.setVisible(true);
		// if (linkEditButton != null) {
		// linkEditButton.setVisible(true);
		// linkRemoveButton.setVisible(true);
		// }

	}
}
