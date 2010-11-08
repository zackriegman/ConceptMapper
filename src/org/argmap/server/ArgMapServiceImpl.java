package org.argmap.server;

import java.io.IOException;
import java.io.StringReader;
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
import org.argmap.client.Nodes;
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

public class ArgMapServiceImpl extends RemoteServiceServlet implements
		ArgMapService {

	static {
		ObjectifyService.register(Node.class);
		ObjectifyService.register(Proposition.class);
		ObjectifyService.register(Argument.class);
		ObjectifyService.register(Change.class);
		ObjectifyService.register(Search.class);
		ObjectifyService.register(CombinationGenerator.class);
	}
	private static final Logger log = Logger.getLogger(ArgMapServiceImpl.class
			.getName());

	private static final long serialVersionUID = 1L; // just to get rid of the
	// warnings...
	private final Objectify ofy = ObjectifyService.begin();

	private void logln(String message) {
		log.severe(message);
	}

	@SuppressWarnings("unused")
	private void deleteAllArgsAndProps() {
		ofy.delete(ofy.query(Argument.class));
		ofy.delete(ofy.query(Proposition.class));
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
	public PropsAndArgs getPropsAndArgs(int depthLimit) {

		PropsAndArgs propsAndArgs = new PropsAndArgs();
		/*
		 * TODO: now that we aren't building a prop tree any longer there is no
		 * reason to do this. Instead we can just query for all props and all
		 * args and return them all.
		 */
		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"linkCount =", 0).order("-creationDate").limit(30);

		List<Proposition> rootProps = propQuery.list();
		Nodes nodes = new Nodes();

		for (Proposition prop : rootProps) {
			recursiveGetProps(prop, nodes, depthLimit);
		}
		propsAndArgs.rootProps = rootProps;
		propsAndArgs.nodes = nodes;

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

	private Lock getNodeLock(Long nodeID) {
		return Lock.getLock("NODE_ID:" + nodeID);
	}

	@Override
	public Long addProp(Long parentArgID, int position, String content) {

		//log.finest("addProp()");
		Proposition newProposition = new Proposition();
		newProposition.creationDate = new Date();
		newProposition.content = content;
		newProposition.tokens = getTokensForIndexingOrQuery(content, 30);
		Argument parentArg = null;

		if (parentArgID != null) {
			// exception will be generated if there is a bogus parentArgID

			Lock lock = getNodeLock(parentArgID);
			try {
				lock.lock();
				parentArg = ofy.get(Argument.class, parentArgID);
				newProposition.linkCount = 1;
				ofy.put(newProposition);
				parentArg.childIDs.add(position, newProposition.id);

				ofy.put(parentArg);
			} finally {
				lock.unlock();
			}
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
			Long propositionID) {
		Lock parentLock = getNodeLock(parentArgID);
		Lock childLock = getNodeLock(propositionID);
		try {
			parentLock.lock();
			childLock.lock();
			Proposition proposition = ofy.get(Proposition.class, propositionID);
			Argument argument = ofy.get(Argument.class, parentArgID);

			argument.childIDs.add(position, propositionID);
			ofy.put(argument);
			proposition.linkCount++;
			ofy.put(proposition);
			Change change = new Change(ChangeType.PROP_LINK);
			change.argID = parentArgID;
			change.propID = proposition.id;
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
		Change change = new Change(ChangeType.PROP_DELETION);
		Proposition prop = ofy.get(Proposition.class, propID);
		if (!prop.childIDs.isEmpty()) {
			throw new ServiceException(
					"cannot delete proposition with arguments; delete arguments first");
		}
		change.propID = prop.id;
		change.content = prop.content;
		change.propLinkCount = prop.linkCount;

		/* get all the arguments that use this proposition */
		Query<Argument> query = ofy.query(Argument.class).filter("childIDs",
				propID);

		/* only delete a proposition that is used in 1 or fewer arguments */
		if (query.count() > 1) {
			throw new ServiceException(
					"cannot delete a proposition used by more than one argument; delink from other arguments before deleting");
		}

		/* if the proposition is used in an argument */
		else if (query.count() == 1) {
			Long argID = query.getKey().getId();
			Lock lock = getNodeLock(argID);
			try {
				lock.lock();
				Argument argument = query.get();

				/* record the versioning information */
				change.argID = argument.id;
				change.argPropIndex = argument.childIDs.indexOf(propID);
				// change.argPro = argument.pro;

				/* remove the proposition from the argument */
				argument.childIDs.remove(propID);
				ofy.put(argument);
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

		Lock parentLock = getNodeLock(parentArgID);
		Lock childLock = getNodeLock(propositionID);
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
			ofy.put(argument);

			proposition.linkCount--;
			ofy.put(proposition);

			Change change = new Change(ChangeType.PROP_UNLINK);
			change.argID = parentArgID;
			change.propID = proposition.id;
			change.argPropIndex = propIndex;
			change.content = proposition.content;
			saveVersionInfo(change);
		} finally {
			parentLock.unlock();
			childLock.unlock();
		}
	}

	@Override
	public void updateProp(Long propID, String content) throws ServiceException {
		logln("propID:" + propID + "; content:" + content);

		Change change = new Change(ChangeType.PROP_MODIFICATION);
		Lock lock = getNodeLock(propID);
		try {
			lock.lock();
			Proposition prop = ofy.get(Proposition.class, propID);
			if (prop.getContent() != null
					&& prop.getContent().trim().equals(content.trim())) {
				throw new ServiceException(
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
		} finally {
			lock.unlock();
		}
	}

	public void deleteArg(Long argID) throws ServiceException {

		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"childIDs", argID);
		Lock parentLock = getNodeLock(propQuery.getKey().getId());
		Lock childLock = getNodeLock(argID);
		try {
			parentLock.lock();
			childLock.lock();

			Argument argument = ofy.get(Argument.class, argID);
			if (!argument.childIDs.isEmpty()) {
				throw new ServiceException(
						"Cannot delete an argumet that still has child propositions.  First remove child propositions.");
			}

			Proposition parentProp = propQuery.get();

			Change argDeletionChange = new Change(ChangeType.ARG_DELETION);
			argDeletionChange.propID = parentProp.id;
			argDeletionChange.argID = argument.id;
			argDeletionChange.argPro = argument.pro;
			argDeletionChange.content = argument.content;
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
		} finally {
			parentLock.unlock();
			childLock.unlock();
		}
	}

	@Override
	public Long addArg(Long parentPropID, boolean pro) {
		LapTimer timer = new LapTimer();
		Lock lock = getNodeLock(parentPropID);
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
			// ofy.put(newProp);

			Argument newArg = new Argument();
			newArg.creationDate = new Date();
			timer.lap("----");
			// newArg.propIDs.add(0, newProp.id);

			newArg.pro = pro;
			timer.lap("====");
			ofy.put(newArg);
			timer.lap("]]]]");
			parentProp.childIDs.add(newArg.id);
			timer.lap("\\\\");
			ofy.put(parentProp);
			timer.lap(";;;;");
			Change change = new Change(ChangeType.ARG_ADDITION);
			change.argID = newArg.id;
			change.propID = parentPropID;
			saveVersionInfo(change);
			timer.lap("````");
			return newArg.id;
		} finally {
			lock.unlock();
			log.fine(timer.getRecord());
		}
	}

	@Override
	public void updateArg(Long argID, String content) throws ServiceException {
		Lock lock = getNodeLock(argID);
		try {
			lock.lock();
			Change change = new Change(ChangeType.ARG_MODIFICATION);
			Argument arg = ofy.get(Argument.class, argID);
			if (arg.content != null
					&& arg.content.trim().equals(content.trim())) {
				throw new ServiceException(
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
		change.sessionID = request.getSession().getId();

		// logln("Change Logged -- " + change.toString());

		ofy.put(change);
	}

	public HttpServletRequest getHttpServletRequest() {
		return getThreadLocalRequest();
	}

	@Override
	public NodeChangesMaps getChanges(List<Long> propIDs, List<Long> argIDs) {

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
	public Map<Long, NodeWithChanges> getPropsWithChanges(List<Long> propIDs) {

		Map<Long, NodeWithChanges> map = new HashMap<Long, NodeWithChanges>();
		for (Long propID : propIDs) {
			NodeWithChanges propWithChanges = new NodeWithChanges();
			// try {
			propWithChanges.node = ofy.get(Proposition.class, propID);
			// } catch (NotFoundException e) {
			// /*
			// * if the node doesn't currently exist because it has been
			// * deleted, just return null
			// */
			// propWithChanges.node = null;
			// }
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
			// try {
			argWithChanges.node = ofy.get(Argument.class, argID);
			// } catch (NotFoundException e) {
			// /*
			// * if the node doesn't currently exist because it has been
			// * deleted, just return null
			// */
			// argWithChanges.node = null;
			// }
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
	public PropsAndArgs searchProps(String searchString, String searchName,
			int resultLimit, List<Long> filterNodeIDs) {
		Set<String> tokenSet = getTokensForIndexingOrQuery(searchString, 6);
		if (tokenSet.isEmpty()) {
			return null;
		}

		Search search = new Search(ofy, tokenSet, resultLimit, filterNodeIDs);
		PropsAndArgs propsAndArgs = search.getBatch(ofy);
		getHttpServletRequest().getSession().setAttribute(searchName, search);
		return propsAndArgs;
	}

	@Override
	public PropsAndArgs continueSearchProps(String searchName) {
		Search search = (Search) getHttpServletRequest().getSession()
				.getAttribute(searchName);
		PropsAndArgs propsAndArgs = search.getBatch(ofy);
		getHttpServletRequest().getSession().setAttribute(searchName, search);
		return propsAndArgs;
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
	public Nodes replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID) throws ServiceException {
		Lock parentLock = getNodeLock(parentArgID);
		Lock oldChildLock = getNodeLock(removePropID);
		Lock newChildLock = getNodeLock(linkPropID);
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
	public Nodes getNodesChildren(List<Long> nodeIDs, int depth)
			throws ServiceException {
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
	}

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

}
