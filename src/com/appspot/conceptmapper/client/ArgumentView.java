package com.appspot.conceptmapper.client;

import com.google.gwt.user.client.ui.TreeItem;

public class ArgumentView extends TreeItem {
	public Argument argument;
	
	public ArgumentView( Argument arg ){
		super();
		setLabel( arg.pro );
		
		argument = arg;
	}
	
	public ArgumentView( boolean pro ){
		super();
		setLabel( pro );
		argument = new Argument();
	}
	
	private void setLabel( boolean pro ){
		if( pro ){
			setText("Argument For");
		}
		else{
			setText("Argument Against");
		}
	}

}
