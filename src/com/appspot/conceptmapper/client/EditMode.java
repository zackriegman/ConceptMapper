package com.appspot.conceptmapper.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

public class EditMode extends VerticalPanel {

	Tree tree;

	public EditMode() {
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

		add(addPropButton);
		/**
		 * annoyingly, by default the Tree eats the arrow key events so they
		 * can't be used for moving in a text box. Setting a handler on the tree
		 * to keep the events from doing their default behavior or propagating
		 * doesn't seem to work. I found this fix on stack overflow
		 */
		tree = new Tree() {
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
		};

		add(tree);

		ServerComm.fetchProps(new ServerComm.FetchPropsCallback() {

			@Override
			public void call(Proposition[] props) {
				for (Proposition prop : props) {
					tree.addItem(recursiveBuildPropositionView(prop, null));
				}
				openTree();
			}
		});

		tree.setAnimationEnabled(false);
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
		if (realPropView.getState()) {
			for (int i = 0; i < realPropView.getChildCount(); i++) {
				ArgumentView realArgView = (ArgumentView) realPropView
						.getChild(i);
				ArgumentView cloneArgView = realArgView.createClone();
				argIndex.put(cloneArgView.argument.id, cloneArgView);
				clonePropView.addItem(cloneArgView);
				if (realArgView.getState()) {
					for (int j = 0; j < realArgView.getChildCount(); j++) {
						cloneArgView.addItem(recursiveTreeClone(
								(PropositionView) realArgView.getChild(j),
								propIndex, argIndex));
					}
				}
			}
		}
		return clonePropView;
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

	private PropositionView recursiveBuildPropositionView(Proposition prop,
			ArgumentView argView) {
		PropositionView propView = new PropositionView(prop, true);
		for (Argument arg : prop.args) {
			propView.addItem(recursiveBuildArgumentView(arg));
		}
		return propView;
	}

	private ArgumentView recursiveBuildArgumentView(Argument arg) {
		ArgumentView argView = new ArgumentView(arg);
		for (Proposition prop : arg.props) {
			argView.addItem(recursiveBuildPropositionView(prop, argView));
		}
		return argView;
	}

}
