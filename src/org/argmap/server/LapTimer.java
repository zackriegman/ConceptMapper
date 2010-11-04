package org.argmap.server;

public class LapTimer {
	private final StringBuilder stringBuilder = new StringBuilder();
	private long lapTimeBegin = System.currentTimeMillis();;
	public long lap( String marker ){
		long newTime = System.currentTimeMillis();
		long lapTime = newTime - lapTimeBegin;
		lapTimeBegin = newTime;
		stringBuilder.append( "\n" + marker + " : " + lapTime );
		return lapTime;
	}
	
	public String getRecord(){
		return stringBuilder.toString();
	}
}
