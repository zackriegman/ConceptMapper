package org.argmap.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.argmap.client.ArgMapAdminService;
import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.Node;
import org.argmap.client.Proposition;

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

	static final int ROOT_NODES = 20;
	static final int AVERAGE_PROPS_AT_ROOT = 5;
	static final int AVERAGE_ARGS_AT_ROOT = 3;
	static final int PROPS_STEP = 2;
	static final int ARGS_STEP = 2;
	static final int ARGS_STANDARD_DEVIATION = 4;
	static final int PROPS_STANDARD_DEVIATION = 2;
	
	static final Random random = new Random();

	@Override
	public void populateDatastore() throws Exception {
		try {
			TextGenerator textGenerator = new TextGenerator();
			Count count = new Count();
			Count lastPrint = new Count();
			for (int i = 0; i < ROOT_NODES; i++) {
				Long propID = argMapService.addProp(null, 0,
						textGenerator.getSentence());
				recursiveCreateProp(propID, AVERAGE_PROPS_AT_ROOT, AVERAGE_ARGS_AT_ROOT,
						textGenerator, count, lastPrint);
			}
			log.severe("nodes created:"+ count.num);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}
	
	private class Count{
		public long num = 0;
	}

	private void recursiveCreateProp(Long propID, double averageProps, double averageArgs,
			TextGenerator textGenerator, Count count, Count lastPrint) throws Exception {
		double argCount = random.nextGaussian() * ARGS_STANDARD_DEVIATION + averageArgs;
		for (int i = 0; i < argCount; i++) {
			if(count.num++ > lastPrint.num + 100){
				log.severe("" + count.num);
				lastPrint.num += 100;
			}
			Long argID = argMapService.addArg(propID, random.nextBoolean());
			argMapService.updateArg(argID, textGenerator.getSentence());
			double propCount = random.nextGaussian() * PROPS_STANDARD_DEVIATION + averageProps;
			for (int j = 0; j < propCount; j++) {
				count.num++;
				Long childPropID = argMapService.addProp(argID, 0,
						textGenerator.getSentence());
				recursiveCreateProp(childPropID, averageProps - PROPS_STEP, averageArgs - ARGS_STEP,
						textGenerator, count, lastPrint);
			}
		}
	}

	private final ArgMapServiceImpl argMapService = new ArgMapServiceImpl() {
		@Override
		public HttpServletRequest getHttpServletRequest() {
			return ArgMapAdminServiceImpl.this.getThreadLocalRequest();
		}
	};

	private class TextGenerator {
		private Scanner scanner;
		File file = new File("aristotle.txt");

		public TextGenerator() throws FileNotFoundException {
			init();
		}

		private void init() throws FileNotFoundException {
			scanner = new Scanner(file);
			scanner.useDelimiter("\\.");
		}

		public String getSentence() throws IOException {
			String string = getASingleSentence();
			while (string.length() < 20) {
				string = getASingleSentence();
			}
			return string;
		}

		private String getASingleSentence() throws FileNotFoundException {
			if( !scanner.hasNext()){
				init();
			}
			String string = scanner.next();
			String cleaned = string.replaceAll("\\W", " ");
			cleaned = cleaned.replaceAll("\\s+", " ");
			String trimmed = cleaned.trim();
			return trimmed + ".";
		}
	}

}
