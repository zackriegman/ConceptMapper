package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.annotation.Cached;

@Cached
public class Argument implements Serializable {

	private static final long serialVersionUID = 1L; //to suppress warnings
	public static final int MAX_LENGTH = 70;
	
	@Id
	public Long id;
	public String title;
	public boolean pro;
	
	/* note the difference between 'transient' and '@Transient'
	 * 'transient' will not be sent over the wire, but will be saved to the datastore.
	 * '@Transient' will not be saved to the datastore, but will be sent over the wire.
	 */
	public List<Long> propIDs = new LinkedList<Long>();
	
	@Transient
	public List<Change> changes;
	@Transient
	public Date changesTo;
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("id:");
		buffer.append(id );
		buffer.append("; pro:");
		buffer.append( pro );
		buffer.append("; propIDs:[" );
		if( propIDs != null ){
			for(Long id : propIDs){
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
		title = argument.title;
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
