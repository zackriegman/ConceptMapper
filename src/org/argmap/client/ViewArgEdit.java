package org.argmap.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;

public class ViewArgEdit extends ViewArg implements ChangeHandler,
		KeyDownHandler {
	public static ViewArgFactory<ViewArgEdit> FACTORY = new ViewArgFactory<ViewArgEdit>() {
		@Override
		public ViewArgEdit create(Argument arg) {
			return new ViewArgEdit(arg);
		}
	};

	public ViewArgEdit(Argument arg) {
		super(arg);
		initialize();
	}

	public ViewArgEdit(boolean pro) {
		super(pro);
		initialize();
	}

	public void initialize() {
		textBox.addChangeHandler(this);
		textBox.addKeyDownHandler(this);
	}

	@Override
	public void onChange(ChangeEvent event) {
		try {
			String trimmedTextBoxContent = textBox.getText() == null ? ""
					: textBox.getText().trim();
			String trimmedArgumentTitle = argument.content == null ? ""
					: argument.content.trim();
			if (!trimmedArgumentTitle.equals(trimmedTextBoxContent)) {
				argument.content = trimmedTextBoxContent;
				ServerComm.updateArgument(argument);
			}
		} catch (Exception e) {
			ServerComm.handleClientException(e);
		}
	}

	public void haveFocus() {
		textBox.setFocus(true);
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		try {
			int charCode = event.getNativeKeyCode();
			Object source = event.getSource();
			if (source == textBox) {
				if ((charCode == KeyCodes.KEY_BACKSPACE || charCode == KeyCodes.KEY_DELETE)
						&& textBox.getText().equals("")) {
					int indexOfThis = getParentItem().getChildIndex(this);
					if (indexOfThis == 0) {
						((ViewPropEdit) getParentItem()).haveFocus();
					} else {
						((ViewArgEdit) getParentItem()
								.getChild(indexOfThis - 1)).haveFocus();
					}
					remove();
					ServerComm.deleteArgument(argument);

					event.preventDefault();
				}
			}
		} catch (Exception e) {
			ServerComm.handleClientException(e);
		}
	}
}
