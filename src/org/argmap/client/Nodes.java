package org.argmap.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Nodes extends HashMap<Long, Node> implements Serializable{
	/* added to suppress warnings */
	private static final long serialVersionUID = 1L;
	public Map<Long, Node> propsT = this;
	public Map<Long, Node> argsT = this;
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("\nNodes:");
		for( Node node : values() ){
			String type;
			if( node instanceof Proposition ){
				type = "prop:";
			} else if (node instanceof Argument){
				type = "arg:";
			} else {
				type = "error (type not recognized):";
			}
			sb.append( "\n" + type + node );
		}
		return sb.toString();
	}
	
//	public String toString(){
//		StringBuilder sb = new StringBuilder();
//		sb.append("\nPropositions:");
//		for( Proposition prop : props.values() ){
//			sb.append( "\n" + prop );
//		}
//		sb.append("\nArguments:");
//		for( Argument arg : args.values() ){
//			sb.append( "\n" + arg );
//		}
//		
//		return sb.toString();
//	}
}