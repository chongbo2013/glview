/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.glview.hwui.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * LinkedList is an implementation of {@link List}, backed by a doubly-linked list.
 * All optional operations including adding, removing, and replacing elements are supported.
 *
 * <p>All elements are permitted, including null.
 *
 * <p>This class is primarily useful if you need queue-like behavior. It may also be useful
 * as a list if you expect your lists to contain zero or one element, but still require the
 * ability to scale to slightly larger numbers of elements. In general, though, you should
 * probably use {@link ArrayList} if you don't need the queue-like behavior.
 *
 * @since 1.2
 */
public class PooledLinkedList<E> extends AbstractSequentialList<E> implements
        List<E>, Cloneable, Serializable {

    private static final long serialVersionUID = 876323262645176354L;

    transient int size = 0;

    transient Link<E> voidLink;

    private static final class Link<ET> {
        ET data;

        Link<ET> previous, next;

        Link(ET o, Link<ET> p, Link<ET> n) {
            data = o;
            previous = p;
            next = n;
        }
    }
    
    private final static int MAX_LINK_POOL_SIZE = 200;
    private final ArrayList<Link<E>> mLinkPool = new ArrayList<PooledLinkedList.Link<E>>(MAX_LINK_POOL_SIZE);
    private Link<E> obtainLink(E o, Link<E> p, Link<E> n) {
    	if (mLinkPool.size() > 0) {
    		Link<E> link = mLinkPool.remove(mLinkPool.size() - 1);
    		link.data = o;
    		link.previous = p;
    		link.next = n;
    		return link;
    	}
    	return new Link<E>(o, p, n);
    }
    
    private boolean recycleLink(Link<E> link) {
    	if (mLinkPool.size() < MAX_LINK_POOL_SIZE) {
    		link.data = null;
        	link.next = null;
        	link.previous = null;
    		mLinkPool.add(link);
    		return true;
    	}
    	return false;
    }

    /**
     * Constructs a new empty instance of {@code LinkedList}.
     */
    public PooledLinkedList() {
        voidLink = new Link<E>(null, null, null);
        voidLink.previous = voidLink;
        voidLink.next = voidLink;
    }

    /**
     * Constructs a new instance of {@code LinkedList} that holds all of the
     * elements contained in the specified {@code collection}. The order of the
     * elements in this new {@code LinkedList} will be determined by the
     * iteration order of {@code collection}.
     *
     * @param collection
     *            the collection of elements to add.
     */
    public PooledLinkedList(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    /**
     * Inserts the specified object into this {@code LinkedList} at the
     * specified location. The object is inserted before any previous element at
     * the specified location. If the location is equal to the size of this
     * {@code LinkedList}, the object is added at the end.
     *
     * @param location
     *            the index at which to insert.
     * @param object
     *            the object to add.
     * @throws IndexOutOfBoundsException
     *             if {@code location < 0 || location > size()}
     */
    @Override
    public void add(int location, E object) {
        if (location >= 0 && location <= size) {
            Link<E> link = voidLink;
            if (location < (size / 2)) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i = size; i > location; i--) {
                    link = link.previous;
                }
            }
            Link<E> previous = link.previous;
            Link<E> newLink = obtainLink(object, previous, link);//new Link<E>(object, previous, link);
            previous.next = newLink;
            link.previous = newLink;
            size++;
            modCount++;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Adds the specified object at the end of this {@code LinkedList}.
     *
     * @param object
     *            the object to add.
     * @return always true
     */
    @Override
    public boolean add(E object) {
        return addLastImpl(object);
    }

    private boolean addLastImpl(E object) {
        Link<E> oldLast = voidLink.previous;
        Link<E> newLink = obtainLink(object, oldLast, voidLink);//new Link<E>(object, oldLast, voidLink);
        voidLink.previous = newLink;
        oldLast.next = newLink;
        size++;
        modCount++;
        return true;
    }

    /**
     * Inserts the objects in the specified collection at the specified location
     * in this {@code LinkedList}. The objects are added in the order they are
     * returned from the collection's iterator.
     *
     * @param location
     *            the index at which to insert.
     * @param collection
     *            the collection of objects
     * @return {@code true} if this {@code LinkedList} is modified,
     *         {@code false} otherwise.
     * @throws ClassCastException
     *             if the class of an object is inappropriate for this list.
     * @throws IllegalArgumentException
     *             if an object cannot be added to this list.
     * @throws IndexOutOfBoundsException
     *             if {@code location < 0 || location > size()}
     */
    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        if (location < 0 || location > size) {
            throw new IndexOutOfBoundsException();
        }
        int adding = collection.size();
        if (adding == 0) {
            return false;
        }
        Collection<? extends E> elements = (collection == this) ?
                new ArrayList<E>(collection) : collection;

        Link<E> previous = voidLink;
        if (location < (size / 2)) {
            for (int i = 0; i < location; i++) {
                previous = previous.next;
            }
        } else {
            for (int i = size; i >= location; i--) {
                previous = previous.previous;
            }
        }
        Link<E> next = previous.next;
        for (E e : elements) {
            Link<E> newLink = obtainLink(e, previous,null);//new Link<E>(e, previous, null);
            previous.next = newLink;
            previous = newLink;
        }
        previous.next = next;
        next.previous = previous;
        size += adding;
        modCount++;
        return true;
    }

    /**
     * Adds the objects in the specified Collection to this {@code LinkedList}.
     *
     * @param collection
     *            the collection of objects.
     * @return {@code true} if this {@code LinkedList} is modified,
     *         {@code false} otherwise.
     */
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        int adding = collection.size();
        if (adding == 0) {
            return false;
        }
        Collection<? extends E> elements = (collection == this) ?
                new ArrayList<E>(collection) : collection;

        Link<E> previous = voidLink.previous;
        for (E e : elements) {
            Link<E> newLink = obtainLink(e, previous, null);//new Link<E>(e, previous, null);
            previous.next = newLink;
            previous = newLink;
        }
        previous.next = voidLink;
        voidLink.previous = previous;
        size += adding;
        modCount++;
        return true;
    }

    /**
     * Adds the specified object at the beginning of this {@code LinkedList}.
     *
     * @param object
     *            the object to add.
     */
    public void addFirst(E object) {
        addFirstImpl(object);
    }

    private boolean addFirstImpl(E object) {
        Link<E> oldFirst = voidLink.next;
        Link<E> newLink = obtainLink(object, voidLink, oldFirst);//new Link<E>(object, voidLink, oldFirst);
        voidLink.next = newLink;
        oldFirst.previous = newLink;
        size++;
        modCount++;
        return true;
    }

    /**
     * Adds the specified object at the end of this {@code LinkedList}.
     *
     * @param object
     *            the object to add.
     */
    public void addLast(E object) {
        addLastImpl(object);
    }

    /**
     * Removes all elements from this {@code LinkedList}, leaving it empty.
     *
     * @see List#isEmpty
     * @see #size
     */
    @Override
    public void clear() {
        if (size > 0) {
        	Link<E> link = voidLink;
        	for (int i = 0; i < size; i ++) {
        		link = link.next;
        		recycleLink(link);
        	}
            size = 0;
            voidLink.next = voidLink;
            voidLink.previous = voidLink;
            modCount++;
        }
    }

    /**
     * Returns a new {@code LinkedList} with the same elements and size as this
     * {@code LinkedList}.
     *
     * @return a shallow copy of this {@code LinkedList}.
     * @see java.lang.Cloneable
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
        	PooledLinkedList<E> l = (PooledLinkedList<E>) super.clone();
            l.size = 0;
            l.voidLink = new Link<E>(null, null, null);
            l.voidLink.previous = l.voidLink;
            l.voidLink.next = l.voidLink;
            l.addAll(this);
            return l;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Searches this {@code LinkedList} for the specified object.
     *
     * @param object
     *            the object to search for.
     * @return {@code true} if {@code object} is an element of this
     *         {@code LinkedList}, {@code false} otherwise
     */
    @Override
    public boolean contains(Object object) {
        Link<E> link = voidLink.next;
        if (object != null) {
            while (link != voidLink) {
                if (object.equals(link.data)) {
                    return true;
                }
                link = link.next;
            }
        } else {
            while (link != voidLink) {
                if (link.data == null) {
                    return true;
                }
                link = link.next;
            }
        }
        return false;
    }

    @Override
    public E get(int location) {
        if (location >= 0 && location < size) {
            Link<E> link = voidLink;
            if (location < (size / 2)) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i = size; i > location; i--) {
                    link = link.previous;
                }
            }
            return link.data;
        }
        throw new IndexOutOfBoundsException();
    }

    /**
     * Returns the first element in this {@code LinkedList}.
     *
     * @return the first element.
     * @throws NoSuchElementException
     *             if this {@code LinkedList} is empty.
     */
    public E getFirst() {
        return getFirstImpl();
    }

    private E getFirstImpl() {
        Link<E> first = voidLink.next;
        if (first != voidLink) {
            return first.data;
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the last element in this {@code LinkedList}.
     *
     * @return the last element
     * @throws NoSuchElementException
     *             if this {@code LinkedList} is empty
     */
    public E getLast() {
        Link<E> last = voidLink.previous;
        if (last != voidLink) {
            return last.data;
        }
        throw new NoSuchElementException();
    }

    @Override
    public int indexOf(Object object) {
        int pos = 0;
        Link<E> link = voidLink.next;
        if (object != null) {
            while (link != voidLink) {
                if (object.equals(link.data)) {
                    return pos;
                }
                link = link.next;
                pos++;
            }
        } else {
            while (link != voidLink) {
                if (link.data == null) {
                    return pos;
                }
                link = link.next;
                pos++;
            }
        }
        return -1;
    }

    /**
     * Searches this {@code LinkedList} for the specified object and returns the
     * index of the last occurrence.
     *
     * @param object
     *            the object to search for
     * @return the index of the last occurrence of the object, or -1 if it was
     *         not found.
     */
    @Override
    public int lastIndexOf(Object object) {
        int pos = size;
        Link<E> link = voidLink.previous;
        if (object != null) {
            while (link != voidLink) {
                pos--;
                if (object.equals(link.data)) {
                    return pos;
                }
                link = link.previous;
            }
        } else {
            while (link != voidLink) {
                pos--;
                if (link.data == null) {
                    return pos;
                }
                link = link.previous;
            }
        }
        return -1;
    }

    /**
     * Returns a ListIterator on the elements of this {@code LinkedList}. The
     * elements are iterated in the same order that they occur in the
     * {@code LinkedList}. The iteration starts at the specified location.
     *
     * @param location
     *            the index at which to start the iteration
     * @return a ListIterator on the elements of this {@code LinkedList}
     * @throws IndexOutOfBoundsException
     *             if {@code location < 0 || location > size()}
     * @see ListIterator
     */
    @Override
    public ListIterator<E> listIterator(int location) {
    	throw new UnsupportedOperationException("This array does not support iterator!");
    }

    /**
     * Removes the object at the specified location from this {@code LinkedList}.
     *
     * @param location
     *            the index of the object to remove
     * @return the removed object
     * @throws IndexOutOfBoundsException
     *             if {@code location < 0 || location >= size()}
     */
    @Override
    public E remove(int location) {
        if (location >= 0 && location < size) {
        	Link<E> link = voidLink;
        	try {
	            if (location < (size / 2)) {
	                for (int i = 0; i <= location; i++) {
	                    link = link.next;
	                }
	            } else {
	                for (int i = size; i > location; i--) {
	                    link = link.previous;
	                }
	            }
	            Link<E> previous = link.previous;
	            Link<E> next = link.next;
	            previous.next = next;
	            next.previous = previous;
	            size--;
	            modCount++;
	            return link.data;
        	} finally {
        		recycleLink(link);
        	}
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean remove(Object object) {
         Link<E> link = voidLink.next;
         if (object != null) {
             while (link != voidLink) {
                 if (object.equals(link.data)) {
					Link<E> previous = link.previous;
					Link<E> next = link.next;
					previous.next = next;
					next.previous = previous;
					recycleLink(link);
					return true;
                 }
                 link = link.next;
             }
         } else {
             while (link != voidLink) {
                 if (link.data == null) {
					Link<E> previous = link.previous;
					Link<E> next = link.next;
					previous.next = next;
					next.previous = previous;
					recycleLink(link);
					return true;
                 }
                 link = link.next;
             }
         }
    	return false;
    }

    /**
     * Removes the first object from this {@code LinkedList}.
     *
     * @return the removed object.
     * @throws NoSuchElementException
     *             if this {@code LinkedList} is empty.
     */
    public E removeFirst() {
        return removeFirstImpl();
    }

    private E removeFirstImpl() {
        Link<E> first = voidLink.next;
        if (first != voidLink) {
        	try {
	            Link<E> next = first.next;
	            voidLink.next = next;
	            next.previous = voidLink;
	            size--;
	            modCount++;
	            return first.data;
        	} finally {
        		recycleLink(first);
        	}
        }
        throw new NoSuchElementException();
    }

    /**
     * Removes the last object from this {@code LinkedList}.
     *
     * @return the removed object.
     * @throws NoSuchElementException
     *             if this {@code LinkedList} is empty.
     */
    public E removeLast() {
        return removeLastImpl();
    }

    private E removeLastImpl() {
        Link<E> last = voidLink.previous;
        if (last != voidLink) {
        	try {
	            Link<E> previous = last.previous;
	            voidLink.previous = previous;
	            previous.next = voidLink;
	            size--;
	            modCount++;
	            return last.data;
        	} finally {
        		recycleLink(last);
        	}
        }
        throw new NoSuchElementException();
    }

    /**
     * Replaces the element at the specified location in this {@code LinkedList}
     * with the specified object.
     *
     * @param location
     *            the index at which to put the specified object.
     * @param object
     *            the object to add.
     * @return the previous element at the index.
     * @throws ClassCastException
     *             if the class of an object is inappropriate for this list.
     * @throws IllegalArgumentException
     *             if an object cannot be added to this list.
     * @throws IndexOutOfBoundsException
     *             if {@code location < 0 || location >= size()}
     */
    @Override
    public E set(int location, E object) {
        if (location >= 0 && location < size) {
            Link<E> link = voidLink;
            if (location < (size / 2)) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i = size; i > location; i--) {
                    link = link.previous;
                }
            }
            E result = link.data;
            link.data = object;
            return result;
        }
        throw new IndexOutOfBoundsException();
    }

    /**
     * Returns the number of elements in this {@code LinkedList}.
     *
     * @return the number of elements in this {@code LinkedList}.
     */
    @Override
    public int size() {
        return size;
    }

    public boolean offer(E o) {
        return addLastImpl(o);
    }

    public E poll() {
        return size == 0 ? null : removeFirst();
    }

    public E remove() {
        return removeFirstImpl();
    }

    public E peek() {
        return peekFirstImpl();
    }

    private E peekFirstImpl() {
        Link<E> first = voidLink.next;
        return first == voidLink ? null : first.data;
    }

    public E element() {
        return getFirstImpl();
    }

    /**
     * Returns a new array containing all elements contained in this
     * {@code LinkedList}.
     *
     * @return an array of the elements from this {@code LinkedList}.
     */
    @Override
    public Object[] toArray() {
        int index = 0;
        Object[] contents = new Object[size];
        Link<E> link = voidLink.next;
        while (link != voidLink) {
            contents[index++] = link.data;
            link = link.next;
        }
        return contents;
    }

    /**
     * Returns an array containing all elements contained in this
     * {@code LinkedList}. If the specified array is large enough to hold the
     * elements, the specified array is used, otherwise an array of the same
     * type is created. If the specified array is used and is larger than this
     * {@code LinkedList}, the array element following the collection elements
     * is set to null.
     *
     * @param contents
     *            the array.
     * @return an array of the elements from this {@code LinkedList}.
     * @throws ArrayStoreException
     *             if the type of an element in this {@code LinkedList} cannot
     *             be stored in the type of the specified array.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] contents) {
        int index = 0;
        if (size > contents.length) {
            Class<?> ct = contents.getClass().getComponentType();
            contents = (T[]) Array.newInstance(ct, size);
        }
        Link<E> link = voidLink.next;
        while (link != voidLink) {
            contents[index++] = (T) link.data;
            link = link.next;
        }
        if (index < contents.length) {
            contents[index] = null;
        }
        return contents;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(size);
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            stream.writeObject(it.next());
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        stream.defaultReadObject();
        size = stream.readInt();
        voidLink = new Link<E>(null, null, null);
        Link<E> link = voidLink;
        for (int i = size; --i >= 0;) {
            Link<E> nextLink = obtainLink((E) stream.readObject(), link, null);//new Link<E>((E) stream.readObject(), link, null);
            link.next = nextLink;
            link = nextLink;
        }
        link.next = voidLink;
        voidLink.previous = link;
    }
}
