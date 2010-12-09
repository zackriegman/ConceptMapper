package org.argmap.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

public class TaskSessionCleanup extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/*
	 * expire session data after 24 hours
	 */
	private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

	private static final Logger log = Logger.getLogger(TaskSessionCleanup.class
			.getName());

	static {
		ObjectifyRegistrator.register();
	}

	private final Objectify ofy = ObjectifyService.begin();
	private long startMillis;

	private boolean timeLeft() {
		return System.currentTimeMillis() - startMillis < 20000 ? true : false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			startMillis = System.currentTimeMillis();
			long expirationCutoff = System.currentTimeMillis()
					- EXPIRE_AFTER_TIME;

			int i = 0;
			for (Key key : ofy.query(Search.class)
					.filter("created <", expirationCutoff).fetchKeys()) {
				if (timeLeft()) {
					ofy.delete(key);
				} else {
					queueTaskSessionCleanup();
					break;
				}

			}

			resp.getWriter().println(
					"success: startMillis: " + startMillis
							+ "; expirationCutoff: " + expirationCutoff);
		} catch (Exception ex) {
			String strCallResult = "FAIL: TaskSessionCleanup: "
					+ ex.getMessage();
			log.severe(strCallResult);
			resp.getWriter().println(strCallResult);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	public static void queueTaskSessionCleanup() {
		TaskOptions taskOptions = TaskOptions.Builder
				.url("/tasks/sessionCleanup");
		QueueFactory.getQueue("wipe").add(taskOptions);
	}
}
