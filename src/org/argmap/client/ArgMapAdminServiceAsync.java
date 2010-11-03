package org.argmap.client;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface ArgMapAdminServiceAsync {

	void clearDatastore(AsyncCallback<Void> callback);

	void populateDatastore(AsyncCallback<Void> callback);

	void getPopulateDatastoreCount(AsyncCallback<Integer> callback);

}
