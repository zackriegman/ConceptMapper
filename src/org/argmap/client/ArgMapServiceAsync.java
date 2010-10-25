package org.argmap.client;


import java.util.List;
import java.util.Map;

import org.argmap.client.ArgMapService.AllPropsAndArgs;
import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodeWithChanges;
import org.argmap.client.ArgMapService.NodesWithHistory;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ArgMapServiceAsync {
	
	
	/*void getAllProps(AsyncCallback<Proposition[]> callback);*/
	

	void deleteProp(Long propID, AsyncCallback<Void> callback);

	void addArg(Long parentPropID, boolean pro,
			AsyncCallback<Argument> callback);

	void updateProp(Long propID, String content,
			AsyncCallback<Void> callback);


	void linkProposition(Long parentArgID, int position, Long propositionID,
			AsyncCallback<Void> callback);


	void unlinkProp(Long parentArgID, Long propositionID,
			AsyncCallback<Void> callback);

	void searchProps(String string, Long filterArgID,
			AsyncCallback<List<Proposition>> callback);

	void replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID, AsyncCallback<Nodes> callback);


	void addProp(Long parentArgID, int position, String content,
			AsyncCallback<Long> callback);

	void getAllPropsAndArgs(AsyncCallback<AllPropsAndArgs> callback);

	void getPropCurrentVersionAndHistory(Long propID,
			AsyncCallback<NodesWithHistory> callback);

	void getArgCurrentVersionAndHistory(Long argID,
			AsyncCallback<NodesWithHistory> callback);

	void updateArg(Long argID, String content, AsyncCallback<Void> callback);

	void getChanges(List<Long> propIDs, List<Long> argIDs,
			AsyncCallback<NodeChangesMaps> callback);

	void deleteArg(Long argID, AsyncCallback<Void> callback);

	void logClientException(String exceptionStr, AsyncCallback<Void> callback);

	void getPropsWithChanges(List<Long> propIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);

	void getArgsWithChanges(List<Long> argIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);




}
