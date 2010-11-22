package org.argmap.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Id;

import org.argmap.client.ArgMapService.PartialTrees;
import org.argmap.client.Node;
import org.argmap.client.Proposition;
import org.argmap.client.ServiceException;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cached;

@Cached
public class Rating implements Serializable {

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
	 * proposition. (Of course, when I want to get all the ratings by a
	 * particular user or all the ratings for a particular proposition, I'll
	 * still need to query on those fields...)
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

	public static void prepWithRatings(PartialTrees trees) {
		// TODO think about how to make sure there is a valid user in the other
		// methods too if necessary...
		User user = UserServiceFactory.getUserService().getCurrentUser();
		if (user == null) {
			return;
		}
		String userID = user.getUserId();

		List<Long> propIDs = new ArrayList<Long>();

		for (Node node : trees.nodes.values()) {
			if (node instanceof Proposition) {
				propIDs.add(node.id);
			}
		}
		getRatings(propIDs, userID, trees.ratings);
	}

	/*
	 * puts into the provided map the integer ratings associated with each
	 * propID
	 */
	public static void getRatings(List<Long> propIDs, String userID,
			Map<Long, Integer> results) {

		// first build a list of rating ids based on the userID and the propID
		List<String> ratingIDs = new ArrayList<String>(propIDs.size());
		for (Long id : propIDs) {
			ratingIDs.add(buildID(userID, id));
		}

		// then batch get the rating objects
		Map<String, Rating> ratings = ofy.get(Rating.class, ratingIDs);

		// then build a return map of the integer rating and the proposition id
		for (Rating rating : ratings.values()) {
			results.put(rating.propID, rating.eval);
		}
	}

	public static Integer getRating(Long propID) {
		User user = UserServiceFactory.getUserService().getCurrentUser();
		if (user == null) {
			return null;
		}
		String userID = user.getUserId();

		Rating rating = ofy.find(Rating.class, buildID(userID, propID));
		if (rating != null) {
			return rating.eval;
		} else {
			return null;
		}
	}

	public static void setRating(Long propID, int rating)
			throws ServiceException {
		User user = UserServiceFactory.getUserService().getCurrentUser();
		if (user == null) {
			throw new ServiceException(
					"user not logged in: only logged in users can rate propositions");
		}
		String userID = user.getUserId();
		Rating.rate(propID, userID, rating);
	}

	/*
	 * casts a rating by a user for a particular node, overwriting the existing
	 * rating if it exists
	 */
	public static void rate(Long propID, String userID, int eval) {

		Lock lock = Lock.getNodeLock(propID);
		try {
			lock.lock();

			Rating rating = ofy.find(Rating.class, buildID(userID, propID));
			Proposition prop = ofy.get(Proposition.class, propID);

			/* if the user has previously rated back out previous rating */
			if (rating != null) {
				prop.ratingSum -= rating.eval;
				prop.ratingCount--;
			}
			/* otherwise create a new rating object to hold this rating */
			else {
				rating = new Rating();
				rating.setID(userID, propID);
			}

			/* update the proposition with the new evaluation */
			prop.ratingSum += eval;
			prop.ratingCount++;

			/*
			 * TODO need to queue a task to recalculate the scores for all the
			 * nodes that depend on this node's score or average rating.
			 */

			rating.eval = eval;
			ofy.put(rating);

			/*
			 * notice that I do not call the ArgMapServiceImpl.updateNode()
			 * function here because I do not want to have the updated time
			 * change. The updated time is used by the live refresh system to
			 * determine when a client refresh is needed, and I don't want to
			 * trigger a client refresh because, at the moment I'm not even
			 * sending the ratingSum to the client (and don't plan to) (it's
			 * marked 'transient', so it doesn't get sent.)
			 */
			ofy.put(prop);
		} finally {
			lock.unlock();
		}
	}

}
