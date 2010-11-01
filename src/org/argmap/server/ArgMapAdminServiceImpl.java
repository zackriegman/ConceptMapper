package org.argmap.server;

import java.util.logging.Logger;

import org.argmap.client.ArgMapAdminService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ArgMapAdminServiceImpl extends RemoteServiceServlet implements
ArgMapAdminService{
	private static final Logger log = Logger.getLogger(ArgMapAdminServiceImpl.class
			.getName());

	/** add to suppress warnings... */
	private static final long serialVersionUID = 1L;

	@Override
	public void clearDatastore() {
		
		log.severe("METHOD CALLED");
		
	}

}
