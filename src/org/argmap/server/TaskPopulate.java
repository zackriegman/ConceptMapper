package org.argmap.server;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import org.argmap.client.Argument;
import org.argmap.client.Proposition;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;

@SuppressWarnings("serial")
public class TaskPopulate extends HttpServlet {

	private static final Logger log = Logger.getLogger(TaskPopulate.class
			.getName());

	private static Cache cache;

	private static void initializeMemcache() {
		if (cache == null) {
			Map<Object, Object> props = new HashMap<Object, Object>();
			props.put(GCacheFactory.EXPIRATION_DELTA, 3600);
			props.put(MemcacheService.SetPolicy.SET_ALWAYS, true);

			try {
				CacheFactory cacheFactory = CacheManager.getInstance()
						.getCacheFactory();
				cache = cacheFactory.createCache(props);
			} catch (CacheException e) {
				log.severe("failed to create memcache");
			}
		}
	}

	private static final Queue populateQueue = QueueFactory
			.getQueue("populate");

	private static ArgMapServiceForTask argMapService = new ArgMapServiceForTask();

	static {
		ObjectifyRegistrator.register();
	}

	private static class ArgMapServiceForTask extends ArgMapServiceImpl {
		private HttpServletRequest thisRequest;

		private void setCurrentRequest(HttpServletRequest request) {
			this.thisRequest = request;
		}

		@Override
		public HttpServletRequest getHttpServletRequest() {
			return thisRequest;
		}
	};

	private static final Random random = new Random();

	private static final String RANDOM_SENTENCE_FILE_POSITION = "RANDOM_SENTENCE_FILE_POSITION";
	private static final String RANDOM_SENTENCE_LOCK = "RANDOM_SENTENCE_LOCK";
	private static final String RANDOM_SENTENCE_COUNT = "RANDOM_SENTENCE_COUNT";
	private static final String RANDOM_SENTENCE_LIST = "RANDOM_SENTENCE_LIST";
	private static int RANDOM_SENTENCE_BUFFER_SIZE = 200;

	private static final String SENTENCES_FILE = "sentences";

	/*
	 * The sentences file has about 80K sentences if I remember correctly, so to
	 * avoid exceeding that I should shoot to create fewer than 80K dummy nodes.
	 * These settings should generate roughly 50K nodes with a nice distribution
	 * of depth/propositions/arguments: ROOT_NODES = 100; AVERAGE_PROPS_AT_ROOT
	 * = 3; AVERAGE_ARGS_AT_ROOT = 2; PROPS_STEP = 1; ARGS_STEP = .75;
	 * ARGS_STANDARD_DEVIATION = 1; PROPS_STANDARD_DEVIATION = 3;
	 */
	private static final int ROOT_NODES = 1;
	private static final int AVERAGE_PROPS_AT_ROOT = 3;
	private static final int AVERAGE_ARGS_AT_ROOT = 2;
	private static final int PROPS_STEP = 1;
	private static final double ARGS_STEP = .75;
	private static final int ARGS_STANDARD_DEVIATION = 1;
	private static final int PROPS_STANDARD_DEVIATION = 3;

	private static final String PARAM_PROPID = "PARAM_PROPID";
	private static final String PARAM_AVERAGEPROPS = "PARAM_AVERAGEPROPS";
	private static final String PARAM_AVERAGEARGS = "PARAM_AVERAGEARGS";

	public static int getRandomSentenceCount() {
		initializeMemcache();
		Integer returnValue = (Integer) cache.get(RANDOM_SENTENCE_COUNT);
		if (returnValue == null) {
			returnValue = new Integer(0);
		}
		return returnValue;
	}

