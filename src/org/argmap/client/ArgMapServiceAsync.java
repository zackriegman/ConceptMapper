package org.argmap.client;


import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.argmap.client.ArgMapService.ForwardChanges;
import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodeWithChanges;
import org.argmap.client.ArgMapService.PropsAndArgs;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ArgMapServiceAsync {
	
	
	/*void getAllProps(AsyncCallback<Proposition[]> callback);*/
	

	void deleteProp(Long propID, AsyncCallback<Void> callback);

	void addArg(Long parentPropID, boolean pro,
			AsyncCallback<Long> callback);

	void updateProp(Long propID, String content,
			AsyncCallback<Void> callback);


	void linkProposition(Long parentArgID, int position, Long propositionID,
			AsyncCallback<Void> callback);


	void unlinkProp(Long parentArgID, Long propositionID,
			AsyncCallback<Void> callback);

	void replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID, AsyncCallback<Nodes> callback);


	void addProp(Long parentArgID, int position, String content,
			AsyncCallback<Long> callback);

	void updateArg(Long argID, String content, AsyncCallback<Void> callback);

	void getChanges(List<Long> propIDs, List<Long> argIDs,
			AsyncCallback<NodeChangesMaps> callback);

	void deleteArg(Long argID, AsyncCallback<Void> callback);

	void logClientException(String exceptionStr, AsyncCallback<Void> callback);

	void getPropsWithChanges(List<Long> propIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);

	void getArgsWithChanges(List<Long> argIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);

	void getPropsAndArgs(int depthLimit, AsyncCallback<PropsAndArgs> callback);

	void getNodesChildren(List<Long> nodeIDs, int depth,
			AsyncCallback<Nodes> callback);

	void getLoginInfo(String requestURI, AsyncCallback<LoginInfo> callback);

	void continueSearchProps(String searchName,
			AsyncCallback<PropsAndArgs> callback);

	void searchProps(String searchString, String searchName, int resultLimit,
			List<Long> filerNodeIDs, AsyncCallback<PropsAndArgs> callback);

	void getNewChanges_DELETE_ME(Date date, Set<Long> propIDs, Set<Long> argIDs,
			AsyncCallback<ForwardChanges> callback);


}
