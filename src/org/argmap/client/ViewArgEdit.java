package org.argmap.client;

import org.argmap.client.ModeEdit.EditModeTree;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.Button;

public class ViewArgEdit extends ViewArg implements ChangeHandler,
		KeyDownHandler, MouseOverHandler, MouseOutHandler, ClickHandler,
		FocusHandler {

	private final Button expandButton = new Button("+");

	public ViewArgEdit() {
		super();
		initialize();
	}

	public ViewArgEdit(Argument arg) {
		super(arg);
		initialize();
	}

	public ViewArgEdit(boolean pro) {
		super(pro);
		initialize();
	}

	private final void initialize() {
		textBox.addChangeHandler(this);
		textBox.addKeyDownHandler(this);
		textBox.addFocusHandler(this);
		focusPanel.addMouseOutHandler(this);
		focusPanel.addMouseOverHandler(this);
		expandButton.setStylePrimaryName("expandButton");
		// expandButton.setStylePrimaryName("button");
		expandButton.addClickHandler(this);
	}

	@Override
	public void onChange(ChangeEvent event) {

		String trimmedTextBoxContent = textBox.getText() == null ? "" : textBox
				.getText().trim();
		String trimmedArgumentTitle = argument.content == null ? ""
				: argument.content.trim();
		if (!trimmedArgumentTitle.equals(trimmedTextBoxContent)) {
			argument.content = trimmedTextBoxContent;
			ServerComm.updateArg(argument);
		}

	}

	public void haveFocus() {
		textBox.setFocus(true);
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {

		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == textBox) {
			if ((charCode == KeyCodes.KEY_BACKSPACE || charCode == KeyCodes.KEY_DELETE)
					&& textBox.getText().equals("") && getChildCount() == 0) {
				int indexOfThis = getParentItem().getChildIndex(this);
				if (indexOfThis == 0) {
					((ViewPropEdit) getParentItem()).haveFocus();
				} else {
					((ViewArgEdit) getParentItem().getChild(indexOfThis - 1))
							.haveFocus();
				}
				remove();
				ServerComm.deleteArg(argument);

				event.preventDefault();
			} else if (charCode == KeyCodes.KEY_ENTER
					&& textBox.getCursorPos() == textBox.getText().length()) {
				final ViewPropEdit newPropView = new ViewPropEdit();
				insertItem(0, newPropView);
				newPropView.haveFocus();
				setOpen(true);
				newPropView.setOpen(true);
				getEditModeTree().resetState();
				ServerComm.addProp(newPropView.proposition, argument, 0,
						new LocalCallback<Void>() {
							@Override
							public void call(Void t) {
								newPropView.setLoaded(true);
							}
						});
				// ServerComm.addProp(newPropView.proposition, argument, 0, new
				// LocalCallback<Proposition>() {
				// @Override
				// public void call(Proposition result) {
				// newPropView.addPropositionCallback(newPropView, result);
				// }
				// });
				event.preventDefault();

			}
		}

	}

	@Override
	public ViewNode createChild() {
		return new ViewPropEdit();
	}

	@Override
	public void onMouseOut(MouseOutEvent event) {
		horizontalPanel.remove(expandButton);
	}

	@Override
	public void onMouseOver(MouseOverEvent event) {
		if (!isLoaded() && hasID()) {
			horizontalPanel.add(expandButton);
		}
	}

	private EditModeTree getEditModeTree() {
		return (EditModeTree) getTree();
	}

	@Override
	public void onClick(ClickEvent event) {
		expandButton.setVisible(false);
		getEditModeTree().getEditMode().loadFromServer(this, 10, 10);
		setOpen(true);
		getEditModeTree().resetState();

	}

	@Override
	public void onFocus(FocusEvent event) {
		getEditModeTree().getEditMode().hideSearchBox();
	}
}
