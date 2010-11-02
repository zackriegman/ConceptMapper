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

	public void logClientException(String exceptionStr);

	public Long addProp(Long parentArgID, int position, String content)
			throws Exception;

	public void deleteProp(Long propID) throws Exception;

	public void deleteArg(Long argID) throws Exception;

	public Long addArg(Long parentPropID, boolean pro) throws Exception;

	public void updateProp(Long propID, String content) throws Exception;

	public void updateArg(Long argID, String content) throws Exception;

	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws Exception;

	public void unlinkProp(Long parentArgID, Long propositionID)
			throws Exception;

	public class PropsAndArgs implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public List<Proposition> rootProps;
		public Nodes nodes;
	}

	public PropsAndArgs getPropsAndArgs(int depthLimit);

	public Nodes replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
			Long removePropID) throws Exception;

	public class NodesWithHistory implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public Nodes nodes;
		public SortedMap<Date, Change> changes;
	}

	public class NodeWithChanges implements Serializable {
		private static final long serialVersionUID = 1L;
		public Node node;
		public NodeChanges nodeChanges;
		public Map<Long, Proposition> unlinkedLinks = new HashMap<Long, Proposition>();
	}

	public Nodes getNodesChildren(List<Long> nodeIDs, int depth)
			throws Exception;

	public Map<Long, NodeWithChanges> getPropsWithChanges(List<Long> propIDs)
			throws Exception;

	public Map<Long, NodeWithChanges> getArgsWithChanges(List<Long> argIDs)
			throws Exception;

	public class NodeChangesMaps implements Serializable {
		private static final long serialVersionUID = 1L;
		public Map<Long, NodeChanges> argChanges = new HashMap<Long, NodeChanges>();
		public Map<Long, NodeChanges> propChanges = new HashMap<Long, NodeChanges>();
		public Map<Long, Proposition> unlinkedLinks = new HashMap<Long, Proposition>();

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("\nargChanges:::::::");
			for (Long id : argChanges.keySet()) {
				buffer.append("\n arg.id:");
				buffer.append(id);
				buffer.append("; nodeChange:");
				buffer.append(argChanges.get(id).toString());
			}
			buffer.append("\npropChanges::::::::");
			for (Long id : propChanges.keySet()) {
				buffer.append("\n prop.id:");
				buffer.append(id);
				buffer.append("; nodeChange:");
				buffer.append(propChanges.get(id).toString());
			}
			return buffer.toString();

		}

	}

	public NodeChangesMaps getChanges(List<Long> propIDs, List<Long> argIDs)
			throws Exception;

	public PropsAndArgs searchProps(String searchString, String searchName, Long filerArgID,
			Long filterPropID) throws Exception;
	
	public PropsAndArgs continueSearchProps( String searchName ) throws Exception;
	
	public LoginInfo getLoginInfo( String requestURI ) throws Exception;

}
