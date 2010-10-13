package com.appspot.conceptmapper.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;

public class ViewArgEdit extends ViewArg implements ChangeHandler {
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
	}

	@Override
	public void onChange(ChangeEvent event) {
		String trimmedTextBoxContent = textBox.getText() == null ? "" : textBox
				.getText().trim();
		String trimmedArgumentTitle = argument.title == null ? ""
				: argument.title.trim();
		if (!trimmedArgumentTitle.equals(trimmedTextBoxContent)) {
			argument.title = trimmedTextBoxContent;
			ServerComm.updateArgument(argument);
		}
	}
}
