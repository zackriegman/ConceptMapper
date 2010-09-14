package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Argument implements Serializable {
	List<Proposition> propositions = new ArrayList<Proposition>();

	public Proposition getProposition(int i) {
		return propositions.get(i);
	}

	public int getCount() {
		return propositions.size();
	}

	public Proposition insertPropositionAt(int i) {
		Proposition proposition = new Proposition();
		propositions.add(i, proposition);
		return proposition;
	}

	public void deletePropositionl(Proposition proposition) {
		propositions.remove(proposition);
	}
	
}
