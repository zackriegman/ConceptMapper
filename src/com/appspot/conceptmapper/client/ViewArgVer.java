package com.appspot.conceptmapper.client;

import java.util.HashMap;
import java.util.Map;

public class ViewArgVer extends ViewArg {
	
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
}
