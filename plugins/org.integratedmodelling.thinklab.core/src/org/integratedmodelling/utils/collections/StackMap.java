package org.integratedmodelling.utils.collections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A normal Java Map, but with Set behavior (objects can be in no more than once) and iterates its keys like
 * a Stack (straight or in reverse), plus a method to move existing objects to the top. 
 * Basically a golem collection for special purposes.
 * 
 * @author Ferdinando Villa
 *
 * @param <T>
 */
public class StackMap<K,T> {

	private static final long serialVersionUID = 1988080719023977747L;
	private HashMap<K,T> _map = new HashMap<K,T>();
	private LinkedList<K>     _stk = new LinkedList<K>();
	
	public class ValueIterator implements Iterator<T> {

		Iterator<K> _it = null;
		
		public ValueIterator(Iterator<K> iterator) {
			_it = iterator;
		}

		@Override
		public boolean hasNext() {
			return _it.hasNext();
		}

		@Override
		public T next() {
			return _map.get(_it.next());
		}

		@Override
		public void remove() {
			// don't
		}
		
	}
	
	public synchronized T pop() {
		K key = _stk.pop();
		return _map.remove(key);
	}

	public boolean containsKey(K key) {
		return _map.containsKey(key);
	}
	
	public synchronized T push(K key, T val) {
		if (containsKey(key)) {
			remove(key);
		}
		_map.put(key, val);
		_stk.push(key);
		return val;
	}

	public void clear() {
		_stk.clear();
		_map.clear();
	}

	public boolean remove(K key) {
		_map.remove(key);
		return _stk.remove(key);
	}

	public void toTop(K key) {
		if (_map.containsKey(key)) {
			_stk.remove(key);
			_stk.push(key);
		}
	}
	
	public Iterator<K> iterator() {
		return _stk.iterator();
	}
	
	public int size() {
		return _stk.size();
	}
	
	public K get(int i) {
		return _stk.get(i);
	}
	
	public T get(K key) {
		return _map.get(key);
	}
	
	public T top() {
		return _map.get(_stk.peek());
	}

	public Iterator<T> valueIterator() {
		return new ValueIterator(_stk.iterator());
	}
	
	public Iterator<T> reverseValueIterator() {
		return new ValueIterator(_stk.descendingIterator());
	}

}
