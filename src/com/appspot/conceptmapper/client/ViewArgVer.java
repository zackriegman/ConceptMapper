package com.appspot.conceptmapper.client;

import java.util.HashMap;
import java.util.Map;


public class ViewArgVer extends ViewArg {
	public static ViewArgFactory<ViewArgVer> FACTORY = new ViewArgFactory<ViewArgVer>() {
		@Override
		public ViewArgVer create(Argument arg) {
			return new ViewArgVer(arg);
		}
	};
	
	public Map<Long, ViewPropVer> deletedViews = new HashMap<Long, ViewPropVer>();

	public ViewArgVer( boolean pro ){
		super( pro );
	}
	
	public ViewArgVer( Argument arg ){
		super( arg );
	}
	
	public void removeAndSaveChildView(Long id){
		ViewPropVer viewNode = (ViewPropVer)removeChildView(id);
		deletedViews.put(id, viewNode);
	}
	
	public void reviveDeletedView( Long id, int index ){
		insertChildViewAt(index, deletedViews.remove(id));
	}
}
