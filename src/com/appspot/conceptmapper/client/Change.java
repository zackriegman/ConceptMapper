package com.appspot.conceptmapper.client;

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
		PROP_DELETION, PROP_ADDITION, PROP_MODIFICATION, ARG_ADDITION, PROP_UNLINK, PROP_LINK
	}

	public String remoteAddr;
	public String remoteHost;
	public int remotePort;
	public String remoteUser;

	// not yet used ... will be used when I implement users
	public Long userID;

	/*
	 * When a change is a deletion, proposition holds the proposition that was
	 * deleted. When a change is an update, proposition holds the proposition
	 * that was updated in it's pre-updated state. When a change is an addition,
	 * proposition will be empty.
	 */
	@Embedded
	public Proposition proposition;
	

	/* if the change type is a proposition deletion or a proposition de-linking
	 * then argID stores the argument from which the proposition was removed
	 *  and the argPropIndex stores the point in the arg's prop list that the proposition
	 *  was before the removal. 
	 */
	public Long argID;
	public int argPropIndex = -1;
	public boolean argPro;
	
	public Change(){
		
	}
	
	public Change( ChangeType changeType ){
		this.changeType = changeType;
	}
}
