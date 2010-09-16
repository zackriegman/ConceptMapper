package com.appspot.conceptmapper.client;


import com.google.gwt.user.client.rpc.AsyncCallback;

public interface PropositionServiceAsync {

	void getRootPropositions(AsyncCallback<Proposition[]> callback);

	void makePropChanges(Proposition[] newProps, Proposition[] changedProps,
			Proposition[] deletedProps, Argument[] newArgs,
			Argument[] deletedArgs, AsyncCallback<Void> callback);

	void getAllProps(AsyncCallback<Proposition[]> callback);


	void test(AsyncCallback<Void> callback);

	

	void removeProposition(Long propID, AsyncCallback<Void> callback);


	void addProposition(Long parentArgID, int position,
			AsyncCallback<Long> callback);

	void addArgument(Long parentPropID, boolean pro,
			AsyncCallback<Argument> callback);

	void updateProposition(Long propID, String content,
			AsyncCallback<Void> callback);


}
