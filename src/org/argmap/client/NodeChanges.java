package org.argmap.client;

import java.io.Serializable;
import java.util.ArrayList;

public class NodeChanges implements Serializable {
	/** to suppress warnings */
	private static final long serialVersionUID = 1L;
	public ArrayList<Change> changes = new ArrayList<Change>();
	/* public Date changesTo; */
	public ArrayList<Long> deletedChildIDs = new ArrayList<Long>();

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("\n  changes:");
		for (Change change : changes) {
			buffer.append("\n    ");
			buffer.append(change.toString());
		}
		buffer.append("\n  deletedChildIDs:");
		for (Long id : deletedChildIDs) {
			buffer.append("\n    ");
			buffer.append(id);
		}
		return buffer.toString();
	}
}
