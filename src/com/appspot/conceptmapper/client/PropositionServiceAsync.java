package com.appspot.conceptmapper.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface PropositionServiceAsync {

	void getRootPropositions(AsyncCallback<Proposition[]> callback);

	void addRootProposition(Proposition proposition,
			AsyncCallback<Void> callback);

}
