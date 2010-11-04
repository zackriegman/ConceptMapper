package org.argmap.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.Node;
import org.argmap.client.Proposition;

import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.apphosting.api.DeadlineExceededException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

public class TaskWipe extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger log = Logger.getLogger(TaskWipe.class
			.getName());
	
	static {
		ObjectifyService.register(Node.class);
		ObjectifyService.register(Proposition.class);
		ObjectifyService.register(Argument.class);
		ObjectifyService.register(Change.class);
	}
	
	private final Objectify ofy = ObjectifyService.begin();

	
	private void deleteAll( Class deleteClass ){
		ofy.delete(ofy.query( deleteClass ).fetchKeys());
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			deleteAll(Argument.class);
			deleteAll(Proposition.class);
			deleteAll(Change.class);
		} catch( DeadlineExceededException e ){
			queueTaskWipe();
		}catch (Exception ex) {
			String strCallResult = "FAIL: TaskWipe: " + ex.getMessage();
			log.severe(strCallResult);
			resp.getWriter().println(strCallResult);
		}
	}
	
	public static void queueTaskWipe() {
		TaskOptions taskOptions = TaskOptions.Builder.url("/tasks/wipe");
		QueueFactory.getDefaultQueue().add( taskOptions );
	}
}
