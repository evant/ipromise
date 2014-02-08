package me.tatarka.ipromise;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayPromiseBuffer<T> implements PromiseBuffer<T> {
    private int size;
    private List<T> buffer;

    public ArrayPromiseBuffer(int size) {
        this.size = size;
        if (size > 0) {
            buffer = new ArrayList<T>(size);
        } else {
            buffer = new ArrayList<T>();
        }
    }

    @Override
    public void add(T item) {
        if (size > 0 && buffer.size() + 1 > size) throw new IllegalStateException("PromiseBuffers has exceeded it's maximum size of " + size + " items");
        buffer.add(item);
    }

    @Override
    public Iterator<T> iterator() {
        return buffer.iterator();
    }
}
