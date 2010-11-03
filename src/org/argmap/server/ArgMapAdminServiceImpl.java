package org.argmap.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.argmap.client.ArgMapAdminService;
import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.Node;
import org.argmap.client.Proposition;
import org.argmap.client.ServiceException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

public class ArgMapAdminServiceImpl extends RemoteServiceServlet implements
		ArgMapAdminService {
	private static final Logger log = Logger
			.getLogger(ArgMapAdminServiceImpl.class.getName());

	static {
		ObjectifyService.register(Node.class);
		ObjectifyService.register(Proposition.class);
		ObjectifyService.register(Argument.class);
		ObjectifyService.register(Change.class);
	}

	private final Objectify ofy = ObjectifyService.begin();

	/** add to suppress warnings... */
	private static final long serialVersionUID = 1L;

	@Override
	public void clearDatastore() {
		ofy.delete(ofy.query(Argument.class).fetchKeys());
		ofy.delete(ofy.query(Proposition.class).fetchKeys());
		ofy.delete(ofy.query(Change.class).fetchKeys());
	}

	@Override
	public void populateDatastore() throws ServiceException {
		System.out.print("ArgMapAdminServiceImpl.populateDatastore()");
		try {
			TaskPopulate.queueRootTaskPopulates(getThreadLocalRequest());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int getPopulateDatastoreCount(){
		return TaskPopulate.getRandomSentenceCount();
	}
	

	@Override
	public void doUnexpectedFailure(java.lang.Throwable e) {
		log.log(Level.SEVERE, "Uncaught exception", e);
	}
}
