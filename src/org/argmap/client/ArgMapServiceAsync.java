package org.argmap.client;


import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.argmap.client.ArgMapService.DateAndChildIDs;
import org.argmap.client.ArgMapService.ForwardChanges;
import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodeInfo;
import org.argmap.client.ArgMapService.NodeWithChanges;
import org.argmap.client.ArgMapService.NodesAndNode;
import org.argmap.client.ArgMapService.PartialTrees;
import org.argmap.client.ArgMapService.PartialTrees_DELETE_ME;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ArgMapServiceAsync {
	
	
	/*void getAllProps(AsyncCallback<Proposition[]> callback);*/
	



	void getChanges(List<Long> propIDs, List<Long> argIDs,
			AsyncCallback<NodeChangesMaps> callback);


	void logClientException(String exceptionStr, AsyncCallback<Void> callback);

	void getPropsWithChanges(List<Long> propIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);

	void getArgsWithChanges(List<Long> argIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);

	void getNodesChildren(List<Long> nodeIDs, int depth,
			AsyncCallback<Map<Long, Node>> callback);

	void getLoginInfo(String requestURI, AsyncCallback<LoginInfo> callback);

	void continueSearchProps(String searchName,
			AsyncCallback<PartialTrees_DELETE_ME> callback);

	void searchProps(String searchString, String searchName, int resultLimit,
			List<Long> filerNodeIDs, AsyncCallback<PartialTrees_DELETE_ME> callback);

	void getNewChanges_DELETE_ME(Date date, Set<Long> propIDs, Set<Long> argIDs,
			AsyncCallback<ForwardChanges> callback);

	void getRootProps(int depthLimit, AsyncCallback<PartialTrees_DELETE_ME> callback);

	void getUpToDateNodes(Map<Long, DateAndChildIDs> propInfo,
			Map<Long, DateAndChildIDs> argInfo,
			AsyncCallback<PartialTrees> callback);

	void addProp(NodeInfo parentArg, int position, String content,
			AsyncCallback<NodesAndNode> callback);


	void addArg(NodeInfo parentProp, boolean pro,
			AsyncCallback<NodesAndNode> callback);


	void updateProp(NodeInfo prop, String content,
			AsyncCallback<Map<Long, Node>> callback);


	void updateArg(NodeInfo arg, String content,
			AsyncCallback<Map<Long, Node>> callback);


	void linkProposition(Argument arg, int position, Long propID,
			AsyncCallback<Void> callback);


	void unlinkProp(NodeInfo parentArg, NodeInfo linkProp,
			AsyncCallback<Map<Long, Node>> callback);


	void replaceWithLinkAndGet(NodeInfo parentArg,
			NodeInfo linkProp, Long removePropID,
			AsyncCallback<Map<Long, Node>> callback);


	void deleteProp(Long propID, AsyncCallback<Map<Long, Node>> callback);


	void deleteArg(Long argID, AsyncCallback<Map<Long, Node>> callback);


}
