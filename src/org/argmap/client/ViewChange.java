package org.argmap.client;

public class ViewChange {
	Change change;
	ViewNodeVer viewNode;
	boolean hidden = false;
	boolean alwaysHidden = false;

	@Override
	public String toString() {
		return "change: " + change + "; viewNode:" + viewNode + "; hidden:"
				+ hidden + "; alwaysHidden:" + alwaysHidden;
	}
}