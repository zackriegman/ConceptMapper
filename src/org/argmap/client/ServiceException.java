package org.argmap.client;

import java.io.Serializable;

public class ServiceException extends Exception implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ServiceException( String message ){
		super( message );
	}

}
