package org.argmap.client;

import java.util.Comparator;

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

	public static final Comparator<ViewChange> DATE_COMPARATOR = new Comparator<ViewChange>() {

		@Override
		public int compare(ViewChange first, ViewChange second) {
			return first.change.date.compareTo(second.change.date);
		}

	};
}