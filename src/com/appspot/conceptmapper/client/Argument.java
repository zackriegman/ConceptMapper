package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.Key;

public class Argument implements Serializable {
	@Id
	public Long id;
	public List<Key<Proposition>> propKeys = new LinkedList<Key<Proposition>>();
	public Key<Proposition> aboutPropKey;
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
