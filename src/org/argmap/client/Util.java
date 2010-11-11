package org.argmap.client;

import java.util.Map;
import java.util.Map.Entry;

public class Util {
	public static <K, V> String mapToString( Map<K, V> map ){
		StringBuilder sb = new StringBuilder();
		for( Entry<K, V> entry : map.entrySet() ){
			sb.append("K:" + entry.getKey().toString() + "; V:" + entry.getValue().toString() );
		}
		return sb.toString();
	}
}
