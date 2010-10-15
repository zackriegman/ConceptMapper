package org.argmap.client;

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
		buffer.append( "\n  changes:");
		for( Change change : changes ){
			buffer.append("\n    ");
			buffer.append( change.toString() );
		}
		buffer.append( "\n  deletedChildIDs:" );
		for( Long id: deletedChildIDs){
			buffer.append("\n    ");
			buffer.append( id );
		}
		return buffer.toString();
	}
}
