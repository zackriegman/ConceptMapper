package com.appspot.conceptmapper.client;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TreeItem;

public class ArgumentView extends TreeItem {
	public Argument argument;

	public ArgumentView createClone() {
		ArgumentView argView = new ArgumentView(new Argument(argument));
		argView.setState(getState());
		return argView;

	}

	public String toString() {
		return "text:" + getText() + "; id:" + argument.id;
	}

	public ArgumentView(Argument arg) {
		super();
		setLabel(arg.pro);

		argument = arg;
	}

	public ArgumentView(boolean pro) {
		super();
		setLabel(pro);
		argument = new Argument();
		argument.pro = pro;
	}

	private void setLabel(boolean pro) {
		if (pro) {
			setText("Argument For");
		} else {
			setText("Argument Against");
		}
	}
	
	public PropositionView getPropView( int index ){
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
	

	
	public void printArgRecursive( int level ){
		GWT.log(ConceptMapper.spaces(level * 2) + getText() + "; id:"
				+ argument.id);
		for (int j = 0; j < getChildCount(); j++) {
			getPropView(j).printPropRecursive( level + 1);
		}
	}

}
