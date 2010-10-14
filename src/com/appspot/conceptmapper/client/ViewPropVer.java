package com.appspot.conceptmapper.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewPropVer extends ViewProp {
	public List<ViewChange> viewChanges = new ArrayList<ViewChange>();
	public static ViewPropFactory<ViewPropVer> FACTORY = new ViewPropFactory<ViewPropVer>() {

		@Override
		public ViewPropVer create(Proposition prop) {
			return new ViewPropVer(prop);
		}
	};

	public Map<Long, ViewArgVer> deletedViews = new HashMap<Long, ViewArgVer>();

	public ViewPropVer(Proposition proposition) {
		super(proposition);
	}
	
	public ViewPropVer(){
		this( new Long(-1) );
	}

	public ViewPropVer( Long id ) {
		this(new Proposition(id));
		textArea.setReadOnly(true);
	}

	public static ViewPropVer cloneViewEdit(ViewPropEdit viewEdit) {
		ViewPropVer cloneView = new ViewPropVer(viewEdit.proposition);
		cloneView.textArea.setText(viewEdit.textArea.getText());
		cloneView.setState(viewEdit.getState());
		return cloneView;
	}

	public void removeAndSaveChildView(Long id) {
		ViewArgVer viewNode = (ViewArgVer) removeChildView(id);
		deletedViews.put(id, viewNode);
	}

	public void reviveDeletedView(Long id, int index) {
		insertChildViewAt(index, deletedViews.remove(id));
	}

	public ViewArgVer createDeletedView(Long id) {
		/*
		 * this view is just empty; can set to false, because real value will be
		 * set before it is used when the time machine goes back in time...
		 */
		Argument argument = new Argument();
		argument.id = id;
		ViewArgVer deletedView = new ViewArgVer(argument);
		deletedViews.put(id, deletedView );
		return deletedView;
	}
}
