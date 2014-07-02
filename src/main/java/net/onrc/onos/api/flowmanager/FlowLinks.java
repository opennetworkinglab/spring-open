package net.onrc.onos.api.flowmanager;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * A list of FlowLink objects.
 */
public class FlowLinks implements List<FlowLink> {
    protected final List<FlowLink> links = new LinkedList<FlowLink>();

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
    public Iterator<FlowLink> iterator() {
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
    public boolean add(FlowLink e) {
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
    public boolean addAll(Collection<? extends FlowLink> c) {
        return links.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends FlowLink> c) {
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
    public FlowLink get(int index) {
        return links.get(index);
    }

    @Override
    public FlowLink set(int index, FlowLink element) {
        return links.set(index, element);
    }

    @Override
    public void add(int index, FlowLink element) {
        links.add(index, element);
    }

    @Override
    public FlowLink remove(int index) {
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
    public ListIterator<FlowLink> listIterator() {
        return links.listIterator();
    }

    @Override
    public ListIterator<FlowLink> listIterator(int index) {
        return links.listIterator(index);
    }

    @Override
    public List<FlowLink> subList(int fromIndex, int toIndex) {
        return links.subList(fromIndex, toIndex);
    }
}
