package util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class FilterIterator<T> implements Iterator<T> {
  private Iterator<T> iterator;
  private boolean supportsRemove;
  
  private boolean hasNext;
  private T next;
  
  public FilterIterator(Iterator<T> iterator, boolean supportsRemove) {
   this.iterator = iterator;
   this.supportsRemove = supportsRemove;
   queueNext();
  }
  
  public FilterIterator(Iterator<T> iterator) {
    this(iterator, false);
  }
  
  // hasNext and next are considered invalid
  private void queueNext() {
    boolean passed = false;
    while (iterator.hasNext() && !passed) {
      next = iterator.next();
      passed = allowElement(next);
    }
    hasNext = passed;
  }

  public boolean hasNext() { return hasNext; }
  
  public T next() {
    if (!hasNext) throw new NoSuchElementException();
    T rv = next;
    queueNext();
    return rv;
  }

  // Returns true if an element is allowed, false otherwise
  public abstract boolean allowElement(T elt);
  
  public void remove() {
    if (supportsRemove) {
      iterator.remove();
      queueNext();
    } else throw new UnsupportedOperationException();
  }
}