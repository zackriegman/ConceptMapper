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

	public Proposition addProp(Long parentArgID, int position, String content)
			throws ServiceException;

	public void deleteProp(Long propID) throws ServiceException;

	public void deleteArg(Long argID) throws ServiceException;

	public Argument addArg(Long parentPropID, boolean pro)
			throws ServiceException;

	public void updateProp(Long propID, String content) throws ServiceException;

	public void updateArg(Long argID, String content) throws ServiceException;

	public void linkProposition(Long parentArgID, int position,
			Long propositionID) throws ServiceException;

	public void unlinkProp(Long parentArgID, Long propositionID)
			throws ServiceException;

	public class PartialTrees implements Serializable {
		/* added to suppress warnings */
		private static final long serialVersionUID = 1L;
		public List<Long> rootIDs = new ArrayList<Long>();
		public Map<Long, Node> nodes = new HashMap<Long, Node>();
		public Map<Long, Integer> ratings = new HashMap<Long, Integer>();

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (Long id : rootIDs) {
				Set<Long> parentPath = new HashSet<Long>();
				recursiveToString(sb, 0, nodes.get(id), parentPath);
			}
			return sb.toString();
		}

		private void recursiveToString(StringBuilder sb, int indent, Node node,
				Set<Long> ancestors) {
			sb.append("\n" + Log.spaces(indent * 3) + node.toString());
			if (ancestors.add(node.id)) {
				for (Long id : node.childIDs) {
					Node child = nodes.get(id);
					if (child != null) {
						recursiveToString(sb, indent + 1, child, ancestors);
					}
				}
			}
		}
	}

	public PartialTrees getRootProps(int depthLimit);

	public PartialTrees replaceWithLinkAndGet(Long parentArgID,
			Long linkPropID, Long removePropID, boolean negated)
			throws ServiceException;

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

	public PartialTrees getNodesChildren(List<Long> nodeIDs, int depth)
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

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{ date:" + date + "; childIDs:");
			for (Long id : childIDs) {
				sb.append("" + id + " ");
			}
			sb.append("}");
			return sb.toString();
		}

		/* need no arg constructor for RPC serializer */
		public DateAndChildIDs() {
		}

		public DateAndChildIDs(Node node) {
			date = node.updated;
			childIDs = new HashSet<Long>(node.childIDs);
		}
	}

	public PartialTrees getUpToDateNodes(Map<Long, DateAndChildIDs> propInfo,
			Map<Long, DateAndChildIDs> argInfo);

	public NodeChangesMaps getChanges(List<Long> propIDs, List<Long> argIDs)
			throws ServiceException;

	public PartialTrees searchProps(String searchString, String searchName,
			int resultLimit, List<Long> filerNodeIDs) throws ServiceException;

	public PartialTrees continueSearchProps(String searchName)
			throws ServiceException;

	public Integer getRating(Long propID);

	public void setRating(Long propID, Integer rating) throws ServiceException;

	public LoginInfo getLoginInfo(String requestURI) throws ServiceException;

	public String getTextPage(String pageName) throws ServiceException;

}
