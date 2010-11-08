package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.PropsAndArgs;
import org.argmap.client.Search.SearchResultsHandler;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class ModeEdit extends ResizeComposite implements KeyUpHandler,
		OpenHandler<TreeItem>, CloseHandler<TreeItem>,
		SelectionHandler<TreeItem>, ClickHandler {

	public static final int MAIN_SEARCH_LIMIT = 15;
	public static final String MAIN_SEARCH_NAME = "mainSearch";
	public static final int SIDE_SEARCH_LIMIT = 7;
	public static final String SIDE_SEARCH_NAME = "sideSearch";
	public static final int SEARCH_DELAY = 200;

	private static HTML sideMessageArea;
	private final Label sideSearchLabel;
	private final FlexTable sideSearchResults;
	private final ScrollPanel sideSearchScroll;
	private final ScrollPanel sideMessageScroll;
	public final ScrollPanel treeScrollPanel;
	private final SplitLayoutPanel sideSplit;

	private final TextBox searchTextBox;
	private final EditModeTree tree;
	private final Button addPropButton;
	private final MainSearchTimer mainSearchTimer;
	public final SideSearchTimer sideSearchTimer;
	private final Button mainSearchContinueButton;
	private final Button sideSearchContinueButton;
	private final ArgMap argMap;

	private Search mainSearch;
	private Search sideSearch;
	private final List<ArgMap.Message> sideSearchMessages_DELETE_ME;
	private final List<ArgMap.Message> mainSearchMessages_DELETE_ME;

	public ModeEdit(ArgMap argMap) {
		super();
		this.argMap = argMap;

		/***********************************************************
		 * first setup the tree and start loading the propositions * so that
		 * they can be loading while everything else is * being setup *
		 ***********************************************************/
		/*
		 * setup the tree area
		 */
		tree = new EditModeTree(this);
		tree.addOpenHandlerTracked(this);
		tree.addCloseHandler(this);
		tree.addSelectionHandler(this);
		tree.setAnimationEnabled(false);

		/*
		 * get the props
		 */
		ServerComm.getRootProps(0,
				new ServerComm.LocalCallback<PropsAndArgs>() {

					@Override
					public void call(PropsAndArgs allNodes) {

						Log log = Log.getLog("em.em.cb");
						log.log("Prop Tree From Server");
						for (Proposition proposition : allNodes.rootProps) {

							ViewProp propView = new ViewPropEdit();
							propView.recursiveBuildViewNode(proposition,
									allNodes.nodes, 5);

							tree.addItem(propView);
							// propView.logNodeRecursive(0, "em.em.cb", true);
						}
						tree.resetState();
						if (Log.on)
							tree.logTree(log);
						log.finish();

					}
				});

		/******************
		 * setup side bar *
		 ******************/
		sideMessageArea = new HTML();
		sideSearchMessages_DELETE_ME = new ArrayList<ArgMap.Message>();
		sideSearchLabel = new Label(
				"Would you like to use one of these already existing propositions?");
		sideSearchResults = new FlexTable();
		sideSplit = new SplitLayoutPanel();
		sideSearchTimer = new SideSearchTimer();

		sideSearchContinueButton = new Button("loadMoreResults");
		sideSearchContinueButton.setStylePrimaryName("addPropButton");
		sideSearchContinueButton.addClickHandler(this);
		sideSearchContinueButton.setVisible(false);
		sideSearchLabel.setVisible(false);
		FlowPanel sideSearchArea = new FlowPanel();
		sideSearchArea.add(sideSearchLabel);
		sideSearchArea.add(sideSearchResults);
		sideSearchArea.add(sideSearchContinueButton);

		sideSearchScroll = new ScrollPanel(sideSearchArea);
		sideMessageScroll = new ScrollPanel(sideMessageArea);

		sideSplit.add(sideMessageScroll);
		/*
		 * sideSearchScroll is not added here. Instead it is added/removed as
		 * necessary depending on whether there are search results
		 */

		/*******************
		 * setup main area *
		 *******************/

		/*
		 * setup the search box
		 */
		mainSearchTimer = new MainSearchTimer();
		mainSearchMessages_DELETE_ME = new ArrayList<ArgMap.Message>();
		searchTextBox = new TextBox();

		addPropButton = new Button("Add as new proposition");
		addPropButton.setStylePrimaryName("addPropButton");
		addPropButton.setEnabled(false);
		addPropButton.addClickHandler(this);

		searchTextBox.addKeyUpHandler(this);
		searchTextBox.addStyleName("searchTextBox");
		searchTextBox.setWidth("95%");
		Label searchLabel = new Label("Search:");
		searchLabel.addStyleName("searchLabel");

		DockLayoutPanel searchBoxPanel = new DockLayoutPanel(Unit.EM);
		searchBoxPanel.addStyleName("searchBoxPanel");
		searchBoxPanel.addWest(searchLabel, 4.5);
		/* this flow panel is here so button doesn't grow to tall... */
		FlowPanel addButtonFlowPanel = new FlowPanel();
		addButtonFlowPanel.add(addPropButton);
		searchBoxPanel.addEast(addButtonFlowPanel, 16);
		searchBoxPanel.add(searchTextBox);

		/*
		 * setup the search continue button
		 */
		mainSearchContinueButton = new Button("load more results");
		mainSearchContinueButton.addClickHandler(this);
		mainSearchContinueButton.setVisible(false);
		mainSearchContinueButton.setStylePrimaryName("addPropButton");

		/*
		 * add the tree and the search continue button to a scroll panel
		 */
		FlowPanel treeFlowPanel = new FlowPanel();
		treeFlowPanel.add(tree);
		treeFlowPanel.add(mainSearchContinueButton);
		treeScrollPanel = new ScrollPanel(treeFlowPanel);

		/*
		 * add the search box and tree to the main area
		 */
		DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.EM);
		mainPanel.addNorth(searchBoxPanel, 2.7);
		mainPanel.add(treeScrollPanel);

		/*************************
		 * set up the whole page *
		 *************************/

		SplitLayoutPanel mainSplit = new SplitLayoutPanel();
		mainSplit.addEast(sideSplit, 300);
		mainSplit.add(mainPanel);
		initWidget(mainSplit);
	}

	private void addRootProp() {
		ViewPropEdit newPropView = new ViewPropEdit();

		/*
		 * close the other tree items for (int i = 0; i < tree.getItemCount();
		 * i++) { tree.getItem(i).setState(false); }
		 */

		newPropView.setContent(searchTextBox.getText());
		newPropView.getProposition().setContent(searchTextBox.getText());
		// tree.addItem(newPropView);
		tree.insertItem(0, newPropView);
		newPropView.haveFocus();
		ServerComm.addProp(newPropView.getProposition(), null, 0);
	}

	public static void log(String string) {
		sideMessageArea.setHTML(sideMessageArea.getHTML() + string);
	}

	/* this type of button is used in the side search box */
	private class SideSearchButton extends Button implements ClickHandler {
		int resultIndex;
		List<Proposition> propMatches;

		SideSearchButton(int resultIndex, List<Proposition> propMatches) {
			super("use this");
			this.resultIndex = resultIndex;
			this.propMatches = propMatches;
			addClickHandler(this);
			setStylePrimaryName("addPropButton");
		}

		public void onClick(ClickEvent event) {

			ViewPropEdit propViewToRemove = ViewPropEdit
					.getLastPropositionWithFocus();
			if (propViewToRemove.getChildCount() == 0) {
				class ThisCallback implements LocalCallback<Nodes> {
					ViewArgEdit parentArgView;
					ViewPropEdit propViewToRemove;
					int propIndex;
					Long linkPropID;

					@Override
					public void call(Nodes nodes) {
						parentArgView.removeItem(propViewToRemove);
						Proposition proposition = nodes.props.get(linkPropID);
						ViewProp newViewProp = new ViewPropEdit();
						newViewProp.recursiveBuildViewNode(proposition, nodes,
								5);

						parentArgView.insertChildViewAt(propIndex, newViewProp);
					}
				}
				;
				ThisCallback callback = new ThisCallback();
				/*
				 * TODO handle requests to link to top level nodes more
				 * gracefully... currently throws an exception, instead a
				 * message indicating that top level nodes cannot be linked
				 * would be good...
				 */
				ViewArgEdit parentArgView = propViewToRemove.parentArgView();
				callback.parentArgView = parentArgView;
				callback.propViewToRemove = propViewToRemove;
				callback.propIndex = parentArgView
						.getChildIndex(propViewToRemove);
				Proposition propToLinkTo = propMatches.get(resultIndex);
				callback.linkPropID = propToLinkTo.id;
				ServerComm.replaceWithLinkAndGet(parentArgView.argument,
						propToLinkTo, propViewToRemove.proposition, callback);
			} else {
				ArgMap
						.messageTimed(
								"Cannot link to existing proposition when proposition currently being edited has children",
								MessageType.ERROR);
			}

		}
	}

	/*
	 * this method populates the side bar with results from a server search. it
	 * is called by a ViewPropEdit after a pause in typing or the space bar is
	 * pressed.
	 */
	public void sideSearchCallback(PropsAndArgs propMatches) {

		sideSearchResults.removeAllRows();
		if (propMatches.rootProps.size() > 0) {
			displaySearchBox();
		} else {
			hideSearchBox();
		}
		sideSearchAppendResults(propMatches, 0);
	}

	public void sideSearchContinue() {
		sideSearchMessages_DELETE_ME.add(ArgMap.message("searching...",
				MessageType.INFO));
		ServerComm.continueSearchProps(SIDE_SEARCH_NAME,
				new LocalCallback<ArgMapService.PropsAndArgs>() {

					@Override
					public void call(PropsAndArgs results) {
						sideSearchAppendResults(results, sideSearchResults
								.getRowCount());
					}
				});
	}

	private void sideSearchAppendResults(PropsAndArgs propMatches, int startRow) {
		int i = startRow;
		for (Proposition prop : propMatches.rootProps) {
			sideSearchResults.setText(i, 0, prop.getContent());
			sideSearchResults.setWidget(i, 1, new SideSearchButton(i,
					propMatches.rootProps));
			i++;
		}
		if (propMatches.rootProps.size() == SIDE_SEARCH_LIMIT) {
			sideSearchContinueButton.setVisible(true);
		} else {
			sideSearchContinueButton.setVisible(false);
		}
		ArgMap.Message message = sideSearchMessages_DELETE_ME.remove(0);
		message.setMessage("finished search");
		message.hideAfter(3000);
	}

	public void displaySearchBox() {
		if (!sideSearchScroll.isAttached()) {
			sideSplit.remove(sideMessageScroll);
			sideSplit.addSouth(sideSearchScroll, 400);
			sideSplit.add(sideMessageScroll);
		}
		sideSearchLabel.setVisible(true);
	}

	public void hideSearchBox() {
		sideSplit.remove(sideSearchScroll);
		sideSearchLabel.setVisible(false);
	}

	/**
	 * annoyingly, by default the Tree eats the arrow key events so they can't
	 * be used for moving in a text box. Setting a handler on the tree to keep
	 * the events from doing their default behavior or propagating doesn't seem
	 * to work. I found this fix on stack overflow
	 */
	public class EditModeTree extends ArgTree {

		ModeEdit editMode;

		public EditModeTree(ModeEdit parent) {
			editMode = parent;
		}

		public ModeEdit getEditMode() {
			return editMode;
		}

		@Override
		protected boolean isKeyboardNavigationEnabled(TreeItem inCurrentItem) {
			return false;
		}

		@Override
		public void onBrowserEvent(Event event) {
			int eventType = DOM.eventGetType(event);

			switch (eventType) {
			case Event.ONKEYDOWN:
			case Event.ONKEYPRESS:
			case Event.ONKEYUP:
				return;
			default:
				break;
			}

			super.onBrowserEvent(event);
		}
	}

	public Tree buildTreeCloneOfOpenNodes(Tree cloneTree) {
		Log log = Log.getLog("em.btcoop");
		for (int i = 0; i < tree.getItemCount(); i++) {
			ViewNode viewNode = (ViewNode) tree.getItem(i);
			if (viewNode.getState() || viewNode.isSelected()) {
				/*
				 * notice we use getState() instead of isOpen() becuase for the
				 * root nodes we only want childless nodes if they are currently
				 * selected
				 */
				ViewNode clonedViewNode = recursiveTreeClone(
						(ViewPropEdit) tree.getItem(i), log);
				cloneTree.addItem(clonedViewNode);
			}
		}
		log.finish();
		return cloneTree;
	}

	public ViewNode recursiveTreeClone(ViewNode realViewNode, Log log) {
		log.indent();
		ViewNode cloneViewNode = realViewNode.createViewNodeVerClone();

		for (int i = 0; i < realViewNode.getChildCount(); i++) {
			ViewNode realChild = realViewNode.getChildView(i);
			if (realViewNode.getState()) {
				cloneViewNode.addItem(recursiveTreeClone(realChild, log));
			} else {
				cloneViewNode.addItem(new ViewDummyVer(realChild.getNodeID()));
			}
		}
		log.unindent();
		return cloneViewNode;
	}

	public void getOpenPropsAndArgs(List<Proposition> props, List<Argument> args) {
		for (int i = 0; i < tree.getItemCount(); i++) {
			ViewNode viewNode = (ViewNode) tree.getItem(i);
			/*
			 * notice we use getState() instead of isOpen() becuase for the root
			 * nodes we only we only want childless nodes if they are currently
			 * selected
			 */
			if (viewNode.getState() || viewNode.isSelected()) {
				recursiveGetOpenPropsAndArgs(viewNode, props, args);
			}
		}
	}

	public void recursiveGetOpenPropsAndArgs(ViewNode viewNode,
			List<Proposition> props, List<Argument> args) {
		if (viewNode instanceof ViewProp) {
			props.add((Proposition) viewNode.getNode());
		} else if (viewNode instanceof ViewArg) {
			args.add((Argument) viewNode.getNode());
		}
		if (viewNode.isOpen()) {
			for (int i = 0; i < viewNode.getChildCount(); i++) {
				recursiveGetOpenPropsAndArgs(viewNode.getChildView(i), props,
						args);
			}
		}
	}

	public void getOpenPropsAndArgs_DELETE_ME(List<Proposition> props,
			List<Argument> args) {
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveGetOpenPropsAndArgs(((ViewPropEdit) tree.getItem(i)),
					props, args);
		}
	}

	private abstract class SearchTimer extends Timer {
		public void keyPress(int charCode) {
			if (!getPreviousSearchString().trim().equals(
					getNewSearchString().trim())) {
				/* if its the space bar search and stop the timer */
				if (charCode == 32 || charCode == KeyCodes.KEY_ENTER) {
					cancel();
					run();
				}
				/* if its any other key set timer for .3 seconds */
				else {
					/*
					 * not sure if this is necessary but just in case... (I
					 * don't want to add additional timer to fire rather I want
					 * to replace the previous.)
					 */
					cancel();
					schedule(SEARCH_DELAY);
				}
			}
		}

		public abstract String getPreviousSearchString();

		public abstract String getNewSearchString();
	}

	private class MainSearchTimer extends SearchTimer {
		@Override
		public void run() {
			mainSearch();
		}

		@Override
		public String getNewSearchString() {
			// TODO Auto-generated method stub
		}

		@Override
		public String getPreviousSearchString() {
			// TODO Auto-generated method stub
		}
	}

	public class SideSearchTimer extends SearchTimer {
		private ViewPropEdit viewProp;

		public void setViewProp(ViewPropEdit viewProp) {
			this.viewProp = viewProp;
		}

		@Override
		public void run() {
			sideBarSearch(viewProp);
		}

		@Override
		public String getNewSearchString() {
			// TODO Auto-generated method stub

		}

		@Override
		public String getPreviousSearchString() {
			// TODO Auto-generated method stub

		}
	}

	public void sideBarSearch(ViewPropEdit viewProp) {
		String searchText = viewProp.getContent().trim();
		if (!searchText.equals("") && viewProp.getChildCount() == 0
				&& !viewProp.deleted) {
			sideSearchMessages_DELETE_ME.add(ArgMap.message("searching...",
					MessageType.INFO));
			List<Long> filterIDs = new ArrayList<Long>();
			filterIDs.addAll(viewProp.getAncestorIDs());
			if (viewProp.getParentView() != null) {
				filterIDs.addAll(viewProp.getParentView().getChildIDs());
			}
			ServerComm.searchProps(searchText, ModeEdit.SIDE_SEARCH_NAME,
					ModeEdit.SIDE_SEARCH_LIMIT, filterIDs,
					new LocalCallback<PropsAndArgs>() {
						@Override
						public void call(PropsAndArgs propMatches) {
							sideSearchCallback(propMatches);
						}
					});
		} else {
			hideSearchBox();
		}
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		// Log log = Log.getLog("me.oku");
		int charCode = event.getNativeKeyCode();
		// log.log("registered this key:" + charCode);
		Object source = event.getSource();
		if (source == searchTextBox) {
			String text = searchTextBox.getText().trim();
			if (text.equals("")) {
				addPropButton.setEnabled(false);
			} else {
				addPropButton.setEnabled(true);
			}

			mainSearchTimer.keyPress(charCode);
		}
		// log.finish();
	}

	public void mainSearch() {
		if (mainSearch != null) {
			mainSearch.cancelSearch();
		}
		mainSearch = new Search(searchTextBox.getText(), MAIN_SEARCH_LIMIT,
				null, new SearchResultsHandler() {
					@Override
					public void processSearchResults(PropsAndArgs propsAndArgs) {
						mainSearchAppendResultsToTree(propsAndArgs);
					}

					@Override
					public void searchCompleted() {
						mainSearchContinueButton.setVisible(true);
					}

					@Override
					public void searchExhausted() {
						mainSearchContinueButton.setVisible(false);
					}
				});
		argMap.hideVersions();
		tree.clear();
		mainSearchContinueButton.setVisible(false);
		mainSearch.startSearch();
	}

	public void mainSearchAppendResultsToTree(PropsAndArgs results) {
		for (Proposition proposition : results.rootProps) {

			ViewProp propView = new ViewPropEdit();
			propView.recursiveBuildViewNode(proposition, results.nodes, 1);

			tree.addItem(propView);
		}
		tree.resetState();
	}

	public void loadFromServer(ViewNode viewNode, int loadDepth, int openDepth) {
		List<ViewNode> list = new ArrayList<ViewNode>();
		list.add(viewNode);
		loadFromServer(list, loadDepth, openDepth);
	}

	public void loadFromServer(List<ViewNode> viewNodes, int loadDepth,
			int openDepth) {
		List<Long> viewNodeIDs = new ArrayList<Long>(viewNodes.size());
		for (ViewNode viewNode : viewNodes) {
			assert !viewNode.isLoaded();
			viewNodeIDs.add(viewNode.getNodeID());
		}

		final int openDepthCB = openDepth;
		final List<ViewNode> viewNodesCB = viewNodes;
		ServerComm.getNodesChildren(viewNodeIDs, loadDepth,
				new LocalCallback<Nodes>() {
					@Override
					public void call(Nodes nodes) {
						for (ViewNode source : viewNodesCB) {
							while (source.getChildCount() > 0) {
								source.getChild(0).remove();
							}
							source.recursiveBuildViewNode(source.getNode(),
									nodes, openDepthCB);
							source.setLoaded(true);
						}
						tree.resetState();
					}
				});
	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		if (event.getTarget() instanceof ViewNode) {
			argMap.showVersions();
			ViewNode source = (ViewNode) event.getTarget();
			if (!source.isLoaded()) {
				loadFromServer(source, 2, 1);
			} else {
				List<ViewNode> list = new ArrayList<ViewNode>(source
						.getChildCount());
				for (int i = 0; i < source.getChildCount(); i++) {
					if (!source.getChildView(i).isLoaded()) {
						list.add(source.getChildView(i));
					}
				}
				if (list.size() > 0) {
					loadFromServer(list, 1, 0);
				}
			}
		}
	}

	public void onOpen_OLD(OpenEvent<TreeItem> event) {
		argMap.showVersions();
		Log log = Log.getLog("em.op");
		if (event.getTarget() instanceof ViewNode) {
			ViewNode source = (ViewNode) event.getTarget();
			log.log("AAAA");
			if (!source.isLoaded()) {
				log.log("BBBB");
				loadFromServer(source, 1, 1);
			}
		}
		log.finish();
	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
		if (!somethingForVersions()) {
			argMap.hideVersions();
		}
	}

	private boolean somethingForVersions() {
		if (tree.getSelectedItem() != null) {
			return true;
		}
		for (int i = 0; i < tree.getItemCount(); i++) {
			if (tree.getItem(i).getState()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onSelection(SelectionEvent<TreeItem> event) {
		argMap.showVersions();
	}

	@Override
	public void onClick(ClickEvent event) {
		Object source = event.getSource();
		if (source == mainSearchContinueButton) {
			mainSearch.continueSearch();
		} else if (source == sideSearchContinueButton) {
			sideSearchContinue();
		} else if (source == addPropButton) {
			addRootProp();
		}

	}
}
