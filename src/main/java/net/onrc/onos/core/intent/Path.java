package net.onrc.onos.core.intent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import net.onrc.onos.core.topology.LinkEvent;

/**
 * Base class for Path representation.
 *
 * @author Toshio Koide (t-koide@onlab.us)
 */
public class Path implements List<LinkEvent> {

    private List<LinkEvent> links;

    /**
     * Default constructor to create an empty path.
     */
    public Path() {
        links = new LinkedList<LinkEvent>();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<LinkEvent> i = this.iterator();
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
    public Iterator<LinkEvent> iterator() {
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
    public boolean add(LinkEvent e) {
        return links.add(e);
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
    public boolean addAll(Collection<? extends LinkEvent> c) {
        return links.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends LinkEvent> c) {
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
    public LinkEvent get(int index) {
        return links.get(index);
    }

    @Override
    public LinkEvent set(int index, LinkEvent element) {
        return links.set(index, element);
    }

    @Override
    public void add(int index, LinkEvent element) {
        links.add(index, element);
    }

    @Override
    public LinkEvent remove(int index) {
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
    public ListIterator<LinkEvent> listIterator() {
        return links.listIterator();
    }

    @Override
    public ListIterator<LinkEvent> listIterator(int index) {
        return links.listIterator(index);
    }

    @Override
    public List<LinkEvent> subList(int fromIndex, int toIndex) {
        return links.subList(fromIndex, toIndex);
    }
}
