package com.appspot.conceptmapper.client;

import java.util.Map;

import com.appspot.conceptmapper.client.EditMode.EditModeTree;
import com.google.gwt.core.client.GWT;
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
		KeyDownHandler, KeyUpHandler, FocusHandler, ChangeHandler {

	private static PropositionView lastPropositionWithFocus = null;

	private TextArea textArea = new TextAreaSloppyGrow();
	private Button proButton;
	private Button conButton;
	private Button linkRemoveButton;
	private Button linkEditButton;
	public Proposition proposition;
	boolean deleted = false;
	boolean editable;

	public String toString() {
		return "textArea:" + textArea.getText() + "; id:" + proposition.id;
	}

	public static PropositionView getLastPropositionWithFocus() {
		return lastPropositionWithFocus;
	}

	public PropositionView createClone() {
		PropositionView cloneView = new PropositionView(new Proposition(
				proposition), false);
		cloneView.textArea.setText(textArea.getText());
		cloneView.setState(getState());
		return cloneView;
	}

	public Argument parentArgument() {
		if (parentArgView() != null) {
			return parentArgView().argument;
		} else {
			return null;
		}
	}

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

	public PropositionView(boolean editable) {
		this(new Proposition(), editable);
	}

	public PropositionView(Proposition prop, boolean editable) {
		super();
		this.proposition = prop;
		this.editable = editable;
		VerticalPanel mainPanel = new VerticalPanel();
		setContent(proposition.getContent());
		mainPanel.add(textArea);
		this.setWidget(mainPanel);

		if (editable) {
			HorizontalPanel editPanel = new HorizontalPanel();
			mainPanel.add(editPanel);
			proButton = new Button("For");
			conButton = new Button("Against");
			editPanel.add(proButton);
			editPanel.add(conButton);
			proButton.addClickHandler(this);
			conButton.addClickHandler(this);
			proButton.setVisible(false);
			conButton.setVisible(false);

			if (proposition.linkCount > 1) {
				linkRemoveButton = new Button("Unlink");
				linkEditButton = new Button("Edit");
				editPanel.add(linkRemoveButton);
				editPanel.add(linkEditButton);
				linkRemoveButton.addClickHandler(this);
				linkEditButton.addClickHandler(this);
				linkRemoveButton.setVisible(false);
				linkEditButton.setVisible(false);
				textArea.setReadOnly(true);
			} else {
				textArea.setReadOnly(false);
			}

			textArea.addKeyDownHandler(this);
			textArea.addKeyUpHandler(this);
			textArea.addFocusHandler(this);
			textArea.addChangeHandler(this);
		} else if (!editable) {
			textArea.setReadOnly(true);
		}

		if (proposition.linkCount <= 1) {
			textArea.setStylePrimaryName("propositionTextArea");
		} else if (proposition.linkCount > 1) {
			textArea.setStylePrimaryName("linkedPropositionTextArea");
		}
		setState(true);

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

	public ArgumentView getArgView(int index) {
		return (ArgumentView) getChild(index);
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
			
			/* note: remove() must come last, otherwise parentArgumet() == null */
			remove();
		} else if (event.getSource() == linkEditButton) {
			textArea.setReadOnly(false);
		}
	}

	public void addArgument(boolean pro) {
		ArgumentView newArgView = new ArgumentView(pro);
		PropositionView newPropView = new PropositionView(true);
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
		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == textArea) {
			if (charCode == KeyCodes.KEY_ENTER && parentArgView() == null) {
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
					removePropositionAndParentArgument();
					event.preventDefault();
				} else if (charCode == KeyCodes.KEY_DELETE
						&& textArea.getText().equals("")) {
					removePropositionAndParentArgument();
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
		ArgumentView parentArgView = parentArgView();
		int thisIndex = parentArgView.getChildIndex(this);
		PropositionView nextPropView = ((PropositionView) parentArgView
				.getChild(thisIndex + 1));
		if (nextPropView != null && nextPropView.getChildCount() == 0
				&& nextPropView.proposition.linkCount <= 1) {
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
			deleted = true;
			return;
		}

		ArgumentView parentArgView = parentArgView();
		int thisIndex = parentArgView.getChildIndex(this);

		/* if this is the parent argument's first proposition */
		if (thisIndex == 0) {
			/*
			 * do nothing if textarea is not empty, user must first delete the
			 * text to delete a proposition when not combining propositions.
			 */
			if (!textArea.getText().equals(""))
				return;

			/* if this is the only proposition of the argument */
			if (parentArgView.getChildCount() == 1) {
				/* set the focus on the parent argument's proposition */
				((PropositionView) parentArgView().getParentItem()).textArea
						.setFocus(true);

				/*
				 * remove the argument (which includes the proposition) from the
				 * tree
				 */
				parentArgView().remove();
			}
			/* if there are other children */
			else {
				/* set focus on the next proposition */
				((PropositionView) parentArgView().getChild(thisIndex + 1)).textArea
						.setFocus(true);
				/* just remove this proposition */
				parentArgView().removeItem(this);
			}
			/*
			 * if this is not the parent argument's first proposition we want to
			 * combine propositions
			 */
		} else if (thisIndex != 0) {
			PropositionView prePropView = ((PropositionView) parentArgView
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
		ServerComm.removeProposition(this.proposition);
		deleted = true;
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
		PropositionView newPropView = new PropositionView(true);
		if (cursorPosition == 0) {
			parentArgView().insertPropositionViewAt(treePosition, newPropView);
			ServerComm.addProposition(newPropView.proposition,
					parentArgView().argument, treePosition);
		} else {
			parentArgView().insertPropositionViewAt(treePosition + 1,
					newPropView);

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
			GWT.log("addProposition(): prop sent:" + newPropView.toString());

			ServerComm.addProposition(newPropView.proposition,
					parentArgView().argument, treePosition + 1);
			updatePropOnServerIfChanged();
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
				if (lastPropositionWithFocus.linkEditButton != null) {
					lastPropositionWithFocus.linkEditButton.setVisible(false);
					lastPropositionWithFocus.linkRemoveButton.setVisible(false);
				}
			}
			// make this proposition's button's visible
			if (editable) {
				proButton.setVisible(true);
				conButton.setVisible(true);
				if (linkEditButton != null) {
					linkEditButton.setVisible(true);
					linkRemoveButton.setVisible(true);
				}
				lastPropositionWithFocus = this;
				ServerComm.searchPropositions(textArea.getText(),
						parentArgument(), getEditModeTree().searchCallback);
			}
		}
	}

	private EditModeTree getEditModeTree() {
		return (EditModeTree) getTree();
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
		updatePropOnServerIfChanged();
		if (proposition.linkCount > 1) {
			textArea.setReadOnly(true);
		}
	}

	private void updatePropOnServerIfChanged() {
		GWT.log("AAAAAAA");
		String trimmedTextAreaContent = textArea.getText() == null ? ""
				: textArea.getText().trim();
		String trimmedPropositionContent = proposition.getContent() == null ? ""
				: proposition.getContent().trim();
		if (!trimmedPropositionContent.equals(trimmedTextAreaContent)) {
			this.proposition.setContent(trimmedTextAreaContent);
			ServerComm.updateProposition(this.proposition);
		}
	}

	public static PropositionView recursiveBuildPropositionView(
			Proposition prop, boolean editable, Map<Long, Proposition> props,
			Map<Long, Argument> args,
			Map<Long, PropositionView> propViewIndex,
			Map<Long, ArgumentView> argViewIndex) {

		PropositionView propView = new PropositionView(prop, editable);
		if (propViewIndex != null)
			propViewIndex.put(prop.id, propView);
		for (Long argID : prop.argIDs) {
			Argument argument = args.get(argID);
			propView.addItem(recursiveBuildArgumentView(argument, editable, props, args,
					propViewIndex, argViewIndex));
		}
		return propView;
	}

	public static ArgumentView recursiveBuildArgumentView(Argument arg,
			boolean editable, Map<Long, Proposition> props,
			Map<Long, Argument> args, Map<Long, PropositionView> propViewIndex,
			Map<Long, ArgumentView> argViewIndex) {

		ArgumentView argView = new ArgumentView(arg);
		if (argViewIndex != null)
			argViewIndex.put(arg.id, argView);
		for (Long propID : arg.propIDs) {
			Proposition proposition = props.get(propID);
			argView.addItem(recursiveBuildPropositionView(proposition, editable, props, args,
					propViewIndex, argViewIndex));
		}
		return argView;
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		Object source = event.getSource();
		if (source == textArea && !deleted) {
			ServerComm.searchPropositions(textArea.getText(), parentArgument(),
					((EditModeTree) getTree()).searchCallback);
		}

	}

	public void printPropRecursive(int level) {
		GWT.log(ConceptMapper.spaces(level * 2) + "propID:" + proposition.id
				+ "; content:" + getContent());
		for (int i = 0; i < getChildCount(); i++) {
			ArgumentView arg = getArgView(i);
			arg.printArgRecursive(level + 1);
		}
	}

}
