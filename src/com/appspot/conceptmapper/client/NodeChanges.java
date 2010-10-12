package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NodeChanges<T> implements Serializable {
	/** to suppress warnings */
	private static final long serialVersionUID = 1L;
	public List<Change> changes = new ArrayList<Change>();
	public Date changesTo;
	public List<Long> deletedChildIDs = new ArrayList<Long>();
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append( "changes:[");
		for( Change change : changes ){
			buffer.append( change.toString() );
			buffer.append( "; ");
		}
		buffer.append( "]; deletedChildIDs:[" );
		for( Long id: deletedChildIDs){
			buffer.append( id );
			buffer.append( "; ");
		}
		buffer.append( "]");
		return buffer.toString();
	}
}
