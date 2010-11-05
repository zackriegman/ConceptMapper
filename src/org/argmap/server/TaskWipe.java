package org.argmap.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.Node;
import org.argmap.client.Proposition;

import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
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
	private long startMillis;
	
	private boolean timeLeft(){
		return System.currentTimeMillis() - startMillis  < 20000 ? true : false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		LapTimer timer = new LapTimer();
		try {
			timer.lap("1");
			startMillis = System.currentTimeMillis();
			timer.lap("2");
			List<Class> classesForDelete = new ArrayList<Class>();
			timer.lap("3");
			classesForDelete.add(Argument.class);
			timer.lap("4");
			classesForDelete.add(Proposition.class);
			classesForDelete.add(Change.class);
			int i = 0;
			while( timeLeft() && i < classesForDelete.size() ){
				timer.lap("5 " + i);
				List<Key> keys = ofy.query( classesForDelete.get(i) ).limit(100).listKeys();
				timer.lap("5.1 " + i);
				while( timeLeft() && keys.size() > 0 ){
					timer.lap("6");
					ofy.delete(keys);
					timer.lap("6.1");
					keys = ofy.query( classesForDelete.get(i) ).limit(100).listKeys();
				}
				i++;
			}
			timer.lap("7");
			if( !timeLeft() ){
				queueTaskWipe();
			}
			log.fine(timer.getRecord());
		}catch (Exception ex) {
			timer.lap("8");
			String strCallResult = "FAIL: TaskWipe: " + ex.getMessage() + "\n" + timer.getRecord();
			log.severe(strCallResult);
			resp.getWriter().println(strCallResult);
		}
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}
	
	public static void queueTaskWipe() {
		TaskOptions taskOptions = TaskOptions.Builder.url("/tasks/wipe");
		QueueFactory.getDefaultQueue().add( taskOptions );
	}
}
