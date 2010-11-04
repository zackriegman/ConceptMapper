package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.PropsAndArgs;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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

public class ModeEdit extends ResizeComposite implements
		LocalCallback<PropsAndArgs>, KeyUpHandler, OpenHandler<TreeItem>,
		CloseHandler<TreeItem>, SelectionHandler<TreeItem>, ClickHandler {
	
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

	public ModeEdit(ArgMap argMap) {
		super();
		this.argMap = argMap;
		
		/***********************************************************
		 * first setup the tree and start loading the propositions *
		 * so that they can be loading while everything else is    *
		 * being setup                                             *
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
									allNodes.nodes);

							tree.addItem(propView);
							// propView.logNodeRecursive(0, "em.em.cb", true);
						}
						tree.resetState();
						if (Log.on) tree.logTree(log);
						log.finish();

					}
				});
		
		/******************
		 * setup side bar *
		 ******************/
		sideMessageArea = new HTML();
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
		searchTextBox = new TextBox();
		
		addPropButton = new Button("Add as new proposition");
		addPropButton.setStylePrimaryName("addPropButton");
		addPropButton.setEnabled(false);
		addPropButton.addClickHandler(this);

		searchTextBox.addKeyUpHandler(this);
		searchTextBox.addStyleName("searchTextBox");
		searchTextBox.setWidth("100%");
		Label searchLabel = new Label("Search:");
		searchLabel.addStyleName("searchLabel");

		DockLayoutPanel searchBoxPanel = new DockLayoutPanel(Unit.EM);
		searchBoxPanel.addStyleName("searchBoxPanel");
		searchBoxPanel.addWest(searchLabel, 4.5);
		/* this flow panel is here so button doesn't grow to tall... */
		FlowPanel addButtonFlowPanel = new FlowPanel();
		addButtonFlowPanel.add(addPropButton);
		searchBoxPanel.addEast(addButtonFlowPanel, 14);
		searchBoxPanel.add(searchTextBox);

		/*
		 * setup the search continue button
		 */
		mainSearchContinueButton = new Button("load more results");
		mainSearchContinueButton.addClickHandler(this);
		mainSearchContinueButton.setVisible(false);
		mainSearchContinueButton.setStylePrimaryName("addPropButton");

		/*
		 * add the tree and the search continue button to a scroll
		 * panel
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

		// close the other tree items
		for (int i = 0; i < tree.getItemCount(); i++) {
			tree.getItem(i).setState(false);
		}

		newPropView.setContent(searchTextBox.getText());
		newPropView.getProposition().setContent(searchTextBox.getText());
		tree.addItem(newPropView);
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
						newViewProp.recursiveBuildViewNode(proposition, nodes);

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
				ArgMap.message(
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
	@Override
	public void call(PropsAndArgs propMatches) {

		sideSearchResults.removeAllRows();
		if (propMatches.rootProps.size() > 0) {
			displaySearchBox();
		} else {
			hideSearchBox();
		}
		sideSearchAppendResults(propMatches, 0);
	}

	public void sideSearchContinue() {
		ServerComm.continueSearchProps(SIDE_SEARCH_NAME,
				new LocalCallback<ArgMapService.PropsAndArgs>() {

					@Override
					public void call(PropsAndArgs results) {
						sideSearchAppendResults(results,
								sideSearchResults.getRowCount());
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
		public void keyPress(int charCode){
			/* if its the space bar search and stop the timer */
			if (charCode == 32) {
				cancel();
				run();
			}
			/* if its any other key set timer for .3 seconds */
			else {
				schedule(SEARCH_DELAY);
			}
		}
	}
	
	private class MainSearchTimer extends SearchTimer {
		@Override
		public void run() {
			mainSearch();
		}
	}
	
	public class SideSearchTimer extends SearchTimer {
		private ViewPropEdit viewProp;
		
		public void setViewProp(ViewPropEdit viewProp ){
			this.viewProp = viewProp;
		}

		@Override
		public void run() {
			sideBarSearch( viewProp );
		}
	}
	
	public void sideBarSearch(ViewPropEdit viewProp ) {
		String searchText = viewProp.getContent().trim();
		if (!searchText.equals("") && viewProp.getChildCount() == 0 && !viewProp.deleted) {
			ServerComm.searchProps(searchText, ModeEdit.SIDE_SEARCH_NAME,
					ModeEdit.SIDE_SEARCH_LIMIT, viewProp.parentArgument(), viewProp.getNode(),
					this);
		} else {
			hideSearchBox();
		}
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		Log log = Log.getLog("me.oku");
		int charCode = event.getNativeKeyCode();
		log.log("registered this key:" + charCode);
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
		log.finish();
	}

	public void mainSearch() {
		Log log = Log.getLog("me.ms");
		log.log("SEARCHING");
		ServerComm.searchProps(searchTextBox.getText(), MAIN_SEARCH_NAME,
				MAIN_SEARCH_LIMIT, null, null,
				new LocalCallback<PropsAndArgs>() {

					@Override
					public void call(PropsAndArgs results) {
						argMap.hideVersions();
						tree.clear();
						mainSearchAppendResultsToTree(results);
					}
				});
		log.finish();
	}

	public void mainSearchContinue() {
		ServerComm.continueSearchProps(MAIN_SEARCH_NAME,
				new LocalCallback<ArgMapService.PropsAndArgs>() {

					@Override
					public void call(PropsAndArgs results) {
						mainSearchAppendResultsToTree(results);
					}
				});
	}

	public void mainSearchAppendResultsToTree(PropsAndArgs results) {
		for (Proposition proposition : results.rootProps) {

			ViewProp propView = new ViewPropEdit();
			propView.recursiveBuildViewNode(proposition, results.nodes);

			tree.addItem(propView);
		}
		tree.resetState();
		if (results.rootProps.size() == MAIN_SEARCH_LIMIT) {
			mainSearchContinueButton.setVisible(true);
		} else {
			mainSearchContinueButton.setVisible(false);
		}
	}

	public void loadFromServer(ViewNode viewNode, int depth) {
		assert !viewNode.isLoaded();

		List<Long> list = new ArrayList<Long>();
		list.add(viewNode.getNodeID());

		class Callback implements LocalCallback<Nodes> {
			ViewNode source;

			@Override
			public void call(Nodes nodes) {
				// GWT.log("Returned nodes: " + nodes.toString());
				while (source.getChildCount() > 0) {
					source.getChild(0).remove();
				}
				source.recursiveBuildViewNode(source.getNode(), nodes);
				source.setLoaded(true);
				tree.resetState();
			}
		}

		Callback callback = new Callback();
		callback.source = viewNode;

		ServerComm.getNodesChildren(list, depth, callback);

	}

	@Override
	public void onOpen(OpenEvent<TreeItem> event) {
		argMap.showVersions();
		Log log = Log.getLog("em.op");
		if (event.getTarget() instanceof ViewNode) {
			ViewNode source = (ViewNode) event.getTarget();
			// source.setOpen(true);
			if (!source.isLoaded()) {
				loadFromServer(source, 1);
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
			mainSearchContinue();
		} else if (source == sideSearchContinueButton) {
			sideSearchContinue();
		} else if (source == addPropButton) {
			addRootProp();
		}

	}
}
