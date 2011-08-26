package util;

/** Pointer, primarily for use with closures. */
public class Ptr<T> {
  public T value;
  
  public Ptr() {}
  public Ptr(T val) { this.value = val; }
}
