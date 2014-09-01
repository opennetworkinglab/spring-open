package net.onrc.onos.core.intent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import net.onrc.onos.core.topology.LinkData;

/**
 * Base class for Path representation, which implements the List interface.
 */
public class Path implements List<LinkData> {

    private final List<LinkData> links;

    /**
     * Default constructor to create an empty path.
     */
    public Path() {
        links = new LinkedList<LinkData>();
    }

    /**
     * Returns a string representation of the path.
     *
     * @return "[LinkData src->dst],..."
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<LinkData> i = this.iterator();
        while (i.hasNext()) {
            builder.append(i.next().toString());
            if (i.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    @Override
    public int size() {
        return links.size();
    }

    @Override
    public boolean isEmpty() {
        return links.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return links.contains(o);
    }

    @Override
    public Iterator<LinkData> iterator() {
        return links.iterator();
    }

    @Override
    public Object[] toArray() {
        return links.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return links.toArray(a);
    }

    @Override
    public boolean add(LinkData d) {
        return links.add(d);
    }

    @Override
    public boolean remove(Object o) {
        return links.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return links.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends LinkData> c) {
        return links.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends LinkData> c) {
        return links.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return links.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return links.retainAll(c);
    }

    @Override
    public void clear() {
        links.clear();
    }

    @Override
    public boolean equals(Object o) {
        return links.equals(o);
    }

    @Override
    public int hashCode() {
        return links.hashCode();
    }

    @Override
    public LinkData get(int index) {
        return links.get(index);
    }

    @Override
    public LinkData set(int index, LinkData element) {
        return links.set(index, element);
    }

    @Override
    public void add(int index, LinkData element) {
        links.add(index, element);
    }

    @Override
    public LinkData remove(int index) {
        return links.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return links.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return links.lastIndexOf(o);
    }

    @Override
    public ListIterator<LinkData> listIterator() {
        return links.listIterator();
    }

    @Override
    public ListIterator<LinkData> listIterator(int index) {
        return links.listIterator(index);
    }

    @Override
    public List<LinkData> subList(int fromIndex, int toIndex) {
        return links.subList(fromIndex, toIndex);
    }
}
