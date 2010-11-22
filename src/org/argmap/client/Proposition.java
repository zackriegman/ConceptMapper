package org.argmap.client;

import java.io.Serializable;
import java.util.Set;

import com.googlecode.objectify.annotation.Cached;

@Cached
public class Proposition extends Node implements Serializable {

	private static final long serialVersionUID = 1L;

	/*
	 * remember if you change the name of a field you have to update the server
	 * queries: they operate on fields specified by strings that are not checked
	 * at compile time.
	 */

	public int linkCount;

	/*
	 * note the difference between 'transient' and '@Transient' 'transient' will
	 * not be sent over the wire, but will be saved to the datastore.
	 * '@Transient' will not be saved to the datastore, but will be sent over
	 * the wire.
	 */
	/* not sent to client, but saved */
	transient public Set<String> tokens;

	transient public long ratingSum = 0;
	transient public long ratingCount = 0;

	// public @Transient List<Argument> args = new LinkedList<Argument>();

	public Proposition() {
	}

	public Proposition(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("id:");
		buffer.append(id);
		buffer.append("; content:");
		buffer.append(content);
		buffer.append("; linkCount:");
		buffer.append(linkCount);
		buffer.append("; argIDs:[");
		if (childIDs != null) {
			for (Long id : childIDs) {
				buffer.append(" ");
				buffer.append(id);
			}
		}
		buffer.append("]; tokens:[");
		if (tokens != null) {
			for (String str : tokens) {
				buffer.append(" ");
				buffer.append(str);
			}
		}
		buffer.append("]");
		return buffer.toString();
	}

	public Proposition(Proposition prop) {
		id = prop.id;
		content = prop.content;
		linkCount = prop.linkCount;
	}

	public Proposition(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public Long getID() {
		return id;
	}

	public void setContent(String content) {
		this.content = content;
	}

	/*
	 * public List<Argument> getArgs(){ return args; }
	 * 
	 * public Argument getArgument( int i ){ return args.get( i ); }
	 * 
	 * public int getCount(){ return args.size(); }
	 * 
	 * 
	 * public Argument insertArgumentAt( int i ){ Argument argument = new
	 * Argument(); args.add( i, argument ); return argument; }
	 * 
	 * 
	 * public void deleteArgumentl( Argument argument ){ args.remove( argument
	 * ); }
	 */
}
