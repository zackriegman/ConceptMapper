package org.argmap.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Nodes implements Serializable{
	/* added to suppress warnings */
	private static final long serialVersionUID = 1L;
	public Map<Long, Proposition> props = new HashMap<Long, Proposition>();
	public Map<Long, Argument> args = new HashMap<Long, Argument>();
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("\nPropositions:");
		for( Proposition prop : props.values() ){
			sb.append( "\n" + prop );
		}
		sb.append("\nArguments:");
		for( Argument arg : args.values() ){
			sb.append( "\n" + arg );
		}
		
		return sb.toString();
	}
}