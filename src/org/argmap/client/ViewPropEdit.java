package org.argmap.client;

import java.util.Map;

import org.argmap.client.ModeEdit.EditModeTree;
import org.argmap.client.ModeEdit.SavableNode;
import org.argmap.client.ServerComm.LocalCallback;
import org.argmap.client.StarRating.RatingHandler;

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
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextArea;

public class ViewPropEdit extends ViewProp implements ClickHandler,
		KeyDownHandler, KeyUpHandler, FocusHandler, ChangeHandler,
		MouseOverHandler, MouseOutHandler, SavableNode, RatingHandler {

	private static ViewPropEdit lastPropositionWithFocus = null;
	private final Button proButton;
	private final Button conButton;
	private final Button expandButton;
	private final StarRating ratingView;
	private Button linkRemoveButton;
	private Button linkEditButton;
	// private final HorizontalPanel buttonsPanel;
	private final FlexTable buttonsPanel;
	private static final String[] ratingMessages = {
			"definitely not true (~0% chance of being true)",
			"probably not true (~25% chance of being true)",
			"as likely to be true as not true (~50% chance of being true)",
			"probably true (~75% chance of being true)",
			"definitely true (~100% chance of being true)" };

	boolean deleted = false;

	public static ViewPropEdit getLastPropositionWithFocus() {
		return lastPropositionWithFocus;
	}

	public ViewPropEdit() {
		this(new Proposition());
	}

	public ViewPropEdit(Proposition prop) {
		super(prop);

		// buttonsPanel = new HorizontalPanel();
		// buttonsPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		buttonsPanel = new FlexTable();

		/*
		 * commented out because only attaching the panel when the text area
		 * gets focus
		 */
		// mainPanel.add(buttonsPanel);

		proButton = new Button("For");
		conButton = new Button("Against");
		buttonsPanel.setWidget(0, 0, proButton);
		buttonsPanel.setWidget(0, 1, conButton);

		proButton.addClickHandler(this);
		proButton.setStylePrimaryName("button");
		conButton.addClickHandler(this);
		conButton.setStylePrimaryName("button");

		expandButton = new Button("+");
		expandButton.addClickHandler(this);
		expandButton.setStylePrimaryName("expandButton");
		// expandButton.setStylePrimaryName("button");

		if (proposition.linkCount > 1) {
			setNodeLink(true);
		} else {
			setNodeLink(false);
		}

		ratingView = new StarRating(ratingMessages);
		ratingView.setRatingHandler(this);
		buttonsPanel.setWidget(0, 5, ratingView);

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
			linkEditButton = new Button("Edit");
			buttonsPanel.setWidget(0, 3, linkEditButton);
			linkEditButton.addClickHandler(this);
			linkEditButton.setStylePrimaryName("button");
			textArea.setReadOnly(true);

			if (getParent() != null) {
				linkRemoveButton = new Button("Unlink");
				buttonsPanel.setWidget(0, 2, linkRemoveButton);
				linkRemoveButton.addClickHandler(this);
				linkRemoveButton.setStylePrimaryName("button");
			}
		} else if (!link && linkRemoveButton != null) {
			buttonsPanel.clearCell(0, 2);
			buttonsPanel.clearCell(0, 3);
			linkRemoveButton = null;
			linkEditButton = null;
			textArea.setReadOnly(false);
		}
	}

	public void onLoad() {
		textArea.addKeyDownHandler(getEditMode().updateTimer);
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
		// final ViewArgEdit newArgView = new ViewArgEdit(pro);
		// final ViewPropEdit newPropView = new ViewPropEdit();
		final ViewArgEdit newArgView = new ViewArgEdit(pro);
		final ViewPropEdit newPropView = new ViewPropEdit();
		newArgView.addItem(newPropView);
		this.addItem(newArgView);
		newArgView.setOpen(true);
		this.setOpen(true);
		getEditModeTree().resetState();
		newPropView.haveFocus();
		ServerComm.addArg(pro, this.proposition, newArgView.argument,
				new LocalCallback<Void>() {
					@Override
					public void call(Void t) {
						newArgView.setLoaded(true);
					}
				});
		ServerComm.addProp(newPropView.proposition, newArgView.argument, 0,
				new LocalCallback<Void>() {
					@Override
					public void call(Void t) {
						newPropView.setLoaded(true);
					}
				});
	}

	public void onKeyDown_DELETE_ME(KeyDownEvent event) {
		/*
		 * tell edit mode that there has been a user action so it doesn't
		 * throttle live updates (do it first in case the key press results in a
		 * deletion in which case the getEditMode() will return null)
		 */
		getEditMode().updateTimer.userAction();

		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == textArea) {
			/*
			 * if it's a root proposition add an argument when user presses
			 * enter
			 */
			if (charCode == KeyCodes.KEY_ENTER && parentArgView() == null) {
				onChange(null);
				addArgument(true);
				event.preventDefault();
			}
			/*
			 * if it is not a root proposition, add a sibling proposition when
			 * user presses enter
			 */
			else if (charCode == KeyCodes.KEY_ENTER && parentArgView() != null) {
				onChange(null);
				addProposition();
				event.preventDefault();
			}
			/* only do the following key actions if this is not a link */
			else if (proposition.linkCount <= 1) {
				if (charCode == KeyCodes.KEY_BACKSPACE
						&& textArea.getCursorPos() == 0
						&& textArea.getSelectionLength() == 0) {
					removePropositionAndMaybeParentArgument_DELETE_ME();
					event.preventDefault();
				} else if (charCode == KeyCodes.KEY_DELETE
						&& textArea.getText().equals("")) {
					removePropositionAndMaybeParentArgument_DELETE_ME();
					event.preventDefault();
				} else if (charCode == KeyCodes.KEY_DELETE
						&& textArea.getCursorPos() == textArea.getText()
								.length() && textArea.getSelectionLength() == 0) {
					removeNextProposition_DELETE_ME();
					event.preventDefault();
				} else {
					getEditMode().editContentSaveTimer
							.setNodeForTimedSave(this);
				}
			} else {
				getEditMode().editContentSaveTimer.setNodeForTimedSave(this);
			}
		}
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		/*
		 * tell edit mode that there has been a user action so it doesn't
		 * throttle live updates (do it first in case the key press results in a
		 * deletion in which case the getEditMode() will return null)
		 */
		getEditMode().updateTimer.userAction();

		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == textArea) {
			/*
			 * if it's a root proposition add an argument when user presses
			 * enter
			 */
			if (charCode == KeyCodes.KEY_ENTER && parentArgView() == null) {
				onChange(null);
				addArgument(true);
				event.preventDefault();
			}
			/*
			 * if it is not a root proposition, add a sibling proposition when
			 * user presses enter
			 */
			else if (charCode == KeyCodes.KEY_ENTER && parentArgView() != null) {
				onChange(null);
				addProposition();
				event.preventDefault();
			} else if ((charCode == KeyCodes.KEY_BACKSPACE || charCode == KeyCodes.KEY_DELETE)
					&& handleKeyBackspaceOrDelete(charCode)) {
				event.preventDefault();
			} else {
				getEditMode().editContentSaveTimer.setNodeForTimedSave(this);
			}
		}
	}

	public void addPropositionCallback(ViewProp newPropView, Proposition result) {
		newPropView.setNode(result);
		newPropView.setLoaded(true);
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
		final ViewPropEdit newPropView = new ViewPropEdit();
		if (cursorPosition == 0) {
			parentArgView().insertItem(treePosition, newPropView);
			ServerComm.addProp(newPropView.proposition,
					parentArgView().argument, treePosition,
					new LocalCallback<Void>() {
						@Override
						public void call(Void t) {
							newPropView.setLoaded(true);
						}
					});
		} else {
			parentArgView().insertItem(treePosition + 1, newPropView);

			// then split the text between the current and new proposition
			content = textArea.getText();
			String firstContent = content.substring(0, cursorPosition);
			String secondContent = content.substring(cursorPosition);

			textArea.setText(firstContent);

			/*
			 * The line below commented out because it doesn't work with
			 * saveContentToServiceIfChanged()... the way that
			 * saveContentToServerIfChanged() knows to update the content if it
			 * has changed is because the content is different from what is in
			 * the proposition... this doesn't work if the proposition has been
			 * set to the same thing. saveContentToServerIfChanged() will update
			 * the proposition. (See also, ModeEdit.updateNode() for more code
			 * that relies on the behavoir of saveContentToServerIfChanged()
			 */
			// proposition.setContent(firstContent);

			newPropView.textArea.setText(secondContent);
			newPropView.proposition.setContent(secondContent);

			newPropView.textArea.setCursorPos(0);
			newPropView.textArea.setFocus(true);

			saveContentToServerIfChanged();

			ServerComm.addProp(newPropView.proposition,
					parentArgView().argument, treePosition + 1,
					new LocalCallback<Void>() {
						@Override
						public void call(Void t) {
							newPropView.setLoaded(true);
						}
					});
		}

	}

	public void onFocus(FocusEvent event) {
		Object source = event.getSource();
		if (source == textArea) {
			updateButtons();
			getEditMode().sideSearchTimer.setViewProp(this);

			if (!isLoaded() && hasID()) {
				/*
				 * have to check hasID() too because this might be a new node
				 * that does not yet have an ID. In that case we shouldn't try
				 * to load it--it will be loaded by the client in good time...
				 */
				getEditMode().loadFromServer(this, 2, 0);

			}

			if (!ratingView.hasBeenSet()) {
				ServerComm.getRating(getNodeID(), new LocalCallback<Integer>() {
					@Override
					public void call(Integer t) {
						if (t != null) {
							ratingView.setRating(t);
						}
					}
				});
			}

			if (proposition.linkCount > 1) {
				getEditModeTree().getEditMode().setSideBarText("links");
			} else {
				getEditModeTree().getEditMode().setSideBarText("propositions");
			}
		}

		/*
		 * tell edit mode that there has been a user action so it doesn't
		 * throttle live updates
		 */
		getEditMode().updateTimer.userAction();
	}

	@Override
	public void setRating(Long id, Map<Long, Integer> ratings) {
		Integer rating = ratings.get(id);
		if (rating != null) {
			ratingView.setRating(rating);
		}
	}

	private ModeEdit getEditMode() {
		return ((EditModeTree) getTree()).getEditMode();
	}

	private EditModeTree getEditModeTree() {
		return (EditModeTree) getTree();
	}

	private void updateButtons() {
		if (lastPropositionWithFocus != this
				&& lastPropositionWithFocus != null) {
			lastPropositionWithFocus.mainPanel
					.remove(lastPropositionWithFocus.buttonsPanel);
		}
		mainPanel.add(buttonsPanel);
		lastPropositionWithFocus = this;
	}

	@Override
	public void onChange(ChangeEvent event) {
		saveContentToServerIfChanged();
		if (proposition.linkCount > 1) {
			textArea.setReadOnly(true);
		}

	}

	public void saveContentToServerIfChanged() {
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
			getEditMode().sideSearchTimer.keyPress(charCode);
		}
	}

	@Override
	public ViewNode createChild() {
		return new ViewArgEdit();
	}

	@Override
	public void onMouseOut(MouseOutEvent event) {
		topPanel.remove(expandButton);
	}

	@Override
	public void onMouseOver(MouseOverEvent event) {

		if (!isLoaded() && hasID()) {
			topPanel.add(expandButton);
			expandButton.setVisible(true);
		}
	}

	@Override
	public void rate(Integer rating) {
		if (ArgMap.loggedIn()) {
			ServerComm.setRating(getNodeID(), rating);
		} else {
			ratingView.setRating(null);
			MessageDialog message = new MessageDialog(
					"Must be signed in to rate propositions",
					"In order to contribute your ratings of propositions you must be signed in to "
							+ "your user account.  Please sign in by clicking on the 'Sign in' link "
							+ "in the upper right hand corner and try again.  If you do not have a user "
							+ "account you can create one for free by clicking on the 'Sign in' link "
							+ "in the upper right hand corner.  <a target=\"_blank\" href=\"http://pages.coreason.org/ratings\">Read more.</a>");
			message.center();
			message.show();
		}
	}

	@Override
	public ViewPropEdit getPreceedingSibling() {
		return (ViewPropEdit) super.getPreceedingSibling();
	}

	@Override
	public ViewPropEdit getFollowingSibling() {
		return (ViewPropEdit) super.getFollowingSibling();
	}

	public boolean parentArgumentHasTitle() {
		ViewArg parent = (ViewArg) getParent();
		if (parent != null) {
			if (parent.getTextAreaContent().trim().equals("")) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	/*
	 * gets the node that preceeds the given node on the users screen; will only
	 * work reliably if the given node is itself visible (i.e. a descendant of
	 * only open nodes).
	 */
	private ViewNode getVisiblyPreceedingNode(ViewNode node) {
		ViewNode candidate = node.getPreceedingSibling();
		if (candidate != null) {
			while (candidate.isOpen() && candidate.getChildCount() > 0) {
				candidate = candidate.getChild(candidate.getChildCount() - 1);
			}
			return candidate;
		} else {
			return node.getParent();
		}
	}

	/*
	 * gets the node (not including children of the given node) that follows the
	 * given node on the users screen; will only work reliably if the given node
	 * is itself visible (i.e. a descendant of only open nodes).
	 */
	private ViewNode getVisiblyFollowingNode(ViewNode node) {
		ViewNode followingSibling = node.getFollowingSibling();
		if (followingSibling != null) {
			return followingSibling;
		} else if (node.getParent() != null) {
			return getVisiblyFollowingNode(node.getParent());
		} else {
			return null;
		}
	}

	private void refocusPreceeding(ViewNode node) {
		ViewNode preceeding = getVisiblyPreceedingNode(node);
		if (preceeding != null) {
			preceeding.haveFocus();
			preceeding.setTextAreaCursorPosition(preceeding
					.getTextAreaContent().length());
		} else {
			ViewNode following = getVisiblyFollowingNode(node);
			if (following != null) {
				following.haveFocus();
				following.setTextAreaCursorPosition(0);
			}
		}
	}

	private void refocusFollowing(ViewNode node) {
		ViewNode following = getVisiblyFollowingNode(node);
		if (following != null) {
			following.haveFocus();
			following.setTextAreaCursorPosition(0);
		} else {
			ViewNode preceeding = getVisiblyPreceedingNode(node);
			if (preceeding != null) {
				preceeding.haveFocus();
				preceeding.setTextAreaCursorPosition(preceeding
						.getTextAreaContent().length());
			}
		}
	}

	/*
	 * TODO: what is deleted = true used for?
	 */
	private boolean handleKeyBackspaceOrDelete(int charCode) {
		/* if the user has selected any text */
		if (textArea.getSelectionLength() != 0) {
			/* do normal key stroke */
			return false;
		}
		/*
		 * otherwise if the user has pressed backspace while cursor is in first
		 * position
		 */
		else if (charCode == KeyCodes.KEY_BACKSPACE
				&& textArea.getCursorPos() == 0) {
			ViewPropEdit preceedingSibling = getPreceedingSibling();
			if (preceedingSibling != null && !preceedingSibling.isLink()
					&& !isLink() && !isTopLevel()) {
				if (getChildCount() == 0) {
					/* merge With Previous Sibling */
					mergePropsAndDeleteOne(preceedingSibling, this, true);
				} else if (preceedingSibling.getChildCount() == 0) {
					/* merge Previous Sibling With This */
					mergePropsAndDeleteOne(preceedingSibling, this, false);
				} else {
					/* do nothing; ignore keystroke */
				}
			} else if (textArea.getText().equals("") && getChildCount() == 0
					&& !isLink()) {
				if (isTopLevel() || getParent().getChildCount() > 1
						|| parentArgumentHasTitle()) {
					/*
					 * can't set on parent argument (as opposed to parent node)
					 * because in case where there is a previous sibling that
					 * was a link that will cause a jump
					 */
					refocusPreceeding(this);
					remove();
					ServerComm.deleteProp(this.proposition);
				} else {
					ViewArg parent = parentArgView();
					refocusPreceeding(parent);
					remove();
					ServerComm.deleteProp(this.proposition);
					parent.remove();
					ServerComm.deleteArg(parent.argument);
				}
			} else {
				/* do nothing; ignore keystroke */
			}
			return true;
		}
		/*
		 * otherwise if the user has pressed delete while cursor is in last
		 * position
		 */
		else if (charCode == KeyCodes.KEY_DELETE
				&& textArea.getCursorPos() == textArea.getText().length()) {
			ViewPropEdit followingSibling = getFollowingSibling();
			if (followingSibling != null && !followingSibling.isLink()
					&& !isLink() && !isTopLevel()) {
				if (getChildCount() == 0) {
					/* merge With Subsequent Sibling */
					mergePropsAndDeleteOne(this, followingSibling, false);
				} else if (followingSibling.getChildCount() == 0) {
					/* merge Subsequent Sibling With This */
					mergePropsAndDeleteOne(this, followingSibling, true);
				} else {
					/* do nothing; ignore keystroke */
				}
			} else if (textArea.getText().equals("") && getChildCount() == 0
					&& !isLink()) {
				if (isTopLevel() || getParent().getChildCount() > 1
						|| parentArgumentHasTitle()) {
					refocusFollowing(this);
					// TODO set cursor position here, below and above
					remove();
					ServerComm.deleteProp(this.proposition);
				} else {
					ViewArg parent = parentArgView();
					refocusFollowing(parent);
					remove();
					ServerComm.deleteProp(this.proposition);
					parent.remove();
					ServerComm.deleteArg(parent.argument);
				}
			} else {
				/* do nothing; ignore keystroke */
			}
			return true;
		} else {
			/* do normal keystroke */
			return false;
		}
	}

	private void mergePropsAndDeleteOne(ViewPropEdit firstProp,
			ViewPropEdit secondProp, boolean keepFirst) {
		ViewPropEdit keepProp;
		ViewPropEdit deleteProp;
		if (keepFirst) {
			keepProp = firstProp;
			deleteProp = secondProp;
		} else {
			keepProp = secondProp;
			deleteProp = firstProp;
		}
		String firstText = firstProp.textArea.getText();
		keepProp.textArea.setText(firstText + secondProp.textArea.getText());
		keepProp.textArea.setCursorPos(firstText.length());
		keepProp.haveFocus();
		keepProp.saveContentToServerIfChanged();

		deleteProp.remove();
		ServerComm.deleteProp(deleteProp.proposition);
	}

	public void removeNextProposition_DELETE_ME() {
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

	public void removePropositionAndMaybeParentArgument_DELETE_ME() {
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
			if (!textArea.getText().equals(""))
				return;

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
			prePropView.saveContentToServerIfChanged();
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
}
