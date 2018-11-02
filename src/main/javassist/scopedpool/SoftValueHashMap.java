/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.scopedpool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This Map will remove entries when the value in the map has been cleaned from
 * garbage collection
 * 
 * @version <code>$Revision: 1.4 $</code>
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class SoftValueHashMap<K,V> implements Map<K,V> {
    private static class SoftValueRef<K,V> extends SoftReference<V> {
        public K key;

        private SoftValueRef(K key, V val, ReferenceQueue<V> q) {
            super(val, q);
            this.key = key;
        }

        private static <K,V> SoftValueRef<K,V> create(
                            K key, V val, ReferenceQueue<V> q) {
            if (val == null)
                return null;
            else
                return new SoftValueRef<K,V>(key, val, q);
        }

    }

    /**
     * Returns a set of the mappings contained in this hash table.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        processQueue();
        Set<Entry<K,V>> ret = new HashSet<Entry<K,V>>();
        for (Entry<K,SoftValueRef<K,V>> e:hash.entrySet()) 
                ret.add(new SimpleImmutableEntry<K,V> (
                        e.getKey(), e.getValue().get()));
        return ret;        
    }

    /* Hash table mapping WeakKeys to values */
    private Map<K,SoftValueRef<K,V>> hash;

    /* Reference queue for cleared WeakKeys */
    private ReferenceQueue<V> queue = new ReferenceQueue<V>();

    /*
     * Remove all invalidated entries from the map, that is, remove all entries
     * whose values have been discarded.
     */
    private void processQueue() {
        Object ref;
        if (!hash.isEmpty())
        while ((ref = queue.poll()) != null)
            if (ref instanceof SoftValueRef)  {
                @SuppressWarnings("rawtypes")
                SoftValueRef que =(SoftValueRef) ref;
                if (ref == hash.get(que.key))
                // only remove if it is the *exact* same SoftValueRef
                    hash.remove(que.key);
            }
    }

    /* -- Constructors -- */

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the given initial
     * capacity and the given load factor.
     * 
     * @param initialCapacity
     *            The initial capacity of the <code>WeakHashMap</code>
     * 
     * @param loadFactor
     *            The load factor of the <code>WeakHashMap</code>
     * 
     * @throws IllegalArgumentException
     *             If the initial capacity is less than zero, or if the load
     *             factor is nonpositive
     */
    public SoftValueHashMap(int initialCapacity, float loadFactor) {
        hash = new ConcurrentHashMap<K,SoftValueRef<K,V>>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the given initial
     * capacity and the default load factor, which is <code>0.75</code>.
     * 
     * @param initialCapacity
     *            The initial capacity of the <code>WeakHashMap</code>
     * 
     * @throws IllegalArgumentException
     *             If the initial capacity is less than zero
     */
    public SoftValueHashMap(int initialCapacity) {
        hash = new ConcurrentHashMap<K,SoftValueRef<K,V>>(initialCapacity);
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the default
     * initial capacity and the default load factor, which is <code>0.75</code>.
     */
    public SoftValueHashMap() {
        hash = new ConcurrentHashMap<K,SoftValueRef<K,V>>();
    }

    /**
     * Constructs a new <code>WeakHashMap</code> with the same mappings as the
     * specified <code>Map</code>. The <code>WeakHashMap</code> is created with
     * an initial capacity of twice the number of mappings in the specified map
     * or 11 (whichever is greater), and a default load factor, which is
     * <code>0.75</code>.
     * 
     * @param t     the map whose mappings are to be placed in this map.
     */
    public SoftValueHashMap(Map<K,V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }

    /* -- Simple queries -- */

    /**
     * Returns the number of key-value mappings in this map. <strong>Note:</strong>
     * <em>In contrast with most implementations of the
     * <code>Map</code> interface, the time required by this operation is
     * linear in the size of the map.</em>
     */
    @Override
    public int size() {
        processQueue();
        return hash.size();
    }

    /**
     * Returns <code>true</code> if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty() {
        processQueue();
        return hash.isEmpty();
    }

    /**
     * Returns <code>true</code> if this map contains a mapping for the
     * specified key.
     * 
     * @param key
     *            The key whose presence in this map is to be tested.
     */
    @Override
    public boolean containsKey(Object key) {
        processQueue();
        return hash.containsKey(key);
    }

    /* -- Lookup and modification operations -- */

    /**
     * Returns the value to which this map maps the specified <code>key</code>.
     * If this map does not contain a value for this key, then return
     * <code>null</code>.
     * 
     * @param key
     *            The key whose associated value, if any, is to be returned.
     */
    @Override
    public V get(Object key) {
        processQueue();
        return valueOrNull(hash.get(key));
    }

    /**
     * Updates this map so that the given <code>key</code> maps to the given
     * <code>value</code>. If the map previously contained a mapping for
     * <code>key</code> then that mapping is replaced and the previous value
     * is returned.
     * 
     * @param key
     *            The key that is to be mapped to the given <code>value</code>
     * @param value
     *            The value to which the given <code>key</code> is to be
     *            mapped
     * 
     * @return The previous value to which this key was mapped, or
     *         <code>null</code> if if there was no mapping for the key
     */
    @Override
    public V put(K key, V value) {
        processQueue();
        return valueOrNull(hash.put(key, SoftValueRef.create(key, value, queue)));
    }

    /**
     * Removes the mapping for the given <code>key</code> from this map, if
     * present.
     * 
     * @param key
     *            The key whose mapping is to be removed.
     * 
     * @return The value to which this key was mapped, or <code>null</code> if
     *         there was no mapping for the key.
     */
    @Override
    public V remove(Object key) {
        processQueue();
        return valueOrNull(hash.remove(key));
    }

    /**
     * Removes all mappings from this map.
     */
    @Override
    public void clear() {
        processQueue();
        hash.clear();
    }

    /*
     * Check whether the supplied value exists.
     * @param Object the value to compare.
     * @return true if it was found or null. 
     */
    @Override
    public boolean containsValue(Object arg0) {
        processQueue();
        if (null == arg0)
            return false;
        
        for (SoftValueRef<K,V> e:hash.values())
            if (null != e && arg0.equals(e.get()))
                return true;
        return false;
    }

    /* {@inheritDoc} */
    @Override
    public Set<K> keySet() {
        processQueue();
        return hash.keySet();
    }
    
    /* {@inheritDoc} */
    @Override
    public void putAll(Map<? extends K,? extends V> arg0) {
        processQueue();
        for (K key:arg0.keySet())
            put(key, arg0.get(key));
    }

    /* {@inheritDoc} */
    @Override
    public Collection<V> values() {
        processQueue();
        List<V> ret = new ArrayList<V>();
        for (SoftValueRef<K,V> e:hash.values())
            ret.add(e.get());
        return ret;
    }
    
    private V valueOrNull(SoftValueRef<K,V> rtn) { 
        if (null == rtn)
            return null;
        return rtn.get();
    }
}
