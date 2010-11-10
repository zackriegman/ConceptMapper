package org.argmap.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MultiMap<K, V> {
	private final Map<K, List<V>> map = new HashMap<K, List<V>>();
	
	public List<V> get(K key){
		return map.get( key );
	}
	
	public void put(K key, V value){
		List<V> list = map.get( key );
		if( list == null ){
			list = new ArrayList<V>(1);
			map.put(key, list);
		}
		list.add( value );
	}
	
	public void putAll( MultiMap<K, V> otherMap ){
		Set<Map.Entry<K, List<V>>> entries = map.entrySet();
		for( Map.Entry<K, List<V>> entry : entries ){
			putList( entry.getKey(), entry.getValue() );
		}
	}
	
	private void putList(K key, List<V> values ){
		List<V> list = map.get( key );
		if( list == null ){
			list = new ArrayList<V>(1);
			map.put(key, list);
		}
		list.addAll( values );
	}
	
	public List<V> removeList(K key){
		return map.remove(key);
	}
	
	public List<V> removeAllWithKey(K key){
		return map.remove(key);
	}
	
	public void remove( K key, V value ){
		List<V> list = map.get(key);
		list.remove(value);
		if(list.isEmpty()){
			map.remove(key);
		}
	}
	
	public Collection<List<V>> values() {
		return map.values();
	}
	
	public Set<K> keySet(){
		return map.keySet();
	}
	
	public void clear(){
		map.clear();
	}
}
