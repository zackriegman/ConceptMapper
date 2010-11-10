package org.argmap.client;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;

@Cached
public class Node implements Serializable {
	/** to suppress warnings...	 */
	private static final long serialVersionUID = 1L;
	
	/* remember if you change the name of a field you have to update
	 * the server queries:  they operate on fields specified by strings
	 * that are not checked at compile time.
	 */
	@Id
	public Long id;
	public String content;
	public List<Long> childIDs = new LinkedList<Long>();
	
	public Date updated;
	transient public Date created;
}
