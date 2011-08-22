package util;

import java.util.Iterator;

public abstract class Stack<T> implements Iterable<T> {
  public abstract boolean isEmpty();

  public Stack<T> push(T elt) {
    return new Element<T>(elt, this);
  }
  
  public abstract T head();
  public abstract Stack<T> tail();
  public abstract boolean contains(T elt);
  
  @SuppressWarnings("unchecked")
  public static <T> Stack<T> emptyInstance() {
    return (Stack<T>)Empty.instance;
  }
  
  public Iterator<T> iterator() {
    return new StackIterator<T>(this);
  }
  
  private static class StackIterator<T> implements Iterator<T> {
    Stack<T> stack;
    
    public StackIterator(Stack<T> stack) {
      this.stack = stack;
    }
    public boolean hasNext() {
      return !stack.isEmpty();
    }
    
    public T next() {
      T rv = stack.head();
      stack = stack.tail();
      return rv;
    }
    
    public void remove() { throw new UnsupportedOperationException(); }
  }

  // This should be a singleton with suppress warnings somehow
  public static class Empty<T> extends Stack<T> {
    @SuppressWarnings("rawtypes")
    public final static Empty instance = new Empty();

    public boolean isEmpty() { return true; }
    public T head() { throw new UnsupportedOperationException(); }
    public Stack<T> tail() { throw new UnsupportedOperationException(); }
    public boolean contains(T elt) { return false; }
  }
  
  public static class Element<T> extends Stack<T> {
    public final T head;
    public final Stack<T> tail;
    
    public Element(T head, Stack<T> tail) {
      this.head = head;
      this.tail = tail;
    }
    
    public T head() { return head; }
    public Stack<T> tail() { return tail; }
    public boolean isEmpty() { return false; }
    public boolean contains(T elt) { return head.equals(elt) || tail.contains(elt); }
  }
}
