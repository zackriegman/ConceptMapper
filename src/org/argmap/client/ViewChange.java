package org.argmap.client;

public class ViewChange {
	Change change;
	ViewNodeVer viewNode;
	boolean hidden = false;
	
	public String toString(){
		return "change: " + change + "; viewNode:" + viewNode + "; hidden:" + hidden;
	}
}