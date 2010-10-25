package org.argmap.client;



import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public abstract class ViewArg extends ViewNode {
	public Argument argument;
	protected Label label;
	protected TextBox textBox;
	HorizontalPanel horizontalPanel;

	public ViewArg(Argument arg) {
		super();
		initialize();
		setNode( arg );
	}

	public ViewArg(boolean pro) {
		super();
		initialize();
		Argument argument = new Argument();
		argument.pro = pro;
		setNode( argument );
	}

	public ViewArg() {
		super();
		initialize();
	}
	
	@Override
	public void setNode( Node node ){
		argument = (Argument) node;
		textBox.setText(argument.content);
		setPro (argument.pro);
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
		horizontalPanel = new HorizontalPanel();
		horizontalPanel.setWidth("51.5em");
		horizontalPanel.add(label);
		horizontalPanel.add(textBox);
		setWidget(horizontalPanel);
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

	@Override
	public Long getNodeID() {
		return argument.id;
	}

	public void setArgTitle(String title) {
		textBox.setText(title);
	}

	public String getArgTitle() {
		return textBox.getText();
	}

	@Override
	public String toString() {
		return "text:" + getText() + "; arg:" + argument;
	}

	/*
	public ViewProp getPropView(int index) {
		return (ViewProp) getChild(index);
	}
	*/
	
	@Override
	public Node getChildNodeFromNodeList( Long nodeID, Nodes nodes ){
		return nodes.props.get( nodeID );
	}

}
