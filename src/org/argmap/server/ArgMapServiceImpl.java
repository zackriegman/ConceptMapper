package org.argmap.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.argmap.client.ArgMapService;
import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.Change.ChangeType;
import org.argmap.client.LoginInfo;
import org.argmap.client.Node;
import org.argmap.client.NodeChanges;
import org.argmap.client.Proposition;
import org.argmap.client.ServiceException;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.helper.Monotonic;

public class ArgMapServiceImpl extends RemoteServiceServlet implements
		ArgMapService {

	static {
		ObjectifyRegistrator.register();
	}
	private static final Logger log = Logger.getLogger(ArgMapServiceImpl.class
			.getName());

	private static final long serialVersionUID = 1L; // just to get rid of the
	// warnings...
	private final Objectify ofy = ObjectifyService.begin();

	private void logln(String message) {
		log.info(message);
	}

	@SuppressWarnings("unused")
	private void printAllPropsAndArgs() {
		logln("Arguments: ");
		for (Argument arg : ofy.query(Argument.class)) {
			printArgument(arg);
		}
		logln("Propositions: ");
		for (Proposition prop : ofy.query(Proposition.class)) {
			printProposition(prop);
		}

	}

	private void printArgument(Argument arg) {
		logln(arg.toString());
	}

	private void printProposition(Proposition prop) {
		logln(prop.toString());
	}

	@Override
	public PartialTrees getRootProps(int depthLimit) {

		PartialTrees propsAndArgs = new PartialTrees();
		Query<Proposition> propQuery = ofy.query(Proposition.class)
				.filter("root", true).order("-created").limit(30);

		List<Proposition> rootProps = propQuery.list();
		HashMap<Long, Node> nodes = new HashMap<Long, Node>();

		ArrayList<Long> rootIDs = new ArrayList<Long>();
		for (Proposition prop : rootProps) {
			rootIDs.add(prop.id);
			nodes.put(prop.id, prop);
			recursiveGetProps(prop, nodes, depthLimit);
		}
		propsAndArgs.rootIDs = rootIDs;
		propsAndArgs.nodes = nodes;

		return propsAndArgs;
	}

	private void recursiveGetProps(Proposition prop, Map<Long, Node> nodes,
			int depthLimit) {
		if (depthLimit == 0) {
			return;
		}

		/* get all the prop's arguments */
		Map<Long, Argument> argMap = ofy.get(Argument.class, prop.childIDs);
		/* for each argument */
		for (Argument arg : argMap.values()) {
			/* if it hasn't yet been added to the map */
			if (!nodes.containsKey(arg.id)) {
				/* add it */
				nodes.put(arg.id, arg);
				/* add it's children */
				recursiveGetArgs(arg, nodes, depthLimit - 1);
			}
		}
	}

	private void recursiveGetArgs(Argument arg, Map<Long, Node> nodes,
			int depthLimit) {
		if (depthLimit == 0) {
			return;
		}

		/* get all the props in the argument */
		Map<Long, Proposition> propMap = ofy.get(Proposition.class,
				arg.childIDs);
		for (Proposition prop : propMap.values()) {
			/* if it hasn't yet been added to the map */
			if (!nodes.containsKey(prop.id)) {
				/* add it */
				nodes.put(prop.id, prop);
				/* add it's descendants */
				recursiveGetProps(prop, nodes, depthLimit - 1);
			}
		}
	}

	@Override
	public Proposition addProp(Long parentArgID, int position, String content) {
		content = content.trim();

		// log.finest("addProp()");
		Proposition newProposition = new Proposition();
		newProposition.created = new Date();
		newProposition.content = content;
		newProposition.tokens = getTokensForIndexingOrQuery(content, 30);
		Argument parentArg = null;

		if (parentArgID != null) {
			// exception will be generated if there is a bogus parentArgID

			Lock lock = Lock.getNodeLock(parentArgID);
			try {
				lock.lock();
				parentArg = ofy.get(Argument.class, parentArgID);
				newProposition.linkCount = 1;
				putNode(newProposition);
				parentArg.childIDs.add(position, newProposition.id);

				putNode(parentArg);
			} finally {
				lock.unlock();
			}
		} else {
			newProposition.linkCount = 1;
			newProposition.root = true;
			putNode(newProposition);
		}

		Change change = getNewChange(ChangeType.PROP_ADDITION);
		change.propID = newProposition.id;
		change.argID = parentArgID;
		// TODO this line not needed any longer as we aren't doing forward
		// updates with changes
		change.argPropIndex = position;
		saveVersionInfo(change);

		return newProposition;
	}

	@Override
	public void linkProposition(Long parentArgID, int position,
			Long propositionID) {
		Lock parentLock = Lock.getNodeLock(parentArgID);
		Lock childLock = Lock.getNodeLock(propositionID);
		try {
			parentLock.lock();
			childLock.lock();
			Proposition proposition = ofy.get(Proposition.class, propositionID);
			Argument argument = ofy.get(Argument.class, parentArgID);

			argument.childIDs.add(position, propositionID);
			putNode(argument);
			proposition.linkCount++;
			putNode(proposition);
			Change change = getNewChange(ChangeType.PROP_LINK);
			change.argID = parentArgID;
			change.propID = proposition.id;
			// TODO this line not needed any longer as we aren't doing forward
			// updates with changes
			change.argPropIndex = position;
			saveVersionInfo(change);
		} finally {
			childLock.unlock();
			parentLock.unlock();
		}
	}

	/*
	 * TODO in this method I should also lock the proposition that is being
	 * deleted in case someone gets a copy of the proposition, after I get my
	 * copy, and then adds an argument to it before I delete my copy therebye
	 * creating a dangling argument and corrupting the database. The problem is
	 * that if my strategy for avoiding gridlock is to always lock the parent
	 * first and then the child, this method needs to be rewritten a little bit,
	 * and I don't feel like doing that at the moment.
	 */
	@Override
	public void deleteProp(Long propID) throws ServiceException {
		/* first get the stuff that we'll need for version control */

		Proposition prop = ofy.get(Proposition.class, propID);
		if (!prop.childIDs.isEmpty()) {
			throw new ServiceException(
					"cannot delete proposition with arguments; delete arguments first");
		}
		Change change;
		if (prop.root) {
			change = getNewChange(ChangeType.ROOT_PROP_DELETION);
		} else {
			change = getNewChange(ChangeType.PROP_DELETION);
		}
		change.propID = prop.id;
		change.oldContent = prop.content;
		change.propLinkCount = prop.linkCount;

		/* get all the arguments that use this proposition */
		Query<Argument> query = ofy.query(Argument.class).filter("childIDs",
				propID);

		/* only delete a proposition that is used in 1 or fewer arguments */
		if (query.count() > 1) {
			throw new ServiceException(
					"cannot delete a proposition used by more than one argument; unlink from other arguments before deleting");
		}

		/* if the proposition is used in an argument */
		else if (query.count() == 1) {
			Long argID = query.getKey().getId();
			Lock lock = Lock.getNodeLock(argID);
			try {
				lock.lock();
				Argument argument = query.get();

				/* record the versioning information */
				change.argID = argument.id;
				change.argPropIndex = argument.childIDs.indexOf(propID);
				// change.argPro = argument.pro;

				/* remove the proposition from the argument */
				argument.childIDs.remove(propID);
				boolean negated = argument.negatedChildIDs.remove(propID);
				change.propNegated = negated;
				putNode(argument);
			} finally {
				lock.unlock();
			}
		}

		/* delete the proposition */
		ofy.delete(Proposition.class, propID);

		/* save the version control information */
		saveVersionInfo(change);

	}

	@Override
	public void unlinkProp(Long parentArgID, Long propositionID)
			throws ServiceException {

		Lock parentLock = Lock.getNodeLock(parentArgID);
		Lock childLock = Lock.getNodeLock(propositionID);
		try {
			parentLock.lock();
			childLock.lock();
			/* this will throw an exception if the argument doesn't exist */
			Argument argument = ofy.get(Argument.class, parentArgID);
			/* this will throw an exception if the prop doesn't exist */
			Proposition proposition = ofy.get(Proposition.class, propositionID);

			int propIndex = argument.childIDs.indexOf(propositionID);
			if (propIndex == -1) {
				throw new ServiceException(
						"cannot unlink proposition from argument:  proposition not part of argument");
			}

			argument.childIDs.remove(propositionID);
			boolean negated = argument.negatedChildIDs.remove(propositionID);
			putNode(argument);

			proposition.linkCount--;
			putNode(proposition);

			Change change = getNewChange(ChangeType.PROP_UNLINK);
			change.argID = parentArgID;
			change.propID = proposition.id;
			change.argPropIndex = propIndex;
			change.oldContent = proposition.content;
			change.propNegated = negated;
			saveVersionInfo(change);
		} finally {
			parentLock.unlock();
			childLock.unlock();
		}
	}

	@Override
	public void updateProp(Long propID, String content) throws ServiceException {
		content = content.trim();
		logln("propID:" + propID + "; content:" + content);

		Change change = getNewChange(ChangeType.PROP_MODIFICATION);
		Lock lock = Lock.getNodeLock(propID);
		try {
			lock.lock();
			Proposition prop = ofy.get(Proposition.class, propID);
			if (prop.getContent() != null
					&& prop.getContent().trim().equals(content)) {
				throw new ServiceException(
						"Cannot update proposition content:  content not changed!");
			}

			change.propID = prop.id;
			change.oldContent = prop.content;
			// TODO is this line necessary for some reason?
			// change.propTopLevel = prop.topLevel;

			/*
			 * have to save the version info before the proposition value is
			 * changed (or alternatively, create a new Proposition)
			 */
			saveVersionInfo(change);

			prop.setContent(content);
			prop.tokens = getTokensForIndexingOrQuery(content, 30);
			putNode(prop);
		} finally {
			lock.unlock();
		}
	}

	public void deleteArg(Long argID) throws ServiceException {

		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"childIDs", argID);
		Lock parentLock = Lock.getNodeLock(propQuery.getKey().getId());
		Lock childLock = Lock.getNodeLock(argID);
		try {
			parentLock.lock();
			childLock.lock();

			Argument argument = ofy.get(Argument.class, argID);
			if (!argument.childIDs.isEmpty()) {
				throw new ServiceException(
						"Cannot delete an argumet that still has child propositions.  First remove child propositions.");
			}

			Proposition parentProp = propQuery.get();

			Change argDeletionChange = getNewChange(ChangeType.ARG_DELETION);
			argDeletionChange.propID = parentProp.id;
			argDeletionChange.argID = argument.id;
			argDeletionChange.argPro = argument.pro;
			argDeletionChange.oldContent = argument.content;
			argDeletionChange.argPropIndex = parentProp.childIDs
					.indexOf(argument.id);
			/*
			 * because we are only deleting arguments when they have no
			 * propositions left, we don't have to save the argument's
			 * proposition list
			 */
			parentProp.childIDs.remove(argument.id);

			putNode(parentProp);

			ofy.delete(argument);
			saveVersionInfo(argDeletionChange);
		} finally {
			parentLock.unlock();
			childLock.unlock();
		}
	}

	@Override
	public Argument addArg(Long parentPropID, boolean pro) {
		LapTimer timer = new LapTimer();
		Lock lock = Lock.getNodeLock(parentPropID);
		try {
			timer.lap("<<<<");
			lock.lock();
			timer.lap("????");
			/*
			 * trigger an exception if the ID is invalid to prevent inconsistent
			 * datastore states
			 */
			Proposition parentProp = ofy.get(Proposition.class, parentPropID);
			timer.lap("{{{{");
			// Proposition newProp = new Proposition();
			// newProp.linkCount = 1;
			// putNode(newProp);

			Argument newArg = new Argument();
			newArg.created = new Date();
			timer.lap("----");
			// newArg.propIDs.add(0, newProp.id);

			newArg.pro = pro;
			timer.lap("====");
			putNode(newArg);
			timer.lap("]]]]");
			parentProp.childIDs.add(newArg.id);

			timer.lap("\\\\");
			putNode(parentProp);
			timer.lap(";;;;");
			Change change = getNewChange(ChangeType.ARG_ADDITION);
			change.argID = newArg.id;
			change.propID = parentPropID;
			// TODO this line not needed any longer as we aren't doing forward
			// updates with changes
			change.argPropIndex = parentProp.childIDs.indexOf(newArg.id);
			saveVersionInfo(change);
			timer.lap("````");
			return newArg;
		} finally {
			lock.unlock();
			log.fine(timer.getRecord());
		}
	}

	@Override
	public void updateArg(Long argID, String content) throws ServiceException {
		content = content.trim();
		Lock lock = Lock.getNodeLock(argID);
		try {
			lock.lock();
			Change change = getNewChange(ChangeType.ARG_MODIFICATION);
			Argument arg = ofy.get(Argument.class, argID);
			if (arg.content != null && arg.content.trim().equals(content)) {
				throw new ServiceException(
						"Cannot update argument title:  title not changed!");
			}

			change.argID = arg.id;
			change.oldContent = arg.content;
			// TODO is this line necessary for some reason?
			// change.propTopLevel = prop.topLevel;

			/*
			 * have to save the version info before the proposition value is
			 * changed (or alternatively, create a new Proposition)
			 */
			saveVersionInfo(change);

			arg.content = content;
			putNode(arg);
		} finally {
			lock.unlock();
		}
	}

	private void saveVersionInfo(Change change) {
		HttpServletRequest request = getHttpServletRequest();

		change.date = new Date();
		change.remoteAddr = request.getRemoteAddr();
		change.remoteHost = request.getRemoteHost();
		change.remotePort = request.getRemotePort();
		change.remoteUser = request.getRemoteUser();
		try {
			change.sessionID = request.getSession().getId();
		} catch (java.lang.IllegalStateException e) {
			log.fine("no session information saved in Change object because there was no session found");
		}

		// logln("Change Logged -- " + change.toString());

		ofy.put(change);
	}

	public HttpServletRequest getHttpServletRequest() {
		return getThreadLocalRequest();
	}

	/*
	 * this sets a new Change's id; it's here (instead of in the Change class
	 * where it belongs) because it references a class not on the client and
	 * Changes are sent to the client.
	 */
	public Change getNewChange() {
		Change change = new Change();
		change.id = Monotonic.next(ofy, Change.class, "id");
		return change;
	}

	public Change getNewChange(ChangeType changeType) {
		Change change = getNewChange();
		change.changeType = changeType;
		return change;
	}

	@Override
	public NodeChangesMaps getChanges(ArrayList<Long> propIDs,
			ArrayList<Long> argIDs) {

		NodeChangesMaps nodesChanges = new NodeChangesMaps();

		for (Long propID : propIDs) {
			recursiveGetPropChanges(propID, nodesChanges);
		}
		for (Long argID : argIDs) {
			recursiveGetArgChanges(argID, nodesChanges);
		}

		return nodesChanges;
	}

	@Override
	public NodeChangesMapsAndRootChanges getChangesForDeletedRootProps() {
		NodeChangesMapsAndRootChanges nodeChanges = new NodeChangesMapsAndRootChanges();

		for (Change change : ofy.query(Change.class).filter("changeType",
				"ROOT_PROP_DELETION")) {
			recursiveGetPropChanges(change.propID, nodeChanges.nodeChangesMaps);
			nodeChanges.rootChanges.add(change);
		}

		return nodeChanges;
	}

	private void putNode(Node node) {
		/*
		 * every time a node is saved on the server this method should be called
		 * to update the 'updated' field, unless of course we don't want to
		 * update the 'updated' field for instance when updating the ratingSum.
		 * See Rating class for an example. So remember, if anything is added to
		 * this method in at least one place Nodes are updated without calling
		 * this method.
		 */

		node.updated = new Date();
		ofy.put(node);
	}

	/*
	 * TODO make sure lastUpdate is saved whenever a node is changed
	 */
	@Override
	public PartialTrees getUpToDateNodes(
			HashMap<Long, DateAndChildIDs> propsInfo,
			HashMap<Long, DateAndChildIDs> argsInfo) {
		// Map<Long, Node> results = new HashMap<Long, Node>();
		PartialTrees results = new PartialTrees();

		// logln("got request for these-\nprops:"
		// + Log.mapToString(propsInfo) + "\nargs:"
		// + Log.mapToString(argsInfo));

		Map<Long, Proposition> props = ofy.get(Proposition.class,
				propsInfo.keySet());
		Map<Long, Argument> args = ofy.get(Argument.class, argsInfo.keySet());

		for (Proposition prop : props.values()) {
			// logln("propID:" + prop.id + "; DateAndChildIDs:" +
			// propsInfo.get( prop.id ).toString() );
			if (!prop.updated.equals(propsInfo.get(prop.id).date)) {
				results.nodes.put(prop.id, prop);
				results.rootIDs.add(prop.id);
				for (Long childID : prop.childIDs) {
					if (!propsInfo.get(prop.id).childIDs.contains(childID)) {
						Argument child = ofy.get(Argument.class, childID);
						results.nodes.put(child.id, child);
						results.nodes.putAll(ofy.get(Proposition.class,
								child.childIDs));
					}
				}
			}
		}

		for (Argument arg : args.values()) {
			if (!arg.updated.equals(argsInfo.get(arg.id).date)) {
				results.nodes.put(arg.id, arg);
				results.rootIDs.add(arg.id);
				for (Long childID : arg.childIDs) {
					if (!argsInfo.get(arg.id).childIDs.contains(childID)) {
						Proposition child = ofy.get(Proposition.class, childID);
						results.nodes.put(child.id, child);
						results.nodes.putAll(ofy.get(Argument.class,
								child.childIDs));
					}
				}
			}
		}

		// logln("returning these nodes:" + Log.mapToString(results.nodes)
		// + "\nfor these updated root ids:"
		// + Log.listToString(results.rootIDs));
		Rating.prepWithRatings(results);

		return results;
	}

	@Override
	public Map<Long, NodeWithChanges> getPropsWithChanges(List<Long> propIDs) {

		Map<Long, NodeWithChanges> map = new HashMap<Long, NodeWithChanges>();
		for (Long propID : propIDs) {
			NodeWithChanges propWithChanges = new NodeWithChanges();

			/*
			 * we find() instead of get() because the node in question might not
			 * currently exist; it might have been deleted in which case
			 * returning null should be fine
			 */
			propWithChanges.node = ofy.find(Proposition.class, propID);

			propWithChanges.nodeChanges = getPropChanges(propID);
			map.put(propID, propWithChanges);
		}
		return map;
	}

	@Override
	public Map<Long, NodeWithChanges> getArgsWithChanges(List<Long> argIDs) {
		Map<Long, NodeWithChanges> map = new HashMap<Long, NodeWithChanges>();
		for (Long argID : argIDs) {
			NodeWithChanges argWithChanges = new NodeWithChanges();

			/*
			 * we find() instead of get() because the node in question might not
			 * currently exist; it might have been deleted in which case
			 * returning null should be fine
			 */
			argWithChanges.node = ofy.find(Argument.class, argID);

			argWithChanges.nodeChanges = getArgChanges(argID,
					argWithChanges.unlinkedLinks);
			map.put(argID, argWithChanges);
		}
		return map;
	}

	private void recursiveGetPropChanges(Long propID,
			NodeChangesMaps nodeChangesMaps) {
		if (nodeChangesMaps.propChanges.containsKey(propID)) {
			return;
		}

		NodeChanges nodeChanges = getPropChanges(propID);
		nodeChangesMaps.propChanges.put(propID, nodeChanges);

		for (Long id : nodeChanges.deletedChildIDs) {
			recursiveGetArgChanges(id, nodeChangesMaps);
		}
	}

	private NodeChanges getPropChanges(Long propID) {
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
			NodeChangesMaps nodeChangesMaps) {
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
			Map<Long, Proposition> unlinkedLinks) {
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
		logln("");
		for (Change change : ofy.query(Change.class))
			logln("" + change.toString());
	}

	@Override
	public PartialTrees searchProps(String searchString, String searchName,
			int resultLimit, ArrayList<Long> filterNodeIDs,
			Double percentTermsMatching) {
		logln("recieved request for search '" + searchName + "'");
		Set<String> tokenSet = getTokensForIndexingOrQuery(searchString, 6);
		if (tokenSet.isEmpty()) {
			PartialTrees result = new PartialTrees();
			/*
			 * set rootIDs to null as a signal to the client that not only where
			 * there no results for this particular combination but that all
			 * combinations have been exhausted (which is true because there are
			 * no tokens to search for (probably because the string contained
			 * only stop words
			 */
			result.rootIDs = null;
			return result;
		}

		Search search = new Search(ofy, tokenSet, resultLimit, filterNodeIDs,
				percentTermsMatching, getHttpServletRequest().getSession()
						.getId(), searchName);
		PartialTrees propsAndArgs = search.getBatch(ofy);
		// getHttpServletRequest().getSession().setAttribute(searchName,
		// search);
		// logln("saved search '" + searchName + "' in session '"
		// + getHttpServletRequest().getSession().getId() + "'");
		ofy.put(search);
		return propsAndArgs;
	}

	@Override
	public PartialTrees continueSearchProps(String searchName)
			throws ServiceException {
		Search search;
		try {
			// search = (Search) getHttpServletRequest().getSession()
			// .getAttribute(searchName);
			search = ofy.get(Search.class, Search.buildID(
					getHttpServletRequest().getSession().getId(), searchName));
			PartialTrees propsAndArgs = search.getBatch(ofy);
			// getHttpServletRequest().getSession().setAttribute(searchName,
			// search);
			ofy.put(search);
			logln("re-saved search '" + searchName + "' for session '"
					+ getHttpServletRequest().getSession().getId() + "'");
			return propsAndArgs;
		} catch (NullPointerException e) {
			StringBuilder sb = new StringBuilder();
			sb.append("failed to retrieve '" + searchName + "' from session");
			if (getHttpServletRequest() == null) {
				sb.append("\ngetHttpServletRequest() == null");
			} else if (getHttpServletRequest().getSession() == null) {
				sb.append("\ngetHttpServletRequest().getSession() == null");
			} else if (getHttpServletRequest().getSession().getAttribute(
					searchName) == null) {
				sb.append(" with id '"
						+ getHttpServletRequest().getSession().getId() + "'");
				sb.append("\ngetHttpServletRequest().getSession().getAttribute(searchName) == null");
			}
			log.severe(sb.toString());
			throw new ServiceException(sb.toString());
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
			throw new RuntimeException(e);
		}

		return returnSet;

	}

	/*
	 * TODO consider whether locking three nodes at once could lead to gridlock
	 * somehow. if so, one possible solution would be to remove this method and
	 * replace it with a link method, and let the client handle putting together
	 * the two steps of deleting and linking.
	 */
	@Override
	public PartialTrees replaceWithLinkAndGet(Long parentArgID,
			Long linkPropID, Long removePropID, boolean negated)
			throws ServiceException {
		Lock parentLock = Lock.getNodeLock(parentArgID);
		Lock oldChildLock = Lock.getNodeLock(removePropID);
		Lock newChildLock = Lock.getNodeLock(linkPropID);
		try {
			parentLock.lock();
			oldChildLock.lock();
			newChildLock.lock();

			Argument parentArg = ofy.get(Argument.class, parentArgID);

			if (parentArg.childIDs.contains(linkPropID)) {
				throw new ServiceException(
						"cannot link to proposition in argument which already links to that proposition");
			}
			Proposition removeProp = ofy.get(Proposition.class, removePropID);
			if (!removeProp.childIDs.isEmpty()) {
				throw new ServiceException(
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
				Change change = getNewChange(ChangeType.PROP_DELETION);
				change.propID = removeProp.id;
				change.oldContent = removeProp.content;
				change.propLinkCount = removeProp.linkCount;

				ofy.delete(removeProp);
				saveVersionInfo(change);
			}
			/*
			 * can't reuse the removeProposition function because don't want to
			 * delete the argument where it is empty. [could fix this by just
			 * adding the link before deleting...]
			 */
			else if (query.count() == 1) {
				Change change = getNewChange(ChangeType.PROP_DELETION);
				change.propID = removeProp.id;
				change.oldContent = removeProp.content;
				change.propLinkCount = removeProp.linkCount;
				change.argID = parentArgID;
				change.argPropIndex = index;

				parentArg.childIDs.remove(index);

				ofy.delete(removeProp);
				putNode(parentArg);
				saveVersionInfo(change);
			}
			/*
			 * if used in more than one argument, unlink instead. can't use the
			 * unlinkProposition function because don't want to delete the
			 * argument where it is empty.
			 */
			else if (query.count() > 1) {
				Change change = getNewChange(ChangeType.PROP_UNLINK);
				change.argID = parentArgID;
				change.propID = removePropID;
				change.argPropIndex = index;

				parentArg.childIDs.remove(index);

				removeProp.linkCount--;

				putNode(removeProp);
				putNode(parentArg);
				saveVersionInfo(change);
			}

			Change change = getNewChange(ChangeType.PROP_LINK);
			change.argID = parentArgID;
			change.propID = linkPropID;

			parentArg.childIDs.add(index, linkPropID);
			if (negated) {
				parentArg.negatedChildIDs.add(linkPropID);
			}
			putNode(parentArg);
			linkProp.linkCount++;
			putNode(linkProp);
			saveVersionInfo(change);

			PartialTrees trees = new PartialTrees();
			// Map<Long, Node> nodes = new HashMap<Long, Node>();
			trees.nodes.put(linkProp.id, linkProp);
			recursiveGetProps(linkProp, trees.nodes, 2);

			Rating.prepWithRatings(trees);

			return trees;
		} finally {
			parentLock.unlock();
			oldChildLock.unlock();
			newChildLock.unlock();
		}
	}

	@Override
	public void logClientException(String exceptionStr) {
		log.severe("CLIENT EXCEPTION (string)" + exceptionStr);
	}

	@Override
	public PartialTrees getNodesChildren(List<Long> nodeIDs, int depth)
			throws ServiceException {
		// Map<Long, Node> nodes = new HashMap<Long, Node>();
		PartialTrees trees = new PartialTrees();
		for (Long id : nodeIDs) {
			try {
				Argument arg = ofy.get(Argument.class, id);
				recursiveGetArgs(arg, trees.nodes, depth);
			} catch (NotFoundException e) {
				Proposition prop = ofy.get(Proposition.class, id);
				recursiveGetProps(prop, trees.nodes, depth);
			}
		}

		// Vote.prepWithVotes(nodes);

		Rating.prepWithRatings(trees);

		return trees;
	}

	@Override
	public LoginInfo getLoginInfo(String requestUri) {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		LoginInfo loginInfo = new LoginInfo();

		if (user != null) {
			loginInfo.loggedIn = true;
			loginInfo.email = user.getEmail();
			loginInfo.nickName = user.getNickname();
			loginInfo.logOutURL = userService.createLogoutURL(requestUri);
			loginInfo.isAdmin = userService.isUserAdmin();
		} else {
			loginInfo.loggedIn = false;
			loginInfo.isAdmin = false;
			loginInfo.logInURL = userService.createLoginURL(requestUri);
		}
		return loginInfo;
	}

	@Override
	public void doUnexpectedFailure(java.lang.Throwable e) {
		log.log(Level.SEVERE, "Uncaught exception", e);
	}

	@Override
	public Integer getRating(Long propID) {
		return Rating.getRating(propID);
	}

	public void setRating(Long propID, Integer rating) throws ServiceException {
		Rating.setRating(propID, rating);
	}

	public String getTextPage(String pageName) throws ServiceException {
		for (int i = 0; i < pageName.length(); i++) {
			if (!Character.isLetter(pageName.charAt(i))) {
				throw new ServiceException(
						"not a valid page name:  page names can consist of letters only");
			}
		}

		try {
			String fileName = "text_pages/" + pageName;
			if (Cache.contains(fileName)) {
				return (String) Cache.get(fileName);
			} else {
				String fileText = textFileToString(fileName);
				/*
				 * put it in the cache to expire in 6 hours (so when text is
				 * updated old text will be served for only 6 hours
				 */
				// int textPageExpirationTime = 60 * 60 * 6;
				int textPageExpirationTime = 1;
				Cache.putOnlyIfNotPresent(fileName, fileText,
						textPageExpirationTime);
				return fileText;
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "IOException during getTextPage(\""
					+ pageName + "\")", e);
			throw new ServiceException("IOException prevented page load");
		}
	}

	private String textFileToString(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}
		return stringBuilder.toString();
	}
}
