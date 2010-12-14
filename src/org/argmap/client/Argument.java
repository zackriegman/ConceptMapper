package org.argmap.client;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.googlecode.objectify.annotation.Cached;

@Cached
public class Argument extends Node implements Serializable {
	private static final long serialVersionUID = 1L; // to suppress warnings
	public static final int MAX_LENGTH = 60;

	/*
	 * remember if you change the name of a field you have to update the server
	 * queries: they operate on fields specified by strings that are not checked
	 * at compile time.
	 */
	public boolean pro;

	/*
	 * if a proposition's negation is being used in an argument (instead of the
	 * proposition itself) then it's id should be added to this list.
	 */
	public Set<Long> negatedChildIDs = new HashSet<Long>();

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("id:");
		buffer.append(id);
		buffer.append("; pro:");
		buffer.append(pro);
		buffer.append("; propIDs:[");
		if (childIDs != null) {
			for (Long id : childIDs) {
				buffer.append(id);
				buffer.append(" ");

			}
		}
		buffer.append("]");
		return buffer.toString();
	}

	public Argument() {
	}

	public Argument(Argument argument) {
		id = argument.id;
		pro = argument.pro;
		content = argument.content;
	}

	/*
	 * public List<Proposition> getProps(){ return props; }
	 * 
	 * public Proposition getProposition(int i) { return props.get(i); }
	 * 
	 * public int getCount() { return props.size(); }
	 * 
	 * public Proposition insertPropositionAt(int i) { Proposition proposition =
	 * new Proposition(); props.add(i, proposition); return proposition; }
	 * 
	 * public void deletePropositionl(Proposition proposition) {
	 * props.remove(proposition); }
	 */

}
