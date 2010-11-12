package org.argmap.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;

public class Log {
	public static final boolean on = true;
	private static final List<Log> openLogs = new LinkedList<Log>();

	private boolean immediatePrint;
	private int indent;
	private final String logName;
	StringBuilder content;

	/*
	 * for now must return a log regardless of whether logging is on because the
	 * logging code is not being automatically removed... eventually perhaps I
	 * can figure out how the GWT Logger framework has its code removed and
	 * emulate that
	 */
	public static Log getLog(String logName) {
		return getLog(logName, false);
	}

	public static Log getLog(String logName, boolean immediatePrint) {
		Log log = new Log(logName);
		log.immediatePrint = immediatePrint;
		openLogs.add(log);
		return log;
	}

	private Log(String logName) {
		this.logName = logName;
		content = new StringBuilder();
	}

	public void finish() {
		if (on) {
			assert openLogs.contains(this);
			if (!immediatePrint) {
				GWT.log(logName + ": " + content.toString());
			}
			openLogs.remove(this);
		}
	}

	public static void finishOpenLogs() {
		if (on) {
			for (Log log : openLogs) {
				log.finish();
			}
		}
	}

	public static void log(String logName, String logMessage) {
		if (on) {
			GWT.log(logName + ": " + logMessage);
		}
	}

	public void log(String string) {
		if (on) {
			assert openLogs.contains(this);
			String indentString = spaces(indent * 4);
			if (immediatePrint) {
				GWT.log(logName + ": " + indentString + string);
			} else {
				content.append(indentString + string);
			}
		}
	}

	public void logln(String string) {
		if (on) {
			log("\n" + string);
		}
	}

	public void indent() {
		if (on) {
			assert openLogs.contains(this);
			indent++;
		}
	}

	public void unindent() {
		if (on) {
			assert openLogs.contains(this);
			indent--;
		}
	}

	public static String spaces(int spaces) {
		if (on) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < spaces; i++) {
				sb.append(" ");
			}
			return sb.toString();
		} else {
			return null;
		}
	}

	public static void logNull(String string, Object... vars) {
		if (on) {
			string += ": ";
			for (int i = 0; i < vars.length; i++) {
				if (vars[i] == null) {
					string += i + " ";
				}
			}
			GWT.log(string);
		}
	}

	public static <K, V> String mapToString(Map<K, V> map) {
		StringBuilder sb = new StringBuilder();
		for (Entry<K, V> entry : map.entrySet()) {
			sb.append("\nK:" + entry.getKey().toString() + "; V:"
					+ entry.getValue().toString());
		}
		return sb.toString();
	}

	public static <K, V> String multiMapToString(MultiMap<K, V> map) {
		StringBuilder sb = new StringBuilder();
		for (K key : map.keySet()) {
			sb.append("\nK:" + key);
			List<V> values = map.get(key);
			for (V value : values) {
				sb.append("\n" + spaces(4) + "V:" + value);
			}
		}
		return sb.toString();
	}
	
	public static <T> String listToString(List<T> list){
		StringBuilder sb = new StringBuilder();
		for( T item : list ){
			sb.append(item + " ");
		}
		return sb.toString();
	}
}
