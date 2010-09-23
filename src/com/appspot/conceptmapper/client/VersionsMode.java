package com.appspot.conceptmapper.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Tree;

public class VersionsMode extends HorizontalPanel {

	private HTML messageArea = new HTML();
	private ListBox versionList = new ListBox();
	private EditMode editMode;
	private Tree treeClone = null;
	Map<Long, PropositionView> propViewIndex;
	Map<Long, ArgumentView> argViewIndex;
	private List<Change> changes;

	public VersionsMode(EditMode editModePair) {
		this.editMode = editModePair;

		add(versionList);
		add(messageArea);
		versionList.setVisibleItemCount(30);
		versionList.setWidth("20em");

		versionList.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				if (treeClone != null) {
					remove(treeClone);
				}
				treeClone = new Tree();
				propViewIndex = new HashMap<Long, PropositionView>();
				argViewIndex = new HashMap<Long, ArgumentView>();
				editMode.buildTreeCloneWithIndexes(treeClone, propViewIndex,
						argViewIndex);
				modifyTreeToVersion();
				add(treeClone);
			}
		});
	}

	public void modifyTreeToVersion() {
		int index = versionList.getSelectedIndex();
		//TODO shouldn't this be index + 1, rather than index?  but indexOutOfBoundsException is thrown...
		for (int i = 0; i < index; i++) {
			Change change = changes.get(i);
			switch (change.changeType) {
			case PROP_DELETION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView newPropView = new PropositionView();
				propViewIndex.put(change.propID, newPropView);
				newPropView.setText(change.propContent);
				argView.insertPropositionViewAt(change.argPropIndex,
						newPropView);
				break;
			}
			case PROP_ADDITION: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = propViewIndex.get(change.propID);
				argView.removeItem(propView);
				break;
			}
			case PROP_MODIFICATION: {
				PropositionView propView = propViewIndex.get(change.propID);
				propView.setContent(change.propContent);
				break;
			}
			case ARG_ADDITION: {
				PropositionView propView = propViewIndex.get(change.propID);
				ArgumentView argView = argViewIndex.get(change.argID);
				propView.removeItem(argView);
				break;
			}
			case PROP_UNLINK: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = propViewIndex.get(change.propID);
				argView.removeItem(propView);
				break;
			}
			case PROP_LINK: {
				ArgumentView argView = argViewIndex.get(change.argID);
				PropositionView propView = propViewIndex.get(change.propID);
				/* TODO need to think through linking and unlinking in more detail.
				 * What I have here will not be enough.  Specifically the server
				 * probably doesn't currently send the linked proposition's content
				 * which will be important for the client to display when reviewing revisions.
				 * Furthermore, with all the other operations, a given change will only
				 * add a node.  But here the change will add hundreds of nodes, the children
				 * of the linked proposition.  Probably the way to show that is simply
				 * to add the proposition, and then lazy load the tree and someone browses.
				 */
				argView.insertPropositionViewAt(change.argPropIndex, propView);
				break;
			}
			}

		}
	}

	public void println(String string) {
		print('\n' + string + "<br />");
	}

	public void print(String string) {
		messageArea.setHTML(string + messageArea.getHTML());
	}

	public void displayVersions() {
		List<Proposition> props = new LinkedList<Proposition>();
		List<Argument> args = new LinkedList<Argument>();
		editMode.getOpenPropsAndArgs(props, args);
		ServerComm.getChanges(null, props, args,
				new ServerComm.GetChangesCallback() {

					@Override
					public void call(List<Change> chngs) {
						changes = chngs;
						println("Got back " + changes.size() + " changes");
						for (int i = 0; i < versionList.getItemCount(); i++) {
							versionList.removeItem(i);
						}
						for (Change change : changes) {
							versionList.addItem(changeToString(change), ""
									+ change.date);
							println("\nChange Logged -- "
									+ changeToString(change));
						}
					}
				});
	}

	private String changeToString(Change change) {
		return "changeType:" + change.changeType + "; argID:" + change.argID
				+ "; argPropIndex:" + change.argPropIndex + "; argPro:"
				+ change.argPro + "; propID:" + change.propID
				+ "; propContent:" + change.propContent + "; propTopLevel:"
				+ change.propTopLevel + "; date:" + change.date
				+ "; remoteAddr:" + change.remoteAddr + "; remoteHost:"
				+ change.remoteHost + "; remotePort:" + change.remotePort
				+ "; remoteUser:" + change.remoteUser;
	}

}
