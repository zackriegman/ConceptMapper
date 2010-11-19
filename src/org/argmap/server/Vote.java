package org.argmap.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Id;

import org.argmap.client.Proposition;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cached;

@Cached
public class Vote implements Serializable {

	static {
		ObjectifyRegistrator.register();
	}
	private static final Objectify ofy = ObjectifyService.begin();

	/**
	 * added to suppress warnings
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * here is my plan for ids: make each id the concatenation of the user's id
	 * and the node's id. That way I can construct the id without having the
	 * object and I can avoid what the Objectify people say are expensive
	 * queries and instead just do batch gets when I know both the user and
	 * proposition. (Of course, when I want to get all the votes by a particular
	 * user or all the votes for a particular proposition, I'll still need to
	 * query on those fields...)
	 */
	@Id
	public String id;

	public String userID;
	public Long propID;

	/*
	 * at the moment this should be a number from 0-4 inclusive representing the
	 * users evalution of the veracity of the node. 0 translates roughly to:
	 * 'definitely not true'. 1 translates roughly to: 'probably not true'. 2
	 * translates roughly to: 'don't know' or 'about as likely to be true as not
	 * true'. 3 translates roughly to: 'probably true'. 4 translates roughly to:
	 * 'definitely true'.
	 */
	public int eval;

	public void setID(String userID, Long propID) {
		this.userID = userID;
		this.propID = propID;
		this.id = buildID(userID, propID);
	}

	private static String buildID(String userID, Long propID) {
		/*
		 * This format string formats a long with leading zeros. '%' indicates
		 * beginning of a format string. '0' is a flag indicating that number is
		 * to be zero padded. '20' is a width long enough to contain a long. 'd'
		 * is the 'conversion' indicating that the numbers are to be printed as
		 * decimal integers.
		 */
		String format = "%020d";
		return userID + String.format(format, propID);
	}

	/* returns the integer votes associated with each propID */
	public static Map<Long, Integer> getVotes(Set<Long> propIDs, String userID) {
		// UserService userService = UserServiceFactory.getUserService();
		// User user = userService.getCurrentUser();
		// String userID = user.getUserId();

		// first build a list of vote ids based on the userID and the propID
		List<String> voteIDs = new ArrayList<String>(propIDs.size());
		for (Long id : propIDs) {
			voteIDs.add(buildID(userID, id));
		}

		// then batch get the vote objects
		Map<String, Vote> votes = ofy.get(Vote.class, voteIDs);

		// then build a return map of the integer vote and the proposition id
		Map<Long, Integer> results = new HashMap<Long, Integer>();
		for (Vote vote : votes.values()) {
			results.put(vote.propID, vote.eval);
		}

		return results;
	}

	/*
	 * casts a vote by a user for a particular node, overwriting the existing
	 * vote if it exists
	 */
	public static void castVote(Long propID, String userID, int eval) {

		Lock lock = Lock.getNodeLock(propID);
		try {
			lock.lock();

			Vote vote = ofy.find(Vote.class, buildID(userID, propID));
			Proposition prop = ofy.get(Proposition.class, propID);

			/* if the user has previously voted back out previous vote */
			if (vote != null) {
				prop.voteSum -= vote.eval;
				prop.voteCount--;
			}
			/* otherwise create a new vote object to hold this vote */
			else {
				vote = new Vote();
				vote.setID(userID, propID);
			}

			/* update the proposition with the new evaluation */
			prop.voteSum += eval;
			prop.voteCount++;

			/*
			 * TODO need to queue a task to recalculate the scores for all the
			 * nodes that depend on this node's score or average vote.
			 */

			vote.eval = eval;
			ofy.put(vote);

			/*
			 * notice that I do not call the ArgMapServiceImpl.updateNode()
			 * function here because I do not want to have the updated time
			 * change. The updated time is used by the live refresh system to
			 * determine when a client refresh is needed, and I don't want to
			 * trigger a client refresh because, at the moment I'm not even
			 * sending the voteSum to the client (and don't plan to) (it's
			 * marked 'transient', so it doesn't get sent.)
			 */
			ofy.put(prop);
		} finally {
			lock.unlock();
		}
	}

}
