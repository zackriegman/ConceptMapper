package org.argmap.client;



import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class ViewProp extends ViewNode {

	protected TextArea textArea = new TextAreaSloppyGrow();
	protected VerticalPanel mainPanel = new VerticalPanel();
	public Proposition proposition;

	public ViewProp(Proposition proposition) {
		super();
		setNode( proposition );
		mainPanel.add(textArea);
		this.setWidget(mainPanel);
	}
	
	public void setNodeLink( boolean link ){};
	
	@Override
	public void setNode( Node node ){
		proposition = (Proposition)node;
		setContent(proposition.getContent());
		if (proposition.linkCount <= 1) {
			textArea.setStylePrimaryName("propositionTextArea");
			setNodeLink( false );
		} else if (proposition.linkCount > 1) {
			textArea.setStylePrimaryName("linkedPropositionTextArea");
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
		public TextAreaSloppyGrow() {
			this(80);
		}

		@Override
		public void setText(String text) {
			super.setText(text);
			resize();
		}

		private void resize() {
			int widthInCharacters = getCharacterWidth();
			int length = getText().length();

			int lineEstimate = length / widthInCharacters;
			if (lineEstimate < 1) {
				lineEstimate = 1;
			}
			// ArgMap("onKeyPress: line estimate = " + lineEstimate
			// );
			setVisibleLines(lineEstimate);
		}

		public TextAreaSloppyGrow(int width) {
			super();

			// this.setCharacterWidth(width);
			this.setWidth("50em");
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
