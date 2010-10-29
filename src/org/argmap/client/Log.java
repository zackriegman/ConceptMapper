package org.argmap.client;

import com.google.gwt.core.client.GWT;

public class Log {
	public static final boolean on = true;

	private boolean immediatePrint;
	private int indent;
	private final String logName;
	StringBuilder content;

	/* for now must return a log regardless
	 * of whether logging is on because
	 * the logging code is not being automatically
	 * removed... eventually perhaps I can figure out
	 * how the GWT Logger framework has its code
	 * removed and emulate that */
	public static Log getLog(String logName) {
		return new Log(logName);
	}

	private Log(String logName) {
		this.logName = logName;
		content = new StringBuilder();
	}

	public void flush() {
		if (on) {
			if (!immediatePrint) {
				GWT.log(logName + ": " + content.toString());
			}
		}
	}

	public void log(String string) {
		if (on) {
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
			indent++;
		}
	}

	public void unindent() {
		if (on) {
			indent--;
		}
	}

	public static String spaces(int spaces) {
		if (on) {
			String string = "";
			for (int i = 0; i < spaces; i++) {
				string = string + " ";
			}
			return string;
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
}
