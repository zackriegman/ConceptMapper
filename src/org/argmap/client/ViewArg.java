package org.argmap.client;


import org.argmap.client.ViewProp.ViewPropFactory;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class ViewArg extends ViewNode {
	public Argument argument;
	protected Label label;
	protected TextBox textBox;
	HorizontalPanel horizontalPanel;

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

	public ViewArg() {
		initialize();
	}
	
	@Override
	public ViewNode createViewNodeVerClone() {
		ViewArgVer argView = new ViewArgVer(new Argument(argument));
		argView.setOpen(getModifiedState());
		argView.setLoaded(getModifiedState());
		return argView;
	}

	private void initialize() {
		label = new Label();
		textBox = new TextBox();
		textBox.setVisibleLength(Argument.MAX_LENGTH);
		textBox.setMaxLength(Argument.MAX_LENGTH);
		textBox.setText(argument.content);
		horizontalPanel = new HorizontalPanel();
		horizontalPanel.setWidth("51.5em");
		horizontalPanel.add(label);
		horizontalPanel.add(textBox);
		setWidget(horizontalPanel);
		setPro( argument.pro );
	}
	
	public void setPro( boolean pro ){
		if( argument != null ){
			argument.pro = pro;
		}
		if (pro) {
			label.setText("Argument For: ");
			horizontalPanel.setStylePrimaryName("proArg");
			textBox.setStylePrimaryName("proArgTextBox");
		} else {
			label.setText("Argument Against: ");
			horizontalPanel.setStylePrimaryName("conArg");
			textBox.setStylePrimaryName("conArgTextBox");
		}
	}

	public Long getNodeID() {
		return argument.id;
	}

	public void setArgTitle(String title) {
		textBox.setText(title);
	}

	public String getArgTitle() {
		return textBox.getText();
	}

	public String toString() {
		return "text:" + getText() + "; arg:" + argument;
	}

	public ViewProp getPropView(int index) {
		return (ViewProp) getChild(index);
	}

	/*
	public void printArgRecursive(int level) {
		GWT.log(ArgMap.spaces(level * 2) + getText() + "; id:"
				+ argument.id);
		for (int j = 0; j < getChildCount(); j++) {
			getPropView(j).printNodeRecursive(level + 1);
		}
	}
	*/

	/* DELETE ME: replaced by same method in ViewNode 
	public void insertPropositionViewAt(int index, ViewProp propView) {
		

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
*/

	public interface ViewArgFactory<A extends ViewArg> {
		public A create(Argument arg);
	}

	public static <P extends ViewProp, A extends ViewArg> A recursiveBuildArgumentView(
			Argument arg, Nodes nodes, ViewPropFactory<P> viewPropFactory,
			ViewArgFactory<A> viewArgFactory) {

		EditMode.log("<br/>" + arg.toString());
		A argView = viewArgFactory.create(arg);
	
		for (Long propID : arg.childIDs) {
			Proposition proposition = nodes.props.get(propID);
			assert proposition != null : "childID [" + propID + "] passed with corresponding child node";
			argView.addItem(ViewProp.recursiveBuildPropositionView(proposition,
					nodes,  viewPropFactory,
					viewArgFactory));
		}
		return argView;
	}

}
