package com.appspot.conceptmapper.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("propServ")
public interface PropositionService extends RemoteService {
	Proposition[] getRootPropositions();
	void addRootProposition( Proposition proposition );
	void deleteProposition( Proposition proposition );
}
