package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.annotation.Cached;

@Cached
public class Argument implements Serializable {

	private static final long serialVersionUID = 1L; //to suppress warnings
	
	@Id
	public Long id;
	

	/* note the difference between 'transient' and '@Transient'
	 * 'transient' will not be sent over the wire, but will be saved to the datastore.
	 * '@Transient' will not be saved to the datastore, but will be sent over the wire.
	 * Here the propIDs and aboutPropID don't need to be sent over the wire because
	 * that info is stored in the Argument.props and Propsition.args lists (which
	 * in turn are not stored in the datastore.
	 */
	transient public List<Long> propIDs = new LinkedList<Long>();
	transient public Long aboutPropID;
	
	public boolean pro;
	
	@Transient
	public List<Proposition> props = new LinkedList<Proposition>();
	
	public Argument(){
	}
	
	public Argument( Argument argument ){
		id = argument.id;
		pro = argument.pro;
	}
	
	
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
	
}
