package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.Transient;

public class Proposition implements Serializable {
	
	private static final long serialVersionUID = 1L;
	@Id
	
	public Long id;
	public String content;
	public boolean topLevel;
	
	public @Transient List<Argument> args = new LinkedList<Argument>();
	
	public Proposition(){
	
	}
	
	public Proposition( String content ){
		this.content = content;
	}
	
	public String getContent(){
		return content;
	}
	
	public Long getID(){
		return id;
	}
	
	public void setContent(String content){
		this.content = content;
	}
	
	public List<Argument> getArgs(){
		return args;
	}
	
	public Argument getArgument( int i ){
		return args.get( i );
	}
	
	public int getCount(){
		return args.size();
	}
	
	public Argument insertArgumentAt( int i ){
		Argument argument = new Argument();
		args.add( i, argument  );
		return argument;
	}
	
	public void deleteArgumentl( Argument argument ){
		args.remove( argument );
	}
	
	
}
