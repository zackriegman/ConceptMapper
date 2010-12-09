package org.argmap.client;

import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.PartialTrees;

public abstract class Search implements ServerComm.LocalCallback<PartialTrees> {

	private final String searchString;
	private final int resultLimit;
	private final List<Long> filterNodeIDs;
	private final String searchName;
	private boolean cancelled = false;
	private int resultCount;
	private ArgMap.Message userMessage;

	public Search(String searchString, String searchName, int resultLimit,
			List<Long> filterNodeIDs) {
		this.searchString = searchString.trim();
		this.resultLimit = resultLimit;
		this.filterNodeIDs = filterNodeIDs;

		this.searchName = searchName;
	}

	public void startSearch() {
		searchStarted();
		if (!searchString.equals("")) {
			resultCount = 0;
			ServerComm.searchProps(searchString, searchName, resultLimit,
					filterNodeIDs, this);
			userMessage = ArgMap.message("searching...", MessageType.INFO);
		}
	}

	public void continueSearch() {
		searchContinued();
		resultCount = 0;
		ServerComm.continueSearchProps(searchName, this);
		userMessage.setMessage("searching for more...");
		userMessage.display();
	}

	public void cancelSearch() {
		searchCancelled();
		cancelled = true;
		userMessage.hide();
	}

	public String getSearchString() {
		return searchString;
	}

	@Override
	public void call(PartialTrees propsAndArgs) {
		if (!cancelled) {
			if (propsAndArgs.rootIDs != null) {
				resultCount += propsAndArgs.rootIDs.size();

				if (resultCount < resultLimit) {
					ServerComm.continueSearchProps(searchName, this);

				} else {
					userMessage.setMessage("search completed");
					userMessage.hideAfter(3000);
					searchCompleted();
				}

				if (propsAndArgs.rootIDs.size() > 0) {
					processSearchResults(propsAndArgs);
				}
			} else {
				userMessage.setMessage("search completed: no more results");
				userMessage.hideAfter(3000);
				searchExhausted();
			}
		}
	}

	/*
	 * returns true if the first 150 characters of the trimmed strings are
	 * different, ignoring case. Since the server is only searching on the first
	 * 5 or 6 non-stop-word tokens, this should be enough, since the first 5 or
	 * 6 non-stop-words are likely to occur within the first 150 characters.
	 * This saves the client and server from doing unnecessary searches when
	 * someone is typing a long ass proposition.
	 */
	public static boolean stringsEffectivelyDifferent(String oldString,
			String newString) {
		oldString = oldString.trim();
		newString = newString.trim();
		int length = oldString.length() > newString.length() ? oldString
				.length() : newString.length();
		length = length > 150 ? 150 : length;
		return !oldString.regionMatches(true, 0, newString, 0, length);
	}

	/***********************
	 * The Handler Methods *
	 ***********************/

	public abstract void processSearchResults(PartialTrees propsAndArgs);

	public abstract void searchExhausted();

	public abstract void searchCompleted();

	public void searchStarted() {
	}

	public void searchContinued() {
	}

	public void searchCancelled() {
	}

}
