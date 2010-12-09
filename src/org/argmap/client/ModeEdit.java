package org.argmap.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.argmap.client.ArgMap.Message;
import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.DateAndChildIDs;
import org.argmap.client.ArgMapService.PartialTrees;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

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
	private PopupPanel sideSearchPopupPanel;
	private ScrollPanel sideSearchScroll;
	private ScrollPanel sideMessageScroll;
	public ScrollPanel treeScrollPanel;
	// private SplitLayoutPanel sideSplit;

	private TextBox searchTextBox;
	private final EditModeTree tree;
	private Button addPropButton;
	private MainSearchTimer mainSearchTimer;
	public SideSearchTimer sideSearchTimer;
	public EditContentSaveTimer editContentSaveTimer;
	private Button mainSearchContinueButton;
	private Button sideSearchContinueButton;
	private final ArgMap argMap;
	private final Map<String, String> loadedPages = new HashMap<String, String>();

	public final UpdateTimer updateTimer;
	private Search mainSearch;
	private Search sideSearch;

	/*
	 * rather than keep these lists of loadedProps and loadedArgs it really
	 * might just make a lot more sense to walk through the tree to generate a
	 * list of loaded nodes to send to the server, and then walk through the
	 * tree to decide whether each node needs to be updated. [I initially
	 * started keeping these lists when when updates were implemented throw
	 * Change lists, but now that we aren't using Changes, these lists probably
	 * aren't necessary anymore] On the other hand if they save even a fraction
	 * of a second while performing an update they might be worth it because
	 * updates are running every few seconds to even a small delay could be
	 * jarring
	 */
	private final MultiMap<Long, ViewProp> loadedProps = new MultiMap<Long, ViewProp>();
	private final MultiMap<Long, ViewArg> loadedArgs = new MultiMap<Long, ViewArg>();

	// private Date lastUpdate;

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
		tree = new EditModeTree();
		tree.addOpenHandlerTracked(this);
		tree.addCloseHandler(this);
		tree.addSelectionHandler(this);
		tree.setAnimationEnabled(false);

		/*
		 * get the props and preload the callback
		 */
		getRootProps();
		getRootPropsCallback(null);

		/*
		 * setup the update timer
		 */
		updateTimer = new UpdateTimer();

		GWT.runAsync(new RunAsyncCallback() {

			@Override
			public void onSuccess() {
				/***********************
				 * setup search pop up *
				 ***********************/

				sideSearchLabel = new Label(
						"Would you like to use one of these already existing propositions?");
				sideSearchLabel.addStyleName("sideSearchLabel");
				sideSearchResults = new FlexTable();
				// sideSplit = new SplitLayoutPanel();
				sideSearchTimer = new SideSearchTimer();

				sideSearchContinueButton = new Button("load more results");
				sideSearchContinueButton.setStylePrimaryName("addPropButton");
				sideSearchContinueButton.addClickHandler(ModeEdit.this);
				sideSearchContinueButton.addClickHandler(updateTimer);
				sideSearchContinueButton.setVisible(false);

				Button sideSearchCloseButton = new Button("x");
				sideSearchCloseButton.addClickHandler(new ClickHandler() {

					@Override
					public void onClick(ClickEvent event) {
						sideSearchPopupPanel.hide();
					}
				});
				// sideSearchLabel.setVisible(false);
				FlowPanel sideSearchArea = new FlowPanel();
				sideSearchArea.add(sideSearchCloseButton);
				sideSearchArea.add(sideSearchLabel);
				sideSearchArea.add(sideSearchResults);
				sideSearchArea.add(sideSearchContinueButton);

				sideSearchScroll = new ScrollPanel(sideSearchArea);
				sideSearchPopupPanel = new PopupPanel();
				sideSearchPopupPanel.setAnimationEnabled(true);
				sideSearchPopupPanel.setWidget(sideSearchScroll);
				sideSearchPopupPanel.setWidth("20em");
				sideSearchPopupPanel.setHeight("30em");

				/******************
				 * setup side bar *
				 ******************/
				sideMessageArea = new HTML();
				// sideMessageArea.addStyleName("sideMessageArea");
				sideMessageScroll = new ScrollPanel(sideMessageArea);
				sideMessageScroll.setStyleName("sideMessageArea");

				/*******************
				 * setup main area *
				 *******************/

				/*
				 * setup the search box
				 */
				mainSearchTimer = new MainSearchTimer();
				searchTextBox = new TextBox();
				editContentSaveTimer = new EditContentSaveTimer();

				addPropButton = new Button("Add as new proposition");
				addPropButton.setStylePrimaryName("addPropButton");
				addPropButton.setEnabled(false);
				addPropButton.addClickHandler(ModeEdit.this);
				addPropButton.addClickHandler(updateTimer);

				searchTextBox.addKeyUpHandler(ModeEdit.this);
				searchTextBox.addKeyUpHandler(updateTimer);
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
				mainSearchContinueButton.addClickHandler(updateTimer);
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
				mainSplit.addEast(sideMessageScroll, 300);
				mainSplit.add(mainPanel);
				setSideBarText("welcome");
				initWidget(mainSplit);
			}

			@Override
			public void onFailure(Throwable reason) {
				ArgMap.messageTimed("Code download failed", MessageType.ERROR);
				Log.log("me.me", "Code download failed" + reason.toString());
			}
		});
	}

	private void getRootPropsCallback(final PartialTrees allNodes) {
		GWT.runAsync(new RunAsyncCallback() {

			@Override
			public void onSuccess() {
				if (allNodes == null) {
					return;
				}
				Log log = Log.getLog("em.em.cb");
				log.log("Prop Tree From Server");
				for (Long id : allNodes.rootIDs) {
					Proposition proposition = (Proposition) allNodes.nodes
							.get(id);
					ViewProp propView = new ViewPropEdit();
					propView.recursiveBuildViewNode(proposition,
							allNodes.nodes, 5, allNodes.ratings);

					tree.addItem(propView);
					// propView.logNodeRecursive(0, "em.em.cb", true);
				}
				tree.resetState();
				if (Log.on) tree.logTree(log);

				updateTimer.start();
				log.finish();

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
				new ServerComm.LocalCallback<PartialTrees>() {

					@Override
					public void call(PartialTrees allNodes) {
						getRootPropsCallback(allNodes);
					}
				});
	}

	/*
	 * TODO after I get the basics working, I'll want to think about what
	 * happens when a Node is updated... for instance lets say we add a
	 * proposition to an argument. The argument is being updated. Presumably we
	 * want to set the argument's last update time to the time that we added the
	 * proposition... but actually maybe we don't need to do that. If we just
	 * leave the last update time as it is then on the next update the server
	 * will send back a notice that the proposition was updated, and the client
	 * will update to the new content... hmmm... actually that could be bad, if
	 * the client is in the middle of ongoing edits to a proposition the edits
	 * could cause the server to thing the client needs to be updated, and then
	 * the process of updating might overwrite new edist the user is making.
	 * Anyway... I need to think about this after I get the basics working. One
	 * solution is just to send back a copy of the node with every
	 * update/add/delete operation, and update the updated field on the client
	 * so it has the latest updates and doesn't need to get them...
	 */

	/*
	 * This is how I prevent searches and updates from stepping on each other:
	 * When a search begins it pauses updates, and if there is an update that
	 * returns after a search has happened the update throws out the result. In
	 * the case where an update returns before a search has start, and is still
	 * working when a search clears the tree and gets search results back, I
	 * don't there there should be any problems. The update will be working with
	 * the new loadedProps and loadedArgs list so might have references to nodes
	 * that don't need to be updated... but I think that all that means is that
	 * some up-to-date nodes get unnecessarily updated.
	 */
	private void getUpdatesAndApply() {
		final Date startTime = updateTimer.getStartDate();
		if (startTime == null) {
			Log.log("me.guaa",
					"strange: startTime == null on getUpdatesAndApply()");
		}

		Map<Long, DateAndChildIDs> propsInfo = new HashMap<Long, DateAndChildIDs>();

		loadNodeInfo(loadedProps, propsInfo);
		Map<Long, DateAndChildIDs> argsInfo = new HashMap<Long, DateAndChildIDs>();
		loadNodeInfo(loadedArgs, argsInfo);

		ServerComm.getUpdates(propsInfo, argsInfo,
				new LocalCallback<PartialTrees>() {

					@Override
					public void call(PartialTrees results) {
						Log log = Log.getLog("me.guaa.cb", false);
						log.logln("loadedNodes:\npropViews:"
								+ Log.multiMapToString(loadedProps)
								+ "\nargViews:"
								+ Log.multiMapToString(loadedArgs));
						log.logln("the current tree");
						tree.logTree(log);
						log.logln("live update results returned from server:");
						log.log(results.toString());
						/*
						 * makes sure that there hasn't been an update since
						 * this update started... for instance if a search has
						 * replaced all the nodes, we just throw this batch of
						 * updates out since they are no longer relevant.
						 */
						if (!startTime.equals(updateTimer.getStartDate())) {
							Log.log("me.guaa.cb",
									"start times not equal, returns");
							return;
						}

						for (Long id : results.rootIDs) {
							Node node = results.nodes.get(id);
							log.logln("prossesing node:" + node);
							if (node instanceof Proposition) {
								/*
								 * need to create a new list here because in the
								 * process of updateNode() loadedProps will
								 * potentially be updated with new nodes which
								 * are already up-to-date and which therefore
								 * should not be processed.
								 */
								List<ViewProp> viewProps = new ArrayList<ViewProp>(
										loadedProps.get(node.id));
								for (ViewProp viewProp : viewProps) {
									log.logln("prossesing ViewProp:" + viewProp);
									updateNode(viewProp, node, results);
								}
							} else if (node instanceof Argument) {
								List<ViewArg> viewArgs = new ArrayList<ViewArg>(
										loadedArgs.get(node.id));
								for (ViewArg viewArg : viewArgs) {
									// log.log("prossesing ViewArg:" + viewArg);
									updateNode(viewArg, node, results);
								}
							} else
								assert false;
						}
						log.finish();
					}
				});
	}

	private void updateNode(ViewNode viewNode, Node node, PartialTrees results) {
		Log log = Log.getLog("me.un");

		/*
		 * I compare the node return from the server to both the ViewNode Node's
		 * childIDs and also to an on the spot generated list of the ViewNode's
		 * actual childIDs. The reason is just to save a few cycles. Since the
		 * ViewNode's actual childIDs have to be generated by putting them in a
		 * new list, we can avoid doing that by just using the ViewNode's Node's
		 * list, which will be up to date 99% of the time. But the ViewNode's
		 * node's list won't be up to date right after an addition or deletion.
		 * So to make sure we aren't doing an unnecessary update (and causing
		 * havoc to the focus and everything) we do the second check to make
		 * sure the update really needs to be done...
		 */
		if (!node.childIDs.equals(viewNode.getNode().childIDs)
				&& !node.childIDs.equals(viewNode.getChildIDs())) {
			log.logln("node.childIDs:");
			log.log(node.childIDs);
			log.logln("viewNode.getNode().childIDs:");
			log.log(viewNode.getNode().childIDs);

			/*
			 * basic idea here is to make sure we have the correct children in
			 * the correct order we remove all the children, and then add them
			 * back according to the new order, and get any missing ones (i.e.
			 * new ones that didn't exist in the copy of the parent that we
			 * already had) from the map the server sent.
			 */

			Map<Long, ViewNode> removed = new HashMap<Long, ViewNode>();
			while (viewNode.getChildCount() != 0) {
				ViewNode child = viewNode.getChild(0);
				removed.put(child.getNodeID(), child);
				child.remove();
			}
			for (int i = 0; i < node.childIDs.size(); i++) {
				Long id = node.childIDs.get(i);
				if (removed.containsKey(id)) {
					viewNode.addItem(removed.get(id));
				} else if (results.nodes.containsKey(id)) {
					ViewNode child = viewNode.createChild();
					viewNode.addItem(child);
					child.recursiveBuildViewNode(results.nodes.get(id),
							results.nodes, 0, results.ratings);
				} else {
					/*
					 * either the child node should already exist on the client
					 * or be in the set of nodes return by the server, so this
					 * should never happen
					 */
					assert false;
				}
			}
		}

		log.logln("setting node -- \nold:" + viewNode.getNode().toString()
				+ "\n new:" + node.toString());

		/*
		 * In order to avoid saving when unecessary both the ViewArgEdit and
		 * ViewPropEdit save the content of their TextArea/Box to their Nodes
		 * whenever they save to the server so they can compare their
		 * TextArea/Box content with their Node content to see if their is
		 * anything worth saving the next time. This means that
		 * ModeEdit.updateNode() can compare the new Nodes content with the old
		 * nodes content to determine if the TextArea/Box content needs to be
		 * updated.
		 * 
		 * If the old Node and new Node are different and the TextArea/Box
		 * content is the same as the old Node's content then the TextArea/Box
		 * content needs to be updated. This indicates that there has been a
		 * change to the content on the server since the last update, and that
		 * there has not been a change to the content on the client.
		 * 
		 * If the old Node and new Node are the same (and the TextArea/Box
		 * content is either different from the old Node's content or the same)
		 * then the TextArea/Box does not need to be updated.
		 * 
		 * If the two are different and the TextArea/Box content is different
		 * from the old Node's content, then I think that means we have a
		 * conflict and we can pop up a conflict dialog for the user?
		 */
		String oldContent = viewNode.getNodeContent().trim();
		String newContent = node.content.trim();
		String editContent = viewNode.getTextAreaContent().trim();

		if (oldContent.equals(editContent)) {
			viewNode.setNode(node);
			if (resolveConflictDialog != null
					&& viewNode == resolveConflictDialog.getViewNode()) {
				resolveConflictDialog.updateConflictInfo(newContent);
			}
		} else {
			if (oldContent.equals(newContent)) {
				viewNode.setNodeButNotTextAreaContent(node);
			} else {
				viewNode.setNode(node);
				if (resolveConflictDialog == null) {
					resolveConflictDialog = new ResolveConflictDialog();
				}

				resolveConflictDialog.setConflictInfo(viewNode, node,
						editContent, newContent);

				resolveConflictDialog.center();
				resolveConflictDialog.show();
			}
		}
		log.finish();
	}

	private class ResolveConflictDialog extends DialogBox {
		private final HTML clientHTML = new HTML();
		private final HTML otherHTML = new HTML();
		private ViewNode viewNode;
		private Node node;
		private String clientContent;

		public void hideDialog() {
			viewNode = null;
			hide();
		}

		public ViewNode getViewNode() {
			return viewNode;
		}

		public void setConflictInfo(ViewNode viewNode, Node node,
				String client, String other) {
			this.viewNode = viewNode;
			this.node = node;
			clientContent = client;

			clientHTML.setHTML("Your version: \""
					+ SafeHtmlUtils.htmlEscape(client) + "\"");
			otherHTML.setHTML("Other user's version: \""
					+ SafeHtmlUtils.htmlEscape(other) + "\"");
		}

		public void updateConflictInfo(String other) {
			otherHTML.setHTML("Other user's version (updated): \""
					+ SafeHtmlUtils.htmlEscape(other) + "\"");
		}

		public ResolveConflictDialog() {
			super();
			setGlassEnabled(true);
			setAnimationEnabled(true);
			// setWidth("50%");

			setText("Resolve Conflicting Change");

			VerticalPanel dialogContents = new VerticalPanel();
			dialogContents.setWidth("60em");
			dialogContents.setSpacing(4);
			setWidget(dialogContents);

			HTML details = new HTML(
					"Another user has made changes to the text you are editing.  "
							+ "Please review the text below and choose a version to keep.  "
							+ "If you would like to merge text from both versions, copy the text you"
							+ " would like to merge, and paste it after choosing a version to work from.  "
							+ "If you continue getting this message wait a short time for the other user"
							+ " to finish editing and try again.");
			details.getElement().getStyle().setMarginBottom(2, Unit.EM);
			dialogContents.add(details);

			dialogContents.add(clientHTML);

			Button myVersionButton = new Button("use my version",
					new ClickHandler() {
						public void onClick(ClickEvent event) {
							viewNode.setTextAreaContent(clientContent);

							((SavableNode) viewNode)
									.saveContentToServerIfChanged();

							resolveConflictDialog.hideDialog();
						}
					});
			dialogContents.add(myVersionButton);

			otherHTML.getElement().getStyle().setMarginTop(2, Unit.EM);
			dialogContents.add(otherHTML);

			Button otherVersionButton = new Button("use other user's version",
					new ClickHandler() {
						public void onClick(ClickEvent event) {
							/*
							 * don't need to do anything because node was set to
							 * other user's content before showing this dialog
							 * (have to do it that way otherwise clicking on a
							 * dialog button causes textArea's focus to be lost,
							 * causing the user's changes to be saved and
							 * overwriting the other user's changes
							 */
							resolveConflictDialog.hideDialog();
						}
					});
			dialogContents.add(otherVersionButton);
		}
	};

	private ResolveConflictDialog resolveConflictDialog;

	private <T extends ViewNode> void loadNodeInfo(
			MultiMap<Long, T> loadedNodes, Map<Long, DateAndChildIDs> nodesInfo) {
		for (Long id : loadedNodes.keySet()) {
			List<T> viewNodeList = loadedNodes.get(id);

			Node node = viewNodeList.get(0).getNode();

			nodesInfo.put(id, new DateAndChildIDs(node));
		}
	}

	private void addRootProp() {
		final ViewPropEdit newPropView = new ViewPropEdit();

		/*
		 * close the other tree items for (int i = 0; i < tree.getItemCount();
		 * i++) { tree.getItem(i).setState(false); }
		 */

		newPropView.setContent(searchTextBox.getText());
		newPropView.getProposition().setContent(searchTextBox.getText());
		// tree.addItem(newPropView);
		tree.insertItem(0, newPropView);
		newPropView.haveFocus();
		ServerComm.addProp(newPropView.getProposition(), null, 0,
				new LocalCallback<Void>() {
					@Override
					public void call(Void t) {
						newPropView.setLoaded(true);
					}
				});
	}

	public static void log(String string) {
		sideMessageArea.setHTML(sideMessageArea.getHTML() + string);
	}

	/* this type of button is used in the side search box */
	private class SideSearchButton extends Button implements ClickHandler {
		int resultIndex;
		PartialTrees propMatches;

		SideSearchButton(int resultIndex, PartialTrees propMatches) {
			super("use this");
			this.resultIndex = resultIndex;
			this.propMatches = propMatches;
			addClickHandler(this);
			setStylePrimaryName("addPropButton");
		}

		public void onClick(ClickEvent event) {
			UseNegationDialog useNegationDialog = new UseNegationDialog(this);

			useNegationDialog.center();
			useNegationDialog.show();
		}
	}

	private class UseNegationDialog extends DialogBox {
		public UseNegationDialog(final SideSearchButton sideSearchButton) {
			super();
			setGlassEnabled(true);
			setAnimationEnabled(true);

			setText("Use Negation Of Proposition?");

			VerticalPanel dialogContents = new VerticalPanel();
			dialogContents.setWidth("40em");
			dialogContents.setSpacing(4);
			setWidget(dialogContents);

			HTML details = new HTML(
					"Would you like to assert this proposition"
							+ "as is in your argument, or would you like to assert the opposite?");
			dialogContents.add(details);

			Button asIsButton = new Button("Use Proposition",
					new ClickHandler() {
						public void onClick(ClickEvent event) {
							hide();
							replacePropositionWithLink(
									sideSearchButton.resultIndex,
									sideSearchButton.propMatches, false);
						}
					});
			dialogContents.add(asIsButton);

			Button asNegationButton = new Button("Use Negation",
					new ClickHandler() {

						@Override
						public void onClick(ClickEvent event) {
							hide();
							replacePropositionWithLink(
									sideSearchButton.resultIndex,
									sideSearchButton.propMatches, true);
						}
					});
			dialogContents.add(asNegationButton);
		}
	}

	public void replacePropositionWithLink(int resultIndex,
			PartialTrees propMatches, final boolean negated) {
		final ViewPropEdit propViewToRemove = ViewPropEdit
				.getLastPropositionWithFocus();
		if (propViewToRemove.getChildCount() == 0) {
			final ViewArgEdit parentArgView = propViewToRemove.parentArgView();
			final Proposition propToLinkTo = (Proposition) propMatches.nodes
					.get(propMatches.rootIDs.get(resultIndex));
			ServerComm.replaceWithLinkAndGet(parentArgView.argument,
					propToLinkTo, propViewToRemove.proposition, negated,
					new LocalCallback<ArgMapService.PartialTrees>() {

						@Override
						public void call(PartialTrees trees) {
							int propIndex = parentArgView
									.getChildIndex(propViewToRemove);
							parentArgView.removeItem(propViewToRemove);
							Proposition proposition = (Proposition) trees.nodes
									.get(propToLinkTo.id);
							ViewProp newViewProp = new ViewPropEdit();
							parentArgView.insertItem(propIndex, newViewProp);
							newViewProp.recursiveBuildViewNode(proposition,
									trees.nodes, 5, trees.ratings);
							newViewProp.setNegated(negated);
						}
					});
		} else {
			ArgMap.messageTimed(
					"Cannot link to existing proposition when proposition currently being edited has children",
					MessageType.ERROR);
		}
	}

	public void sideSearch(ViewPropEdit viewProp) {
		if (sideSearch != null) {
			sideSearch.cancelSearch();
			sideSearch = null;
		}

		sideSearchResults.removeAllRows();
		sideSearchContinueButton.setVisible(false);

		String searchString = viewProp.getTextAreaContent().trim();
		if (!searchString.equals("") && viewProp.getChildCount() == 0
				&& !viewProp.deleted) {
			List<Long> filterIDs = new ArrayList<Long>();
			filterIDs.addAll(viewProp.getAncestorIDs());
			if (viewProp.getParent() != null) {
				filterIDs.addAll(viewProp.getParent().getChildIDs());
			}
			sideSearch = new Search(searchString, "SIDE_SEARCH",
					ModeEdit.SIDE_SEARCH_LIMIT, filterIDs) {

				@Override
				public void searchExhausted() {
					sideSearchContinueButton.setVisible(false);
					hideSearchBox();
				}

				@Override
				public void searchCompleted() {
					sideSearchContinueButton.setVisible(true);
				}

				@Override
				public void processSearchResults(PartialTrees propMatches) {
					sideSearchAppendResults(propMatches);
					// if (sideSearchResults.getRowCount() > 0) {
					displaySearchBox();
					// } else {
					// hideSearchBox();
					// }
				}
			};
			sideSearch.startSearch();
		} else {
			hideSearchBox();
		}
	}

	private void sideSearchAppendResults(PartialTrees propMatches) {
		int i = sideSearchResults.getRowCount();
		HTMLTable.RowFormatter rowFormatter = sideSearchResults
				.getRowFormatter();
		for (Long id : propMatches.rootIDs) {
			Proposition prop = (Proposition) propMatches.nodes.get(id);
			sideSearchResults.setText(i, 0, prop.getContent());
			sideSearchResults.setWidget(i, 1, new SideSearchButton(i,
					propMatches));
			// rowFormatter.addStyle(i, "sideSearchRow");
			rowFormatter.setStylePrimaryName(i, "sideSearchRow");
			if (i % 2 != 0) {
				rowFormatter.addStyleName(i, "sideSearchAltRow");
			}
			i++;
		}
	}

	public void displaySearchBox() {
		sideSearchPopupPanel.setPopupPositionAndShow(new PositionCallback() {

			@Override
			public void setPosition(int offsetWidth, int offsetHeight) {
				// GWT.log("offsetWidth: " + offsetWidth);
				// GWT.log("Window.getclientWidth()
				sideSearchPopupPanel.setPopupPosition(Window.getClientWidth()
						- offsetWidth, Window.getClientHeight() - offsetHeight);

			}
		});
	}

	public void hideSearchBox() {
		sideSearchPopupPanel.hide();
	}

	// public void displaySearchBox() {
	// if (!sideSearchScroll.isAttached()) {
	// sideSplit.remove(sideMessageScroll);
	// sideSplit.addSouth(sideSearchScroll, 400);
	// sideSplit.add(sideMessageScroll);
	// }
	// sideSearchLabel.setVisible(true);
	// }
	//
	// public void hideSearchBox() {
	// sideSplit.remove(sideSearchScroll);
	// sideSearchLabel.setVisible(false);
	// }

	/**
	 * annoyingly, by default the Tree eats the arrow key events so they can't
	 * be used for moving in a text box. Setting a handler on the tree to keep
	 * the events from doing their default behavior or propagating doesn't seem
	 * to work. I found this fix on stack overflow
	 */
	public class EditModeTree extends ArgTree {

		public ModeEdit getEditMode() {
			return ModeEdit.this;
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

		@Override
		public void onLoadedNodeAdd(ViewNode node) {
			if (node instanceof ViewProp) {
				loadedProps.put(node.getNodeID(), (ViewProp) node);
			} else if (node instanceof ViewArg) {
				loadedArgs.put(node.getNodeID(), (ViewArg) node);
			}
		}

		@Override
		public void onLoadedNodeRemove(ViewNode node) {
			if (node instanceof ViewProp) {
				loadedProps.remove(node.getNodeID(), (ViewProp) node, false);
			} else if (node instanceof ViewArg) {
				loadedArgs.remove(node.getNodeID(), (ViewArg) node, false);
			}
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
			ViewNode realChild = realViewNode.getChild(i);
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
				recursiveGetOpenPropsAndArgs(viewNode.getChild(i), props, args);
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

	public class UpdateTimer extends Timer implements KeyDownHandler,
			KeyUpHandler, ClickHandler, OpenHandler<TreeItem>,
			CloseHandler<TreeItem> {
		private Date startDate;
		private boolean on = true;
		private long lastUserAction;
		private int currentFrequency;

		private final int INITIAL_FREQUENCY = 4000;
		private final int SECOND = 1000;
		private final int MINUTE = 60 * SECOND;
		private final int HOUR = 60 * MINUTE;
		private final int DAY = 24 * HOUR;
		private final Message message = ArgMap.getMessage();

		public void userAction() {
			lastUserAction = System.currentTimeMillis();

			if (currentFrequency != INITIAL_FREQUENCY) {
				currentFrequency = INITIAL_FREQUENCY;
				message.hide();
				if (startDate != null) {
					schedule(currentFrequency);
				}
			}
		}

		private void message(String delay) {
			message.setMessage(
					"because of inactivity reducing update frequency to every "
							+ delay, MessageType.INFO);
			message.display();
		}

		@Override
		public void run() {
			long timeSinceUserAction = System.currentTimeMillis()
					- lastUserAction;
			if (timeSinceUserAction < 10 * MINUTE) {
				currentFrequency = INITIAL_FREQUENCY;
			} else if (timeSinceUserAction < HOUR) {
				currentFrequency = 30 * SECOND;
				message("30 seconds");
			} else if (timeSinceUserAction < DAY) {
				currentFrequency = 30 * MINUTE;
				message("30 minutes");
			} else {
				currentFrequency = DAY;
				message("24 hours");
			}

			schedule(currentFrequency);
			if (on) getUpdatesAndApply();
		}

		public void start() {
			startDate = new Date();
			Log.log("me.ut.s", "started");
			schedule(INITIAL_FREQUENCY);
			userAction();
		}

		public Date getStartDate() {
			return startDate;
		}

		/*
		 * note that this method cannot override Timer.cancel() becuase that
		 * method is called by Timer every time schedule() is called.
		 */
		public void cancelTimer() {
			Log.log("me.ut.s", "cancelled");
			cancel();
			startDate = null;
		}

		public void setOn(boolean on) {
			this.on = on;
		}

		public boolean getOn() {
			return on;
		}

		@Override
		public void onClose(CloseEvent<TreeItem> event) {
			userAction();
		}

		@Override
		public void onOpen(OpenEvent<TreeItem> event) {
			userAction();
		}

		@Override
		public void onClick(ClickEvent event) {
			userAction();
		}

		@Override
		public void onKeyUp(KeyUpEvent event) {
			userAction();
		}

		@Override
		public void onKeyDown(KeyDownEvent event) {
			userAction();
		}
	};

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
			return viewProp.getTextAreaContent();

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

	public interface SavableNode {
		public void saveContentToServerIfChanged();
	}

	public class EditContentSaveTimer extends Timer {
		/*
		 * I think this is fine for now. Saves after the user has stopped typing
		 * for 3 seconds. Is that too frequent? Or not frequent enough? If
		 * someone is typing for a minute it might be nice to see it before they
		 * stop. On the other hand how common is it for someone to type
		 * continuously for a minute without pausing for three seconds?
		 */

		private SavableNode viewNode;

		public void setNodeForTimedSave(SavableNode viewNode) {
			this.viewNode = viewNode;
			schedule(3000);
		}

		@Override
		public void run() {
			viewNode.saveContentToServerIfChanged();
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
			mainSearch = new Search(searchString, "MAIN_SEARCH",
					MAIN_SEARCH_LIMIT, null) {
				@Override
				public void processSearchResults(PartialTrees propsAndArgs) {
					mainSearchAppendResultsToTree(propsAndArgs);
				}

				@Override
				public void searchCompleted() {
					mainSearchContinueButton.setVisible(true);
					updateTimer.start();
				}

				@Override
				public void searchExhausted() {
					mainSearchContinueButton.setVisible(false);
					updateTimer.start();
				}

				@Override
				public void searchStarted() {
					updateTimer.cancelTimer();
				}

				@Override
				public void searchContinued() {
					updateTimer.cancelTimer();
				}

				@Override
				public void searchCancelled() {
					updateTimer.start();
				}
			};
			mainSearch.startSearch();
		} else {
			updateTimer.cancelTimer();
			getRootProps();
		}
	}

	public void mainSearchAppendResultsToTree(PartialTrees results) {
		for (Long id : results.rootIDs) {
			Proposition proposition = (Proposition) results.nodes.get(id);
			ViewProp propView = new ViewPropEdit();
			propView.recursiveBuildViewNode(proposition, results.nodes, 1,
					results.ratings);

			tree.addItem(propView);
		}
		tree.resetState();
	}

	public void loadFromServer(ViewNode viewNode, int loadDepth, int openDepth) {
		List<ViewNode> list = new ArrayList<ViewNode>();
		list.add(viewNode);
		loadFromServer(list, loadDepth, openDepth);
	}

	public void loadFromServer(final List<ViewNode> viewNodes, int loadDepth,
			final int openDepth) {
		List<Long> viewNodeIDs = new ArrayList<Long>(viewNodes.size());
		for (ViewNode viewNode : viewNodes) {
			assert !viewNode.isLoaded();
			viewNodeIDs.add(viewNode.getNodeID());
		}

		ServerComm.getNodesChildren(viewNodeIDs, loadDepth,
				new LocalCallback<PartialTrees>() {
					@Override
					public void call(PartialTrees trees) {
						for (ViewNode source : viewNodes) {
							source.removeItems();
							// while (source.getChildCount() > 0) {
							// source.getChild(0).remove();
							// }
							source.recursiveBuildViewNode(source.getNode(),
									trees.nodes, openDepth, trees.ratings);
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
				List<ViewNode> list = new ArrayList<ViewNode>(
						source.getChildCount());
				for (int i = 0; i < source.getChildCount(); i++) {
					if (!source.getChild(i).isLoaded()) {
						list.add(source.getChild(i));
					}
				}
				if (list.size() > 0) {
					loadFromServer(list, 1, 0);
				}
			}
		}
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

	public void setSideBarText(final String pageName) {
		if (loadedPages.containsKey(pageName)) {
			sideMessageArea.setHTML(loadedPages.get(pageName));
		} else {
			ServerComm.getTextPage(pageName, new LocalCallback<String>() {

				@Override
				public void call(String pageContent) {
					loadedPages.put(pageName, pageContent);
					sideMessageArea.setHTML(pageContent);
				}
			});
		}
	}
}
