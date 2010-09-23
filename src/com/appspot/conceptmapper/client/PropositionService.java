package com.appspot.conceptmapper.client;


import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("propServ")
public interface PropositionService extends RemoteService {

	
	public Long addProposition( Long parentArgID, int position ) throws Exception;
	
	public void removeProposition( Long propID ) throws Exception;
	
	public Argument addArgument( Long parentPropID, boolean pro ) throws Exception;
	
	public void updateProposition( Long propID, String content ) throws Exception;
	
	public void linkProposition( Long parentArgID, int position, Long propositionID) throws Exception;
	
	public void unlinkProposition( Long parentArgID, Long propositionID ) throws Exception;
	
	public Proposition[] getAllProps();
	
	public List<Change> getRevisions( Long changeID, List<Long> propIDs, List<Long> argIDs) throws Exception;
}
