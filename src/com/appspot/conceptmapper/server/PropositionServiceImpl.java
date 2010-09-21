package com.appspot.conceptmapper.server;

import java.util.Date;
import java.util.Map;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.appspot.conceptmapper.client.Argument;
import com.appspot.conceptmapper.client.Change;
import com.appspot.conceptmapper.client.Proposition;
import com.appspot.conceptmapper.client.PropositionService;
import com.appspot.conceptmapper.client.Change.ChangeType;

public class PropositionServiceImpl extends RemoteServiceServlet implements
		PropositionService {

	private static final long serialVersionUID = 1L; // just to get rid of the
	// warnings...
	private Objectify objectify;

	public void test() {
		Objectify ofy = ofy();
		//ofy.delete(Argument.class, 817);
		//println ("deleted Argument 817");

	}

	public Objectify ofy() {
		if (objectify == null) {
			ObjectifyService.register(Proposition.class);
			ObjectifyService.register(Argument.class);
			ObjectifyService.register(Change.class);
			objectify = ObjectifyService.begin();
		}
		return objectify;
	}

	public void println(String message) {
		System.out.println(message);
		// log.log(Level.SEVERE, message);
	}

	public void print(String message) {
		System.out.print(message);
	}

	public void deleteAllArgsAndProps() {
		Objectify ofy = ofy();
		ofy.delete(ofy.query(Argument.class));
		ofy.delete(ofy.query(Proposition.class));
	}

	public void printAllPropsAndArgs(Objectify ofy) {
		print("Arguments: ");
		for (Argument arg : ofy.query(Argument.class)) {
			printArgument(arg);
		}
		print("Propositions: ");
		for (Proposition prop : ofy.query(Proposition.class)) {
			printProposition(prop);
		}

	}

	public void printArgument(Argument arg) {
		print("id:" + arg.id + " - propKeys:" + arg.propIDs
				+ " - aboutPropKey:" + arg.aboutPropID + " - pro:" + arg.pro);
	}

	public void printProposition(Proposition prop) {
		print(propositionToString(prop));
	}

	public String propositionToString(Proposition prop) {
		return "id:" + prop.id + "; content:" + prop.content + "; topLevel:"
				+ prop.topLevel;
	}

	/*
	 * public Long recursivePutProp(Proposition prop) { ofy.put(prop);
	 * List<Argument> args = prop.getArgs(); for (Argument arg : args) {
	 * arg.aboutPropID = prop.getID(); recursivePutArg(arg); } return
	 * prop.getID(); }
	 * 
	 * public void recursivePutArg(Argument arg) { List<Proposition> props =
	 * arg.getProps(); for (Proposition prop : props) {
	 * arg.propIDs.add(recursivePutProp(prop)); } ofy.put(arg); }
	 */

	@Override
	public Proposition[] getAllProps() {
		Objectify ofy = ofy();
		
		test();
		
		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"topLevel", true);
		Proposition[] returnProps = new Proposition[propQuery.countAll()];
		println("propQuery.countAll(): " + propQuery.countAll());
		int i = 0;
		for (Proposition prop : propQuery) {
			returnProps[i] = prop;
			recursiveBuildProp(prop, ofy);
			i++;
		}

		printProps(returnProps);
		return returnProps;
	}

	public void printProps(Proposition[] props) {
		print("\n");
		for (Proposition prop : props) {
			printPropRecursive(prop, 0);
		}
		print("\n");
	}

	public void printPropRecursive(Proposition propParent, int level) {
		for (int i = 0; i < level; i++)
			print("  ");
		print(propositionToString(propParent));
		print("\n");
		for (Argument arg : propParent.args) {
			for (int i = 0; i < level + 1; i++)
				print("  ");
			if (arg.pro == true)
				print("pro - id:");
			else
				print("con - id:");
			print("" + arg.id);
			print("\n");
			for (Proposition prop : arg.props) {
				printPropRecursive(prop, level + 2);
			}
		}

	}

	public void recursiveBuildProp(Proposition prop, Objectify ofy) {

		Query<Argument> argQuery = ofy.query(Argument.class).filter(
				"aboutPropID", prop.id);
		for (Argument arg : argQuery) {
			// print("processing argument:");
			// printArgument(arg);
			prop.args.add(arg);
			Map<Long, Proposition> propMap = ofy.get(Proposition.class,
					arg.propIDs);
			for (Long id : arg.propIDs) {
				Proposition gotProp = propMap.get(id);
				if (gotProp == null) {
					print("ERROR: datastore in inconsistent state; argument ["
							+ arg.id
							+ "] references proposition which does not exist");
				} else {
					arg.props.add(gotProp);
					recursiveBuildProp(gotProp, ofy);
				}
			}

		}
	}

	public Long addProposition(Long parentArgID, int position) throws Exception {
		Objectify ofy = ofy();

		Proposition newProposition = new Proposition();
		Argument parentArg = null;

		if (parentArgID != null) {
			// exception will be generated if there is a bogus parentArgID
			parentArg = ofy.get(Argument.class, parentArgID);
			newProposition.topLevel = false;
			ofy.put(newProposition);
			parentArg.propIDs.add(position, newProposition.id);
			println("addProposition -- position:" + position + "; parentArgID:"
					+ parentArgID + "; newProposition.id:" + newProposition.id);
			ofy.put(parentArg);
		} else {
			newProposition.topLevel = true;
			ofy.put(newProposition);
		}

		Change change = new Change(ChangeType.PROP_ADDITION);
		change.proposition = newProposition;
		change.argID = parentArgID;
		saveVersionInfo(change, ofy);
		
		//getAllProps();

		return newProposition.id;

	}

	public void saveVersionInfo(Change change, Objectify ofy) {
		change.date = new Date();
		change.remoteAddr = getThreadLocalRequest().getRemoteAddr();
		change.remoteHost = getThreadLocalRequest().getRemoteHost();
		change.remotePort = getThreadLocalRequest().getRemotePort();
		change.remoteUser = getThreadLocalRequest().getRemoteUser();

		if(false)
		print("\nChange Logged -- changeType:" + change.changeType + "; argID:"
				+ change.argID + "; argPropIndex:" + change.argPropIndex
				+ "; argPro:" + change.argPro + "; proposition{"
				+ propositionToString(change.proposition) + "}; date:"
				+ change.date + "; remoteAddr:" + change.remoteAddr
				+ "; remoteHost:" + change.remoteHost + "; remotePort:"
				+ change.remotePort + "; remoteUser:" + change.remoteUser
				+ "; ");

		ofy.put(change);
	}

	@Override
	public void removeProposition(Long propID) throws Exception {
		Objectify ofy = ofy();
		if (ofy.query(Argument.class).filter("aboutPropID", propID).countAll() != 0) {
			throw new Exception(
					"cannot delete proposition with arguments; delete arguments first");
		}

		/* first get the stuff that we'll need for version control */
		Change change = new Change(ChangeType.PROP_DELETION);
		change.proposition = ofy.get(Proposition.class, propID);

		// print("Proposition ID to delete:" + propID);

		/* get all the arguments that use this proposition */
		Query<Argument> query = ofy.query(Argument.class).filter("propIDs",
				propID);
		if (query.countAll() > 1) { // only delete a proposition that is used in
			// 1 or fewer arguments
			throw new Exception(
					"cannot delete a proposition used by more than one argument; delink for other arguments before deleting");
		} else if (query.countAll() == 1) { // if the proposition is used in an
			// argument
			//Argument argument = query.get(); // get the argument
			Argument argument = query.iterator().next();
			change.argID = argument.id; // record the versioning information
			change.argPropIndex = argument.propIDs.indexOf(propID);
			change.argPro = argument.pro;

			argument.propIDs.remove(propID); // remove the proposition from the
			// argument
			if (argument.propIDs.isEmpty()) { // if that's the only proposition,
				// delete the arg
				ofy.delete(argument); // because we are only deleting arguments
				// when they have no propositions left,
				// we don't have to save the arguments
				// proposition list
			} else { // otherwise save the updated arg
				ofy.put(argument);
			}
		}

		ofy.delete(Proposition.class, propID);// delete the proposition

		saveVersionInfo(change, ofy); // save the version control information

	}

	@Override
	public Argument addArgument(Long parentPropID, boolean pro)
			throws Exception {
		Objectify ofy = ofy();

		/*
		 * trigger an exception if the ID is invalid to prevent inconsistent
		 * datastore states
		 */
		ofy.get(Proposition.class, parentPropID);

		Proposition newProp = new Proposition();
		newProp.topLevel = false;
		ofy.put(newProp);

		Argument newArg = new Argument();
		newArg.aboutPropID = parentPropID;
		newArg.propIDs.add(0, newProp.id);

		newArg.pro = pro;

		ofy.put(newArg);

		Change change = new Change(ChangeType.ARG_ADDITION);
		change.argID = newArg.id;
		change.proposition = newProp;
		saveVersionInfo(change, ofy);

		newArg.props.add(newProp);
		
		//getAllProps();
		return newArg;
	}

	@Override
	public void updateProposition(Long propID, String content) throws Exception {
		Objectify ofy = ofy();
		Change change = new Change(ChangeType.PROP_MODIFICATION);
		Proposition prop = ofy.get(Proposition.class, propID);

		println("updateProposition -- " + propositionToString(prop));
		change.proposition = prop;

		/*
		 * have to save the version info before the proposition value is changed
		 * (or alternatively, create a new Proposition)
		 */
		saveVersionInfo(change, ofy);

		prop.setContent(content);
		ofy.put(prop);

	}

	@Override
	public void linkProposition(Long parentArgID, int position,
			Long propositionID) {
		// TODO write PropositionServiceImpl.linkProposition()

	}

	@Override
	public void unlinkProposition(Long parentArgID, Long propositionID) {
		// TODO write PropositionServiceImpl.unlinkProposition()

	}

}
