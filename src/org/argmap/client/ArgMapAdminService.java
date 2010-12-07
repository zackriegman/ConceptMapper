package org.argmap.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("adminServ")
public interface ArgMapAdminService extends RemoteService {

	public void clearDatastore();

	public void populateDatastore() throws Exception;

	public int getPopulateDatastoreCount();

	public void emailFromToCurrentUser(String subject, String contentHTML,
			String contentPlain) throws ServiceException;

}
