package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.Key;

public class Proposition implements Serializable {
	String content;
	List<Argument> arguments = new ArrayList<Argument>();
	Key key;
	
	public Proposition(){
	
	}
	
	public Proposition( String content, Key key ){
		this.content = content;
		this.key = key;
	}
	
	public String getContent(){
		return content;
	}
	
	public void setContent(String content){
		this.content = content;
	}
	
	public Key getKey(){
		return key;
	}
	
	public Argument getArgument( int i ){
		return arguments.get( i );
	}
	
	public int getCount(){
		return arguments.size();
	}
	
	public Argument insertArgumentAt( int i ){
		Argument argument = new Argument();
		arguments.add( i, argument  );
		return argument;
	}
	
	public void deleteArgumentl( Argument argument ){
		arguments.remove( argument );
	}
	
	
}
