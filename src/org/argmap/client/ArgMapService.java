package org.argmap.client;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("argServ")
public interface ArgMapService extends RemoteService {

	public void logClientException(String ExceptionStr);

	public Long addProp(Long parentArgID, int position, String content)
			throws ServiceException;

	public void deleteProp(Long propID) throws ServiceException;

	public void deleteArg(Long argID) throws ServiceException;

	public Long addArg(Long parentPropID, boolean pro) throws ServiceException;

	public void updateProp(Long propID, String content) throws ServiceException;

	public void updateArg(Long argID, String content) throws ServiceException;

	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws ServiceException;

	public void unlinkProp(Long parentArgID, Long propositionID)
			throws ServiceException;

	public class PartialTrees implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public List<Proposition> rootProps;
		public Map<Long, Node> nodes;
	}

	public PartialTrees getRootProps(int depthLimit);

	public Map<Long, Node> replaceWithLinkAndGet(Long parentArgID, Long linkPropID,
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
	
	public class DateAndChildIDs implements Serializable {
		private static final long serialVersionUID = 1L;
		public Date date;
		public Set<Long> childIDs;
	}
	
	public class ForwardChanges implements Serializable {
		private static final long serialVersionUID = 1L;
		public List<Change> changes;
		public Date date;
	}
	
	public ForwardChanges getNewChanges_DELETE_ME(Date date, Set<Long> propIDs, Set<Long> argIDs ) throws ServiceException;

	public Map<Long, Node> getUpToDateNodes( Map<Long, DateAndChildIDs> propInfo, Map<Long, DateAndChildIDs> argInfo );
	
	public NodeChangesMaps getChanges(List<Long> propIDs, List<Long> argIDs)
			throws ServiceException;

	public PartialTrees searchProps(String searchString, String searchName, int resultLimit, List<Long> filerNodeIDs) throws ServiceException;
	
	public PartialTrees continueSearchProps( String searchName ) throws ServiceException;
	
	public LoginInfo getLoginInfo( String requestURI ) throws ServiceException;

}
