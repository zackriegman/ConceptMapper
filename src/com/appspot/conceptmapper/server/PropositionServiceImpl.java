package com.appspot.conceptmapper.server;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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

	public void print(String message) {
		System.out.print(message);
	}

	public void deleteAllArgsAndProps() {
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

	@Override
	public Proposition[] getAllProps() {

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
		saveVersionInfo(change, ofy);

		// getAllProps();

		return newProposition.id;

	}

	public void saveVersionInfo(Change change, Objectify ofy) {
		change.date = new Date();
		change.remoteAddr = getThreadLocalRequest().getRemoteAddr();
		change.remoteHost = getThreadLocalRequest().getRemoteHost();
		change.remotePort = getThreadLocalRequest().getRemotePort();
		change.remoteUser = getThreadLocalRequest().getRemoteUser();

		print("\nChange Logged -- " + changeToString(change));

		ofy.put(change);
	}

	public String changeToString(Change change) {
		return "changeType:" + change.changeType + "; argID:" + change.argID
				+ "; argPropIndex:" + change.argPropIndex + "; argPro:"
				+ change.argPro + "; propID:" + change.propID
				+ "; propContent:" + change.propContent + "; propTopLevel:"
				+ change.propTopLevel + "; date:" + change.date
				+ "; remoteAddr:" + change.remoteAddr + "; remoteHost:"
				+ change.remoteHost + "; remotePort:" + change.remotePort
				+ "; remoteUser:" + change.remoteUser;
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
				// we don't have to save the argument's
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
		change.propID = newProp.id;
		saveVersionInfo(change, ofy);

		newArg.props.add(newProp);

		// getAllProps();
		return newArg;
	}

	@Override
	public void updateProposition(Long propID, String content) throws Exception {
		Change change = new Change(ChangeType.PROP_MODIFICATION);
		Proposition prop = ofy.get(Proposition.class, propID);

		println("updateProposition -- " + propositionToString(prop));
		change.propID = prop.id;
		change.propContent = prop.content;
		change.propTopLevel = prop.topLevel;

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
		saveVersionInfo(change, ofy);
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
		saveVersionInfo(change, ofy);

	}

	public void getPropAtTime() {
		/*
		 * TODO ok, so the main approach i'm thinking of now is that the server
		 * will be able to return a prop or arg as it existed (or didn't) at any
		 * time. The returned prop (or arg) will include references to it's
		 * children, which the client can build at with stubs saying
		 * "loading from server", and when the prop or arg is opened the client
		 * can send a request to the server, asking for the children at that
		 * time (i.e. lazy loading the tree at the time). Of course, to make
		 * that more seamless to the user, the server can send not only the
		 * proposition but its children too, or perhaps even its children's
		 * children (etc.) with each request.
		 * 
		 * The issue however is how to represent that to the user in terms of an
		 * easily digestible change list. We want to show the user the changes
		 * relevant to the part of the tree that he is viewing. As he opens more
		 * sublevels however, more changes become apparent. We could update the
		 * change list as sub levels are opened to reflect the changes that can
		 * now been seen in the sublevels. However, what about changes that add
		 * or delete nodes. As you move through the history those nodes will
		 * appear and dispear. When one disapears do you remove the changes from
		 * the changeList that had to do with that node? Probably not, that
		 * would be disorienting. Instead you have to keep track of whether a
		 * user opened or closed a node the last time he saw it... thats getting
		 * too complicated
		 * 
		 * An alternative would be to show the user all the changes for the top
		 * level proposition that he is currently navigating from, even for
		 * nodes that are not visible on the screen and/or that are currently
		 * closed, or even currently deleted, etc. That might be disorienting
		 * however, because as the user clicks through the change list, he'll
		 * see changes that dont' seem to change anything on screen.
		 * 
		 * I think the ideal solution might be the complicated one, but maybe it
		 * doesn't make sense to try to implement the ideal.
		 * 
		 * maybe a relatively simple, perhaps temporary, until I can think of
		 * something better, is to show changes for things that are open in the
		 * edit screen, and open in the version screen. As someone opens or
		 * closes a change in the version screen, the version list is updated,
		 * to show just the versions for those nodes showing.
		 * 
		 * As they walk through the version list, when new nodes are added, they
		 * are added automatically in an open position. And there changes are
		 * immediately added.
		 * 
		 * Better yet, all the changes for the currently opened nodes are added
		 * at the beginning including changes to any descendants of open nodes,
		 * except those descendants that are all ready closed.
		 * 
		 * So lets walk through this plan. We have a tree with some open nodes
		 * and some closed nodes in the edit window. The user clicks on versions
		 * and goes to the version window where they see that tree, as it exists
		 * in the edit window.
		 * 
		 * In the versions box they see all the versions for the currently
		 * opened nodes. As they move back in time, nodes disappear at times
		 * preceding their creation. The versions box does not change however to
		 * reflect the disappearance of nodes unless a node is closed. Closing a
		 * node will remove versions from the version list and opening a node
		 * will add versions to the version list. But moving through time never
		 * changes the version list by itself. Lets say you move back past a
		 * deletion, and as a consequence a new node pops up. Should the node
		 * pop up as opened (implying that it's changes are already part of the
		 * change list) or should it pop up as closed (meaning the changes are
		 * not part of the change list until it is explicitly opened). Either
		 * way, we don't need to remember it's state if we pop back to the
		 * beginning of the list before it existed, because the change list
		 * itself remembers its state. I think as you move backwards in time
		 * deletions should start visible, which means that you need to fetch
		 * all the deleted descendants of a node when you fetch an opened node.
		 * 
		 * Steps to implementation: 
		 * 1. fetch deleted descendants when fetching an open node 
		 * 2. add place holders to closed nodes, and handle open/close
		 * events to automatically load children
		 * 3. add changes to the version list when a node is opened
		 * 4. remove changes from the version list when a node is closed
		 * 5. make sure that argument additions/deletions are handled properly 
		 * (given that there is no explicit argument deletion event on the server)
		 */
	}

	@Override
	public List<Change> getRevisions(Long changeID, List<Long> propIDs,
			List<Long> argIDs) throws Exception {

		println("\ngetRevisions propIDs:");
		for (Long id : propIDs)
			print(" - " + id);
		println("\ngetRevisions argIDs:");
		for (Long id : argIDs)
			print(" - " + id);

		SortedMap<Date, Change> map = new TreeMap<Date, Change>();
		if (changeID != null) {
			Change changeEnd = ofy.get(Change.class, changeID);
			if (propIDs != null && !propIDs.isEmpty()) {
				for (Change change : ofy.query(Change.class).filter(
						"propID in", propIDs).filter("date <", changeEnd.date)
						.order("-date")) {
					map.put(change.date, change);
				}
			}
			if (argIDs != null && !argIDs.isEmpty()) {
				for (Change change : ofy.query(Change.class).filter("argID in",
						argIDs).filter("date <", changeEnd.date).order("-date")) {
					map.put(change.date, change);
				}
			}
		} else {
			if (propIDs != null && !propIDs.isEmpty()) {
				for (Change change : ofy.query(Change.class).filter(
						"propID in", propIDs).order("-date")) {
					map.put(change.date, change);
				}
			}
			if (argIDs != null && !argIDs.isEmpty()) {
				for (Change change : ofy.query(Change.class).filter("argID in",
						argIDs).order("-date")) {
					map.put(change.date, change);
				}
			}
		}

		// printAllChanges();

		// reverse the order of the list
		List<Change> returnList = new LinkedList<Change>();
		for (Change change : map.values()) {
			returnList.add(0, change);
		}

		return returnList;
	}

	public void printAllChanges() {
		println("");
		for (Change change : ofy.query(Change.class))
			println(changeToString(change));
	}
}
