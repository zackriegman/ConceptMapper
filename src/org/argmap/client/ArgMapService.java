package org.argmap.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("argServ")
public interface ArgMapService extends RemoteService {

	public void logClientException(String ExceptionStr);

	public NodesAndNode addProp(NodeInfo parentArg, int position, String content)
			throws ServiceException;

	public Map<Long, Node> deleteProp(Long propID) throws ServiceException;

	public Map<Long, Node> deleteArg(Long argID) throws ServiceException;

	public NodesAndNode addArg(NodeInfo parentProp, boolean pro) throws ServiceException;

	public Map<Long, Node> updateProp(NodeInfo prop, String content) throws ServiceException;

	public Map<Long, Node> updateArg(NodeInfo arg, String content) throws ServiceException;

	public void linkProposition(Argument arg, int position,
			Long propID) throws ServiceException;

	public Map<Long, Node> unlinkProp(NodeInfo parentArg, NodeInfo linkProp)
			throws ServiceException;

	public class PartialTrees_DELETE_ME implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public List<Proposition> rootProps;
		public Map<Long, Node> nodes;
	}
	
	public class PartialTrees implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public List<Long> rootIDs = new ArrayList<Long>();
		public Map<Long, Node> nodes = new HashMap<Long, Node>();
	}

	public PartialTrees_DELETE_ME getRootProps(int depthLimit);

	public Map<Long, Node> replaceWithLinkAndGet(NodeInfo parentArg, NodeInfo linkProp,
			Long removePropID) throws ServiceException;

	public class NodesWithHistory implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public Map<Long, Node> nodes;
		public SortedMap<Date, Change> changes;
	}

	public class NodeWithChanges implements Serializable {
		private static final long serialVersionUID = 1L;
		public Node node;
		public NodeChanges nodeChanges;
		public Map<Long, Proposition> unlinkedLinks = new HashMap<Long, Proposition>();
	}

	public Map<Long, Node> getNodesChildren(List<Long> nodeIDs, int depth)
			throws ServiceException;

	public Map<Long, NodeWithChanges> getPropsWithChanges(List<Long> propIDs)
			throws ServiceException;

	public Map<Long, NodeWithChanges> getArgsWithChanges(List<Long> argIDs)
			throws ServiceException;

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
	
	public class NodesAndNode implements Serializable {
		private static final long serialVersionUID = 1L;
		public Map<Long, Node> nodes;
		public Node node;
	}
	
	public class NodeInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		public DateAndChildIDs info = new DateAndChildIDs();
		public Long id;
		
		public NodeInfo( Node node ){
			info.date = node.updated;
			info.childIDs = new HashSet<Long>( node.childIDs );
			id = node.id;
		}
	}
	
	public class DateAndChildIDs implements Serializable {
		private static final long serialVersionUID = 1L;
		public Date date;
		public Set<Long> childIDs;
		
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("{ date:" + date + "; childIDs:");
			for( Long id : childIDs ){
				sb.append("" + id + " ");
			}
			sb.append("}");
			return sb.toString();
		}
	}
	
	public class ForwardChanges implements Serializable {
		private static final long serialVersionUID = 1L;
		public List<Change> changes;
		public Date date;
	}
	
	public ForwardChanges getNewChanges_DELETE_ME(Date date, Set<Long> propIDs, Set<Long> argIDs ) throws ServiceException;

	public PartialTrees getUpToDateNodes( Map<Long, DateAndChildIDs> propInfo, Map<Long, DateAndChildIDs> argInfo );
	
	public NodeChangesMaps getChanges(List<Long> propIDs, List<Long> argIDs)
			throws ServiceException;

	public PartialTrees_DELETE_ME searchProps(String searchString, String searchName, int resultLimit, List<Long> filerNodeIDs) throws ServiceException;
	
	public PartialTrees_DELETE_ME continueSearchProps( String searchName ) throws ServiceException;
	
	public LoginInfo getLoginInfo( String requestURI ) throws ServiceException;

}
