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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.argmap.client.ArgMapService;
import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.Change.ChangeType;
import org.argmap.client.Node;
import org.argmap.client.NodeChanges;
import org.argmap.client.Nodes;
import org.argmap.client.Proposition;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

public class ArgMapServiceImpl extends RemoteServiceServlet implements
		ArgMapService {

	static {
		ObjectifyService.register(Node.class);
		ObjectifyService.register(Proposition.class);
		ObjectifyService.register(Argument.class);
		ObjectifyService.register(Change.class);
	}
	private static final Logger log = Logger.getLogger(ArgMapServiceImpl.class
			.getName());

	private static final long serialVersionUID = 1L; // just to get rid of the
	// warnings...
	private final Objectify ofy = ObjectifyService.begin();

	private void println(String message) {
		log.fine(message);
	}

	@SuppressWarnings("unused")
	private void deleteAllArgsAndProps() {
		ofy.delete(ofy.query(Argument.class));
		ofy.delete(ofy.query(Proposition.class));
	}

	@SuppressWarnings("unused")
	private void printAllPropsAndArgs() {
		println("Arguments: ");
		for (Argument arg : ofy.query(Argument.class)) {
			printArgument(arg);
		}
		println("Propositions: ");
		for (Proposition prop : ofy.query(Proposition.class)) {
			printProposition(prop);
		}

	}

	private void printArgument(Argument arg) {
		println(arg.toString());
	}

	private void printProposition(Proposition prop) {
		println(prop.toString());
	}

	@Override
	public PropsAndArgs getPropsAndArgs(int depthLimit) {
		PropsAndArgs propsAndArgs = new PropsAndArgs();
		try {
			/*
			 * TODO: now that we aren't building a prop tree any longer there is
			 * no reason to do this. Instead we can just query for all props and
			 * all args and return them all.
			 */
			Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
					"linkCount =", 0);

			List<Proposition> rootProps = propQuery.list();
			Nodes nodes = new Nodes();

			for (Proposition prop : rootProps) {
				recursiveGetProps(prop, nodes, depthLimit);
			}
			propsAndArgs.rootProps = rootProps;
			propsAndArgs.nodes = nodes;

		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
		}
		return propsAndArgs;
	}

	private void recursiveGetProps(Proposition prop, Nodes nodes, int depthLimit) {
		if (depthLimit == 0) {
			return;
		}

		/* get all the prop's arguments */
		Map<Long, Argument> argMap = ofy.get(Argument.class, prop.childIDs);
		/* for each argument */
		for (Argument arg : argMap.values()) {
			/* if it hasn't yet been added to the map */
			if (!nodes.args.containsKey(arg.id)) {
				/* add it */
				nodes.args.put(arg.id, arg);
				/* add it's children */
				recursiveGetArgs(arg, nodes, depthLimit - 1);
			}
		}
	}

	private void recursiveGetArgs(Argument arg, Nodes nodes, int depthLimit) {
		if (depthLimit == 0) {
			return;
		}

		/* get all the props in the argument */
		Map<Long, Proposition> propMap = ofy.get(Proposition.class,
				arg.childIDs);
		for (Proposition prop : propMap.values()) {
			/* if it hasn't yet been added to the map */
			if (!nodes.props.containsKey(prop.id)) {
				/* add it */
				nodes.props.put(prop.id, prop);
				/* add it's descendants */
				recursiveGetProps(prop, nodes, depthLimit - 1);
			}
		}
	}

	@Override
	public Long addProp(Long parentArgID, int position, String content)
			throws Exception {
		try {

			// println("addProposition(): parentArgID:"+parentArgID+"; position:"+position+"; content:"+content);
			Proposition newProposition = new Proposition();
			newProposition.content = content;
			newProposition.tokens = getTokensForIndexingOrQuery(content, 30);
			Argument parentArg = null;

			if (parentArgID != null) {
				// exception will be generated if there is a bogus parentArgID
				parentArg = ofy.get(Argument.class, parentArgID);
				newProposition.linkCount = 1;
				ofy.put(newProposition);
				parentArg.childIDs.add(position, newProposition.id);

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

		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws Exception {
		try {
			Argument argument = ofy.get(Argument.class, parentArgID);
			Proposition proposition = ofy.get(Proposition.class, propositionID);

			argument.childIDs.add(position, propositionID);
			ofy.put(argument);
			proposition.linkCount++;
			ofy.put(proposition);

			Change change = new Change(ChangeType.PROP_LINK);
			change.argID = argument.id;
			change.propID = proposition.id;
			saveVersionInfo(change);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public void deleteProp(Long propID) throws Exception {
		try {
			/* first get the stuff that we'll need for version control */
			Change change = new Change(ChangeType.PROP_DELETION);
			Proposition prop = ofy.get(Proposition.class, propID);
			if (!prop.childIDs.isEmpty()) {
				throw new Exception(
						"cannot delete proposition with arguments; delete arguments first");
			}
			change.propID = prop.id;
			change.content = prop.content;
			change.propLinkCount = prop.linkCount;

			/* get all the arguments that use this proposition */
			Query<Argument> query = ofy.query(Argument.class).filter(
					"childIDs", propID);

			/* only delete a proposition that is used in 1 or fewer arguments */
			if (query.count() > 1) {
				throw new Exception(
						"cannot delete a proposition used by more than one argument; delink from other arguments before deleting");
			}

			/* if the proposition is used in an argument */
			else if (query.count() == 1) {
				Argument argument = query.iterator().next();

				/* record the versioning information */
				change.argID = argument.id;
				change.argPropIndex = argument.childIDs.indexOf(propID);
				// change.argPro = argument.pro;

				/* remove the proposition from the argument */
				argument.childIDs.remove(propID);
				ofy.put(argument);
			}

			/* delete the proposition */
			ofy.delete(Proposition.class, propID);

			/* save the version control information */
			saveVersionInfo(change);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}

	}

	/* the below functions are not yet used anywhere... consider deleting them */

	@Override
	public void unlinkProp(Long parentArgID, Long propositionID)
			throws Exception {
		try {
			/* this will throw an exception if the argument doesn't exist */
			Argument argument = ofy.get(Argument.class, parentArgID);
			/* this will throw an exception if the prop doesn't exist */
			Proposition proposition = ofy.get(Proposition.class, propositionID);

			int propIndex = argument.childIDs.indexOf(propositionID);
			if (propIndex == -1) {
				throw new Exception(
						"cannot unlink proposition from argument:  proposition not part of argument");
			}

			argument.childIDs.remove(propositionID);
			ofy.put(argument);

			proposition.linkCount--;
			ofy.put(proposition);

			Change change = new Change(ChangeType.PROP_UNLINK);
			change.argID = parentArgID;
			change.propID = proposition.id;
			change.argPropIndex = propIndex;
			change.content = proposition.content;
			saveVersionInfo(change);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public void updateProp(Long propID, String content) throws Exception {
		try {
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
			 * have to save the version info before the proposition value is
			 * changed (or alternatively, create a new Proposition)
			 */
			saveVersionInfo(change);

			prop.setContent(content.trim());
			prop.tokens = getTokensForIndexingOrQuery(content, 30);
			ofy.put(prop);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}

	}

	public void deleteArg(Long argID) throws Exception {
		try {
			Argument argument = ofy.get(Argument.class, argID);
			if (!argument.childIDs.isEmpty()) {
				throw new Exception(
						"Cannot delete an argumet that still has child propositions.  First remove child propositions.");
			}
			Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
					"childIDs", argID);
			Proposition parentProp = propQuery.iterator().next();

			Change argDeletionChange = new Change(ChangeType.ARG_DELETION);
			argDeletionChange.propID = parentProp.id;
			argDeletionChange.argID = argument.id;
			argDeletionChange.argPro = argument.pro;
			argDeletionChange.argPropIndex = parentProp.childIDs
					.indexOf(argument.id);
			/*
			 * because we are only deleting arguments when they have no
			 * propositions left, we don't have to save the argument's
			 * proposition list
			 */
			parentProp.childIDs.remove(argument.id);

			ofy.put(parentProp);
			ofy.delete(argument);
			saveVersionInfo(argDeletionChange);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public Argument addArg(Long parentPropID, boolean pro) throws Exception {
		try {

			/*
			 * trigger an exception if the ID is invalid to prevent inconsistent
			 * datastore states
			 */
			Proposition parentProp = ofy.get(Proposition.class, parentPropID);

			// Proposition newProp = new Proposition();
			// newProp.linkCount = 1;
			// ofy.put(newProp);

			Argument newArg = new Argument();
			// newArg.propIDs.add(0, newProp.id);

			newArg.pro = pro;
			ofy.put(newArg);

			parentProp.childIDs.add(newArg.id);
			ofy.put(parentProp);

			Change change = new Change(ChangeType.ARG_ADDITION);
			change.argID = newArg.id;
			change.propID = parentPropID;
			saveVersionInfo(change);

			return newArg;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public void updateArg(Long argID, String content) throws Exception {
		try {
			Change change = new Change(ChangeType.ARG_MODIFICATION);
			Argument arg = ofy.get(Argument.class, argID);
			if (arg.content != null
					&& arg.content.trim().equals(content.trim())) {
				throw new Exception(
						"Cannot update argument title:  title not changed!");
			}

			change.argID = arg.id;
			change.content = arg.content;
			// TODO is this line necessary for some reason?
			// change.propTopLevel = prop.topLevel;

			/*
			 * have to save the version info before the proposition value is
			 * changed (or alternatively, create a new Proposition)
			 */
			saveVersionInfo(change);

			arg.content = content.trim();
			ofy.put(arg);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	private void saveVersionInfo(Change change) {
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
		try {
			NodeChangesMaps nodesChanges = new NodeChangesMaps();

			for (Long propID : propIDs) {
				recursiveGetPropChanges(propID, nodesChanges);
			}
			for (Long argID : argIDs) {
				recursiveGetArgChanges(argID, nodesChanges);
			}

			return nodesChanges;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public Map<Long, NodeWithChanges> getPropsWithChanges(List<Long> propIDs)
			throws Exception {
		try {
			Map<Long, NodeWithChanges> map = new HashMap<Long, NodeWithChanges>();
			for (Long propID : propIDs) {
				NodeWithChanges propWithChanges = new NodeWithChanges();
				try {
					propWithChanges.node = ofy.get(Proposition.class, propID);
				} catch (NotFoundException e) {
					/*
					 * if the node doesn't currently exist because it has been
					 * deleted, just return null
					 */
					propWithChanges.node = null;
				}
				propWithChanges.nodeChanges = getPropChanges(propID);
				map.put(propID, propWithChanges);
			}
			return map;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public Map<Long, NodeWithChanges> getArgsWithChanges(List<Long> argIDs)
			throws Exception {
		try {
			Map<Long, NodeWithChanges> map = new HashMap<Long, NodeWithChanges>();
			for (Long argID : argIDs) {
				NodeWithChanges argWithChanges = new NodeWithChanges();
				try {
					argWithChanges.node = ofy.get(Argument.class, argID);
				} catch (NotFoundException e) {
					/*
					 * if the node doesn't currently exist because it has been
					 * deleted, just return null
					 */
					argWithChanges.node = null;
				}
				argWithChanges.nodeChanges = getArgChanges(argID,
						argWithChanges.unlinkedLinks);
				map.put(argID, argWithChanges);
			}
			return map;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	// @Override
	// public NodesWithHistory getPropCurrentVersionAndHistory(Long propID)
	// throws Exception {
	// try {
	// return getPropOrArgCurrentVersionAndHistory(propID, null);
	// } catch (Exception e) {
	// log.log(Level.SEVERE, "Uncaught exception", e);
	// throw e;
	// }
	// }

	// @Override
	// public NodesWithHistory getArgCurrentVersionAndHistory(Long argID)
	// throws Exception {
	// try {
	// return getPropOrArgCurrentVersionAndHistory(null, argID);
	// } catch (Exception e) {
	// log.log(Level.SEVERE, "Uncaught exception", e);
	// throw e;
	// }
	// }

	/*
	 * warning: I commented out critical code so this function will not work as
	 * expected... it's meant to be deleted soon
	 */
	// private NodesWithHistory getPropOrArgCurrentVersionAndHistory(Long
	// propID,
	// Long argID) throws Exception {
	// try {
	// println("start getPropositionCurrentVersionAndHistory()");
	// NodesWithHistory versions = new NodesWithHistory();
	// versions.nodes = new Nodes();
	// try {
	// if (propID != null && argID == null) {
	// Proposition proposition = ofy
	// .get(Proposition.class, propID);
	// versions.nodes.props.put(proposition.id, proposition);
	// recursiveGetProps(proposition, versions.nodes);
	// }
	// if (argID != null && propID == null) {
	// Argument argument = ofy.get(Argument.class, argID);
	// versions.nodes.args.put(argument.id, argument);
	// recursiveGetArgs(argument, versions.nodes);
	// } else {
	// throw new Exception(
	// "getPropOrArgCurrentVersionAndHistory: Only one non-null value accepted");
	// }
	// } catch (EntityNotFoundException e) {
	// /*
	// * if the prop is not found that merely means it doesn't exist
	// * in the current version of the tree... not a problem since it
	// * might have been deleted. In that case we don't need to look
	// * for its children either because they have also been deleted.
	// */
	// }
	//
	// /*
	// * I deleted the get revisions method so this is throwing an error
	// * versions.changes = getRevisions(null, new ArrayList<Long>(
	// * versions.nodes.props.keySet()), new ArrayList<Long>(
	// * versions.nodes.args.keySet()));
	// */
	//
	// println("end start getPropositionCurrentVersionAndHistory()");
	// return versions;
	// } catch (Exception e) {
	// log.log(Level.SEVERE, "Uncaught exception", e);
	// throw e;
	// }
	// }

	private void recursiveGetPropChanges(Long propID,
			NodeChangesMaps nodeChangesMaps) throws Exception {
		if (nodeChangesMaps.propChanges.containsKey(propID)) {
			return;
		}

		NodeChanges nodeChanges = getPropChanges(propID);
		nodeChangesMaps.propChanges.put(propID, nodeChanges);

		for (Long id : nodeChanges.deletedChildIDs) {
			recursiveGetArgChanges(id, nodeChangesMaps);
		}
	}

	private NodeChanges getPropChanges(Long propID) throws Exception {
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

	private void recursiveGetArgChanges(Long argID,
			NodeChangesMaps nodeChangesMaps) throws Exception {
		if (nodeChangesMaps.argChanges.containsKey(argID)) {
			return;
		}

		NodeChanges nodeChanges = getArgChanges(argID,
				nodeChangesMaps.unlinkedLinks);
		nodeChangesMaps.argChanges.put(argID, nodeChanges);

		for (Long id : nodeChanges.deletedChildIDs) {
			recursiveGetPropChanges(id, nodeChangesMaps);
		}
	}

	private NodeChanges getArgChanges(Long argID,
			Map<Long, Proposition> unlinkedLinks) throws Exception {
		NodeChanges nodeChanges = new NodeChanges();
		Query<Change> query = ofy.query(Change.class).filter("argID = ", argID);
		for (Change change : query) {
			switch (change.changeType) {
			case PROP_UNLINK:
				if (!unlinkedLinks.containsKey(change.propID)) {
					unlinkedLinks.put(change.propID,
							ofy.get(Proposition.class, change.propID));
				}
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

	@SuppressWarnings("unused")
	private void printAllChanges() {
		println("");
		for (Change change : ofy.query(Change.class))
			println("" + change.toString());
	}

	@Override
	public PropsAndArgs searchProps(String string, Long filterArgID, Long filterPropID)
			throws Exception {
		try {
			Set<String> tokenSet = getTokensForIndexingOrQuery(string, 5);
			List<String> tokens = new ArrayList<String>(tokenSet);
			if (tokens.isEmpty()) {
				return getPropsAndArgs(0);
			}
			Argument filterArg = null;
			if (filterArgID != null) {
				filterArg = ofy.get(Argument.class, filterArgID);
			}
			List<Proposition> results = new LinkedList<Proposition>();
			Set<Long> duplicateFilter = new HashSet<Long>();

			/*
			 * this runs through all the possible combinations of tokens that
			 * can be searched for (eventually it should stop when it gets a
			 * certain number or has spent a certain amount of time searching,
			 * and let the client re-request to continue where it left off if
			 * the client wants more results...or wants to continue waiting...).
			 * It starts by searching for all the tokens, and then searches for
			 * each combination of one fewer than all the tokens, then for two
			 * fewer, and so forth until it gets to just one term.
			 */
			int comboCount = 0;
			int[] combination;
			for (int i = tokens.size(); i > 0; i--) {
				CombinationGenerator x = new CombinationGenerator(
						tokens.size(), i);
				while (x.hasMore()) {
					comboCount++;
					combination = x.getNext();
					Query<Proposition> query = ofy.query(Proposition.class);
					for (int j = 0; j < combination.length; j++) {
						query.filter("tokens", tokens.get(combination[j]));
					}
					for (Proposition proposition : query) {
						if ((filterArg != null
								&& filterArg.childIDs.contains(proposition.id)) ||
								proposition.id.equals(filterPropID) ) {
							continue;
						}
						if (duplicateFilter.add(proposition.id)) {
							results.add(proposition);
						}
					}
				}
			}
			log.fine("comboCount: " + comboCount);

			PropsAndArgs propsAndArgs = new PropsAndArgs();
			propsAndArgs.rootProps = results;
			propsAndArgs.nodes = new Nodes();

			return propsAndArgs;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	public PropsAndArgs searchProps_OLD(String string, Long filterArgID)
			throws Exception {
		try {
			// System.out.println("start:  searchPropositions");
			Set<String> tokens = getTokensForIndexingOrQuery(string, 5);
			if (tokens.isEmpty()) {
				return getPropsAndArgs(0);
			}
			/*
			 * String printString = ""; for (String str : tokens) { printString
			 * = printString + " " + str; } System.out.println("searching for: "
			 * + printString);
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
				if (filterArg != null
						&& filterArg.childIDs.contains(proposition.id)) {
					continue;
				}
				results.add(proposition);
			}
			PropsAndArgs propsAndArgs = new PropsAndArgs();
			propsAndArgs.rootProps = results;
			propsAndArgs.nodes = new Nodes();

			return propsAndArgs;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
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

		Set<String> returnSet = new HashSet<String>();
		if (index_raw == null) {
			return returnSet;
		}
		String indexCleanedOfHTMLTags = index_raw.replaceAll("\\<.*?>", " ");

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
			log.log(Level.SEVERE, "Uncaught exception", e);
		}

		return returnSet;

	}

	@Override
	public Nodes replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID) throws Exception {
		try {
			Argument parentArg = ofy.get(Argument.class, parentArgID);

			if (parentArg.childIDs.contains(linkPropID)) {
				throw new Exception(
						"cannot link to proposition in argument which already links to that proposition");
			}
			Proposition removeProp = ofy.get(Proposition.class, removePropID);
			if (!removeProp.childIDs.isEmpty()) {
				throw new Exception(
						"cannot replace a proposition with a link to a second proposition when the first has arguments");
			}
			Proposition linkProp = ofy.get(Proposition.class, linkPropID);
			int index = -1;
			if (parentArg != null) {
				index = parentArg.childIDs.indexOf(removePropID);
			}

			/* get all the arguments that use this proposition */
			Query<Argument> query = ofy.query(Argument.class).filter(
					"childIDs", removePropID);

			/* only delete a proposition that is used in 1 or fewer arguments. */
			if (query.count() == 0) {
				Change change = new Change(ChangeType.PROP_DELETION);
				change.propID = removeProp.id;
				change.content = removeProp.content;
				change.propLinkCount = removeProp.linkCount;

				ofy.delete(removeProp);
				saveVersionInfo(change);
			}
			/*
			 * can't reuse the removeProposition function because don't want to
			 * delete the argument where it is empty. [just add the link before
			 * deleting then...]
			 */
			else if (query.count() == 1) {
				Change change = new Change(ChangeType.PROP_DELETION);
				change.propID = removeProp.id;
				change.content = removeProp.content;
				change.propLinkCount = removeProp.linkCount;
				change.argID = parentArgID;
				change.argPropIndex = index;

				parentArg.childIDs.remove(index);

				ofy.delete(removeProp);
				ofy.put(parentArg);
				saveVersionInfo(change);
			}
			/*
			 * if used in more than one argument, unlink instead. can't use the
			 * unlinkProposition function because don't want to delete the
			 * argument where it is empty.
			 */
			else if (query.count() > 1) {
				Change change = new Change(ChangeType.PROP_UNLINK);
				change.argID = parentArgID;
				change.propID = removePropID;
				change.argPropIndex = index;

				parentArg.childIDs.remove(index);

				removeProp.linkCount--;

				ofy.put(removeProp);
				ofy.put(parentArg);
				saveVersionInfo(change);
			}

			Change change = new Change(ChangeType.PROP_LINK);
			change.argID = parentArgID;
			change.propID = linkPropID;

			parentArg.childIDs.add(index, linkPropID);
			ofy.put(parentArg);
			linkProp.linkCount++;
			ofy.put(linkProp);
			saveVersionInfo(change);

			Nodes nodes = new Nodes();
			nodes.props.put(linkProp.id, linkProp);
			recursiveGetProps(linkProp, nodes, 100);
			return nodes;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

	@Override
	public void logClientException(String exceptionStr) {
		log.severe("CLIENT EXCEPTION (string)" + exceptionStr);
	}

	@Override
	public Nodes getNodesChildren(List<Long> nodeIDs, int depth)
			throws Exception {
		try {
			Nodes nodes = new Nodes();
			for (Long id : nodeIDs) {
				try {
					Argument arg = ofy.get(Argument.class, id);
					recursiveGetArgs(arg, nodes, depth);
				} catch (NotFoundException e) {
					Proposition prop = ofy.get(Proposition.class, id);
					recursiveGetProps(prop, nodes, depth);
				}
			}
			return nodes;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Uncaught exception", e);
			throw e;
		}
	}

}
