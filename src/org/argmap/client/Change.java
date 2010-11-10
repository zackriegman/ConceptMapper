package org.argmap.client;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Embedded;
import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;

/*
 * OK, I need to think through what information I need to back out each of
 * deletion, addition, and modification for each of propositions and arguments.
 * 
 * Proposition deletion:  At the moment there is no linking so if a proposition 
 * is deleted it needs to be removed only from its parent argument.  However in the future
 * there will be a more complicated relationship for arguments and propositions.  When
 * that happens a proposition deletion may not make sense at all.  Rather you would
 * only have proposition unlinking.  Maybe a proposition would be deleted if it had
 * no arguments that used it... but maybe that just means it's a top level proposition
 * and should be kept.  It may make sense to implement linking before I implement versioning
 * (or vice-versa) because the design of one might have a big impact on the design of the other.
 * Maybe instead of proposition deletion, there should be proposition spam marking to mark
 * those propositions that should not be returned in searches because they are spam/vandalism.
 * Linking/unlinking of propositions to arguments would be handled by an argument modification
 * change.  But how does this work with the current editing scheme, where, when a proposition is
 * deleted by editing it's content.  How would a system of linking and unlinking deal with minor changes?
 * 
 * Proposition addition:  This seems simple to version. Simply delete the proposition to go backwards
 * in time.  Don't have to worry about updating the arguments that held the proposition, because those
 * changes would be held in separate argument change objects.  By the time you've gone backwards in time
 * enough to delete a proposition there should be no arguments that refer to it anymore.
 * 
 * Proposition modifications:  I think these should be easy, all you've got to do
 * is record what the proposition looked like before the modification.
 * 
 * Argument addition: 
 * 
 * Argument deletion:
 * 
 * Argument modification:
 */
@Cached
public class Change implements Serializable {

	private static final long serialVersionUID = 1L; // to suppress warnings

	@Id
	public Long id;

	public Date date; // the date and time of the change
	public ChangeType changeType; // the type of the change, either deletion,

	// addition or modification

	public enum ChangeType {
		PROP_DELETION, PROP_ADDITION, PROP_MODIFICATION, ARG_ADDITION, ARG_DELETION, ARG_MODIFICATION, PROP_UNLINK, PROP_LINK
	}

	public String remoteAddr;
	public String remoteHost;
	public int remotePort;
	public String remoteUser;
	public String sessionID;

	// not yet used ... will be used when I implement users
	public Long userID;

	/*
	 * For change types: PROP_DELETION, PROP_ADDITION, PROP_UNLINK, PROP_LINK
	 * propID refers to the child proposition of the argument
	 * 
	 * For change type: PROP_MODIFICATION propID refers to the proposition whose
	 * content has been modified
	 * 
	 * For change type: ARG_ADDITION, ARG_DELETION propID refers to the parent
	 * proposition of the argument. (But note that it is not actually necessary
	 * to record the parent proposition for an ARG_ADDITION because the arg
	 * knows its parent and can only have one, so we can figure out what to
	 * delete merely with the argID. However currently it's used on the client
	 * side, so I'm keeping it here for now.)
	 */
	public Long propID;

	public int propLinkCount;

	/*
	 * For change type: PROP_MODIFICATION content refers to the proposition
	 * content
	 * 
	 * For change type: ARG_MODIFICATION content refers to the argument title
	 */
	public String oldContent;
	
	public String newContent_DELETE_ME;

	/*
	 * For change types: PROP_DELETION, PROP_ADDITION, PROP_UNLINK, PROP_LINK
	 * argID refers to a parent argument of the proposition
	 * 
	 * For change type: PROP_MODIFICATION argID should be null
	 * 
	 * For change type: ARG_ADDITION, ARG_DELETION argID refers to the child
	 * argument of the proposition.
	 */
	public Long argID;
	public int argPropIndex = -1;
	public boolean argPro;
	
	@Embedded
	public Proposition link;

	public Change() {

	}

	public Long getParentID() {
		switch (changeType) {
		case PROP_DELETION:
		case PROP_ADDITION:
		case PROP_UNLINK:
		case PROP_LINK:
			return argID;
		case ARG_ADDITION:
		case ARG_DELETION:
			return propID;
		}
		throw new RuntimeException( "this change type does not have parent:" + toString() );
	}

	public Long getDeletedID() {
		switch (changeType) {
		case ARG_DELETION:
			return argID;
		case PROP_DELETION:
		case PROP_UNLINK:
			return propID;
		}
		throw new RuntimeException( "this change type does not have deleted ID:" + toString() );
	}
	
	public Long getAddedID() {
		switch (changeType) {
		case ARG_ADDITION:
			return argID;
		case PROP_ADDITION:
		case PROP_LINK:
			return propID;
		}
		throw new RuntimeException( "this change type does not have added ID:" + toString() );
	}
	
	public boolean isDeletion(){
		switch (changeType) {
		case ARG_DELETION:
		case PROP_DELETION:
		case PROP_UNLINK:
			return true;
		}
		return false;
	}
	
	public boolean isAddition(){
		switch (changeType) {
		case ARG_ADDITION:
		case PROP_ADDITION:
		case PROP_LINK:
			return true;
		}
		return false;
	}

	public Change(ChangeType changeType) {
		this.changeType = changeType;
	}

	@Override
	public String toString() {
		return "id:" + id + "; changeType:" + changeType + "; argID:" + argID
				+ "; propID:" + propID + "; content:" + oldContent + "; date:"
				+ date + "; sessionID:" + sessionID;
	}

	public String toStringLong() {
		return toString() + "; argPro:" + argPro + "; argPropIndex:"
				+ argPropIndex + "; propLinkCount:" + propLinkCount;
	}

	public String toStringLonger() {
		return toStringLong() + "; remoteAddr:" + remoteAddr + "; remoteHost:"
				+ remoteHost + "; remotePort:" + remotePort + "; remoteUser:"
				+ remoteUser;
	}

}
