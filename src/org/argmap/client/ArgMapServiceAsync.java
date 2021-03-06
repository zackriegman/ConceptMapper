package org.argmap.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argmap.client.ArgMapService.DateAndChildIDs;
import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodeChangesMapsAndRootChanges;
import org.argmap.client.ArgMapService.NodeWithChanges;
import org.argmap.client.ArgMapService.PartialTrees;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ArgMapServiceAsync {

	/* void getAllProps(AsyncCallback<Proposition[]> callback); */

	void deleteProp(Long propID, AsyncCallback<Void> callback);

	void updateProp(Long propID, String content, AsyncCallback<Void> callback);

	void linkProposition(Long parentArgID, int position, Long propositionID,
			AsyncCallback<Void> callback);

	void unlinkProp(Long parentArgID, Long propositionID,
			AsyncCallback<Void> callback);

	void updateArg(Long argID, String content, AsyncCallback<Void> callback);

	void deleteArg(Long argID, AsyncCallback<Void> callback);

	void logClientException(String exceptionStr, AsyncCallback<Void> callback);

	void getPropsWithChanges(List<Long> propIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);

	void getArgsWithChanges(List<Long> argIDs,
			AsyncCallback<Map<Long, NodeWithChanges>> callback);

	void getLoginInfo(String requestURI, AsyncCallback<LoginInfo> callback);

	void addProp(Long parentArgID, int position, String content,
			AsyncCallback<Proposition> callback);

	void addArg(Long parentPropID, boolean pro, AsyncCallback<Argument> callback);

	void continueSearchProps(String searchName,
			AsyncCallback<PartialTrees> callback);

	void getRootProps(int depthLimit, AsyncCallback<PartialTrees> callback);

	void getNodesChildren(List<Long> nodeIDs, int depth,
			AsyncCallback<PartialTrees> callback);

	void getRating(Long propID, AsyncCallback<Integer> callback);

	void setRating(Long propID, Integer rating, AsyncCallback<Void> callback);

	void getTextPage(String pageName, AsyncCallback<String> callback);

	void replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID, boolean negated,
			AsyncCallback<PartialTrees> callback);

	void getChangesForDeletedRootProps(
			AsyncCallback<NodeChangesMapsAndRootChanges> callback);

	void getUpToDateNodes(HashMap<Long, DateAndChildIDs> propInfo,
			HashMap<Long, DateAndChildIDs> argInfo,
			AsyncCallback<PartialTrees> callback);

	void getChanges(ArrayList<Long> propIDs, ArrayList<Long> argIDs,
			AsyncCallback<NodeChangesMaps> callback);

	void searchProps(String searchString, String searchName, int resultLimit,
			ArrayList<Long> filerNodeIDs, Double percentTermsMatching,
			AsyncCallback<PartialTrees> callback);
}
