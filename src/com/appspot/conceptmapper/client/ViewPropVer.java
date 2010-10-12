package com.appspot.conceptmapper.client;

import java.util.HashMap;
import java.util.Map;

public class ViewPropVer extends ViewProp {
	
	public Map<Long, ViewArgVer> deletedViews = new HashMap<Long, ViewArgVer>();

	public ViewPropVer( Proposition proposition ){
		super( proposition );
	}
	
	public ViewPropVer(){
		this( new Proposition() );
		textArea.setReadOnly(true);
	}
	
	public static ViewPropVer cloneViewEdit( ViewPropEdit viewEdit) {
		ViewPropVer cloneView = new ViewPropVer(viewEdit.proposition);
		cloneView.textArea.setText(viewEdit.textArea.getText());
		cloneView.setState(viewEdit.getState());
		return cloneView;
	}
	
	public void removeAndSaveChildView(Long id){
		ViewArgVer viewNode = (ViewArgVer)removeChildView(id);
		deletedViews.put(id, viewNode);
	}
}
