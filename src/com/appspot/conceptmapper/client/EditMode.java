package com.appspot.conceptmapper.client;

import java.util.List;
import java.util.Map;

import com.appspot.conceptmapper.client.PropositionService.AllPropsAndArgs;
import com.appspot.conceptmapper.client.ServerComm.LocalCallback;
import com.google.gwt.core.client.GWT;
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
				GWT.log("Root Props:");
				for (Long propID : allNodes.rootProps.keySet()) {
					GWT.log("propID:" + propID + "; prop:"
							+ allNodes.rootProps.get(propID).toString());
				}
				GWT.log("Props:");
				for (Long propID : allNodes.nodes.props.keySet()) {
					GWT.log("propID:" + propID + "; prop:"
							+ allNodes.nodes.props.get(propID).toString());
				}
				GWT.log("Args:");
				for (Long argID : allNodes.nodes.args.keySet()) {
					GWT.log("argID:" + argID + "; arg:"
							+ allNodes.nodes.args.get(argID).toString());
				}

				for (Long propID : allNodes.rootProps.keySet()) {
					Proposition proposition = allNodes.rootProps.get(propID);
					ViewPropEdit propView = recursiveBuildPropositionView(proposition, true,
									allNodes.nodes, null, null);
					tree.addItem(propView);
					propView.printPropRecursive(0);
				}
				openTree();
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
							ViewPropEdit newPropView = recursiveBuildPropositionView(proposition,
											true, nodes,
											null, null);
							parentArgView.insertPropositionViewAt(propIndex,
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
					ConceptMapper
							.message("Cannot link to existing proposition when proposition currently being edited has children");
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

		public void printTree() {
			for (int i = 0; i < getItemCount(); i++) {
				((ViewPropEdit) getItem(i)).printPropRecursive(0);
			}
		}
	}

	public Tree buildTreeCloneOfOpenNodesWithIndexes(Tree cloneTree,
			Map<Long, ViewPropVer> propIndex,
			Map<Long, ViewArgVer> argIndex) {
		// TODO build the index
		for (int i = 0; i < tree.getItemCount(); i++) {
			ViewPropVer clonedPropView = recursiveTreeClone(
					(ViewPropEdit) tree.getItem(i), propIndex, argIndex);
			cloneTree.addItem(clonedPropView);
		}
		return cloneTree;
	}

	public ViewPropVer recursiveTreeClone(ViewPropEdit realPropView,
			Map<Long, ViewPropVer> propIndex,
			Map<Long, ViewArgVer> argIndex) {
		// TODO make prop view non-editable
		ViewPropVer clonePropView = ViewPropVer.cloneViewEdit(realPropView );
		propIndex.put(clonePropView.getProposition().id, clonePropView);

		/* if the proposition is open then clone it */
		if (realPropView.getState()) {
			for (int i = 0; i < realPropView.getChildCount(); i++) {
				ViewArgEdit realArgView = (ViewArgEdit) realPropView
						.getChild(i);
				ViewArgVer cloneArgView = realArgView.createClone();
				argIndex.put(cloneArgView.argument.id, cloneArgView);
				clonePropView.addItem(cloneArgView);

				/* if the argument is open then clone it */
				if (realArgView.getState()) {
					for (int j = 0; j < realArgView.getChildCount(); j++) {
						cloneArgView.addItem(recursiveTreeClone(
								(ViewPropEdit) realArgView.getChild(j),
								propIndex, argIndex));
					}
				}
				/*
				 * if the argument is not open, but does have child
				 * propositions, then insert a place holder
				 */
				else if (realArgView.getChildCount() > 0) {
					cloneArgView.addItem(newLoadDummyTreeItemArg());
				}
			}
		}
		/*
		 * if the proposition is not open, but does have child args, insert a
		 * place holder
		 */
		else if (realPropView.getChildCount() > 0) {
			clonePropView.addItem(newLoadDummyTreeItemProp());
		}
		return clonePropView;
	}

	public TreeItem newLoadDummyTreeItemProp() {
		TreeItem treeItem = new TreeItem("loading from server...");
		treeItem.addStyleName("loadDummyProp");
		return treeItem;
	}

	public TreeItem newLoadDummyTreeItemArg() {
		TreeItem treeItem = new TreeItem("loading from server...");
		treeItem.addStyleName("loadDummyArg");
		return treeItem;
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
								(ViewPropEdit) argView.getChild(j), props,
								args);
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
	
	public ViewPropEdit recursiveBuildPropositionView(Proposition prop,
			boolean editable, Nodes nodes,
			Map<Long, ViewPropEdit> propViewIndex,
			Map<Long, ViewArgEdit> argViewIndex) {

		ViewPropEdit propView = new ViewPropEdit(prop);
		if (propViewIndex != null)
			propViewIndex.put(prop.id, propView);
		for (Long argID : prop.argIDs) {
			Argument argument = nodes.args.get(argID);
			propView.addItem(recursiveBuildArgumentView(argument, editable,
					nodes, propViewIndex, argViewIndex));
		}
		return propView;
	}

	public ViewArgEdit recursiveBuildArgumentView(Argument arg,
			boolean editable, Nodes nodes,
			Map<Long, ViewPropEdit> propViewIndex,
			Map<Long, ViewArgEdit> argViewIndex) {

		ViewArgEdit argView = new ViewArgEdit(arg);
		if (argViewIndex != null)
			argViewIndex.put(arg.id, argView);
		for (Long propID : arg.propIDs) {
			Proposition proposition = nodes.props.get(propID);
			argView.addItem(recursiveBuildPropositionView(proposition,
					editable, nodes, propViewIndex, argViewIndex));
		}
		return argView;
	}
}
