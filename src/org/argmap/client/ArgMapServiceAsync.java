package org.argmap.client;


import java.util.List;

import org.argmap.client.ArgMapService.AllPropsAndArgs;
import org.argmap.client.ArgMapService.ArgWithChanges;
import org.argmap.client.ArgMapService.NodeChangesMaps;
import org.argmap.client.ArgMapService.NodesWithHistory;
import org.argmap.client.ArgMapService.PropWithChanges;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ArgMapServiceAsync {
	
	
	/*void getAllProps(AsyncCallback<Proposition[]> callback);*/
	

	void deleteProposition(Long propID, AsyncCallback<Void> callback);

	void addArgument(Long parentPropID, boolean pro,
			AsyncCallback<Argument> callback);

	void updateProposition(Long propID, String content,
			AsyncCallback<Void> callback);


	void linkProposition(Long parentArgID, int position, Long propositionID,
			AsyncCallback<Void> callback);


	void unlinkProposition(Long parentArgID, Long propositionID,
			AsyncCallback<Void> callback);

	void searchPropositions(String string, Long filterArgID,
			AsyncCallback<List<Proposition>> callback);

	void replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID, AsyncCallback<Nodes> callback);


	void addProposition(Long parentArgID, int position, String content,
			AsyncCallback<Long> callback);

	void getAllPropsAndArgs(AsyncCallback<AllPropsAndArgs> callback);

	void getPropositionCurrentVersionAndHistory(Long propID,
			AsyncCallback<NodesWithHistory> callback);

	void getArgumentCurrentVersionAndHistory(Long argID,
			AsyncCallback<NodesWithHistory> callback);

	void updateArgument(Long argID, String content, AsyncCallback<Void> callback);

	void getChanges(List<Long> propIDs, List<Long> argIDs,
			AsyncCallback<NodeChangesMaps> callback);

	void deleteArgument(Long argID, AsyncCallback<Void> callback);

	void getPropositionsWithChanges(List<Long> propIDs,
			AsyncCallback<List<PropWithChanges>> callback);

	void getArgumentsWithChanges(List<Long> argIDs,
			AsyncCallback<List<ArgWithChanges>> callback);
}
