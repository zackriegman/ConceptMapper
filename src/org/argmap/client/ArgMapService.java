package org.argmap.client;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("argServ")
public interface ArgMapService extends RemoteService {
	
	public void logClientException( String exceptionStr );

	public Long addProposition(Long parentArgID, int position, String content) throws Exception;

	public void deleteProposition(Long propID) throws Exception;
	
	public void deleteArgument(Long argID) throws Exception;

	public Argument addArgument(Long parentPropID, boolean pro)
			throws Exception;

	public void updateProposition(Long propID, String content) throws Exception;
	
	public void updateArgument(Long argID, String content) throws Exception;

	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws Exception;

	public void unlinkProposition(Long parentArgID, Long propositionID)
			throws Exception;

	public class AllPropsAndArgs implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public Map<Long, Proposition> rootProps;
		public Nodes nodes;
	}
	
	public AllPropsAndArgs getAllPropsAndArgs();
	
	public Nodes replaceWithLinkAndGet(Long parentArgID, Long linkPropID, Long removePropID) throws Exception;

	public class NodesWithHistory implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public Nodes nodes;
		public SortedMap<Date, Change> changes;
	}

	public NodesWithHistory getPropositionCurrentVersionAndHistory(
			Long propID) throws Exception;

	public NodesWithHistory getArgumentCurrentVersionAndHistory(Long argID)
			throws Exception;
	
	public class NodeWithChanges implements Serializable {
		private static final long serialVersionUID = 1L;
		public Node node;
		public NodeChanges nodeChanges;
	}
	
	public Map<Long, NodeWithChanges> getPropositionsWithChanges( List<Long> propIDs ) throws Exception;
	
	public Map<Long, NodeWithChanges> getArgumentsWithChanges( List<Long> argIDs ) throws Exception;
	
	public class NodeChangesMaps implements Serializable {
		private static final long serialVersionUID = 1L;
		public Map<Long, NodeChanges> argChanges = new HashMap<Long, NodeChanges>();
		public Map<Long, NodeChanges> propChanges = new HashMap<Long, NodeChanges>();
		
		public String toString(){
			StringBuffer buffer = new StringBuffer();
			buffer.append( "\nargChanges:::::::");
			for(Long id : argChanges.keySet()){
				buffer.append( "\n arg.id:");
				buffer.append( id);
				buffer.append("; nodeChange:");
				buffer.append(argChanges.get(id).toString());
			}
			buffer.append("\npropChanges::::::::");
			for(Long id : propChanges.keySet()){
				buffer.append( "\n prop.id:");
				buffer.append( id);
				buffer.append("; nodeChange:");
				buffer.append(propChanges.get(id).toString());
			}
			return buffer.toString();
		}
		
	}
	
	/* to replace getRevisions above*/
	public NodeChangesMaps getChanges(List<Long> propIDs, List<Long> argIDs) throws Exception;
	
	public List<Proposition> searchPropositions( String string, Long filerArgID ) throws Exception;

}
