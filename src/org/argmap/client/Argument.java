package org.argmap.client;

import java.io.Serializable;

import com.googlecode.objectify.annotation.Cached;

@Cached
public class Argument extends Node implements Serializable {
	private static final long serialVersionUID = 1L; //to suppress warnings
	public static final int MAX_LENGTH = 70;
	
	/* remember if you change the name of a field you have to update
	 * the server queries:  they operate on fields specified by strings
	 * that are not checked at compile time.
	 */
	public boolean pro;
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("id:");
		buffer.append(id );
		buffer.append("; pro:");
		buffer.append( pro );
		buffer.append("; propIDs:[" );
		if( childIDs != null ){
			for(Long id : childIDs){
				buffer.append( id );
				buffer.append( " ");
				
			}
		}
		buffer.append("]");
		return buffer.toString();
	}
	
	/*transient public Long aboutPropID;*/
	/*
	@Transient
	public List<Proposition> props = new LinkedList<Proposition>();
	*/
	
	public Argument(){
	}
	
	public Argument( Argument argument ){
		id = argument.id;
		pro = argument.pro;
		content = argument.content;
	}
	
	/*
	public List<Proposition> getProps(){
		return props;
	}

	public Proposition getProposition(int i) {
		return props.get(i);
	}

	public int getCount() {
		return props.size();
	}

	public Proposition insertPropositionAt(int i) {
		Proposition proposition = new Proposition();
		props.add(i, proposition);
		return proposition;
	}

	public void deletePropositionl(Proposition proposition) {
		props.remove(proposition);
	}
	*/
	
}
