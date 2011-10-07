package util;

import java.util.Comparator;
import java.util.Iterator;

public abstract class Stack<T> implements Iterable<T> {
  public final static String DEFAULT_DELIM = ", ";

  public abstract boolean isEmpty();

  public Stack<T> push(T elt) {
    return new Element<T>(elt, this);
  }
  
  public Stack<T> concat(Stack<T> stack) {
    Stack<T> rv = this;
    for (T elt : stack) {
      rv = rv.push(elt);
    }
    return rv;
  }
  
  public Stack<T> addUnique(Stack<T> stack) {
    Stack<T> rv = this;
    for (T elt : stack) {
      if (rv.contains(elt)) continue;
      rv = rv.push(elt);
    }
    return rv;
  }
  
  public abstract T head();
  public abstract Stack<T> tail();
  public abstract boolean contains(T elt);
  public abstract int size();
  // returns a stack where the first occurrence of the given element is removed using equals
  public abstract Stack<T> remove(T elt);
  
  public abstract Stack<T> reverse();
  
  protected abstract Stack<T> reverseImpl(Stack<T> stack);
  
  @SuppressWarnings("unchecked")
  public static <T> Stack<T> emptyInstance() {
    return (Stack<T>)Empty.instance;
  }
  
  // Warning, uses equals to test for containment.
  public boolean contains(Stack<T> stack) {
    if (stack.isEmpty()) return true;
    if (isEmpty()) return false;  // stack is not empty
    
    return head().equals(stack.head()) ? tail().contains(stack.tail()) : false;
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
    public int size() { return 0; }
    public Stack<T> remove(T elt) { throw new java.util.NoSuchElementException(); }
    
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
    public int size() { return 1+tail.size(); }
    
    public Stack<T> remove(T elt) {
      if (head.equals(elt)) return tail;
      
      return tail.remove(elt).push(head);
    }

    public Stack<T> reverse() {
      return reverseImpl(Stack.<T>emptyInstance());
    }
    
    protected Stack<T> reverseImpl(Stack<T> stack) {
      return tail.reverseImpl(stack.push(head));
    }
  }
  
  public final static Comparator<Stack<String>> STRING_COMP = Stack.<String>lexicalComparatorInstance();

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> Comparator<Stack<T>> lexicalComparatorInstance() {
    return LexicalStackComparator.INSTANCE;
  }
  
  public static <T> Stack<T> makeStack(Iterator<T> it) {
    if (!it.hasNext()) return Stack.<T>emptyInstance();
    
    T next = it.next();
    return Stack.<T>makeStack(it).push(next);
  }
  
  public static <T> Stack<T> makeStack(Iterable<T> iter) {
    return Stack.<T>makeStack(iter.iterator());
  }
  
  // Too bad Java doesn't support generic arrays or varargs
  public static Stack<String> makeName(String ...strings) {
    Stack<String> rv = Stack.<String>emptyInstance();
    for (int i = strings.length-1; i >= 0; i--) {
      rv = rv.push(strings[i]);
    }
    return rv;
  }
  
  public static class LexicalStackComparator<T extends Comparable<? super T>> implements Comparator<Stack<T>> {
    @SuppressWarnings("rawtypes")
    private static final LexicalStackComparator INSTANCE = new LexicalStackComparator();
    private LexicalStackComparator() {}
    public int compare(Stack<T> arg0, Stack<T> arg1) {
      if (arg0.isEmpty()) {
        return arg1.isEmpty() ? 0 : -1;
      } else {
        if (arg1.isEmpty()) return 1;
        int rv = arg0.head().compareTo(arg1.head());
        return rv == 0 ? compare(arg0.tail(), arg1.tail()) : rv;
      }
    }
  }
}