	public static String getRandomSentence() {
		initializeMemcache();

		LapTimer timer = new LapTimer();
		Lock lock = Lock.getLock(RANDOM_SENTENCE_LOCK);
		try {
			timer.lap("++++");
			lock.lock();

			@SuppressWarnings("unchecked")
			List<String> sentenceList = (List<String>) cache
					.get(RANDOM_SENTENCE_LIST);
			if (sentenceList != null && sentenceList.size() > 0) {
				String returnSentence = sentenceList
						.remove(sentenceList.size() - 1);
				cache.put(RANDOM_SENTENCE_LIST, sentenceList);
				return returnSentence;
			} else {
				sentenceList = new ArrayList<String>(
						RANDOM_SENTENCE_BUFFER_SIZE);
				timer.lap("))))");

				// Long offset = (Long)
				// cache.get(RANDOM_SENTENCE_FILE_POSITION);
				Long offset = Property.getLong(RANDOM_SENTENCE_FILE_POSITION);
				if (offset == null) {
					offset = new Long(0);
				}
				timer.lap("****");
				Integer count = (Integer) cache.get(RANDOM_SENTENCE_COUNT);
				if (count == null) {
					count = new Integer(0);
				}

				String result = null;
				try {
					timer.lap("&&&&");
					RandomAccessFile file = new RandomAccessFile(
							SENTENCES_FILE, "r");
					timer.lap("^^^^");
					file.seek(offset);
					timer.lap("%%%%");
					for (int i = 0; i < RANDOM_SENTENCE_BUFFER_SIZE; i++) {
						sentenceList.add(file.readLine());
					}
					result = file.readLine();
					timer.lap("$$$$");
					offset = file.getFilePointer();
					timer.lap("####");
					file.close();
					timer.lap("@@@@");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				count = count + RANDOM_SENTENCE_BUFFER_SIZE;
				timer.lap("TTTT");
				cache.put(RANDOM_SENTENCE_COUNT, count);
				timer.lap("YYYY");
				// cache.put(RANDOM_SENTENCE_FILE_POSITION, offset);
				Property.put(RANDOM_SENTENCE_FILE_POSITION, offset);
				cache.put(RANDOM_SENTENCE_LIST, sentenceList);
				timer.lap("UUUU");
				return result;
			}
		} finally {
			log.fine(timer.getRecord());
			lock.unlock();
		}
	}

	public static void queueRootTaskPopulates(HttpServletRequest req) {
		argMapService.setCurrentRequest(req);
		for (int i = 0; i < ROOT_NODES; i++) {
			Proposition prop = argMapService.addProp(null, 0,
					getRandomSentence());
			queueTaskPopulate(prop.id, AVERAGE_PROPS_AT_ROOT,
					AVERAGE_ARGS_AT_ROOT);
		}
	}

	public static void queueTaskPopulate(Long propID, double averageProps,
			double averageArgs) {
		TaskOptions taskOptions = TaskOptions.Builder.url("/tasks/populate");
		taskOptions.param(PARAM_PROPID, "" + propID);
		taskOptions.param(PARAM_AVERAGEPROPS, "" + averageProps);
		taskOptions.param(PARAM_AVERAGEARGS, "" + averageArgs);
		populateQueue.add(taskOptions);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		LapTimer timer = new LapTimer();
		try {
			// log.info("AAAA");
			timer.lap("AAAAA");
			argMapService.setCurrentRequest(req);
			timer.lap("BBBB");

			resp.setContentType("text/plain");

			Long propID = Long.parseLong(req.getParameter(PARAM_PROPID));
			double averageProps = Double.parseDouble(req
					.getParameter(PARAM_AVERAGEPROPS));
			double averageArgs = Double.parseDouble(req
					.getParameter(PARAM_AVERAGEARGS));

			double argCount = random.nextGaussian() * ARGS_STANDARD_DEVIATION
					+ averageArgs;
			timer.lap("CCCC");

			for (int i = 0; i < argCount; i++) {
				timer.lap("DDDD " + i);
				// create roughly 3/4 arguments pro and 1/4 con
				boolean pro = random.nextInt(4) <= 2 ? true : false;
				timer.lap("0000 " + i);
				Argument arg = argMapService.addArg(propID, pro);
				timer.lap("1111 " + i);
				String randomSentence = getRandomSentence();
				timer.lap("2222 " + i);
				argMapService.updateArg(arg.id, randomSentence);
				timer.lap("3333 " + i);
				double propCount = random.nextGaussian()
						* PROPS_STANDARD_DEVIATION + averageProps;
				timer.lap("4444 " + i);
				propCount = propCount >= 1 ? propCount : 1;
				timer.lap("5555 " + i);
				for (int j = 0; j < propCount; j++) {
					timer.lap("EEEE " + j);
					Proposition childProp = argMapService.addProp(arg.id, 0,
							getRandomSentence());
					queueTaskPopulate(childProp.id, averageProps - PROPS_STEP,
							averageArgs - ARGS_STEP);
					timer.lap("FFFF " + j);
				}
				timer.lap("GGGG " + i);
			}
			timer.lap("CCCC");
			log.finest(timer.getRecord());
		} catch (Exception ex) {
			timer.lap("ZZZZ");
			String strCallResult = "FAIL: TaskPopulate: " + ex.getMessage()
					+ timer.getRecord();
			log.log(Level.SEVERE, "FAIL: TaskPopulate: ", ex);
			resp.getWriter().println(strCallResult);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

}
