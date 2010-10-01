package com.appspot.conceptmapper.client;


import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import com.appspot.conceptmapper.client.PropositionService.ArgTreeWithHistory;
import com.appspot.conceptmapper.client.PropositionService.PropTreeWithHistory;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface PropositionServiceAsync {
	
	
	void getAllProps(AsyncCallback<Proposition[]> callback);
	

	void removeProposition(Long propID, AsyncCallback<Void> callback);


	void addProposition(Long parentArgID, int position,
			AsyncCallback<Long> callback);

	void addArgument(Long parentPropID, boolean pro,
			AsyncCallback<Argument> callback);

	void updateProposition(Long propID, String content,
			AsyncCallback<Void> callback);


	void linkProposition(Long parentArgID, int position, Long propositionID,
			AsyncCallback<Void> callback);


	void unlinkProposition(Long parentArgID, Long propositionID,
			AsyncCallback<Void> callback);


	void getRevisions(Long changeID, List<Long> propIDs, List<Long> argIDs,
			AsyncCallback<SortedMap<Date, Change>> callback);

	void getPropositionCurrentVersionAndHistory(Long propID,
			AsyncCallback<PropTreeWithHistory> callback);


	void getArgumentCurrentVersionAndHistory(Long argID,
			AsyncCallback<ArgTreeWithHistory> callback);

	void searchPropositions(String string, Long excludePropID,
			AsyncCallback<List<Proposition>> callback);
}
