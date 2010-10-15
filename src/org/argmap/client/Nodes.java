package org.argmap.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Nodes implements Serializable{
	/* added to suppress warnings */
	private static final long serialVersionUID = 1L;
	public Map<Long, Proposition> props = new HashMap<Long, Proposition>();
	public Map<Long, Argument> args = new HashMap<Long, Argument>();
}