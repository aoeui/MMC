package util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class RangeGenerator<T> implements Iterable<T> {
  final List<T> list;
  final int start;  // index of first element to be iterated
  final int end;  // index of last element to be iterated

  public RangeGenerator(List<T> list, int start, int end) {
    this.list = list; 
    this.start = start;
    this.end = end;
  }

  public Iterator<T> iterator() {
    class RangeIterator implements Iterator<T> {
      int next;
      RangeIterator() { next = start; }
      public boolean hasNext() { return next <= end; }
      public T next() {
        if (!hasNext()) { throw new NoSuchElementException(); }       
        return list.get(next++);
      }       
      public void remove() { throw new UnsupportedOperationException(); }
    }
    return new RangeIterator();
  }
}

