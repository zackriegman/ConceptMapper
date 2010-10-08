package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("propServ")
public interface PropositionService extends RemoteService {

	public Long addProposition(Long parentArgID, int position, String content) throws Exception;

	public void removeProposition(Long propID) throws Exception;

	public Argument addArgument(Long parentPropID, boolean pro)
			throws Exception;

	public void updateProposition(Long propID, String content) throws Exception;

	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws Exception;

	public void unlinkProposition(Long parentArgID, Long propositionID)
			throws Exception;

	public class AllPropsAndArgs implements Serializable {
		public Map<Long, Proposition> rootProps;
		public Map<Long, Proposition> props;
		public Map<Long, Argument> args;
	}
	
	public AllPropsAndArgs getAllPropsAndArgs();
	
	public Proposition replaceWithLinkAndGet(Long parentArgID, Long linkPropID, Long removePropID) throws Exception;

	public SortedMap<Date, Change> getRevisions(Long changeID,
			List<Long> propIDs, List<Long> argIDs) throws Exception;

	public class PropTreeWithHistory implements Serializable {

		/* added to supress warnings */
		private static final long serialVersionUID = 1L;
		public Proposition proposition;
		public SortedMap<Date, Change> changes;
	}

	public PropTreeWithHistory getPropositionCurrentVersionAndHistory(
			Long propID) throws Exception;

	public class ArgTreeWithHistory implements Serializable {

		/* added to supress warnings */
		private static final long serialVersionUID = 1L;
		public Argument argument;
		public SortedMap<Date, Change> changes;
	}

	public ArgTreeWithHistory getArgumentCurrentVersionAndHistory(Long argID)
			throws Exception;
	
	public List<Proposition> searchPropositions( String string, Long filerArgID ) throws Exception;

}
