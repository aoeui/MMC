package util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class IndexGenerator<T> implements Iterable<T> {
  final List<T> values;
  final Iterable<Integer> indexer;

  public IndexGenerator(List<T> values, Iterable<Integer> indexer) {
    this.values = values;
    this.indexer = indexer;
  }

  public Iterator<T> iterator() {
    class IndexIterator implements Iterator<T> {
      Iterator<Integer> indexIterator;
      IndexIterator() {
        indexIterator = indexer.iterator();
      }
      public boolean hasNext() { return indexIterator.hasNext(); }
      public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        return values.get(indexIterator.next());
      }
      public void remove() { throw new UnsupportedOperationException(); }
    }
    return new IndexIterator();
  }
}
