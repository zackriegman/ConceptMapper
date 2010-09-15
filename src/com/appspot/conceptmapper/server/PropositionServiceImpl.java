package com.appspot.conceptmapper.server;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;

import org.mortbay.log.Log;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.appspot.conceptmapper.client.Argument;
import com.appspot.conceptmapper.client.Proposition;
import com.appspot.conceptmapper.client.PropositionService;

public class PropositionServiceImpl extends RemoteServiceServlet implements
		PropositionService {
	private static final Logger log = Logger
			.getLogger(PropositionServiceImpl.class.getName());

	private Objectify ofy;

	@Override
	public Proposition[] getRootPropositions() {
		return null;
	}

	@Override
	public void addRootProposition(Proposition proposition) {
		ofy = ObjectifyService.begin();
		ofy.put(proposition);

		try {
			Proposition prop = ofy.get(Proposition.class, proposition.getID());
			print(prop.getContent());
		} catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void print(String message) {
		System.out.println(message);
		// log.log(Level.SEVERE, message);
	}

	public void deleteAllArgsAndProps() {
		ofy = ObjectifyService.begin();
		ofy.delete(ofy.query(Argument.class));
		ofy.delete(ofy.query(Proposition.class));
	}

	/*
	 * @Override public Proposition[] getRootPropositions() { // Proposition[]
	 * propositions = new Proposition[1]; // propositions[0] = new
	 * Proposition(); // propositions[0].setContent("Message Received!!!"); //
	 * return propositions;
	 * 
	 * PersistenceManager pm = PMF.get().getPersistenceManager(); String query =
	 * "select from " + PropositionPersistent.class.getName() +
	 * " order by content"; List<PropositionPersistent> propositions =
	 * (List<PropositionPersistent>) pm .newQuery(query).execute();
	 * 
	 * Proposition[] returnProps = new Proposition[ propositions.size()]; for(
	 * int i = 0; i < propositions.size(); i++){ returnProps[ i ] =
	 * propositions.get(i).getProposition(); } pm.close(); return returnProps; }
	 */

	/*
	 * @Override public void addRootProposition(Proposition proposition) {
	 * 
	 * PersistenceManager pm = PMF.get().getPersistenceManager(); try {
	 * pm.makePersistent(new PropositionPersistent(proposition)); } finally {
	 * pm.close(); } }
	 */

	@Override
	public void deleteProposition(Proposition proposition) {
	}

	@Override
	public void makePropChanges(Proposition[] newProps,
			Proposition[] changedProps, Proposition[] deletedProps,
			Argument[] newArgs, Argument[] deletedArgs) {
		ofy = ObjectifyService.begin();

		if (changedProps != null)
			ofy.put(changedProps);

		// TODO: should check to make sure that deleted Props and Args have no
		// children before deleting them
		// theoretically, the client should prevent deleting/merging of props
		// with
		// arguments for them because what do you do with the arguments.
		// similarly, to delete an argument you should first delete all the
		// props
		if (deletedProps != null)
			ofy.delete(deletedProps);
		if (deletedArgs != null)
			ofy.delete(deletedArgs);

		// TODO: check to make sure that the whole tree of new props and args
		// serializes intact
		if (newProps != null)
			for (Proposition prop : newProps) {
				recursiveAddProp(prop);
			}

		if (newArgs != null)
			for (Argument arg : newArgs) {
				recursiveAddArg(arg);
			}

		// printAllPropsAndArgs();
		// deleteAllArgsAndProps();
		printAllPropsAndArgs();

		// TODO: for new Props and Args must get the new ids back to client!!!
	}

	public void printAllPropsAndArgs() {
		print("Arguments: ");
		for (Argument arg : ofy.query(Argument.class)) {
			print("id:" + arg.id + " - propKeys:" + arg.propKeys
					+ " - aboutPropKey" + arg.aboutPropKey + " - pro:"
					+ arg.pro);
		}
		print("Propositions: ");
		for (Proposition prop : ofy.query(Proposition.class)) {
			print("id:" + prop.id + " - content:" + prop.content);
		}

	}

	public Key<Proposition> recursiveAddProp(Proposition prop) {
		ofy.put(prop);
		Key<Proposition> propKey = new Key<Proposition>(Proposition.class, prop
				.getID());
		List<Argument> args = prop.getArgs();
		for (Argument arg : args) {
			arg.aboutPropKey = propKey;
			recursiveAddArg(arg);
		}
		return propKey;
	}

	public void recursiveAddArg(Argument arg) {
		List<Proposition> props = arg.getProps();
		for (Proposition prop : props) {
			arg.propKeys.add(recursiveAddProp(prop));
		}
		ofy.put(arg);
	}

	@Override
	public Proposition[] getAllProps() {
		ofy = ObjectifyService.begin();
		printAllPropsAndArgs();
		// TODO: hmmm... how to find just the root propositions? This brings up
		// data structure question. Should each proposition hold a list of the
		// arguments it is a part of, or vice-versa, or both?
		return null;
	}

}
