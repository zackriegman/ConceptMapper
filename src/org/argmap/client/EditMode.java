package org.argmap.client;

import java.util.List;

import org.argmap.client.ArgMap.MessageType;
import org.argmap.client.ArgMapService.AllPropsAndArgs;
import org.argmap.client.ServerComm.LocalCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class EditMode extends ResizeComposite implements
		LocalCallback<List<Proposition>> {

	private HTML messageArea = new HTML();
	private Label searchLabel = new Label(
			"Would you like to use one of these already existing propositions?");
	private FlexTable searchResults = new FlexTable();
	private EditModeTree tree;

	public EditMode() {
		super();

		FlowPanel mainPanel = new FlowPanel();

		SplitLayoutPanel mainSplit = new SplitLayoutPanel();
		SplitLayoutPanel sideSplit = new SplitLayoutPanel();

		sideSplit.addNorth(new ScrollPanel(messageArea), 280);

		FlowPanel searchFlowPanel = new FlowPanel();
		searchFlowPanel.add(searchLabel);
		searchLabel.setVisible(false);
		searchFlowPanel.add(searchResults);
		sideSplit.add(new ScrollPanel(searchFlowPanel));

		Button addPropButton = new Button("Add A New Proposition");
		addPropButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				ViewPropEdit newPropView = new ViewPropEdit();

				// close the other tree items
				for (int i = 0; i < tree.getItemCount(); i++) {
					tree.getItem(i).setState(false);
				}

				tree.addItem(newPropView);
				newPropView.haveFocus();
				ServerComm.addProposition(newPropView.getProposition(), null, 0);
			}
		});

		mainPanel.add(addPropButton);

		tree = new EditModeTree();
		tree.searchCallback = this;

		mainPanel.add(tree);

		ServerComm.fetchProps(new ServerComm.LocalCallback<AllPropsAndArgs>() {

			@Override
			public void call(AllPropsAndArgs allNodes) {
				/*
				 * GWT.log("Root Props:"); for (Long propID :
				 * allNodes.rootProps.keySet()) { GWT.log("propID:" + propID +
				 * "; prop:" + allNodes.rootProps.get(propID).toString()); }
				 * GWT.log("Props:"); for (Long propID :
				 * allNodes.nodes.props.keySet()) { GWT.log("propID:" + propID +
				 * "; prop:" + allNodes.nodes.props.get(propID).toString()); }
				 * GWT.log("Args:"); for (Long argID :
				 * allNodes.nodes.args.keySet()) { GWT.log("argID:" + argID +
				 * "; arg:" + allNodes.nodes.args.get(argID).toString()); }
				 */
				ArgMap.logStart("em.em.cb");
				ArgMap.log("em.em.cb", "Prop Tree From Server");
				for (Long propID : allNodes.rootProps.keySet()) {
					Proposition proposition = allNodes.rootProps.get(propID);
					ViewProp propView = ViewProp.recursiveBuildPropositionView(
							proposition, allNodes.nodes, null, null,
							ViewPropEdit.FACTORY, ViewArgEdit.FACTORY);
					tree.addItem(propView);
					propView.logNodeRecursive(0, "em.em.cb", true);
				}
				openTree();
				ArgMap.logEnd("em.em.cb");
			}
		});

		tree.setAnimationEnabled(false);

		mainSplit.addEast(sideSplit, 400);
		mainSplit.add(new ScrollPanel(mainPanel));
		initWidget(mainSplit);
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
							ViewProp newPropView = ViewProp
									.recursiveBuildPropositionView(proposition,
											nodes, null, null,
											ViewPropEdit.FACTORY,
											ViewArgEdit.FACTORY);
							parentArgView.insertChildViewAt(propIndex,
									newPropView);
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
		searchResults.removeAllRows();
		if (propMatches.size() > 0) {
			searchLabel.setVisible(true);
		} else {
			searchLabel.setVisible(false);
		}
		int i = 0;
		for (Proposition prop : propMatches) {
			searchResults.setText(i, 0, prop.getContent());
			searchResults.setWidget(i, 1, new SearchButton(i, propMatches));
			i++;
		}
	}

	/**
	 * annoyingly, by default the Tree eats the arrow key events so they can't
	 * be used for moving in a text box. Setting a handler on the tree to keep
	 * the events from doing their default behavior or propagating doesn't seem
	 * to work. I found this fix on stack overflow
	 */
	public class EditModeTree extends Tree {
		public LocalCallback<List<Proposition>> searchCallback;

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

	private void openTree() {
		for (int i = 0; i < tree.getItemCount(); i++) {
			recursiveOpenTreeItem(tree.getItem(i));
		}
	}

	private void recursiveOpenTreeItem(TreeItem item) {
		item.setState(true);
		for (int i = 0; i < item.getChildCount(); i++) {
			recursiveOpenTreeItem(item.getChild(i));
		}
	}
}
