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
		CloseHandler<TreeItem>, SelectionHandler<TreeItem> {

	private static HTML sideMessageArea = new HTML();
	private final Label sideSearchLabel = new Label(
			"Would you like to use one of these already existing propositions?");
	private final FlexTable sideSearchResults = new FlexTable();
	private final ScrollPanel sideSearchScroll;
	private final ScrollPanel sideMessageScroll;
	public final ScrollPanel treeScrollPanel;
	private final SplitLayoutPanel sideSplit = new SplitLayoutPanel();
	private final SearchTimer searchTimer = new SearchTimer();

	TextBox searchTextBox = new TextBox();
	private final EditModeTree tree;
	Button addPropButton;
	private final ArgMap argMap;

	public ModeEdit(ArgMap argMap) {
		super();
		this.argMap = argMap;

		/******************
		 * setup side bar *
		 ******************/

		sideSearchLabel.setVisible(false);
		FlowPanel sideSearchArea = new FlowPanel();
		sideSearchArea.add(sideSearchLabel);
		sideSearchArea.add(sideSearchResults);

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
		addPropButton = new Button("Add as new proposition");
		addPropButton.setStylePrimaryName("addPropButton");
		addPropButton.setEnabled(false);
		addPropButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				addRootProp();

			}
		});

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
		// searchTextBox.addStyleName("flowPanel-left");
		// addPropButton.addStyleName("flowPanel-left");
		// searchLabel.addStyleName("flowPanel-left");

		// ScrollPanel searchBoxScrollPanel = new ScrollPanel( searchBoxPanel );

		/*
		 * setup the tree
		 */
		tree = new EditModeTree(this);
		tree.addOpenHandlerTracked(this);
		tree.addCloseHandler(this);
		tree.addSelectionHandler(this);
		tree.setAnimationEnabled(false);
		treeScrollPanel = new ScrollPanel(tree);

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

		/*****************
		 * get the props *
		 *****************/

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
						if (Log.on)
							tree.logTree(log);
						log.finish();

					}
				});
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

	@Override
	public void call(PropsAndArgs propMatches) {
		class SearchButton extends Button implements ClickHandler {
			int resultIndex;
			List<Proposition> propMatches;

			SearchButton(int resultIndex, List<Proposition> propMatches) {
				super("use this");
				this.resultIndex = resultIndex;
				this.propMatches = propMatches;
				addClickHandler(this);
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
							Proposition proposition = nodes.props
									.get(linkPropID);
							ViewProp newViewProp = new ViewPropEdit();
							newViewProp.recursiveBuildViewNode(proposition,
									nodes);

							parentArgView.insertChildViewAt(propIndex,
									newViewProp);
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
					ViewArgEdit parentArgView = propViewToRemove
							.parentArgView();
					callback.parentArgView = parentArgView;
					callback.propViewToRemove = propViewToRemove;
					callback.propIndex = parentArgView
							.getChildIndex(propViewToRemove);
					Proposition propToLinkTo = propMatches.get(resultIndex);
					callback.linkPropID = propToLinkTo.id;
					ServerComm.replaceWithLinkAndGet(parentArgView.argument,
							propToLinkTo, propViewToRemove.proposition,
							callback);
				} else {
					ArgMap.message(
							"Cannot link to existing proposition when proposition currently being edited has children",
							MessageType.ERROR);
				}

			}
		}
		sideSearchResults.removeAllRows();
		if (propMatches.rootProps.size() > 0) {
			displaySearchBox();
		} else {
			hideSearchBox();
		}
		int i = 0;
		for (Proposition prop : propMatches.rootProps) {
			sideSearchResults.setText(i, 0, prop.getContent());
			sideSearchResults.setWidget(i, 1, new SearchButton(i,
					propMatches.rootProps));
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

	public void getOpenPropsAndArgs_DELETE_ME(List<Proposition> props,
			List<Argument> args) {
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveGetOpenPropsAndArgs(((ViewPropEdit) tree.getItem(i)),
					props, args);
		}
	}
	
	private class SearchTimer extends Timer {

		@Override
		public void run() {
			mainSearch();			
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
			
			/* if its the space bar search and stop the timer */
			if (charCode == 32) {
				log.logln( "canceling timer and doing search");
				searchTimer.cancel();
				mainSearch();
			} 
			/* if its any other key set timer for .3 seconds */
			else {
				log.logln( "reseting timer for 500 millis");
				searchTimer.schedule(500);
			}
			
		}
		log.finish();
	}
	
	public void mainSearch(){
		Log log = Log.getLog("me.ms");
		log.log("SEARCHING");
		ServerComm.searchProps(searchTextBox.getText(), "mainSearch", null, null,
				new LocalCallback<PropsAndArgs>() {

					@Override
					public void call(PropsAndArgs results) {
						argMap.hideVersions();
						tree.clear();
						for (Proposition proposition : results.rootProps) {

							ViewProp propView = new ViewPropEdit();
							propView.recursiveBuildViewNode(proposition,
									results.nodes);

							tree.addItem(propView);
						}
						tree.resetState();
					}
				});
		log.finish();
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
}
