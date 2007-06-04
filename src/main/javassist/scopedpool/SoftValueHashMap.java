/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.scopedpool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This Map will remove entries when the value in the map has been cleaned from
 * garbage collection
 * 
 * @version <tt>$Revision: 1.4 $</tt>
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class SoftValueHashMap extends AbstractMap implements Map {
    private static class SoftValueRef extends SoftReference {
        public Object key;

        private SoftValueRef(Object key, Object val, ReferenceQueue q) {
            super(val, q);
            this.key = key;
        }

        private static SoftValueRef create(Object key, Object val,
                ReferenceQueue q) {
            if (val == null)
                return null;
            else
                return new SoftValueRef(key, val, q);
        }

    }

    /**
     * Returns a set of the mappings contained in this hash table.
     */
    public Set entrySet() {
        processQueue();
        return hash.entrySet();
    }

    /* Hash table mapping WeakKeys to values */
    private Map hash;

    /* Reference queue for cleared WeakKeys */
    private ReferenceQueue queue = new ReferenceQueue();

    /*
     * Remove all invalidated entries from the map, that is, remove all entries
     * whose values have been discarded.
     */
    private void processQueue() {
        SoftValueRef ref;
        while ((ref = (SoftValueRef)queue.poll()) != null) {
            if (ref == (SoftValueRef)hash.get(ref.key)) {
                // only remove if it is the *exact* same WeakValueRef
                //
                hash.remove(ref.key);
            }
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
        hash = new HashMap(initialCapacity, loadFactor);
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
        hash = new HashMap(initialCapacity);
    }

    /**
     * Constructs a new, empty <code>WeakHashMap</code> with the default
     * initial capacity and the default load factor, which is <code>0.75</code>.
     */
    public SoftValueHashMap() {
        hash = new HashMap();
    }

    /**
     * Constructs a new <code>WeakHashMap</code> with the same mappings as the
     * specified <tt>Map</tt>. The <code>WeakHashMap</code> is created with
     * an initial capacity of twice the number of mappings in the specified map
     * or 11 (whichever is greater), and a default load factor, which is
     * <tt>0.75</tt>.
     * 
     * @param t     the map whose mappings are to be placed in this map.
     */
    public SoftValueHashMap(Map t) {
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
    public int size() {
        processQueue();
        return hash.size();
    }

    /**
     * Returns <code>true</code> if this map contains no key-value mappings.
     */
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
    public Object get(Object key) {
        processQueue();
        SoftReference ref = (SoftReference)hash.get(key);
        if (ref != null)
            return ref.get();
        return null;
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
    public Object put(Object key, Object value) {
        processQueue();
        Object rtn = hash.put(key, SoftValueRef.create(key, value, queue));
        if (rtn != null)
            rtn = ((SoftReference)rtn).get();
        return rtn;
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
    public Object remove(Object key) {
        processQueue();
        return hash.remove(key);
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        processQueue();
        hash.clear();
    }
}
