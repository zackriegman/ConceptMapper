package com.appspot.conceptmapper.client;

import java.util.List;
import java.util.Map;

import com.appspot.conceptmapper.client.ServerComm.SearchPropositionsCallback;
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
		SearchPropositionsCallback {

	private HTML messageArea = new HTML();
	private Label searchLabel = new Label(
			"Would you like to use one of these already existing propositions?");
	private FlexTable searchResults = new FlexTable();
	private EditModeTree tree;
	private List<Proposition> propositionMatches;

	public EditMode() {
		super();

		FlowPanel mainPanel = new FlowPanel();

		SplitLayoutPanel mainSplit = new SplitLayoutPanel();
		SplitLayoutPanel sideSplit = new SplitLayoutPanel();

		sideSplit.addNorth(messageArea, 280);

		FlowPanel searchFlowPanel = new FlowPanel();
		searchFlowPanel.add(searchLabel);
		searchLabel.setVisible(false);
		searchFlowPanel.add(searchResults);
		sideSplit.add(searchFlowPanel);

		Button addPropButton = new Button("Add A New Proposition");
		addPropButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				PropositionView newPropView = new PropositionView(true);

				// close the other tree items
				for (int i = 0; i < tree.getItemCount(); i++) {
					tree.getItem(i).setState(false);
				}

				tree.addItem(newPropView);
				newPropView.haveFocus();
				ServerComm
						.addProposition(newPropView.getProposition(), null, 0);
			}
		});

		mainPanel.add(addPropButton);

		tree = new EditModeTree();
		tree.searchCallback = this;

		mainPanel.add(tree);

		ServerComm.fetchProps(new ServerComm.FetchPropsCallback() {

			@Override
			public void call(Proposition[] props) {
				for (Proposition prop : props) {
					tree.addItem(PropositionView.recursiveBuildPropositionView(
							prop, true, null, null));
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
	public void searchPropositionsCallback(List<Proposition> propMatches) {
		class SearchButton extends Button implements ClickHandler {
			int resultIndex;

			SearchButton(int resultIndex) {
				super("use this");
				this.resultIndex = resultIndex;
				addClickHandler(this);
			}

			public void onClick(ClickEvent event) {
				ConceptMapper.message("users want to use proposition:"
						+ searchResults.getText(resultIndex, 0) + "; with this propID:" +
						propositionMatches.get(resultIndex ).id);
			}
		}
		propositionMatches = propMatches;
		searchResults.removeAllRows();
		int i = 0;
		for (Proposition prop : propMatches) {
			searchLabel.setVisible(true);
			searchResults.setText(i, 0, prop.getContent());
			searchResults.setWidget(i, 1, new SearchButton(i));
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
		public SearchPropositionsCallback searchCallback;

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

	public Tree buildTreeCloneOfOpenNodesWithIndexes(Tree cloneTree,
			Map<Long, PropositionView> propIndex,
			Map<Long, ArgumentView> argIndex) {
		// TODO build the index
		for (int i = 0; i < tree.getItemCount(); i++) {
			PropositionView clonedPropView = recursiveTreeClone(
					(PropositionView) tree.getItem(i), propIndex, argIndex);
			cloneTree.addItem(clonedPropView);
		}
		return cloneTree;
	}

	public PropositionView recursiveTreeClone(PropositionView realPropView,
			Map<Long, PropositionView> propIndex,
			Map<Long, ArgumentView> argIndex) {
		// TODO make prop view non-editable
		PropositionView clonePropView = realPropView.createClone();
		propIndex.put(clonePropView.getProposition().id, clonePropView);

		/* if the proposition is open then clone it */
		if (realPropView.getState()) {
			for (int i = 0; i < realPropView.getChildCount(); i++) {
				ArgumentView realArgView = (ArgumentView) realPropView
						.getChild(i);
				ArgumentView cloneArgView = realArgView.createClone();
				argIndex.put(cloneArgView.argument.id, cloneArgView);
				clonePropView.addItem(cloneArgView);

				/* if the argument is open then clone it */
				if (realArgView.getState()) {
					for (int j = 0; j < realArgView.getChildCount(); j++) {
						cloneArgView.addItem(recursiveTreeClone(
								(PropositionView) realArgView.getChild(j),
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
			recursiveGetOpenPropsAndArgs(((PropositionView) tree.getItem(i)),
					props, args);
		}
	}

	public void recursiveGetOpenPropsAndArgs(PropositionView propView,
			List<Proposition> props, List<Argument> args) {
		props.add(propView.getProposition());
		if (propView.getState() || propView.getChildCount() == 0) {
			/*
			 * state seems to return false when item has no children so we also
			 * need to include childless nodes
			 */
			for (int i = 0; i < propView.getChildCount(); i++) {
				ArgumentView argView = ((ArgumentView) propView.getChild(i));
				args.add(argView.argument);
				if (argView.getState() || propView.getChildCount() == 0) {
					for (int j = 0; j < argView.getChildCount(); j++) {
						recursiveGetOpenPropsAndArgs((PropositionView) argView
								.getChild(j), props, args);
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
