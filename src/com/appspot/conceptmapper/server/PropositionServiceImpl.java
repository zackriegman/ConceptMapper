package com.appspot.conceptmapper.server;

import java.util.List;

import javax.jdo.PersistenceManager;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.appspot.conceptmapper.client.Proposition;
import com.appspot.conceptmapper.client.PropositionService;

public class PropositionServiceImpl extends RemoteServiceServlet implements
		PropositionService {

	@Override
	public Proposition[] getRootPropositions() {
		// Proposition[] propositions = new Proposition[1];
		// propositions[0] = new Proposition();
		// propositions[0].setContent("Message Received!!!");
		// return propositions;

		PersistenceManager pm = PMF.get().getPersistenceManager();
		String query = "select from " + PropositionPersistent.class.getName()
				+ " order by content";
		List<PropositionPersistent> propositions = (List<PropositionPersistent>) pm
				.newQuery(query).execute();
		
		Proposition[] returnProps = new Proposition[ propositions.size()];
		for( int i = 0; i < propositions.size(); i++){
			returnProps[ i ] = propositions.get(i).getProposition();
		}
		pm.close();
		return returnProps;
	}

	@Override
	public void addRootProposition(Proposition proposition) {

		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			pm.makePersistent(new PropositionPersistent(proposition));
		} finally {
			pm.close();
		}
	}

	@Override
	public void deleteProposition(Proposition proposition) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			pm.deletePersistent(new PropositionPersistent( proposition ));
		} finally {
			pm.close();
		}
		
	}

}
