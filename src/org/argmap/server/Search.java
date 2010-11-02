package org.argmap.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.argmap.client.ArgMapService.PropsAndArgs;
import org.argmap.client.Argument;
import org.argmap.client.Nodes;
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
	private final Set<Long> filterIDs = new HashSet<Long>();

	public Search(Objectify ofy, Set<String> tokenSet, Long filterArgID,
			Long filterPropID) {
		if (filterArgID != null) {
			filterIDs.addAll(ofy.get(Argument.class, filterArgID).childIDs);
		}
		filterIDs.add(filterPropID);
		tokens = new ArrayList<String>(tokenSet);
		combinationSetSize = tokens.size();
	}
	
	private Search(){
		
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
	public PropsAndArgs getBatch(Objectify ofy, int limit) {

		/* prepare the return variables */
		List<Proposition> results = new LinkedList<Proposition>();
		PropsAndArgs propsAndArgs = new PropsAndArgs();
		propsAndArgs.rootProps = results;
		propsAndArgs.nodes = new Nodes();

		/*
		 * if the previous call finished in the middle of a particular query
		 * then setup that query and set the cursor to begin where it left off
		 */
		Query<Proposition> currentQuery = null;
		if (currentCombination != null && cursorString != null) {
			currentQuery = buildQueryFromCurrentCombination(ofy);
			currentQuery.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		/*
		 * keep collect results until there are no more possible token
		 * combinations, or until the result limit for this call has been
		 * reached.
		 */
		while (true) {
			/*
			 * on the very first search currentQuery will be null and we skip to
			 * the subsequent if clauses to build up the query
			 */
			if (currentQuery != null) {
				/*
				 * we iterator through the keys (instead of the proposition) to
				 * avoid reading and de-serializing all the filtered props, of
				 * which there are probably many...
				 */
				QueryResultIterable<Key<Proposition>> keysQuery = currentQuery
						.fetchKeys();
				QueryResultIterator<Key<Proposition>> iterator = keysQuery
						.iterator();
				/* while there are more results */
				while (iterator.hasNext()) {
					Key<Proposition> propKey = iterator.next();
					/*
					 * skip this result if it is in filterIDs (and add it to the
					 * filterIDs as we don't want duplicates in our results)
					 */
					if (filterIDs.add(propKey.getId())) {
						/* add the result (might be better to add the result
						 * to a list and then do a batch get for all the props at once) */
						results.add(ofy.get(propKey));

						/* if we've reached the limit save the cursor and return */
						if (results.size() == limit) {
							cursorString = iterator.getCursor().toWebSafeString();
							log.severe("returning " + results.size()
									+ " results after reaching limit");
							return propsAndArgs;
						}
					}
				}
			}

			/*
			 * after the query has been exhausted (or if no query exists yet) we
			 * setup a new query
			 */

			/* if there are more combinations with the current number of terms */
			if (currentCombinationGenerator != null
					&& currentCombinationGenerator.hasMore()) {
				currentCombination = currentCombinationGenerator.getNext();
				currentQuery = buildQueryFromCurrentCombination(ofy);
			}
			/*
			 * if there are no more combinations with the current number of
			 * terms but there are more combinations with fewer terms
			 */
			else if (combinationSetSize > 0) {
				currentCombinationGenerator = new CombinationGenerator(
						tokens.size(), combinationSetSize);
				currentCombination = currentCombinationGenerator.getNext();
				currentQuery = buildQueryFromCurrentCombination(ofy);
				combinationSetSize--;
			}
			/*
			 * if all possible combinations of all possible number of terms has
			 * been searched
			 */
			else {
				log.severe("returning " + results.size()
						+ " results after exchausting search combinations");
				return propsAndArgs;
			}
		}
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
