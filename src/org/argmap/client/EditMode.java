package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.PropsAndArgs;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class EditMode extends ResizeComposite implements
		LocalCallback<List<Proposition>>, KeyDownHandler,
		OpenHandler<TreeItem>, CloseHandler<TreeItem> {

	private static HTML sideMessageArea = new HTML();
	private final Label sideSearchLabel = new Label(
			"Would you like to use one of these already existing propositions?");
	private final FlexTable sideSearchResults = new FlexTable();
	private final ScrollPanel sideSearchScroll;
	private final ScrollPanel sideMessageScroll;
	private final SplitLayoutPanel sideSplit = new SplitLayoutPanel();

	TextBox searchTextBox = new TextBox();
	private final EditModeTree tree;

	public EditMode() {
		super();

		FlowPanel mainPanel = new FlowPanel();

		SplitLayoutPanel mainSplit = new SplitLayoutPanel();

		FlowPanel sideSearchArea = new FlowPanel();
		sideSearchArea.add(sideSearchLabel);
		sideSearchLabel.setVisible(false);
		sideSearchArea.add(sideSearchResults);
		sideSearchScroll = new ScrollPanel(sideSearchArea);

		sideMessageScroll = new ScrollPanel(sideMessageArea);
		sideSplit.add(sideMessageScroll);

		Button addPropButton = new Button("Add a new proposition");
		addPropButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					ViewPropEdit newPropView = new ViewPropEdit();

					// close the other tree items
					for (int i = 0; i < tree.getItemCount(); i++) {
						tree.getItem(i).setState(false);
					}

					tree.addItem(newPropView);
					newPropView.haveFocus();
					ServerComm.addProp(newPropView.getProposition(), null, 0);
				} catch (Exception e) {
					ServerComm.handleClientException(e);
				}
			}
		});

		HorizontalPanel searchBoxPanel = new HorizontalPanel();
		searchTextBox.addKeyDownHandler(this);
		searchTextBox.setVisibleLength(50);
		searchBoxPanel.add(new Label("Search:"));
		searchBoxPanel.add(searchTextBox);
		searchBoxPanel.add(addPropButton);
		mainPanel.add(searchBoxPanel);

		tree = new EditModeTree( this );
		tree.addCloseHandlerTracked(this);
		tree.addOpenHandlerTracked(this);
		tree.searchCallback = this;

		mainPanel.add(tree);

		ServerComm.getRootProps(3,
				new ServerComm.LocalCallback<PropsAndArgs>() {

					@Override
					public void call(PropsAndArgs allNodes) {
						try {
							ArgMap.logStart("em.em.cb");
							ArgMap.log("em.em.cb", "Prop Tree From Server");
							for (Long propID : allNodes.rootProps.keySet()) {

								Proposition proposition = allNodes.rootProps
										.get(propID);
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

		if (false)
			ServerComm.getRootProps(100,
					new ServerComm.LocalCallback<PropsAndArgs>() {

						@Override
						public void call(PropsAndArgs allNodes) {
							try {

								ArgMap.logStart("em.em.cb");
								ArgMap.log("em.em.cb", "Prop Tree From Server");
								for (Long propID : allNodes.rootProps.keySet()) {

									Proposition proposition = allNodes.rootProps
											.get(propID);
									ViewProp propView = new ViewPropEdit();
									propView.recursiveBuildViewNode(
											proposition, allNodes.nodes);

									tree.addItem(propView);
									propView.logNodeRecursive(0, "em.em.cb",
											true);
								}
								tree.resetState();
								ArgMap.logEnd("em.em.cb");
							} catch (Exception e) {
								ServerComm.handleClientException(e);
							}
						}
					});

		tree.setAnimationEnabled(false);

		mainSplit.addEast(sideSplit, 400);
		mainSplit.add(new ScrollPanel(mainPanel));
		initWidget(mainSplit);
	}

	public static void log(String string) {
		sideMessageArea.setHTML(sideMessageArea.getHTML() + string);
	}

	@Override
	public void call(List<Proposition> propMatches) {
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
		if (propMatches.size() > 0) {
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
		for (Proposition prop : propMatches) {
			sideSearchResults.setText(i, 0, prop.getContent());
			sideSearchResults.setWidget(i, 1, new SearchButton(i, propMatches));
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
		public LocalCallback<List<Proposition>> searchCallback;
		
		EditMode editMode;
		
		public EditModeTree( EditMode parent ){
			editMode = parent;
		}
		
		public EditMode getEditMode(){
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

	// private void openTree() {
	// for (int i = 0; i < tree.getItemCount(); i++) {
	// recursiveOpenTreeItem(tree.getItem(i));
	// }
	// }

	// private void recursiveOpenTreeItem(TreeItem item) {
	// item.setState(true);
	// for (int i = 0; i < item.getChildCount(); i++) {
	// recursiveOpenTreeItem(item.getChild(i));
	// }
	// }

	@Override
	public void onKeyDown(KeyDownEvent event) {
		int charCode = event.getNativeKeyCode();
		Object source = event.getSource();
		if (source == searchTextBox) {
			if (charCode == 32) {
				// ServerComm.searchProps(searchTextBox.getText(), null, new
				// LocalCallback<List<Proposition>>() {
				//
				// @Override
				// public void call(List<Proposition> t) {
				// //TODO add list to main panel with dummy nodes ready for lazy
				// load!
				//
				// }
				// });
			}
		}

	}

	@Override
	public void onClose(CloseEvent<TreeItem> event) {
		if (event.getSource() instanceof ViewNode) {
			ViewNode source = (ViewNode) event.getSource();
			source.setOpen(false);
		}
	}

	public void loadFromServer(ViewNode viewNode, int depth) {
		assert ! viewNode.isLoaded();

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
			source.setOpen(true);
			if (!source.isLoaded()) {
				loadFromServer(source, 1);
			}
		}
		ArgMap.logEnd("em.op");
	}
}
