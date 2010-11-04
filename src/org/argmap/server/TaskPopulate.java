package org.argmap.server;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

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

	private static final Random random = new Random();

	private static final String RANDOM_SENTENCE_FILE_POSITION = "RANDOM_SENTENCE_FILE_POSITION";
	private static final String RANDOM_SENTENCE_LOCK = "RANDOM_SENTENCE_LOCK";
	private static final String RANDOM_SENTENCE_COUNT = "RANDOM_SENTENCE_COUNT";

	private static final String SENTENCES_FILE = "sentences";

	/*
	 * The sentences file has about 80K sentences if I remember correctly, so to
	 * avoid exceeding that I should shoot to create fewer than 80K dummy nodes.
	 * These settings should generate roughly 50K nodes with a nice
	 * distribution of depth/propositions/arguments: ROOT_NODES = 100;
	 * AVERAGE_PROPS_AT_ROOT = 3; AVERAGE_ARGS_AT_ROOT = 2; PROPS_STEP = 1;
	 * ARGS_STEP = .75; ARGS_STANDARD_DEVIATION = 1; PROPS_STANDARD_DEVIATION =
	 * 3;
	 */
	private static final int ROOT_NODES = 20;
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

		MemcacheLock.lock(RANDOM_SENTENCE_LOCK);
		Long offset = (Long) cache.get(RANDOM_SENTENCE_FILE_POSITION);
		if (offset == null) {
			offset = new Long(0);
		}
		Integer count = (Integer) cache.get(RANDOM_SENTENCE_COUNT);
		if (count == null) {
			count = new Integer(0);
		}

		String result = null;
		try {
			RandomAccessFile file = new RandomAccessFile(SENTENCES_FILE, "r");
			file.seek(offset);
			result = file.readLine();
			offset = file.getFilePointer();
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		count = count + 1;
		cache.put(RANDOM_SENTENCE_COUNT, count);
		cache.put(RANDOM_SENTENCE_FILE_POSITION, offset);
		MemcacheLock.unlock(RANDOM_SENTENCE_LOCK);
		return result;
	}

	public static void queueRootTaskPopulates(HttpServletRequest req) {
		final HttpServletRequest request = req;
		ArgMapServiceImpl argMapService = new ArgMapServiceImpl() {
			@Override
			public HttpServletRequest getHttpServletRequest() {
				return request;
			}
		};
		for (int i = 0; i < ROOT_NODES; i++) {
			Long propID = argMapService.addProp(null, 0, getRandomSentence());
			queueTaskPopulate(propID, AVERAGE_PROPS_AT_ROOT,
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
		final HttpServletRequest request = req;
		ArgMapServiceImpl argMapService = new ArgMapServiceImpl() {
			@Override
			public HttpServletRequest getHttpServletRequest() {
				return request;
			}
		};

		resp.setContentType("text/plain");

		try {
			Long propID = Long.parseLong(req.getParameter(PARAM_PROPID));
			double averageProps = Double.parseDouble(req
					.getParameter(PARAM_AVERAGEPROPS));
			double averageArgs = Double.parseDouble(req
					.getParameter(PARAM_AVERAGEARGS));

			double argCount = random.nextGaussian() * ARGS_STANDARD_DEVIATION
					+ averageArgs;
			for (int i = 0; i < argCount; i++) {
				//create roughly 3/4 arguments pro and 1/4 con
				boolean pro = random.nextInt( 4 ) <= 2 ? true : false; 
				Long argID = argMapService.addArg(propID, pro);
				argMapService.updateArg(argID, getRandomSentence());
				double propCount = random.nextGaussian()
						* PROPS_STANDARD_DEVIATION + averageProps;
				propCount = propCount >= 1 ? propCount : 1;
				for (int j = 0; j < propCount; j++) {
					Long childPropID = argMapService.addProp(argID, 0,
							getRandomSentence());
					queueTaskPopulate(childPropID, averageProps - PROPS_STEP,
							averageArgs - ARGS_STEP);
				}
			}
		} catch (Exception ex) {
			String strCallResult = "FAIL: TaskPopulate: "
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

}
