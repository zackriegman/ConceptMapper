package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.PropsAndArgs;
import org.argmap.client.Search.SearchResultsHandler;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
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
import com.google.gwt.user.client.ui.HTMLTable;
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
	private Label sideSearchLabel;
	private FlexTable sideSearchResults;
	private ScrollPanel sideSearchScroll;
	private ScrollPanel sideMessageScroll;
	public ScrollPanel treeScrollPanel;
	private SplitLayoutPanel sideSplit;

	private TextBox searchTextBox;
	private final EditModeTree tree;
	private Button addPropButton;
	private MainSearchTimer mainSearchTimer;
	public SideSearchTimer sideSearchTimer;
	private Button mainSearchContinueButton;
	private Button sideSearchContinueButton;
	private final ArgMap argMap;

	private Search mainSearch;
	private Search sideSearch;

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
		getRootProps();

		GWT.runAsync(new RunAsyncCallback() {

			@Override
			public void onSuccess() {
				/******************
				 * setup side bar *
				 ******************/
				sideMessageArea = new HTML();
				sideSearchLabel = new Label(
						"Would you like to use one of these already existing propositions?");
				sideSearchLabel.addStyleName("sideSearchLabel");
				sideSearchResults = new FlexTable();
				sideSplit = new SplitLayoutPanel();
				sideSearchTimer = new SideSearchTimer();

				sideSearchContinueButton = new Button("loadMoreResults");
				sideSearchContinueButton.setStylePrimaryName("addPropButton");
				sideSearchContinueButton.addClickHandler(ModeEdit.this);
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
				 * sideSearchScroll is not added here. Instead it is
				 * added/removed as necessary depending on whether there are
				 * search results
				 */

				/*******************
				 * setup main area *
				 *******************/

				/*
				 * setup the search box
				 */
				mainSearchTimer = new MainSearchTimer();
				searchTextBox = new TextBox();

				addPropButton = new Button("Add as new proposition");
				addPropButton.setStylePrimaryName("addPropButton");
				addPropButton.setEnabled(false);
				addPropButton.addClickHandler(ModeEdit.this);

				searchTextBox.addKeyUpHandler(ModeEdit.this);
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
				mainSearchContinueButton.addClickHandler(ModeEdit.this);
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

			@Override
			public void onFailure(Throwable reason) {
				ArgMap.messageTimed("Code download failed", MessageType.ERROR);
				Log.log("me.me", "Code download failed" + reason.toString());
			}
		});
	}

	private void getRootProps() {
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

	public void sideSearch(ViewPropEdit viewProp) {
		;
		if (sideSearch != null) {
			sideSearch.cancelSearch();
			sideSearch = null;
		}

		sideSearchResults.removeAllRows();
		sideSearchContinueButton.setVisible(false);

		String searchString = viewProp.getContent().trim();
		if (!searchString.equals("") && viewProp.getChildCount() == 0
				&& !viewProp.deleted) {
			List<Long> filterIDs = new ArrayList<Long>();
			filterIDs.addAll(viewProp.getAncestorIDs());
			if (viewProp.getParentView() != null) {
				filterIDs.addAll(viewProp.getParentView().getChildIDs());
			}
			sideSearch = new Search(searchString, ModeEdit.SIDE_SEARCH_LIMIT,
					filterIDs, new SearchResultsHandler() {

						@Override
						public void searchExhausted() {
							sideSearchContinueButton.setVisible(false);
						}

						@Override
						public void searchCompleted() {
							sideSearchContinueButton.setVisible(true);
						}

						@Override
						public void processSearchResults(
								PropsAndArgs propMatches) {
							sideSearchAppendResults(propMatches);
							if (sideSearchResults.getRowCount() > 0) {
								displaySearchBox();
							} else {
								hideSearchBox();
							}
						}
					});
			sideSearch.startSearch();
		} else {
			hideSearchBox();
		}
	}

	private void sideSearchAppendResults(PropsAndArgs propMatches) {
		int i = sideSearchResults.getRowCount();
		HTMLTable.RowFormatter rowFormatter = sideSearchResults
				.getRowFormatter();
		for (Proposition prop : propMatches.rootProps) {
			sideSearchResults.setText(i, 0, prop.getContent());
			sideSearchResults.setWidget(i, 1, new SideSearchButton(i,
					propMatches.rootProps));
			// rowFormatter.addStyle(i, "sideSearchRow");
			rowFormatter.setStylePrimaryName(i, "sideSearchRow");
			i++;
		}
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

	private abstract class SearchTimer extends Timer {
		public void keyPress(int charCode) {
			if (stringsDifferent()) {
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

		protected boolean stringsDifferent() {
			return Search.stringsEffectivelyDifferent(
					getPreviousSearchString(), getNewSearchString());
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
			return searchTextBox.getText();
		}

		@Override
		public String getPreviousSearchString() {
			if (mainSearch != null) {
				return mainSearch.getSearchString();
			} else {
				return "";
			}
		}
	}

	public class SideSearchTimer extends SearchTimer {
		private ViewPropEdit viewProp;

		public void setViewProp(ViewPropEdit viewProp) {
			this.viewProp = viewProp;
			if (stringsDifferent()) {
				run();
			}
		}

		@Override
		public void run() {
			sideSearch(viewProp);
		}

		@Override
		public String getNewSearchString() {
			return viewProp.getContent();

		}

		@Override
		public String getPreviousSearchString() {
			if (sideSearch != null) {
				return sideSearch.getSearchString();
			} else {
				return "";
			}
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
			mainSearch = null;
		}

		argMap.hideVersions();
		tree.clear();
		mainSearchContinueButton.setVisible(false);

		String searchString = searchTextBox.getText().trim();

		if (!searchString.equals("")) {
			mainSearch = new Search(searchString, MAIN_SEARCH_LIMIT, null,
					new SearchResultsHandler() {
						@Override
						public void processSearchResults(
								PropsAndArgs propsAndArgs) {
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
			mainSearch.startSearch();
		} else {
			getRootProps();
		}
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
			sideSearch.continueSearch();
		} else if (source == addPropButton) {
			addRootProp();
		}

	}
}
