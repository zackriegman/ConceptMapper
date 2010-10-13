package com.appspot.conceptmapper.client;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.appspot.conceptmapper.client.PropositionService.NodeChangesMaps;
import com.appspot.conceptmapper.client.PropositionService.NodesWithHistory;
import com.appspot.conceptmapper.client.ServerComm.LocalCallback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class VersionsMode extends ResizeComposite implements
		CloseHandler<TreeItem>, OpenHandler<TreeItem>, ChangeHandler {

	private ListBox versionList = new ListBox();
	private EditMode editMode;
	private Tree treeClone = null;
	private TimeTraveler mainTT;
	private FlowPanel treePanel = new FlowPanel();
	private final int LIST_WIDTH = 20;

	// SplitLayoutPanel mainPanel;

	private HandlerRegistration listBoxChangeHandlerRegistration;

	public VersionsMode(EditMode editModePair) {
		super();
		this.editMode = editModePair;
		DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.EM);
		// mainPanel = new SplitLayoutPanel();

		// add(versionList);
		mainPanel.addWest(versionList, LIST_WIDTH);
		mainPanel.add(new ScrollPanel(treePanel));
		// mainPanel.add(new Label("HEELO"));

		versionList.setVisibleItemCount(2);
		versionList.setWidth(LIST_WIDTH + "em");

		listBoxChangeHandlerRegistration = versionList.addChangeHandler(this);
		initWidget(mainPanel);
	}

	public void resetState(Tree tree) {
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveResetState(tree.getItem(i));
		}
	}

	public void recursiveResetState(TreeItem item) {
		/*
		 * if this item has children, and the first child is not a dummy node
		 * place holder loading message
		 */
		if (item.getChildCount() > 0
				&& !item.getChild(0).getStyleName().equals("loadDummyProp")
				&& !item.getChild(0).getStyleName().equals("loadDummyArg")) {
			item.setState(true);
			for (int i = 0; i < item.getChildCount(); i++) {
				recursiveResetState(item.getChild(i));
			}
		}
	}

	/*
	 * public void printPropRecursive(PropositionView propViewParent, int level)
	 * { GWT.log(spaces(level * 2) + "propID:" + propViewParent.proposition.id +
	 * "; content:" + propViewParent.getContent()); for (int i = 0; i <
	 * propViewParent.getChildCount(); i++) { ArgumentView arg = (ArgumentView)
	 * propViewParent.getChild(i); printArgRecursive( arg, level + 1 ); } }
	 * 
	 * public void printArgRecursive( ArgumentView arg, int level ){
	 * GWT.log(spaces(level * 2) + arg.getText() + "; id:" + arg.argument.id);
	 * for (int j = 0; j < arg.getChildCount(); j++) {
	 * printPropRecursive((PropositionView) arg.getChild(j), level + 1); } }
	 * 
	 * public String spaces(int spaces) { String string = ""; for (int i = 0; i
	 * < spaces; i++) { string = string + " "; } return string; }
	 */

	public void displayVersions() {
		versionList.clear();
		versionList.addItem("Loading Revision History From Server...");
		if (treeClone != null) {
			treePanel.remove(treeClone);
		}
		List<Proposition> props = new LinkedList<Proposition>();
		List<Argument> args = new LinkedList<Argument>();
		editMode.getOpenPropsAndArgs(props, args);
		ServerComm.getRevisions(null, props, args,
				new ServerComm.LocalCallback<SortedMap<Date, Change>>() {

					@Override
					public void call(SortedMap<Date, Change> changes) {

						GWT.log("Got back " + changes.size() + " changes");

						Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
						Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

						treeClone = new Tree();
						treeClone.addCloseHandler(VersionsMode.this);
						treeClone.addOpenHandler(VersionsMode.this);
						/*
						 * TODO this could be done outside of the callback to
						 * reduce the user's wait time, but would need to ensure
						 * that it finishing before the callback proceeds. can
						 * the callback just keep calling something like wait()
						 * until it finds that the clone is finished?
						 */
						editMode.buildTreeCloneOfOpenNodesWithIndexes(
								treeClone, propViewIndex, argViewIndex);
						treePanel.add(treeClone);

						mainTT = new TimeTraveler(changes, propViewIndex,
								argViewIndex, treeClone);

						loadVersionListFromTimeMachine();

						versionList.setSelectedIndex(0);

						onChange(null);
					}
				});
		ServerComm.getChanges( props, args,
				new ServerComm.LocalCallback<NodeChangesMaps>() {


					@Override
					public void call(NodeChangesMaps changesMaps) {
						GWT.log(changesMaps.toString());
						
					}
		});
	}

	private void loadVersionListFromTimeMachine() {
		listBoxChangeHandlerRegistration.removeHandler();
		int selectedIndex = versionList.getSelectedIndex();
		String currentDate;
		if (selectedIndex >= 0) {
			currentDate = versionList.getValue(selectedIndex);
		} else {
			currentDate = null;
		}
		versionList.clear();

		List<Change> reverseList = mainTT.getChangeList();
		Collections.reverse(reverseList);
		int i = 0;
		int newSelectionIndex = 0;
		for (Change change : reverseList) {
			String timeString = "" + change.date.getTime();
			if (timeString.equals(currentDate)) {
				GWT.log("###########   Selecting Item:" + i);
				newSelectionIndex = i;
			}
			versionList.addItem("" + change.date + " [" + change.changeType
					+ "]", "" + timeString);
			i++;
		}

		versionList.setSelectedIndex(newSelectionIndex);

		listBoxChangeHandlerRegistration = versionList
				.addChangeHandler(VersionsMode.this);
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0) {
			if (treeItem.getChild(0).getStyleName().equals("loadDummyProp")) {
				// ServerComm.getPropositionCurrentVersionAndHistory(prop,
				// localCallback)
				GWT.log("VersionsMode.onClose[loadDummyProp]:  NOT REMOVING -- METHOD NOT IMPLEMENTED!");
			} else if (treeItem.getChild(0).getStyleName()
					.equals("loadDummyArg")) {
				GWT.log("VersionsMode.onClose[loadDummyArg]:  NOT REMOVING -- METHOD NOT IMPLEMENTED!");
			}
		}

	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		TreeItem treeItem = event.getTarget();
		if (treeItem.getChildCount() > 0) {
			TreeItem child = treeItem.getChild(0);
			if (child.getStyleName().equals("loadDummyProp")) {
				class Callback implements
						ServerComm.LocalCallback<NodesWithHistory> {
					ViewPropEdit propView;

					@Override
					public void call(NodesWithHistory propTreeWithHistory) {
						mergeLoadedProposition(propView.proposition, propTreeWithHistory);
					}
				}
				Callback callback = new Callback();
				callback.propView = (ViewPropEdit) treeItem;

				ServerComm.getPropositionCurrentVersionAndHistory(
						((ViewPropEdit) treeItem).proposition, callback);
			} else if (child.getStyleName().equals("loadDummyArg")) {
				class Callback implements LocalCallback<NodesWithHistory> {
					ViewArgVer argView;

					@Override
					public void call(NodesWithHistory argTreeWithHistory) {
						mergeLoadedArgument(argView.argument, argTreeWithHistory);
					}
				}
				Callback callback = new Callback();
				callback.argView = (ViewArgVer) treeItem;

				ServerComm.getArgumentCurrentVersionAndHistory(
						((ViewArgVer) treeItem).argument, callback);
			}
		}

	}

	public void mergeLoadedProposition(Proposition proposition, NodesWithHistory propTreeWithHistory) {

		GWT.log("mergeLoadedPropositon: start");
		Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
		Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

		ViewPropVer propGraft = ViewProp.recursiveBuildPropositionView(proposition, 
						propTreeWithHistory.nodes,
						propViewIndex, argViewIndex, ViewPropVer.FACTORY, ViewArgVer.FACTORY);

		GWT.log("propTree before timeTravel:");
		propGraft.printPropRecursive(0);
		TimeTraveler timeTraveler = new TimeTraveler(
				propTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		timeTraveler.travelToDate(mainTT.getCurrentDate());
		GWT.log("propTree after timeTravel:");
		propGraft.printPropRecursive(0);

		ViewPropVer view = mainTT.absorb(timeTraveler, propGraft);
		GWT.log("old propview after grafting:");
		view.printPropRecursive(0);
		GWT.log("----------------");

		loadVersionListFromTimeMachine();

		// GWT.log("Prop tree transplant");
		// printPropRecursive(propTree, 0);
		GWT.log("changes");
		for (Change change : propTreeWithHistory.changes.values()) {
			GWT.log(change.toString());
		}
		GWT.log("mergeLoadedPropositon: start");
	}

	public void mergeLoadedArgument(Argument argument, NodesWithHistory argTreeWithHistory) {

		GWT.log("mergeLoadedArgument: start");
		Map<Long, ViewPropVer> propViewIndex = new HashMap<Long, ViewPropVer>();
		Map<Long, ViewArgVer> argViewIndex = new HashMap<Long, ViewArgVer>();

		ViewArgVer argGraft = ViewArg.recursiveBuildArgumentView(argument, argTreeWithHistory.nodes,
						propViewIndex, argViewIndex, ViewPropVer.FACTORY, ViewArgVer.FACTORY);

		GWT.log("propTree before timeTravel:");
		argGraft.printArgRecursive(0);
		TimeTraveler timeTraveler = new TimeTraveler(
				argTreeWithHistory.changes, propViewIndex, argViewIndex, null);

		/*
		 * propViewParent.getChild(0).remove(); while (propTree.getChildCount()
		 * > 0) { TreeItem transplant = propTree.getChild(0);
		 * transplant.remove(); propViewParent.addItem(transplant); }
		 */

		timeTraveler.travelToDate(mainTT.getCurrentDate());
		GWT.log("argTree after timeTravel:");
		argGraft.printArgRecursive(0);

		ViewArgVer view = mainTT.absorb(timeTraveler, argGraft);
		GWT.log("old propview after grafting:");
		view.printArgRecursive(0);
		GWT.log("----------------");

		loadVersionListFromTimeMachine();

		// GWT.log("Prop tree transplant");
		// printPropRecursive(propTree, 0);
		GWT.log("changes");
		for (Change change : argTreeWithHistory.changes.values()) {
			GWT.log(change.toString());
		}
		GWT.log("mergeLoadedPropositon: start");
	}

	@Override
	public void onChange(ChangeEvent event) {
		String millisecondStr = versionList.getValue(versionList
				.getSelectedIndex());
		mainTT.travelToDate(new Date(Long.parseLong(millisecondStr)));
		resetState(treeClone);
	}
	
	/*
	public ViewPropVer recursiveBuildPropositionView(Proposition prop,
			boolean editable, Nodes nodes,
			Map<Long, ViewPropVer> propViewIndex,
			Map<Long, ViewArgVer> argViewIndex) {

		ViewPropVer propView = new ViewPropVer(prop);
		if (propViewIndex != null)
			propViewIndex.put(prop.id, propView);
		for (Long argID : prop.argIDs) {
			Argument argument = nodes.args.get(argID);
			propView.addItem(recursiveBuildArgumentView(argument, editable,
					nodes, propViewIndex, argViewIndex));
		}
		return propView;
	}

	public ViewArgVer recursiveBuildArgumentView(Argument arg,
			boolean editable, Nodes nodes,
			Map<Long, ViewPropVer> propViewIndex,
			Map<Long, ViewArgVer> argViewIndex) {

		ViewArgVer argView = new ViewArgVer(arg);
		if (argViewIndex != null)
			argViewIndex.put(arg.id, argView);
		for (Long propID : arg.propIDs) {
			Proposition proposition = nodes.props.get(propID);
			argView.addItem(recursiveBuildPropositionView(proposition,
					editable, nodes, propViewIndex, argViewIndex));
		}
		return argView;
	}
	*/
}
