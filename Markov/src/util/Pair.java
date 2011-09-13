package util;

/** Convenience class for lexicographically comparable pairs. */
public class Pair<T extends Comparable<? super T>> implements Comparable<Pair<T>> {
  public final T first;
  public final T second;
  
  public Pair(T first, T second) {
    this.first = first;
    this.second = second;
  }
  
  public String toString() {
    return "(" + first + ", " + second + ")";
  }
  
  public int hashCode() {
    int accu = 0;
    if (first != null) accu += first.hashCode();
    if (second != null) {
      accu *= 31;
      accu += second.hashCode();
    }
    return accu;
  }
  
  public boolean equals(Object o) {
    try {
      @SuppressWarnings("unchecked")
      Pair<T> other = (Pair<T>)o;
      return (first == other.first || (first != null && first.equals(other.first)))
         && (second == other.second || (second != null && second.equals(other.second)));
    } catch (Exception e) {
      return false;
    }
  }
  
  public int compareTo(Pair<T> pair) {
    int rv = first.compareTo(pair.first);
    if (rv == 0) {
      rv = second.compareTo(pair.second);
    }
    return rv;
  }
  
  public Stack<T> toStack() {
    return Stack.<T>emptyInstance().push(second).push(first);
  }
}
