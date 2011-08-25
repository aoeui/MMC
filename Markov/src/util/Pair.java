package util;

/** Convenience class for lexicographically comparable pairs. */
public class Pair<T extends Comparable<? super T>> implements Comparable<Pair<T>> {
  public final T first;
  public final T second;
  
  public Pair(T first, T second) {
    this.first = first;
    this.second = second;
  }
  
  public int compareTo(Pair<T> pair) {
    int rv = first.compareTo(pair.first);
    if (rv == 0) {
      rv = second.compareTo(pair.second);
    }
    return rv;
  }
}
