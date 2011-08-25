package util;

import java.util.Iterator;

public abstract class Stack<T> implements Iterable<T> {
  public final static String DEFAULT_DELIM = ", ";

  public abstract boolean isEmpty();

  public Stack<T> push(T elt) {
    return new Element<T>(elt, this);
  }
  
  public abstract T head();
  public abstract Stack<T> tail();
  public abstract boolean contains(T elt);
  
  public abstract Stack<T> reverse();
  
  protected abstract Stack<T> reverseImpl(Stack<T> stack);
  
  @SuppressWarnings("unchecked")
  public static <T> Stack<T> emptyInstance() {
    return (Stack<T>)Empty.instance;
  }
  
  public Iterator<T> iterator() {
    return new StackIterator<T>(this);
  }
  
  public String toString() {
    return toString(DEFAULT_DELIM);        
  }
  
  public String toString(String delim) {
    return Joiner.join(this, delim);
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

  // Singleton
  public static class Empty<T> extends Stack<T> {
    public final static Empty<?> instance = new Empty<Object>();

    public boolean isEmpty() { return true; }
    public T head() { throw new UnsupportedOperationException(); }
    public Stack<T> tail() { throw new UnsupportedOperationException(); }
    public boolean contains(T elt) { return false; }
    
    public Stack<T> reverse() { return this; }
    protected Stack<T> reverseImpl(Stack<T> stack) { return stack; }
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

    public Stack<T> reverse() {
      return reverseImpl(Stack.<T>emptyInstance());
    }
    
    protected Stack<T> reverseImpl(Stack<T> stack) {
      return tail.reverseImpl(stack.push(head));
    }
  }
}
