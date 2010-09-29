package com.appspot.conceptmapper.server;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.appengine.api.datastore.EntityNotFoundException;
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

	static {
		ObjectifyService.register(Proposition.class);
		ObjectifyService.register(Argument.class);
		ObjectifyService.register(Change.class);
	}

	private static final long serialVersionUID = 1L; // just to get rid of the
	// warnings...
	private Objectify ofy = ObjectifyService.begin();

	public void test() {
		// ofy.delete(ofy.query( Change.class ));
		// ofy.delete(Argument.class, 817);
		// println ("deleted Argument 817");
	}

	public void println(String message) {
		System.out.println(message);
		// log.log(Level.SEVERE, message);
	}

	public void deleteAllArgsAndProps() {
		ofy.delete(ofy.query(Argument.class));
		ofy.delete(ofy.query(Proposition.class));
	}

	public void printAllPropsAndArgs() {
		println("Arguments: ");
		for (Argument arg : ofy.query(Argument.class)) {
			printArgument(arg);
		}
		println("Propositions: ");
		for (Proposition prop : ofy.query(Proposition.class)) {
			printProposition(prop);
		}

	}

	public void printArgument(Argument arg) {
		println("id:" + arg.id + " - propKeys:" + arg.propIDs
				+ " - aboutPropKey:" + arg.aboutPropID + " - pro:" + arg.pro);
	}

	public void printProposition(Proposition prop) {
		println(propositionToString(prop));
	}

	public String propositionToString(Proposition prop) {
		return "id:" + prop.id + "; content:" + prop.content + "; topLevel:"
				+ prop.topLevel;
	}

	@Override
	public Proposition[] getAllProps() {

		test();

		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"topLevel", true);
		Proposition[] returnProps = new Proposition[propQuery.countAll()];
		// println("getAllProps[propQuery.countAll()]: " +
		// propQuery.countAll());
		int i = 0;
		for (Proposition prop : propQuery) {
			returnProps[i] = prop;
			recursiveBuildProp(prop);
			i++;
		}

		// printProps(returnProps);
		return returnProps;
	}

	public void printProps(Proposition[] props) {
		for (Proposition prop : props) {
			printPropRecursive(prop, 0);
		}
	}

	public String spaces(int spaces) {
		String string = "";
		for (int i = 0; i < spaces; i++) {
			string = string + " ";
		}
		return string;
	}

	public void printPropRecursive(Proposition propParent, int level) {
		println(spaces(level) + propositionToString(propParent));
		for (Argument arg : propParent.args) {
			String printString = spaces(level + 1);
			if (arg.pro == true)
				printString = printString + "pro - id:";
			else
				printString = printString + "con - id:";

			printString = printString + arg.id;
			println(printString);
			for (Proposition prop : arg.props) {
				printPropRecursive(prop, level + 2);
			}
		}

	}

	public void recursiveBuildProp(Proposition prop) {

		/* get all the prop's arguments */
		Query<Argument> argQuery = ofy.query(Argument.class).filter(
				"aboutPropID", prop.id);
		/* for each argument */
		for (Argument arg : argQuery) {
			/* add the argument to the prop */
			prop.args.add(arg);

			/* get all the props in the argument */
			Map<Long, Proposition> propMap = ofy.get(Proposition.class,
					arg.propIDs);

			/*
			 * for each propID check to make sure the prop actually exists, and
			 * if it does add it to the argument, and call this function
			 * recursively on the prop
			 */
			for (Long id : arg.propIDs) {
				Proposition gotProp = propMap.get(id);
				if (gotProp == null) {
					println("ERROR: datastore in inconsistent state; argument ["
							+ arg.id
							+ "] references proposition which does not exist");
				} else {
					arg.props.add(gotProp);
					recursiveBuildProp(gotProp);
				}
			}

		}
	}

	public Long addProposition(Long parentArgID, int position) throws Exception {

		Proposition newProposition = new Proposition();
		Argument parentArg = null;

		if (parentArgID != null) {
			// exception will be generated if there is a bogus parentArgID
			parentArg = ofy.get(Argument.class, parentArgID);
			newProposition.topLevel = false;
			ofy.put(newProposition);
			parentArg.propIDs.add(position, newProposition.id);
			// println("addProposition -- position:" + position +
			// "; parentArgID:"
			// + parentArgID + "; newProposition.id:" + newProposition.id);
			ofy.put(parentArg);
		} else {
			newProposition.topLevel = true;
			ofy.put(newProposition);
		}

		Change change = new Change(ChangeType.PROP_ADDITION);
		change.propID = newProposition.id;
		change.argID = parentArgID;
		saveVersionInfo(change);

		// getAllProps();

		return newProposition.id;

	}

	public void saveVersionInfo(Change change) {
		change.date = new Date();
		change.remoteAddr = getThreadLocalRequest().getRemoteAddr();
		change.remoteHost = getThreadLocalRequest().getRemoteHost();
		change.remotePort = getThreadLocalRequest().getRemotePort();
		change.remoteUser = getThreadLocalRequest().getRemoteUser();

		println("Change Logged -- " + change.toString());

		ofy.put(change);
	}

	@Override
	public void removeProposition(Long propID) throws Exception {
		if (ofy.query(Argument.class).filter("aboutPropID", propID).countAll() != 0) {
			throw new Exception(
					"cannot delete proposition with arguments; delete arguments first");
		}

		/* first get the stuff that we'll need for version control */
		Change change = new Change(ChangeType.PROP_DELETION);
		Proposition prop = ofy.get(Proposition.class, propID);
		change.propID = prop.id;
		change.propContent = prop.content;
		change.propTopLevel = prop.topLevel;

		/* in case there is an argument deletion, create an argument change */
		Change argDeletionChange = null;

		// print("Proposition ID to delete:" + propID);

		/* get all the arguments that use this proposition */
		Query<Argument> query = ofy.query(Argument.class).filter("propIDs",
				propID);
		if (query.countAll() > 1) { // only delete a proposition that is used in
			// 1 or fewer arguments
			throw new Exception(
					"cannot delete a proposition used by more than one argument; delink for other arguments before deleting");
		}

		/* if the proposition is used in an argument */
		else if (query.countAll() == 1) {
			Argument argument = query.iterator().next();

			/* record the versioning information */
			change.argID = argument.id;
			change.argPropIndex = argument.propIDs.indexOf(propID);
			// change.argPro = argument.pro;

			/* remove the proposition from the argument */
			argument.propIDs.remove(propID);

			/*
			 * if that's the only proposition, delete the arg and prepare
			 * versioning info for the arg deletion as well.
			 */
			if (argument.propIDs.isEmpty()) {
				argDeletionChange = new Change(ChangeType.ARG_DELETION);
				argDeletionChange.propID = argument.aboutPropID;
				argDeletionChange.argID = argument.id;
				argDeletionChange.argPro = argument.pro;
				/*
				 * because we are only deleting arguments when they have no
				 * propositions left, we don't have to save the argument's
				 * proposition list
				 */
				ofy.delete(argument);

			}

			/* otherwise save the updated arg */
			else {
				ofy.put(argument);
			}
		}

		/* delete the proposition */
		ofy.delete(Proposition.class, propID);

		/* save the version control information */
		saveVersionInfo(change);

		/*
		 * if an argument was deleted, also save the version control information
		 * for that deletion
		 */
		if (argDeletionChange != null) {
			saveVersionInfo(argDeletionChange);
		}

	}

	@Override
	public Argument addArgument(Long parentPropID, boolean pro)
			throws Exception {

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
		change.propID = parentPropID;
		saveVersionInfo(change);

		newArg.props.add(newProp);

		// getAllProps();
		return newArg;
	}

	@Override
	public void updateProposition(Long propID, String content) throws Exception {
		Change change = new Change(ChangeType.PROP_MODIFICATION);
		Proposition prop = ofy.get(Proposition.class, propID);
		if (prop.getContent() != null && prop.getContent().equals(content)) {
			throw new Exception(
					"Cannot update proposition content:  content not changed!");
		}

		change.propID = prop.id;
		change.propContent = prop.content;
		change.propTopLevel = prop.topLevel;

		/*
		 * have to save the version info before the proposition value is changed
		 * (or alternatively, create a new Proposition)
		 */
		saveVersionInfo(change);

		prop.setContent(content);
		ofy.put(prop);

	}

	@Override
	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws Exception {
		Argument argument = ofy.get(Argument.class, parentArgID);

		/*
		 * we get the proposition, even though the proposition ID is enough to
		 * complete the linking, we want to raise an exception if the prop
		 * doesn't actually exist to prevent garbage data from client putting
		 * the datastore in an inconsistent state
		 */
		Proposition proposition = ofy.get(Proposition.class, propositionID);

		argument.propIDs.add(position, propositionID);
		ofy.put(argument);

		Change change = new Change(ChangeType.PROP_LINK);
		change.argID = argument.id;
		change.propID = proposition.id;
		saveVersionInfo(change);
	}

	@Override
	public void unlinkProposition(Long parentArgID, Long propositionID)
			throws Exception {

		/* this will throw an exception if the argument doesn't exist */
		Argument argument = ofy.get(Argument.class, parentArgID);
		/* this will throw an exception if the prop doesn't exist */
		Proposition proposition = ofy.get(Proposition.class, propositionID);

		int propIndex = argument.propIDs.indexOf(propositionID);
		if (propIndex == -1) {
			throw new Exception(
					"cannot unlink proposition from argument:  proposition not part of argument");
		}

		argument.propIDs.remove(propositionID);

		if (argument.propIDs.isEmpty()) {
			/*
			 * TODO do I need to save any additional version information when I
			 * delete a argument during an unlink in order to recreate the
			 * previous state?
			 */
			ofy.delete(argument);
		} else {
			ofy.put(argument);
		}

		Change change = new Change(ChangeType.PROP_UNLINK);
		change.argID = parentArgID;
		change.propID = proposition.id;
		change.argPropIndex = propIndex;
		saveVersionInfo(change);
	}

	@Override
	public NavigableMap<Date, Change> getRevisions(Long changeID, List<Long> propIDs,
			List<Long> argIDs) throws Exception {

		NavigableMap<Date, Change> map = new TreeMap<Date, Change>();
		// HashSet<Long> processedProps = new HashSet<Long>();
		// HashSet<Long> processedArgs = new HashSet<Long>();
		if (changeID != null) {
			Change changeEnd = ofy.get(Change.class, changeID);
			recursiveQueryChanges(propIDs, argIDs, map, changeEnd.date);
		} else {
			recursiveQueryChanges(propIDs, argIDs, map, null);
		}

		// printAllChanges();

		/* reverse the order of the list
		println("getRevisions: changes:");
		List<Change> returnList = new LinkedList<Change>();
		for (Change change : map.values()) {
			returnList.add(0, change);
			println(change.toString());
		}
		*/
		
		//TODO make sure the map is ordered in the right direction, which it should be, oldest to newest...

		return map;
	}

	public PropTreeWithHistory getPropositionCurrentVersionAndHistory(
			Long propID) throws Exception {
		println("start getPropositionCurrentVersionAndHistory()");
		PropTreeWithHistory propVersions = new PropTreeWithHistory();
		try {
			propVersions.proposition = ofy.get(Proposition.class, propID);
			recursiveBuildProp(propVersions.proposition);
		} catch (EntityNotFoundException e) {
			/*
			 * if the prop is not found that merely means it doesn't exist in
			 * the current version of the tree... not a problem since it might
			 * have been deleted.
			 */
			propVersions.proposition = null;
		}

		List<Long> propIDs = new LinkedList<Long>();
		List<Long> argIDs = new LinkedList<Long>();
		recursiveExtractPropAndArgIDs(propVersions.proposition, propIDs, argIDs);
		propVersions.changes = getRevisions(null, propIDs, argIDs);

		println("end start getPropositionCurrentVersionAndHistory()");
		return propVersions;
	}

	public void recursiveExtractPropAndArgIDs(Proposition prop,
			List<Long> propIDs, List<Long> argIDs) {
		propIDs.add( prop.id );
		for( Argument arg : prop.args ){
			recursiveExtractPropAndArgIDs(arg, propIDs, argIDs );
		}
	}
	
	public void recursiveExtractPropAndArgIDs(Argument arg,
			List<Long> propIDs, List<Long> argIDs) {
		argIDs.add( arg.id );
		for( Proposition prop : arg.props){
			recursiveExtractPropAndArgIDs(prop, propIDs, argIDs );
		}
	} 

	public void recursiveQueryChanges(List<Long> propIDs, List<Long> argIDs,
			Map<Date, Change> map, Date date) {

		println("recursiveQueryChanges: propIDs:");
		if (propIDs != null) {
			String printString = "*";
			for (Long id : propIDs) {
				printString = printString + " - " + id;
			}
			println(printString);
		}
		println("recursiveQueryChanges: argIDs:");
		if (argIDs != null) {
			String printString = "*";
			for (Long id : argIDs) {
				printString = printString + " - " + id;
			}
			println(printString);
		}

		List<Long> deletedProps = new LinkedList<Long>();
		List<Long> deletedArgs = new LinkedList<Long>();

		if (propIDs != null && !propIDs.isEmpty()) {
			Query<Change> query = ofy.query(Change.class).filter("propID in",
					propIDs).order("-date");
			if (date != null) {
				query = query.filter("date <", date);
			}

			for (Change change : query) {
				map.put(change.date, change);
				if (change.changeType == Change.ChangeType.ARG_DELETION) {
					deletedArgs.add(change.argID);
				}
			}
		}

		if (argIDs != null && !argIDs.isEmpty()) {
			Query<Change> query = ofy.query(Change.class).filter("argID in",
					argIDs).order("-date");
			if (date != null) {
				query = query.filter("date <", date);
			}
			for (Change change : query) {
				map.put(change.date, change);
				// TODO must handle unlinking here as well
				if (change.changeType == Change.ChangeType.PROP_DELETION) {
					deletedProps.add(change.propID);
				}
			}
		}
		if (!deletedProps.isEmpty() || !deletedArgs.isEmpty()) {
			recursiveQueryChanges(deletedProps, deletedArgs, map, date);
		}
	}

	public void printAllChanges() {
		println("");
		for (Change change : ofy.query(Change.class))
			println("" + change.toString());
	}
}
