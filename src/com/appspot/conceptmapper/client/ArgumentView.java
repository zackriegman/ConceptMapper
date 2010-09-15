package com.appspot.conceptmapper.client;

import com.google.gwt.user.client.ui.TreeItem;

public class ArgumentView extends TreeItem {
	public Argument argument;
	
	public ArgumentView( Argument arg ){
		argument = arg;
	}
	
	public ArgumentView( String string ){
		super( string );
	}

}
