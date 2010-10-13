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

	public List<V> remove(K key) {
		return map.remove(key);
	}

	public K lastKey() {
		return map.lastKey();
	}

	public Collection<List<V>> values() {
		return map.values();
	}

	public List<V> firstValues() {
		return extractFirstValues( map.values());
	}

	public List<V> firstValuesSublist(K start, boolean startInclusive, K end,
			boolean endInclusive) {
		return extractFirstValues(valuesSublist(start, startInclusive, end,
				endInclusive));
	}
	
	private List<V> extractFirstValues(Collection<List<V>> values ){
		List<V> returnList = new ArrayList<V>();
		for (List<V> list : values) {
			returnList.add(list.get(0));
		}
		return returnList;
	}

	/*
	 * I was using NavigableMap, and it was really elegant and simple and
	 * includes it's own subMap function... but it is not supported in GWT. So
	 * I'm using this ugly inefficient piece of shit instead, and crossing my
	 * fingers that the next GWT release includes a NavigableMap (which seems
	 * sort of essential if you ask me).
	 */
	public List<List<V>> valuesSublist(K start, boolean startInclusive, K end,
			boolean endInclusive) {
		Set<Map.Entry<K, List<V>>> entries = map.entrySet();
		Iterator<Map.Entry<K, List<V>>> iterator = entries.iterator();
		Comparator<? super K> comparator = map.comparator();
		List<List<V>> returnList = new ArrayList<List<V>>();
		if (comparator == null) {
			comparator = new Comparator<K>() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public int compare(K arg0, K arg1) {

					return ((Comparable) arg0).compareTo((Comparable) arg1);
				}

			};
		}

		while (iterator.hasNext()) {
			Map.Entry<K, List<V>> entry = iterator.next();
			int comparison = comparator.compare(start, entry.getKey());
			if (comparison < 0) {
				continue;
			} else if (comparison == 0) {
				if (startInclusive) {
					returnList.add(entry.getValue());
				}
				break;
			} else if (comparison > 0) {
				returnList.add(entry.getValue());
				break;
			}
		}

		while (iterator.hasNext()) {
			Map.Entry<K, List<V>> entry = iterator.next();
			int comparison = comparator.compare(end, entry.getKey());
			if (comparison < 0) {
				returnList.add(entry.getValue());
				continue;
			} else if (comparison == 0) {
				if (endInclusive) {
					returnList.add(entry.getValue());
				}
				break;
			} else if (comparison > 0) {
				break;
			}
		}

		return returnList;
	}

	/*
	 * 
	 * Anyway this simply returns a list of changes that fall within the range
	 * date1, non-inclusive, to date2, inclusive.
	 * 
	 * private Collection<List<V>> valuesSubset(K key1, K key2) { List<List<V>>
	 * list = new ArrayList<List<V>>(map.values()); map. int start = 0; int end
	 * = 0; for (int i = 0; i < list.size(); i++) { if
	 * (list.get(i).date.after(date1)) { start = i; break; } } for (int i =
	 * start; i < list.size(); i++) { if (list.get(i).date.equals(date2)) { end
	 * = i + 1; break; } else if (list.get(i).date.after(date2)) { end = i;
	 * break; } } return list.subList(start, end); }
	 */

}
