package util;

import java.util.Iterator;

public class UnmodifiableIterator<T> implements Iterator<T> {
  Iterator<T> iterator;

  public UnmodifiableIterator(Iterator<T> iterator) {
    this.iterator = iterator;  
  }
  
  public boolean hasNext() { return iterator.hasNext(); }
  public T next() { return iterator.next(); }  
  public void remove() { throw new UnsupportedOperationException(); }
}