package org.argmap.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.argmap.client.ArgMapService.PartialTrees;
import org.argmap.client.Node;
import org.argmap.client.Proposition;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;

public class Search implements Serializable {
	/**
	 * added to suppress warnings
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(ArgMapServiceImpl.class
			.getName());

	@Id
	private Long id;
	private List<String> tokens;

	@Embedded
	private CombinationGenerator currentCombinationGenerator;
	private int[] currentCombination;

	private String cursorString;
	private int combinationSetSize;
	private int limit;
	private final Set<Long> filterIDs = new HashSet<Long>();

	/*
	 * setup a search that filters for three types of unwanted matches. Based on
	 * filterArgID excludes other propositions already in the argument and
	 * excludes the proposition which the argument is for. However, toplevel
	 * propositions do not have an argument parent, so filterPropID is necessary
	 * so the search knows to filter out the proposition currently being
	 * examined when the search is being performed for a proposition edit.
	 */
	public Search(Objectify ofy, Set<String> tokenSet, int limit,
			List<Long> filterNodeIDs) {
		if (filterNodeIDs != null) {
			filterIDs.addAll(filterNodeIDs);
		}
		tokens = new ArrayList<String>(tokenSet);
		combinationSetSize = tokens.size();
		this.limit = limit;
	}

	@SuppressWarnings("unused")
	private Search() {

	}

	public PartialTrees getBatch(Objectify ofy) {
		PartialTrees results = setUpResults();
		Query<Proposition> query = repeatPreviousQueryOrBuildNext(ofy);
		if (query == null) {
			/* set rootProps to null as sign to client that the search has been exhausted */
			results.rootProps = null;
		} else {
			getUpToLimitMatches(results.rootProps, query, ofy);
		}
		return results;
	}

	/*
	 * if the previous call finished in the middle of a particular query then
	 * setup that query and set the cursor to begin where it left off; otherwise
	 * build the next query.
	 */
	public Query<Proposition> repeatPreviousQueryOrBuildNext(Objectify ofy) {
		Query<Proposition> query = null;
		if (currentCombination != null && cursorString != null) {
			query = buildQueryFromCurrentCombination(ofy);
			query.startCursor(Cursor.fromWebSafeString(cursorString));
		} else {
			query = buildNextQuery(ofy);
		}
		return query;
	}

	public PartialTrees setUpResults() {
		List<Proposition> results = new LinkedList<Proposition>();
		PartialTrees propsAndArgs = new PartialTrees();
		propsAndArgs.rootProps = results;
		propsAndArgs.nodes = new HashMap<Long, Node>();
		return propsAndArgs;
	}

	/*
	 * repeated calls to this function will run through all the possible
	 * combinations of tokens that can be searched for. It starts by searching
	 * for all the tokens, and then searches for each combination of one fewer
	 * than all the tokens, then for two fewer, and so forth until it gets to
	 * just one term. On any given call however it will only keep running until
	 * it gets "limit" number of results. Then it saves it state in member
	 * variables and returns. The next call will pick up where it left off.
	 */
	public PartialTrees getBatchToLimit(Objectify ofy) {
		PartialTrees results = setUpResults();

		Query<Proposition> currentQuery = repeatPreviousQueryOrBuildNext(ofy);

		int queryCount = 0;

		/*
		 * keep collect results until there are no more possible token
		 * combinations, or until the result limit for this call has been
		 * reached.
		 */
		while (currentQuery != null) {
			if (getUpToLimitMatches(results.rootProps, currentQuery, ofy)) {
				log.severe("returning " + results.rootProps.size()
						+ " results after reaching limit (" + queryCount
						+ " queries)");
				return results;
			}

			/*
			 * after the query has been exhausted (or if no query exists yet) we
			 * setup a new query
			 */
			currentQuery = buildNextQuery(ofy);
			queryCount++;
		}
		/*
		 * if all possible combinations of all possible number of terms has been
		 * searched we fall out of the while loop and return whatever we got
		 */
		log.severe("returning " + results.rootProps.size()
				+ " results after exchausting search combinations ("
				+ queryCount + " queries)");
		return results;
	}

	/*
	 * returns true if we got the limit number of matches; returns false if we
	 * stopped getting matches because we ran out of matches in the current
	 * query
	 */
	private boolean getUpToLimitMatches(List<Proposition> results,
			Query<Proposition> currentQuery, Objectify ofy) {
		/*
		 * we iterate through the keys (instead of the propositions) to avoid
		 * reading and de-serializing all the filtered props, of which there are
		 * probably many...
		 */
		QueryResultIterable<Key<Proposition>> keysQuery = currentQuery
				.fetchKeys();
		QueryResultIterator<Key<Proposition>> iterator = keysQuery.iterator();
		/* while there are more results */
		while (iterator.hasNext()) {
			Key<Proposition> propKey = iterator.next();
			/*
			 * skip this result if it is in filterIDs (and add it to the
			 * filterIDs as we don't want duplicates in our results)
			 */
			if (filterIDs.add(propKey.getId())) {
				/*
				 * add the result (might be better to add the result to a list
				 * and then do a batch get for all the props at once)
				 */
				results.add(ofy.get(propKey));

				/* if we've reached the limit save the cursor and return */
				if (results.size() == limit) {
					cursorString = iterator.getCursor().toWebSafeString();
					return true;
				}
			}
		}
		cursorString = null;
		currentCombination = null;
		return false;
	}

	/*
	 * returns a query based on the next combination of terms, or if there are
	 * no more possible combinations of terms, returns null
	 */
	private Query<Proposition> buildNextQuery(Objectify ofy) {
		Query<Proposition> nextQuery = null;

		/* if there are more combinations with the current number of terms */
		if (currentCombinationGenerator != null
				&& currentCombinationGenerator.hasMore()) {
			currentCombination = currentCombinationGenerator.getNext();
			nextQuery = buildQueryFromCurrentCombination(ofy);
		}

		/*
		 * if there are no more combinations with the current number of terms
		 * but there are more combinations with fewer terms
		 */
		else if (combinationSetSize > 0) {
			currentCombinationGenerator = new CombinationGenerator(tokens
					.size(), combinationSetSize);
			currentCombination = currentCombinationGenerator.getNext();
			nextQuery = buildQueryFromCurrentCombination(ofy);
			combinationSetSize--;
		}
		return nextQuery;
	}

	private Query<Proposition> buildQueryFromCurrentCombination(Objectify ofy) {
		StringBuffer sb = new StringBuffer();
		sb.append("Building search for these terms:");
		Query<Proposition> query = ofy.query(Proposition.class);
		for (int i = 0; i < currentCombination.length; i++) {
			String token = tokens.get(currentCombination[i]);
			query.filter("tokens", token);
			sb.append(token + " ");
		}
		log.severe(sb.toString());
		return query;
	}
}
