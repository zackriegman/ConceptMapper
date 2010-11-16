package org.argmap.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.DateAndChildIDs;
import org.argmap.client.ArgMapService.PartialTrees;
import org.argmap.client.ArgMapService.PartialTrees_DELETE_ME;
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

	private void getRootPropsCallback(final PartialTrees_DELETE_ME allNodes) {
		GWT.runAsync(new RunAsyncCallback() {

			@Override
			public void onSuccess() {
				if (allNodes == null) {
					return;
				}
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
				new ServerComm.LocalCallback<PartialTrees_DELETE_ME>() {

					@Override
					public void call(PartialTrees_DELETE_ME allNodes) {
						getRootPropsCallback(allNodes);
					}
				});
	}

	public void onEditModeTabSelected() {
		Log.log("me.oemts", "resizing tree");
		// tree.resizeTree();
	}

	/*
	 * TODO also might make sense to think more about how to make sure that
	 * asearch doesn't step on an update. So the flow that I'm concerned aboutin
	 * that case would be, update starts before search (search can
	 * clearlastUpdate when it begins and set it to a new date when it returns)
	 * andsearch starts and returns while update is still working, and then
	 * clears thetree while update is still working... might not be a problem
	 * actually...because the nodes that update was working on will still be in
	 * memory, updatecan finish updating the pointless nodes, even after search
	 * has cleared the tree...hmmm...
	 */
	/*
	 * TODO whether or not a proposition is shown in yellow to designate that it
	 * is linked depends on how many links it has... that is something I'm not
	 * necessarily sending change data for... but maybe I should, it would
	 * require the change info for all of the propositions parents, some of
	 * which would not necessarily be present, so it would mean that I return
	 * all the unlink events featureing a proposition, regardless of whether the
	 * parent item is loaded on the client...
	 */
	/*
	 * TODO does the server currently fetch an extra layer deep? The first
	 * iteration gets changes for existing nodes, including additions of new
	 * nodes. The second iteration gets changes for new nodes to bring them up
	 * to date, as well as additions of grandchildren. The third iteration
	 * brings the grandchildren's content uptodate and creates the information
	 * we need to create dummy nodes. Thus if the children are closed, we have
	 * fully loaded children and grandchildren that are content uptodate with
	 * dummy nodes (i.e. not loaded). So I think the server is doing it right, 3
	 * iterations.
	 */
	/*
	 * TODO in addition to the note below regarding getting rid of the extra
	 * link property in the change I think we can also get rid of the newContent
	 * variable as well. Will I want it for email? I don't see why. If I want to
	 * show incremental changes, I can just construct the email backwards,
	 * working from the current version of the tree. So instead of newContent
	 * property, whenever there is an update we can just assume the update
	 * brings us current with the current content, and send the current content
	 * as the content of the update (and perhaps collapse multiple updates into
	 * a single change). However will this run into problems with updates that
	 * happen after the change list has started to be compiled? The client could
	 * have content from a change that comes after its lastUpdate date. But this
	 * doesn't really cause any problems does it? All that means is that ten
	 * seconds later the client will be updated with content that it already
	 * has. It should cause any extra conflicts/stomping, if the client edits
	 * the node there is a conflict regardless of whether what he started with
	 * was a ten second old copy. However this might be the key to why I decided
	 * to save links in the change. Lets say an update starts at time T setting
	 * the lastUpdate value to time T, and then at time T+1 a newly linked
	 * proposition has a child added to it, then at time T+2 the update grabs a
	 * copy of the newly linked proposition, including the child, then at time
	 * T+3 the update returns, then at time T+4 a new update starts grabbing
	 * changes from the lastUpdate value...which included the addition of the
	 * child, so the child is double added. This is a little bit more serious
	 * that updating the content before it is technically supposed to be
	 * updated, but how much more serious? I guess it depends on how the client
	 * handles a request to add an argument to a linked proposition that already
	 * has an argument with that id... The same thing could happen but with a
	 * delete, and the client would have to handle a deletion of a non-existant
	 * node gracefully. OK, I think for now it makes sense to leave linked props
	 * as properties of changes to avoid having to figure this all out...
	 */

	// private void getNewChangesAndUpdateTree_DELETE_ME(final Date startDate) {
	//
	// ServerComm.getNewChanges_DELETE_ME(lastUpdate, loadedProps.keySet(),
	// loadedArgs.keySet(),
	// new LocalCallback<ArgMapService.ForwardChanges>() {
	//
	// @Override
	// public void call(ForwardChanges changes) {
	// /*
	// * check to make sure that a search subsequent to this
	// * update being called hasn't changed the tree and the
	// * lastUpdate date. If it has, then these changes do not
	// * apply to the existing tree and they should be
	// * discarded.
	// */
	// if (startDate != lastUpdate) {
	// return;
	// }
	// for (Change change : changes.changes) {
	// switch (change.changeType) {
	// case PROP_LINK:
	// case PROP_ADDITION:
	// case PROP_UNLINK:
	// case PROP_DELETION:
	// case ARG_MODIFICATION:
	// for (ViewArg viewArg : loadedArgs
	// .get(change.argID)) {
	// switch (change.changeType) {
	// case PROP_LINK:
	// /*
	// * note that for non-link nodes I'm
	// * updating the children of the first
	// * layer of children but link nodes are
	// * created unloaded with only dummies
	// * for children, because otherwise I
	// * would have to send uptodate copies of
	// * the children seperately or I would
	// * have to send backdated copies of the
	// * children along with the updates
	// * needed to bring them to date. I'm
	// * beginning to think that really it
	// * makes more sense to send the fully
	// * uptodate links...but perhaps not,
	// * because I also have to update the
	// * open+loaded links on the client. So I
	// * need to send and process the link
	// * changes anyway. But why not do both.
	// * Send the link changes for open/loaded
	// * links (as well as any changes of
	// * already open/loaded children) and
	// * uptodate copies of newly linked/links
	// * 2 layers deeps (i.e. prefetched).
	// * This would seem to make it unecessary
	// * to save a complete copy of the link
	// * in the change, which seems sort of
	// * ugly to me (and thus far hasn't been
	// * necessary for going back in time).
	// */
	// ViewPropEdit linkView = new ViewPropEdit(
	// change.link_DELETE_ME);
	// for (Long id : change.link_DELETE_ME.childIDs) {
	// linkView.addItem(new ViewDummyVer(
	// id));
	// }
	// linkView.setLoaded(false);
	// linkView.setOpen(false);
	// viewArg.insertItem(change.argPropIndex,
	// linkView);
	// /*
	// * TODO is the link loaded or isn't it?
	// * I can't remember how I define loaded.
	// * If the link isn't loaded, that means
	// * I won't get updates for it right?
	// * That isn't good. I want content
	// * changes for the link. If the link is
	// * loaded... I think the link is loaded
	// * because it's not a dummy...look at
	// * how I use isLoaded in other parts of
	// * the program
	// */
	// loadedProps
	// .put(change.propID, linkView);
	// break;
	// case PROP_ADDITION:
	// Proposition prop = new Proposition();
	// prop.id = change.propID;
	// prop.content = change.newContent_DELETE_ME;
	// ViewPropEdit child = new ViewPropEdit(
	// prop);
	// viewArg.insertItem(change.argPropIndex,
	// child);
	// /*
	// * TODO hmmm... on the first time
	// * through this loop all the new items
	// * are loaded right. But that isn't true
	// * on the last time through the loop is
	// * it? Or is it? Figure that out one way
	// * or another. And if its not true then
	// * I need to distinguish the first time
	// * through from the last time.
	// */
	// // loadedProps.put(change.propID,
	// // linkView);
	// break;
	// case PROP_UNLINK:
	// viewArg.removeChildWithID(change.propID);
	// break;
	// case PROP_DELETION:
	// viewArg.removeChildWithID(change.propID);
	// break;
	// case ARG_MODIFICATION:
	// viewArg.setArgTitle(change.newContent_DELETE_ME);
	// break;
	// }
	// }
	// break;
	// case PROP_MODIFICATION:
	// case ARG_DELETION:
	// case ARG_ADDITION:
	// for (ViewProp viewProp : loadedProps
	// .get(change.propID)) {
	// switch (change.changeType) {
	// case PROP_MODIFICATION:
	// viewProp.setContent(change.newContent_DELETE_ME);
	// break;
	// case ARG_DELETION:
	// viewProp.removeChildWithID(change.argID);
	// break;
	// case ARG_ADDITION:
	// Argument arg = new Argument();
	// arg.id = change.argID;
	// arg.content = change.newContent_DELETE_ME;
	// arg.pro = change.argPro;
	// ViewArgEdit child = new ViewArgEdit(arg);
	// viewProp.insertItem(
	// change.argPropIndex, child);
	// /*
	// * TODO don't forget to add to the
	// * loadedArgs map if I need too here...
	// */
	// break;
	// }
	// }
	// break;
	// }
	// }
	// // TODO process changes, making sure to update the
	// // loadedProps/Args maps as I go
	// }
	// });
	// }

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
	 * TODO might want to have a way to remove deleted search results so that
	 * the userdoesn't start editing a deleted item. This should be easy, just
	 * supply the serverwith a list of root items, and return add/remove changes
	 * for them...
	 */
	/*
	 * TODO as I'm adding nodes, how do I decide whether to add them as open or
	 * closed, loaded or unloaded? What I decided is that the first layer of
	 * added nodes would be added as preloaded but closed, and the second layer
	 * would be added as closed and unloaded.
	 */
	/*
	 * TODO make sure that updates and searches don't step on each other. The
	 * update callback can just make sure that there is no search in progress,
	 * and if there is it can throw out its results. The search... maybe it
	 * should remove the tree so the update is working on an irrelevant tree? Or
	 * maybe we don't need to worry about this, becuase the search clears the
	 * tree... so update will be working on detached nodes... but what about
	 * root props and what about loadedProps and loadedArgs... the search will
	 * clear those and replenish them and the update will continue working from
	 * them. Maybe the search should replace loadedProps and loadedArgs with new
	 * maps entirely (instead of clearing them) so that the update is working
	 * with the old list and the search can go ahead and create a new list...?
	 */

	/*
	 * TODO make sure that the node.childIDs are updated on every ModeEdit
	 * change...
	 */
	/*
	 * TODO trying to sort out the isLoaded mess. The question is whether or not
	 * a new ViewNode is loaded before it gets its id. We can't keep track of it
	 * in loadedProps/Args until it has an id so in that sense it is not loaded.
	 * On the otherhand it cannot be loaded from the server without an id, so if
	 * someone is checking to see if it should be loaded (which was the previous
	 * and ongoing purpose of isLoaded() they would want to see a yes on a brand
	 * new ViewNode. So I think I decided to go with isLoaded is only true for
	 * Views with ids set, and other uses have to check hasID() to find out if
	 * an unloaded View can be loaded from the server. This means that all the
	 * servercomm add methods have to be updated to accept a ViewNode, and to
	 * updated that ViewNode to loaded upon receiving the ID back from the
	 * server. In my extremely tired state right now I don't see any problem
	 * with this approach, so I just need to change servercomm, and also make
	 * sure that flipping the default isLoaded state from true to false didn't
	 * fuck anything else up... which it probably did...)
	 */
	private void getUpdatesAndApply() {
		final Date startTime = updateTimer.getStartDate();

		Map<Long, DateAndChildIDs> propsInfo = new HashMap<Long, DateAndChildIDs>();
		/*
		 * TODO need to make sure that the list send to the server contains the
		 * oldest versino of the node on the client, and that when updated the
		 * nodes return by the server, that the client checks each node to make
		 * sure that it needs to be updated, because nodes on the client may
		 * have different ages and updated values. For instance, when a link
		 * that is opened twice on the client is updated, there will be two
		 * copies, one will be updated on the client... well... currently I
		 * don't update the Nodes updated value so maybe this actually isn't a
		 * concern... hmmm...
		 */
		loadNodeInfo(loadedProps, propsInfo);
		Map<Long, DateAndChildIDs> argsInfo = new HashMap<Long, DateAndChildIDs>();
		loadNodeInfo(loadedArgs, argsInfo);

		ServerComm.getUpdates(propsInfo, argsInfo,
				new LocalCallback<PartialTrees>() {

					@Override
					public void call(PartialTrees results) {
						Log log = Log.getLog("me.guaa.cb", true);
						log.log("loadedNodes:\npropViews:"
								+ Log.multiMapToString(loadedProps)
								+ "\nargViews:"
								+ Log.multiMapToString(loadedArgs));
						/*
						 * makes sure that there hasn't been an update since
						 * this update started... for instance if a search has
						 * replaced all the nodes, we just throw this batch of
						 * updates out since they are no longer relevant.
						 */
						if (!startTime.equals(updateTimer.getStartDate())) {
							log.log("start times not equal, returns");
							return;
						}

						for (Long id : results.rootIDs) {
							Node node = results.nodes.get(id);
							log.log("prossesing node:" + node);
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
									log.log("prossesing ViewProp:" + viewProp);
									updateNode(viewProp, node, results);
								}
							} else if (node instanceof Argument) {
								List<ViewArg> viewArgs = new ArrayList<ViewArg>(
										loadedArgs.get(node.id));
								for (ViewArg viewArg : viewArgs) {
									log.log("prossesing ViewArg:" + viewArg);
									updateNode(viewArg, node, results);
								}
							} else
								assert false;
						}
						log.finish();
						// tree.resizeTree();
					}
				});
	}

	private void updateNode(ViewNode viewNode, Node node, PartialTrees results) {
		// TODO note: this list comparison also depends on the viewNode's Node
		// having an up-to-date childID list...make sure that it does...
		if (!node.childIDs.equals(viewNode.getNode().childIDs)) {
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
							results.nodes, 0);
				} else {
					/*
					 * TODO when would the results not contain a key for a
					 * child? since I'm using recursiveBuildViewNode it should
					 * take care of a number of children that have been added.
					 * updateNode() should only be called for nodes in the
					 * pre-existing tree. recursiveBuildViewNode takes care of
					 * creating the children and any grandchildren/dummy nodes
					 * necessary. Update node takes care of removing/adding/and
					 * updating the content of the node. So I'm commmenting out
					 * this addItem() and replacing it with an assert false;
					 */
					assert false;
					// viewNode.addItem(new ViewDummyVer(id));
				}
			}
		}

		viewNode.setNode(node);
		// tree.recursiveResizeNode(viewNode);
	}

	private <T extends ViewNode> void loadNodeInfo(
			MultiMap<Long, T> loadedNodes, Map<Long, DateAndChildIDs> nodesInfo) {
		for (Long id : loadedNodes.keySet()) {
			Node node = loadedNodes.get(id).get(0).getNode();
			DateAndChildIDs nodeInfo = new DateAndChildIDs();
			nodeInfo.date = node.updated;
			nodeInfo.childIDs = new HashSet<Long>(node.childIDs);
			nodesInfo.put(id, nodeInfo);
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
		// ServerComm.addProp(newPropView.getProposition(), null, 0,
		// new LocalCallback<Proposition>() {
		// @Override
		// public void call(Proposition result) {
		// newPropView.addPropositionCallback(newPropView, result);
		// }
		// });
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
				class ThisCallback implements LocalCallback<Map<Long, Node>> {
					ViewArgEdit parentArgView;
					ViewPropEdit propViewToRemove;
					int propIndex;
					Long linkPropID;

					@Override
					public void call(Map<Long, Node> nodes) {
						parentArgView.removeItem(propViewToRemove);
						Proposition proposition = (Proposition) nodes
								.get(linkPropID);
						ViewProp newViewProp = new ViewPropEdit();
						newViewProp.recursiveBuildViewNode(proposition, nodes,
								5);

						parentArgView.insertItem(propIndex, newViewProp);
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
				ArgMap.messageTimed(
						"Cannot link to existing proposition when proposition currently being edited has children",
						MessageType.ERROR);
			}

		}
	}

	public void sideSearch(ViewPropEdit viewProp) {
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
			if (viewProp.getParent() != null) {
				filterIDs.addAll(viewProp.getParent().getChildIDs());
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
								PartialTrees_DELETE_ME propMatches) {
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

	private void sideSearchAppendResults(PartialTrees_DELETE_ME propMatches) {
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
		public void onRemovedLoadedNode(ViewNode node) {
			if (node instanceof ViewProp) {
				loadedProps.remove(node.getNodeID(), (ViewProp) node, false);
			} else if (node instanceof ViewArg) {
				loadedArgs.remove(node.getNodeID(), (ViewArg) node, false);
			}
		}

		@Override
		public void onAddLoadedNode(ViewNode node) {
			if (node instanceof ViewProp) {
				loadedProps.put(node.getNodeID(), (ViewProp) node);
			} else if (node instanceof ViewArg) {
				loadedArgs.put(node.getNodeID(), (ViewArg) node);
			}
		}

		@Override
		public void onNodeIsLoaded(ViewNode node) {
			if (node instanceof ViewProp) {
				loadedProps.put(node.getNodeID(), (ViewProp) node);
			} else if (node instanceof ViewArg) {
				loadedArgs.put(node.getNodeID(), (ViewArg) node);
			}
		}

		@Override
		public void onRemoveAllLoadedNodes() {
			loadedProps.clear();
			loadedArgs.clear();
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

	public class UpdateTimer extends Timer {
		private Date startDate;
		private boolean on;

		@Override
		public void run() {
			if (on) getUpdatesAndApply();
			// getNewChangesAndUpdateTree_DELETE_ME( lastUpdate_DELETE_ME);
		}

		public void start() {
			scheduleRepeating(2000);
			startDate = new Date();
		}

		public Date getStartDate() {
			return startDate;
		}

		@Override
		public void cancel() {
			super.cancel();
			startDate = null;
		}

		public void setOn(boolean on) {
			this.on = on;
		}

		public boolean getOn() {
			return on;
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
								PartialTrees_DELETE_ME propsAndArgs) {
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
							updateTimer.cancel();
						}

						@Override
						public void searchContinued() {
							updateTimer.cancel();
						}

						@Override
						public void searchCancelled() {
							updateTimer.start();
						}
					});
			mainSearch.startSearch();
		} else {
			updateTimer.cancel();
			getRootProps();
		}
	}

	public void mainSearchAppendResultsToTree(PartialTrees_DELETE_ME results) {
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
				new LocalCallback<Map<Long, Node>>() {
					@Override
					public void call(Map<Long, Node> nodes) {
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
}
