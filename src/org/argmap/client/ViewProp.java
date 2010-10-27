package org.argmap.client;



import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class ViewProp extends ViewNode {
	
	private static final int PROP_WIDTH_NUMBER = 47;
	private static final String PROP_WIDTH = "" + PROP_WIDTH_NUMBER + "em";

	protected final TextArea textArea = new TextAreaSloppyGrow();
	//protected TextArea textArea = new TextArea();
	protected VerticalPanel mainPanel = new VerticalPanel();
	protected HorizontalPanel topPanel = new HorizontalPanel();
	protected FocusPanel focusPanel;
	public Proposition proposition;

	public ViewProp(Proposition proposition) {
		super();
		setNode( proposition );
		mainPanel.add(topPanel);
		topPanel.add(textArea);
		focusPanel = new FocusPanel( mainPanel );
		this.setWidget(focusPanel);
	}
	
	public void setNodeLink( boolean link ){};
	
	@Override
	public void setNode( Node node ){
		proposition = (Proposition)node;
		setContent(proposition.getContent());
		if (proposition.linkCount <= 1) {
			textArea.addStyleName("propositionTextArea");
			setNodeLink( false );
		} else if (proposition.linkCount > 1) {
			textArea.addStyleName("linkedPropositionTextArea");
			setNodeLink( true );
		}
	}
	
	@Override
	public Proposition getNode(){
		return proposition;
	}

	@Override
	public String toString() {
		return "textArea:" + textArea.getText() + "; prop:" + proposition;
	}

	@Override
	public Long getNodeID() {
		return proposition.id;
	}

	public Argument parentArgument() {
		if (parentArgView() != null) {
			return parentArgView().argument;
		} else {
			return null;
		}
	}

	public ViewArgEdit parentArgView() {
		if (!isTopLevel())
			return ((ViewArgEdit) this.getParentItem());
		else
			return null;
	}
	
	@Override
	public ViewNode createViewNodeVerClone() {
		ViewPropVer cloneView = new ViewPropVer(proposition);
		cloneView.textArea.setText(textArea.getText());
		cloneView.setOpen(getModifiedState());
		cloneView.setLoaded(getModifiedState());
		return cloneView;
	}

	public boolean isTopLevel() {
		return getParentItem() == null;
		// return getTree().equals( getParentItem() );
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

	public ViewArg getArgView(int index) {
		return (ViewArg) getChild(index);
	}

	private static class TextAreaSloppyGrow extends TextArea {

		@Override
		public void setText(String text) {
			super.setText(text);
			resize();
		}

		private void resize() {
			double widthInCharacters = PROP_WIDTH_NUMBER * 1.7;
			double length = getText().length();

			int lineEstimate = (int) (length / widthInCharacters);
			if (lineEstimate < 1) {
				lineEstimate = 1;
			}
			// ArgMap("onKeyPress: line estimate = " + lineEstimate
			// );
			setVisibleLines(lineEstimate);
//			GWT.log("line estimate:"+lineEstimate);
//			GWT.log("length:" + length);
//			GWT.log("widthInCharacters:" + widthInCharacters);
			//setVisibleLines(10);
		}

		public TextAreaSloppyGrow() {
			super();

			// this.setCharacterWidth(width);
			this.setWidth(PROP_WIDTH);
			setVisibleLines(1);

			this.addKeyUpHandler(new KeyUpHandler() {
				public void onKeyUp(KeyUpEvent event) {
					resize();
				}
			});
		}
	}
	
	@Override
	public Node getChildNodeFromNodeList( Long nodeID, Nodes nodes ){
		return nodes.args.get( nodeID );
	}
}
