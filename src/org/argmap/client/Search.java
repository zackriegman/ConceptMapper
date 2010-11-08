package org.argmap.client;

import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.PropsAndArgs;

public class Search implements ServerComm.LocalCallback<PropsAndArgs> {

	private final String searchString;
	private final SearchResultsHandler handler;
	private final int resultLimit;
	private final List<Long> filterNodeIDs;
	private final String searchName;
	private boolean active = true;
	private int resultCount;
	private ArgMap.Message userMessage;
	
	public interface SearchResultsHandler {
		public void processSearchResults(PropsAndArgs propsAndArgs);

		public void searchExhausted();

		public void searchCompleted();
	}

	public Search(String searchString, int resultLimit,
			List<Long> filterNodeIDs, SearchResultsHandler handler) {
		this.searchString = searchString;
		this.handler = handler;
		this.resultLimit = resultLimit;
		this.filterNodeIDs = filterNodeIDs;

		searchName = "" + hashCode();
	}

	public void startSearch() {
		resultCount = 0;
		ServerComm.searchProps(searchString, searchName, resultLimit,
				filterNodeIDs, this);
		userMessage = ArgMap.message("searching...", MessageType.INFO);
	}

	public void continueSearch() {
		resultCount = 0;
		ServerComm.continueSearchProps(searchName, this);
		userMessage.setMessage("searching for more...");
		userMessage.display();
	}

	public void cancelSearch() {
		active = false;
		userMessage.hide();
	}
	
	public boolean sameAs( String newSearchString ){
		return searchString.trim().equals(newSearchString.trim());
	}

	@Override
	public void call(PropsAndArgs propsAndArgs) {
		if (active) {
			if (propsAndArgs.rootProps != null) {
				resultCount += propsAndArgs.rootProps.size();

				if (resultCount < resultLimit) {
					ServerComm.continueSearchProps(searchName, this);

				} else {
					userMessage.setMessage("search completed");
					userMessage.hideAfter(3000);
					handler.searchCompleted();
				}
				
				if (propsAndArgs.rootProps.size() > 0) {
					handler.processSearchResults(propsAndArgs);
				}
			} else {
				userMessage.setMessage("search completed");
				userMessage.hideAfter(3000);
				handler.searchExhausted();
			}
		}
	}
}
