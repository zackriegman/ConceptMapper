package org.argmap.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewArgVer extends ViewArg implements ViewNodeVer {
	public List<ViewChange> viewChanges = new ArrayList<ViewChange>();

	public static ViewArgFactory<ViewArgVer> FACTORY = new ViewArgFactory<ViewArgVer>() {
		@Override
		public ViewArgVer create(Argument arg) {
			return new ViewArgVer(arg);
		}
	};

	public Map<Long, ViewNodeVer> deletedViews = new HashMap<Long, ViewNodeVer>();

	public ViewArgVer(boolean pro) {
		super(pro);
		textBox.setReadOnly(true);
	}

	public ViewArgVer(Argument arg) {
		super(arg);
		textBox.setReadOnly(true);
	}

	public void removeAndSaveChildView(Long id) {
		ViewPropVer viewNode = (ViewPropVer) removeChildView(id);
		deletedViews.put(id, viewNode);
	}

	public void reviveDeletedView(Long id, int index) {
		ViewNode viewNode = (ViewNode) deletedViews.remove(id);
		assert viewNode != null ;
		insertChildViewAt(index, viewNode);
	}

	public ViewPropVer createDeletedView(Long id) {
		/*
		 * this view is just empty; can set to false, because real value will be
		 * set before it is used when the time machine goes back in time...
		 */
		ViewPropVer deletedView = new ViewPropVer(id);
		deletedViews.put(id, deletedView);
		return deletedView;
	}

	public List<ViewChange> getViewChangeList() {
		return viewChanges;
	}

	public ViewNodeVer getChildViewNodeVer(int i) {
		return (ViewNodeVer) getChild(i);
	}
	
	public Collection<ViewNodeVer> getDeletedViewList(){
		return deletedViews.values();
	}
}
