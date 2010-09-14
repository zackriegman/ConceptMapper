package com.appspot.conceptmapper.server;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.appspot.conceptmapper.client.Proposition;
import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class PropositionPersistent {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;

	@Persistent
	private String content;

	public PropositionPersistent(Proposition proposition) {
		content = proposition.getContent();
	}
	
	public Proposition getProposition(){
		return new Proposition( content );
	}
	
	public String getContent(){
		return content;
	}
	
	public void setContent( String content ){
		this.content = content;
	}


}
