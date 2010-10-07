package com.appspot.conceptmapper.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.annotation.Cached;

@Cached
public class Proposition implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	public Long id;
	public String content;
	public List<Long> argIDs = new ArrayList<Long>();
	public int linkCount;
	
	/* not sent to client, but saved */
	transient public Set<String> tokens;
	
	/* sent to client, but not saved */
	@Transient
	public List<Change> changes;
	
	/* sent to client, but not saved */
	@Transient
	public Date changesTo;

	
	//public @Transient List<Argument> args = new LinkedList<Argument>();
	
	public Proposition(){
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("id:");
		buffer.append( id );
		buffer.append( "; content:" );
		buffer.append(content);
		buffer.append( "; linkCount:" );
		buffer.append( linkCount );
		buffer.append( "; argIDs:[" );
		if(argIDs != null){
			for( Long id : argIDs){
				buffer.append( " " );
				buffer.append( id );
			}
		}
		buffer.append( "]; tokens:[");
		if (tokens != null) {
			for (String str : tokens) {
				buffer.append(" ");
				buffer.append(str);
			}
		}
		buffer.append( "]");
		return buffer.toString();
	}
	
	public Proposition( Proposition prop ){
		id = prop.id;
		content = prop.content;
		linkCount = prop.linkCount;
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
	
	/*
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
	*/
}
