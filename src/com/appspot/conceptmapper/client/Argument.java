package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.Transient;


public class Argument implements Serializable {

	private static final long serialVersionUID = 1L; //to suppress warnings
	
	@Id
	public Long id;
	//TODO: don't need to send propIDs list over the wire, mark as nosend
	public List<Long> propIDs = new LinkedList<Long>();
	//TODO: don't need to send aboutPropID over the wire, mark as nosend
	public Long aboutPropID;
	public boolean pro;
	
	@Transient
	public List<Proposition> props = new LinkedList<Proposition>();
	
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
