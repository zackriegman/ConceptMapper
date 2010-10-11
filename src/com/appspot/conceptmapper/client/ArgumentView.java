package com.appspot.conceptmapper.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TreeItem;

public class ArgumentView extends TreeItem implements ChangeHandler {
	public Argument argument;
	private Label label;
	private TextBox textBox;

	public ArgumentView(Argument arg) {
		super();
		argument = arg;
		initialize();
	}

	public ArgumentView(boolean pro) {
		super();
		argument = new Argument();
		argument.pro = pro;
		initialize();
	}

	private void initialize() {
		label = new Label();
		textBox = new TextBox();
		textBox.setVisibleLength(Argument.MAX_LENGTH);
		textBox.setMaxLength(Argument.MAX_LENGTH);
		textBox.setStylePrimaryName("argumentTextBox");
		textBox.addChangeHandler(this);
		textBox.setText(argument.title);
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.add(label);
		horizontalPanel.add(textBox);
		setWidget(horizontalPanel);
		if (argument.pro) {
			label.setText("Argument For: ");
		} else {
			label.setText("Argument Against: ");
		}
	}

	public ArgumentView createClone() {
		ArgumentView argView = new ArgumentView(new Argument(argument));
		argView.setState(getState());
		return argView;

	}
	
	public void setArgTitle( String title ){
		textBox.setText( title );
	}
	
	public String getArgTitle(){
		return textBox.getText();
	}

	public String toString() {
		return "text:" + getText() + "; id:" + argument.id;
	}

	public PropositionView getPropView(int index) {
		return (PropositionView) getChild(index);
	}

	public void insertPropositionViewAt(int index, PropositionView propView) {
		/*
		 * can't figure out how to insert an item at a specific point (instead
		 * items just get inserted as the last of the current TreeItem's
		 * children). So, instead, I'm removing all subsequent TreeItem
		 * children, then adding the new TreeItem (the new proposition) and then
		 * adding back all the subsequent tree items!
		 */

		// first remove all subsequent children
		Queue<TreeItem> removeQueue = new LinkedList<TreeItem>();
		TreeItem currentItem;
		while ((currentItem = getChild(index)) != null) {
			removeQueue.add(currentItem);
			removeItem(currentItem);
		}

		// then add the new one
		addItem(propView);

		// then add back the rest
		while (!removeQueue.isEmpty()) {
			TreeItem toRemove = removeQueue.poll();
			addItem(toRemove);
		}
	}

	public void printArgRecursive(int level) {
		GWT.log(ConceptMapper.spaces(level * 2) + getText() + "; id:"
				+ argument.id);
		for (int j = 0; j < getChildCount(); j++) {
			getPropView(j).printPropRecursive(level + 1);
		}
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
