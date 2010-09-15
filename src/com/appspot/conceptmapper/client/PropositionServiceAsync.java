package com.appspot.conceptmapper.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface PropositionServiceAsync {

	void getRootPropositions(AsyncCallback<Proposition[]> callback);

	void addRootProposition(Proposition proposition,
			AsyncCallback<Void> callback);

	void deleteProposition(Proposition proposition, AsyncCallback<Void> callback);

	void makePropChanges(Proposition[] newProps, Proposition[] changedProps,
			Proposition[] deletedProps, Argument[] newArgs,
			Argument[] deletedArgs, AsyncCallback<Void> callback);

	void getAllProps(AsyncCallback<Proposition[]> callback);

}
