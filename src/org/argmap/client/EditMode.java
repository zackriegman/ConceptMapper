package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.PropsAndArgs;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
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

public class EditMode extends ResizeComposite implements
		LocalCallback<PropsAndArgs>, KeyUpHandler, OpenHandler<TreeItem> {

	private static HTML sideMessageArea = new HTML();
	private final Label sideSearchLabel = new Label(
			"Would you like to use one of these already existing propositions?");
	private final FlexTable sideSearchResults = new FlexTable();
	private final ScrollPanel sideSearchScroll;
	private final ScrollPanel sideMessageScroll;
	public final ScrollPanel treeScrollPanel;
	private final SplitLayoutPanel sideSplit = new SplitLayoutPanel();

	TextBox searchTextBox = new TextBox();
	private final EditModeTree tree;

	public EditMode() {
		super();

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
		Button addPropButton = new Button("Add as new proposition");
		addPropButton.setStylePrimaryName("addPropButton");
		addPropButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					addRootProp();
				} catch (Exception e) {
					ServerComm.handleClientException(e);
				}
			}
		});

		searchTextBox.addKeyUpHandler(this);
		searchTextBox.addStyleName("searchTextBox");
		// searchTextBox.setVisibleLength(60);
		Label searchLabel = new Label("Search:");
		searchLabel.addStyleName("searchLabel");
		FlowPanel searchBoxPanel = new FlowPanel();
		searchBoxPanel.addStyleName("searchBoxPanel");
		searchBoxPanel.add(searchLabel);
		searchBoxPanel.add(searchTextBox);
		searchBoxPanel.add(addPropButton);
		searchTextBox.addStyleName("flowPanel-left");
		addPropButton.addStyleName("flowPanel-left");
		searchLabel.addStyleName("flowPanel-left");

		/*
		 * setup the tree
		 */
		tree = new EditModeTree(this);
		tree.addOpenHandlerTracked(this);
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
						try {
							ArgMap.logStart("em.em.cb");
							ArgMap.log("em.em.cb", "Prop Tree From Server");
							for (Proposition proposition : allNodes.rootProps) {

								ViewProp propView = new ViewPropEdit();
								propView.recursiveBuildViewNode(proposition,
										allNodes.nodes);

								tree.addItem(propView);
								propView.logNodeRecursive(0, "em.em.cb", true);
							}
							tree.resetState();
							ArgMap.logEnd("em.em.cb");
						} catch (Exception e) {
							ServerComm.handleClientException(e);
						}
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
				try {
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
						 * gracefully... currently throws an exception, instead
						 * a message indicating that top level nodes cannot be
						 * linked would be good...
						 */
						ViewArgEdit parentArgView = propViewToRemove
								.parentArgView();
						callback.parentArgView = parentArgView;
						callback.propViewToRemove = propViewToRemove;
						callback.propIndex = parentArgView
								.getChildIndex(propViewToRemove);
						Proposition propToLinkTo = propMatches.get(resultIndex);
						callback.linkPropID = propToLinkTo.id;
						ServerComm.replaceWithLinkAndGet(
								parentArgView.argument, propToLinkTo,
								propViewToRemove.proposition, callback);
					} else {
						ArgMap.message(
								"Cannot link to existing proposition when proposition currently being edited has children",
								MessageType.ERROR);
					}
				} catch (Exception e) {
					ServerComm.handleClientException(e);
				}
			}
		}
		sideSearchResults.removeAllRows();
		if (propMatches.rootProps.size() > 0) {
			if (!sideSearchScroll.isAttached()) {
				sideSplit.remove(sideMessageScroll);
				sideSplit.addSouth(sideSearchScroll, 400);
				sideSplit.add(sideMessageScroll);
			}
			sideSearchLabel.setVisible(true);
		} else {
			sideSplit.remove(sideSearchScroll);
			sideSearchLabel.setVisible(false);
		}
		int i = 0;
		for (Proposition prop : propMatches.rootProps) {
			sideSearchResults.setText(i, 0, prop.getContent());
			sideSearchResults.setWidget(i, 1, new SearchButton(i,
					propMatches.rootProps));
			i++;
		}
	}

	/**
	 * annoyingly, by default the Tree eats the arrow key events so they can't
	 * be used for moving in a text box. Setting a handler on the tree to keep
	 * the events from doing their default behavior or propagating doesn't seem
	 * to work. I found this fix on stack overflow
	 */
	public class EditModeTree extends ArgTree {

		EditMode editMode;

		public EditModeTree(EditMode parent) {
			editMode = parent;
		}

		public EditMode getEditMode() {
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
		ArgMap.logStart("em.btcoop");
		for (int i = 0; i < tree.getItemCount(); i++) {
			ViewNode clonedPropView = recursiveTreeClone((ViewPropEdit) tree
					.getItem(i));
			cloneTree.addItem(clonedPropView);
		}
		ArgMap.logEnd("em.btcoop");
		return cloneTree;
	}

	public ViewNode recursiveTreeClone(ViewNode realViewNode) {
		ArgMap.logIndent("em.btcoop");
		ViewNode cloneViewNode = realViewNode.createViewNodeVerClone();

		for (int i = 0; i < realViewNode.getChildCount(); i++) {
			ViewNode realChild = realViewNode.getChildView(i);
			if (realViewNode.getState()) {
				cloneViewNode.addItem(recursiveTreeClone(realChild));
			} else {
				cloneViewNode.addItem(new ViewDummyVer(realChild.getNodeID()));
			}
		}
		ArgMap.logUnindent("em.btcoop");
		return cloneViewNode;
	}

	public void getOpenPropsAndArgs(List<Proposition> props, List<Argument> args) {
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveGetOpenPropsAndArgs(((ViewPropEdit) tree.getItem(i)),
					props, args);
		}
	}

	public void recursiveGetOpenPropsAndArgs(ViewPropEdit propView,
			List<Proposition> props, List<Argument> args) {
		props.add(propView.getProposition());
		if (propView.getState() || propView.getChildCount() == 0) {
			/*
			 * state seems to return false when item has no children so we also
			 * need to include childless nodes
			 */
			for (int i = 0; i < propView.getChildCount(); i++) {
				ViewArgEdit argView = ((ViewArgEdit) propView.getChild(i));
				args.add(argView.argument);
				if (argView.getState() || propView.getChildCount() == 0) {
					for (int j = 0; j < argView.getChildCount(); j++) {
						recursiveGetOpenPropsAndArgs(
								(ViewPropEdit) argView.getChild(j), props, args);
					}
				}
			}
		}
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		//int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == searchTextBox) {
			// if (charCode == 32) { //keeping this around to I remember how to select for space bar...
			ServerComm.searchProps(searchTextBox.getText(), null, null,
					new LocalCallback<PropsAndArgs>() {

						@Override
						public void call(PropsAndArgs results) {
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
				GWT.log("Returned nodes: " + nodes.toString());
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
		ArgMap.logStart("em.op");
		if (event.getTarget() instanceof ViewNode) {
			ViewNode source = (ViewNode) event.getTarget();
			// source.setOpen(true);
			if (!source.isLoaded()) {
				loadFromServer(source, 1);
			}
		}
		ArgMap.logEnd("em.op");
	}
}
