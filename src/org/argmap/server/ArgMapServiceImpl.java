package org.argmap.server;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.NodeChanges;
import org.argmap.client.Nodes;
import org.argmap.client.Proposition;
import org.argmap.client.ArgMapService;
import org.argmap.client.Change.ChangeType;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

public class ArgMapServiceImpl extends RemoteServiceServlet implements
		ArgMapService {

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
		println(arg.toString());
	}

	public void printProposition(Proposition prop) {
		println(prop.toString());
	}

	@Override
	public AllPropsAndArgs getAllPropsAndArgs() {
		/*
		 * TODO: now that we aren't building a prop tree any longer there is no
		 * reason to do this. Instead we can just query for all props and all
		 * args and return them all.
		 */
		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"linkCount =", 0);

		Map<Long, Proposition> rootProps = new HashMap<Long, Proposition>();
		Nodes nodes = new Nodes();

		for (Proposition prop : propQuery) {
			rootProps.put(prop.id, prop);
			recursiveGetProps(prop, nodes);
		}

		AllPropsAndArgs propsAndArgs = new AllPropsAndArgs();
		propsAndArgs.rootProps = rootProps;
		propsAndArgs.nodes = nodes;
		return propsAndArgs;
	}

	/*
	 * public void printProps(Proposition[] props) { for (Proposition prop :
	 * props) { printPropRecursive(prop, 0); } }
	 */

	public String spaces(int spaces) {
		String string = "";
		for (int i = 0; i < spaces; i++) {
			string = string + " ";
		}
		return string;
	}

	/*
	 * public void printPropRecursive(Proposition propParent, int level) {
	 * println(spaces(level) + propParent.toString()); for (Argument arg :
	 * propParent.args) { String printString = spaces(level + 1); if (arg.pro ==
	 * true) printString = printString + "pro - id:"; else printString =
	 * printString + "con - id:";
	 * 
	 * printString = printString + arg.id; println(printString); for
	 * (Proposition prop : arg.props) { printPropRecursive(prop, level + 2); } }
	 * }
	 */

	public void recursiveGetProps(Proposition prop, Nodes nodes) {

		/* get all the prop's arguments */
		Map<Long, Argument> argMap = ofy.get(Argument.class, prop.argIDs);
		/* for each argument */
		for (Argument arg : argMap.values()) {
			/* if it hasn't yet been added to the map */
			if (!nodes.args.containsKey(arg.id)) {
				/* add it */
				nodes.args.put(arg.id, arg);
				/* add it's children */
				recursiveGetArgs(arg, nodes);
			}
		}
	}

	public void recursiveGetArgs(Argument arg, Nodes nodes) {
		/* get all the props in the argument */
		Map<Long, Proposition> propMap = ofy
				.get(Proposition.class, arg.propIDs);
		for (Proposition prop : propMap.values()) {
			/* if it hasn't yet been added to the map */
			if (!nodes.props.containsKey(prop.id)) {
				/* add it */
				nodes.props.put(prop.id, prop);
				/* add it's children */
				recursiveGetProps(prop, nodes);
			}
		}
	}

	@Override
	public Long addProposition(Long parentArgID, int position, String content)
			throws Exception {

		//println("addProposition(): parentArgID:"+parentArgID+"; position:"+position+"; content:"+content);
		Proposition newProposition = new Proposition();
		newProposition.content = content;
		Argument parentArg = null;

		if (parentArgID != null) {
			// exception will be generated if there is a bogus parentArgID
			parentArg = ofy.get(Argument.class, parentArgID);
			newProposition.linkCount = 1;
			ofy.put(newProposition);
			parentArg.propIDs.add(position, newProposition.id);

			ofy.put(parentArg);
		} else {
			newProposition.linkCount = 0;
			ofy.put(newProposition);
		}

		Change change = new Change(ChangeType.PROP_ADDITION);
		change.propID = newProposition.id;
		change.argID = parentArgID;
		saveVersionInfo(change);

		return newProposition.id;

	}
	
	@Override
	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws Exception {
		Argument argument = ofy.get(Argument.class, parentArgID);
		Proposition proposition = ofy.get(Proposition.class, propositionID);
	
		argument.propIDs.add(position, propositionID);
		ofy.put(argument);
		proposition.linkCount++;
		ofy.put(proposition);
	
		Change change = new Change(ChangeType.PROP_LINK);
		change.argID = argument.id;
		change.propID = proposition.id;
		saveVersionInfo(change);
	}

	// TODO
	@Override
	public void deleteProposition(Long propID) throws Exception {
		if (ofy.query(Argument.class).filter("aboutPropID", propID).countAll() != 0) {
			throw new Exception(
					"cannot delete proposition with arguments; delete arguments first");
		}
	
		/* first get the stuff that we'll need for version control */
		Change change = new Change(ChangeType.PROP_DELETION);
		Proposition prop = ofy.get(Proposition.class, propID);
		change.propID = prop.id;
		change.content = prop.content;
		change.propLinkCount = prop.linkCount;
	
		/* get all the arguments that use this proposition */
		Query<Argument> query = ofy.query(Argument.class).filter("propIDs",
				propID);
	
		/* only delete a proposition that is used in 1 or fewer arguments */
		if (query.countAll() > 1) {
			throw new Exception(
					"cannot delete a proposition used by more than one argument; delink from other arguments before deleting");
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
			ofy.put(argument);
		}
	
		/* delete the proposition */
		ofy.delete(Proposition.class, propID);
	
		/* save the version control information */
		saveVersionInfo(change);
	
	}

	/* the below functions are not yet used anywhere... consider deleting them */
	
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
		ofy.put(argument);
	
		proposition.linkCount--;
		ofy.put(proposition);
	
		Change change = new Change(ChangeType.PROP_UNLINK);
		change.argID = parentArgID;
		change.propID = proposition.id;
		change.argPropIndex = propIndex;
		change.content = proposition.content;
		saveVersionInfo(change);
	}

	@Override
	public void updateProposition(Long propID, String content) throws Exception {
		println("propID:" + propID + "; content:" + content);
	
		Change change = new Change(ChangeType.PROP_MODIFICATION);
		Proposition prop = ofy.get(Proposition.class, propID);
		if (prop.getContent() != null
				&& prop.getContent().trim().equals(content.trim())) {
			throw new Exception(
					"Cannot update proposition content:  content not changed!");
		}
	
		change.propID = prop.id;
		change.content = prop.content;
		// TODO is this line necessary for some reason?
		// change.propTopLevel = prop.topLevel;
	
		/*
		 * have to save the version info before the proposition value is changed
		 * (or alternatively, create a new Proposition)
		 */
		saveVersionInfo(change);
	
		prop.setContent(content.trim());
		prop.tokens = getTokensForIndexingOrQuery(content, 30);
		ofy.put(prop);
	
	}
	
	public void deleteArgument( Long argID ) throws Exception {
		Argument argument = ofy.get(Argument.class, argID );
		if( ! argument.propIDs.isEmpty() ){
			throw new Exception("Cannot delete an argumet that still has child propositions.  First remove child propositions.");
		}
		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"argIDs", argID);
		Proposition parentProp = propQuery.iterator().next();

		Change argDeletionChange = new Change(ChangeType.ARG_DELETION);
		argDeletionChange.propID = parentProp.id;
		argDeletionChange.argID = argument.id;
		argDeletionChange.argPro = argument.pro;
		argDeletionChange.argPropIndex = parentProp.argIDs.indexOf( argument.id );
		/*
		 * because we are only deleting arguments when they have no
		 * propositions left, we don't have to save the argument's
		 * proposition list
		 */
		parentProp.argIDs.remove(argument.id);
		
		ofy.put(parentProp);
		ofy.delete(argument);
		saveVersionInfo(argDeletionChange);
	}

	@Override
	public Argument addArgument(Long parentPropID, boolean pro)
			throws Exception {
	
		/*
		 * trigger an exception if the ID is invalid to prevent inconsistent
		 * datastore states
		 */
		Proposition parentProp = ofy.get(Proposition.class, parentPropID);
	
		//Proposition newProp = new Proposition();
		//newProp.linkCount = 1;
		//ofy.put(newProp);
	
		Argument newArg = new Argument();
		//newArg.propIDs.add(0, newProp.id);
	
		newArg.pro = pro;
		ofy.put(newArg);
	
		parentProp.argIDs.add(newArg.id);
		ofy.put(parentProp);
	
		Change change = new Change(ChangeType.ARG_ADDITION);
		change.argID = newArg.id;
		change.propID = parentPropID;
		saveVersionInfo(change);
	
		return newArg;
	}

	@Override
	public void updateArgument(Long argID, String content) throws Exception {
		Change change = new Change(ChangeType.ARG_MODIFICATION);
		Argument arg = ofy.get(Argument.class, argID);
		if (arg.title != null && arg.title.trim().equals(content.trim())) {
			throw new Exception(
					"Cannot update argument title:  title not changed!");
		}
	
		change.argID = arg.id;
		change.content = arg.title;
		// TODO is this line necessary for some reason?
		// change.propTopLevel = prop.topLevel;
	
		/*
		 * have to save the version info before the proposition value is changed
		 * (or alternatively, create a new Proposition)
		 */
		saveVersionInfo(change);
	
		arg.title = content.trim();
		ofy.put(arg);
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
	public NodeChangesMaps getChanges(List<Long> propIDs, List<Long> argIDs)
			throws Exception {

		NodeChangesMaps nodesChanges = new NodeChangesMaps();
		try {
			for (Long propID : propIDs) {
				recursiveGetPropChanges(propID, nodesChanges);
			}
			for (Long argID : argIDs) {
				recursiveGetArgChanges(argID, nodesChanges);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nodesChanges;
	}
	
	
	@Override
	public List<PropWithChanges> getPropositionsWithChanges(List<Long> propIDs) throws Exception{
		List<PropWithChanges> list = new ArrayList<PropWithChanges>();
		for( Long propID : propIDs ){
			PropWithChanges propWithChanges = new PropWithChanges();
			propWithChanges.proposition = ofy.get(Proposition.class, propID);
			propWithChanges.nodeChanges = getPropChanges( propID );
			list.add(propWithChanges);
		}
		return list;
	}

	
	@Override
	public List<ArgWithChanges> getArgumentsWithChanges(List<Long> argIDs) throws Exception {
		List<ArgWithChanges> list = new ArrayList<ArgWithChanges>();
		for( Long argID : argIDs ){
		ArgWithChanges argWithChanges = new ArgWithChanges();
		argWithChanges.argument = ofy.get(Argument.class, argID );
		argWithChanges.nodeChanges = getArgChanges( argID );
		}
		return list;
	}

	@Override
	public NodesWithHistory getPropositionCurrentVersionAndHistory(Long propID)
			throws Exception {
		return getPropOrArgCurrentVersionAndHistory(propID, null);
	}

	@Override
	public NodesWithHistory getArgumentCurrentVersionAndHistory(Long argID)
			throws Exception {
		return getPropOrArgCurrentVersionAndHistory(null, argID);
	}

	/* warning: I commented out critical code so this function will not 
	 * work as expected... it's meant to be deleted soon
	 */
	private NodesWithHistory getPropOrArgCurrentVersionAndHistory(Long propID,
			Long argID) throws Exception {
		println("start getPropositionCurrentVersionAndHistory()");
		NodesWithHistory versions = new NodesWithHistory();
		versions.nodes = new Nodes();
		try {
			if (propID != null && argID == null) {
				Proposition proposition = ofy.get(Proposition.class, propID);
				versions.nodes.props.put(proposition.id, proposition);
				recursiveGetProps(proposition, versions.nodes);
			}
			if (argID != null && propID == null) {
				Argument argument = ofy.get(Argument.class, argID);
				versions.nodes.args.put(argument.id, argument);
				recursiveGetArgs(argument, versions.nodes);
			} else {
				throw new Exception(
						"getPropOrArgCurrentVersionAndHistory: Only one non-null value accepted");
			}
		} catch (EntityNotFoundException e) {
			/*
			 * if the prop is not found that merely means it doesn't exist in
			 * the current version of the tree... not a problem since it might
			 * have been deleted. In that case we don't need to look for its
			 * children either because they have also been deleted.
			 */
		}

		/* I deleted the get revisions method so this is throwing an error
		versions.changes = getRevisions(null, new ArrayList<Long>(
				versions.nodes.props.keySet()), new ArrayList<Long>(
				versions.nodes.args.keySet()));
				*/

		println("end start getPropositionCurrentVersionAndHistory()");
		return versions;
	}

	public void recursiveGetPropChanges(Long propID,
			NodeChangesMaps nodeChangesMaps) {
		if (nodeChangesMaps.propChanges.containsKey(propID)) {
			return;
		}

		NodeChanges nodeChanges = getPropChanges( propID );
		nodeChangesMaps.propChanges.put(propID, nodeChanges);
		
		for( Long id : nodeChanges.deletedChildIDs ){
			recursiveGetArgChanges(id, nodeChangesMaps);
		}
	}
	
	public NodeChanges getPropChanges( Long propID ){
		NodeChanges nodeChanges = new NodeChanges();
		Query<Change> query = ofy.query(Change.class).filter("propID = ",
				propID);
		for (Change change : query) {
			switch (change.changeType) {
			case ARG_DELETION:
				nodeChanges.deletedChildIDs.add(change.argID);
			case ARG_ADDITION:
			case PROP_MODIFICATION:
				nodeChanges.changes.add(change);
				break;
			case ARG_MODIFICATION:
				// this case should never happen
			case PROP_DELETION:
			case PROP_ADDITION:
			case PROP_LINK:
			case PROP_UNLINK:
				// do nothing
				break;
			}
		}
		return nodeChanges;
	}

	public void recursiveGetArgChanges(Long argID,
			NodeChangesMaps nodeChangesMaps) {
		if (nodeChangesMaps.argChanges.containsKey(argID)) {
			return;
		}

		NodeChanges nodeChanges = getArgChanges( argID );
		nodeChangesMaps.argChanges.put(argID, nodeChanges);
		
		for( Long id : nodeChanges.deletedChildIDs ){
			recursiveGetPropChanges( id, nodeChangesMaps);
		}
	}
	
	public NodeChanges getArgChanges( Long argID ){
		NodeChanges nodeChanges = new NodeChanges();
		Query<Change> query = ofy.query(Change.class).filter("argID = ", argID);
		for (Change change : query) {
			switch (change.changeType) {
			case PROP_UNLINK:
			case PROP_DELETION:
				nodeChanges.deletedChildIDs.add(change.propID);
			case ARG_MODIFICATION:
			case PROP_ADDITION:
			case PROP_LINK:
				nodeChanges.changes.add(change);
				break;
			case PROP_MODIFICATION:
				// this case should never happen
			case ARG_DELETION:
			case ARG_ADDITION:
				// do nothing
				break;
			}
		}
		return nodeChanges;
	}


	public void recursiveQueryChanges(List<Long> propIDs, List<Long> argIDs,
			Map<Date, Change> map, Date date) {
		//TODO: DELETE THIS METHOD
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

	@Override
	public List<Proposition> searchPropositions(String string, Long filterArgID)
			throws Exception {
		// System.out.println("start:  searchPropositions");
		Set<String> tokens = getTokensForIndexingOrQuery(string, 5);
		/*
		 * String printString = ""; for (String str : tokens) { printString =
		 * printString + " " + str; } System.out.println("searching for: " +
		 * printString);
		 */
		Query<Proposition> query = ofy.query(Proposition.class);

		for (String token : tokens) {
			query.filter("tokens", token);
		}

		Argument filterArg = null;
		if (filterArgID != null) {
			filterArg = ofy.get(Argument.class, filterArgID);
		}

		List<Proposition> results = new LinkedList<Proposition>();
		for (Proposition proposition : query) {
			if (filterArg != null && filterArg.propIDs.contains(proposition.id)) {
				continue;
			}
			results.add(proposition);
		}
		return results;
	}

	/**
	 * Uses english stemming (snowball + lucene) + stopwords for getting the
	 * words.
	 * 
	 * @param index
	 * @return
	 */
	public static Set<String> getTokensForIndexingOrQuery(String index_raw,
			int maximumNumberOfTokensToReturn) {

		String indexCleanedOfHTMLTags = index_raw.replaceAll("\\<.*?>", " ");

		Set<String> returnSet = new HashSet<String>();

		try {

			Analyzer analyzer = new SnowballAnalyzer(
					org.apache.lucene.util.Version.LUCENE_30, "English",
					StopAnalyzer.ENGLISH_STOP_WORDS_SET);

			TokenStream tokenStream = analyzer.tokenStream("content",
					new StringReader(indexCleanedOfHTMLTags));

			while (tokenStream.incrementToken()
					&& (returnSet.size() < maximumNumberOfTokensToReturn)) {

				returnSet.add(tokenStream.getAttribute(TermAttribute.class)
						.term());

			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		return returnSet;

	}

	@Override
	public Nodes replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID) throws Exception {

		if (ofy.query(Argument.class).filter("aboutPropID", removePropID)
				.countAll() != 0) {
			throw new Exception(
					"cannot replace a proposition with a link to a second proposition when the first has arguments");
		}

		Argument parentArg = ofy.get(Argument.class, parentArgID);

		if (parentArg.propIDs.contains(linkPropID)) {
			throw new Exception(
					"cannot link to proposition in argument which already links to that proposition");
		}
		Proposition removeProp = ofy.get(Proposition.class, removePropID);
		Proposition linkProp = ofy.get(Proposition.class, linkPropID);
		int index = -1;
		if (parentArg != null) {
			index = parentArg.propIDs.indexOf(removePropID);
		}

		/* get all the arguments that use this proposition */
		Query<Argument> query = ofy.query(Argument.class).filter("propIDs",
				removePropID);

		/* only delete a proposition that is used in 1 or fewer arguments. */
		if (query.countAll() == 0) {
			Change change = new Change(ChangeType.PROP_DELETION);
			change.propID = removeProp.id;
			change.content = removeProp.content;
			change.propLinkCount = removeProp.linkCount;

			ofy.delete(removeProp);
			saveVersionInfo(change);
		}
		/*
		 * can't reuse the removeProposition function because don't want to
		 * delete the argument where it is empty.
		 */
		else if (query.countAll() == 1) {
			Change change = new Change(ChangeType.PROP_DELETION);
			change.propID = removeProp.id;
			change.content = removeProp.content;
			change.propLinkCount = removeProp.linkCount;
			change.argID = parentArgID;
			change.argPropIndex = index;

			parentArg.propIDs.remove(index);

			ofy.delete(removeProp);
			ofy.put(parentArg);
			saveVersionInfo(change);
		}
		/*
		 * if used in more than one argument, unlink instead. can't use the
		 * unlinkProposition function because don't want to delete the argument
		 * where it is empty.
		 */
		else if (query.countAll() > 1) {
			Change change = new Change(ChangeType.PROP_UNLINK);
			change.argID = parentArgID;
			change.propID = removePropID;
			change.argPropIndex = index;

			parentArg.propIDs.remove(index);

			removeProp.linkCount--;

			ofy.put(removeProp);
			ofy.put(parentArg);
			saveVersionInfo(change);
		}

		Change change = new Change(ChangeType.PROP_LINK);
		change.argID = parentArgID;
		change.propID = linkPropID;

		parentArg.propIDs.add(index, linkPropID);
		ofy.put(parentArg);
		linkProp.linkCount++;
		ofy.put(linkProp);
		saveVersionInfo(change);

		Nodes nodes = new Nodes();
		nodes.props.put(linkProp.id, linkProp);
		recursiveGetProps(linkProp, nodes);
		return nodes;
	}
}
