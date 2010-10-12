package com.appspot.conceptmapper.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TreeItem;

public class ViewArg extends ViewNode {
	public Argument argument;
	protected Label label;
	protected TextBox textBox;
	
	public ViewArg(Argument arg) {
		super();
		argument = arg;
		initialize();
	}

	public ViewArg(boolean pro) {
		super();
		argument = new Argument();
		argument.pro = pro;
		initialize();
	}
	
	public ViewArg(){
		initialize();
	}
	
	public ViewArgVer createClone() {
		ViewArgVer argView = new ViewArgVer(new Argument(argument));
		argView.setState(getState());
		return argView;

	}
	
	private void initialize() {
		label = new Label();
		textBox = new TextBox();
		textBox.setVisibleLength(Argument.MAX_LENGTH);
		textBox.setMaxLength(Argument.MAX_LENGTH);
		textBox.setStylePrimaryName("argumentTextBox");
		textBox.setText(argument.title);
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.setStylePrimaryName("argumentPanel");
		horizontalPanel.setWidth("51.5em");
		horizontalPanel.add(label);
		horizontalPanel.add(textBox);
		setWidget(horizontalPanel);
		if (argument.pro) {
			label.setText("Argument For: ");
		} else {
			label.setText("Argument Against: ");
		}
	}
	
	public Long getNodeID(){
		return argument.id;
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

	public ViewPropEdit getPropView(int index) {
		return (ViewPropEdit) getChild(index);
	}
	
	public void printArgRecursive(int level) {
		GWT.log(ConceptMapper.spaces(level * 2) + getText() + "; id:"
				+ argument.id);
		for (int j = 0; j < getChildCount(); j++) {
			getPropView(j).printPropRecursive(level + 1);
		}
	}
	
	public void insertPropositionViewAt(int index, ViewProp propView) {
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

}
