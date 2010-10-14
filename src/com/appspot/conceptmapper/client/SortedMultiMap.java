package com.appspot.conceptmapper.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gwt.core.client.GWT;

public class SortedMultiMap<K, V> {
	private SortedMap<K, List<V>> map = new TreeMap<K, List<V>>();

	public List<V> get(K key) {
		return map.get(key);
	}

	public void put(K key, V value) {
		List<V> list = map.get(key);
		if (list == null) {
			list = new ArrayList<V>(1);
			map.put(key, list);
		}
		list.add(value);
	}

	public void putAll(SortedMultiMap<K, V> otherMap) {
		Set<Map.Entry<K, List<V>>> entries = map.entrySet();
		for (Map.Entry<K, List<V>> entry : entries) {
			putList(entry.getKey(), entry.getValue());
		}
	}

	private void putList(K key, List<V> values) {
		List<V> list = map.get(key);
		if (list == null) {
			list = new ArrayList<V>(1);
			map.put(key, list);
		}
		list.addAll(values);
	}

	/*
	 * removes all the objects associated with the given key
	 */
	public List<V> remove(K key) {
		return map.remove(key);
	}
	
	/*
	 * this efficiently removes just the given object (rather than all the objects associated with that key
	 */
	public void remove( K key, V value ){
		List<V> list = map.get(key);
		list.remove(value);
		if(list.isEmpty()){
			map.remove(key);
		}
	}

	public K lastKey() {
		return map.lastKey();
	}

	public Collection<List<V>> values() {
		return map.values();
	}

	public List<V> firstValues() {
		return extractFirstValues(map.values());
	}

	/*
	 * public List<V> firstValuesSublist(K start, boolean startInclusive, K end,
	 * boolean endInclusive) { return extractFirstValues(valuesSublist(start,
	 * startInclusive, end, endInclusive)); }
	 */

	private List<V> extractFirstValues(Collection<List<V>> values) {
		List<V> returnList = new ArrayList<V>();
		for (List<V> list : values) {
			returnList.add(list.get(0));
		}
		return returnList;
	}

	/*
	public static void test() {
		StringBuilder sb = new StringBuilder();
		SortedMultiMap<Integer, String> map = new SortedMultiMap<Integer, String>();
		map.put(2, "two");
		map.put(4, "four");
		map.put(6, "six");
		map.put(8, "eight");
		map.put(10, "ten");
		map.put(4, "four");
		map.put(4, "four");
		map.put(4, "four");
		map.put(4, "four");

		sb.append("SortedMultiMap.test():\n");
		sb.append("\nTEST 0");
		List<List<String>> subList = map.valuesSublist(4, true, 8, true);
		for (List<String> list : subList) {
			for (String string : list) {
				sb.append("\n" + string);
			}
		}

		sb.append("\nTEST 1");
		subList = map.valuesSublist(4, false, 8, false);
		for (List<String> list : subList) {
			for (String string : list) {
				sb.append("\n" + string);
			}
		}

		sb.append("\nTEST 2");
		subList = map.valuesSublist(3, true, 7, true);
		for (List<String> list : subList) {
			for (String string : list) {
				sb.append("\n" + string);
			}
		}
		GWT.log(sb.toString());
	}
	*/

	/*
	 * I was using NavigableMap, and it was really elegant and simple and
	 * includes it's own subMap function... but it is not supported in GWT. So
	 * I'm using this ugly inefficient piece of shit instead, and crossing my
	 * fingers that the next GWT release includes a NavigableMap (which seems
	 * sort of essential if you ask me).
	 */
	public List<List<V>> valuesSublist(K start, boolean startInclusive, K end,
			boolean endInclusive) {
		//StringBuilder sb = new StringBuilder();
		//sb.append("valuesSublist(): START");
		Set<Map.Entry<K, List<V>>> entries = map.entrySet();
		Iterator<Map.Entry<K, List<V>>> iterator = entries.iterator();
		Comparator<? super K> comparator = map.comparator();
		List<List<V>> returnList = new ArrayList<List<V>>();
		if (comparator == null) {
			comparator = new Comparator<K>() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public int compare(K arg0, K arg1) {

					/*
					 * I'm confused about why I need a '-' here to make this
					 * work... compareTo(Object o), is defined as: "Compares
					 * this object with the specified object for order. Returns
					 * a negative integer, zero, or a positive integer as this
					 * object is less than, equal to, or greater than the
					 * specified
					 * object." And compare(Object o1, Object o2) is defined as: "
					 * Compares its two arguments for order. Returns a negative
					 * integer, zero, or a positive integer as the first
					 * argument is less than, equal to, or greater than the
					 * second."
					 */
					return -((Comparable) arg0).compareTo((Comparable) arg1);
				}

			};
		}

		while (iterator.hasNext()) {
			Map.Entry<K, List<V>> entry = iterator.next();
			//sb.append("\n" + entry.toString() + ": ");
			/*
			 * see note above about sign... maybe this should be negative
			 * instead of reversing my generic comparator... but I don't see why
			 * either of them should be.  But if this class doesn't work
			 * when an explicit comparator is given to the sorted list, try fiddling with
			 * where the '-' is placed her, or above...
			 */
			int comparison = comparator.compare(start, entry.getKey());
			if (comparison < 0) {
				//sb.append("not added");
				continue;
			} else if (comparison == 0) {
				if (startInclusive) {
					returnList.add(entry.getValue());
					//sb.append("added");
				} else {
					//sb.append("not added");
				}
				break;
			} else if (comparison > 0) {
				returnList.add(entry.getValue());
				//sb.append("added");
				break;
			}
		}

		while (iterator.hasNext()) {
			Map.Entry<K, List<V>> entry = iterator.next();
			//sb.append("\n" + entry.toString() + ": ");
			int comparison = comparator.compare(end, entry.getKey());
			if (comparison < 0) {
				returnList.add(entry.getValue());
				//sb.append("added");
				continue;
			} else if (comparison == 0) {
				if (endInclusive) {
					returnList.add(entry.getValue());
					//sb.append("added");
				} else{
					//sb.append("not added");
				}
				break;
			} else if (comparison > 0) {
				//sb.append("not added");
				break;
			}
			//sb.append("end search");
		}

		//sb.append("\nvaluesSublist(): END");
		//GWT.log(sb.toString());
		return returnList;
	}
}
