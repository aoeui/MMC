package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import util.Joiner;

public class Partition<T> implements Iterable<Partition<T>.Block> {
  private static class CategoryComparator implements Comparator<Integer> {
    int[] categories;
    public CategoryComparator(int ... categories) {
      this.categories = new int[categories.length];
      System.arraycopy(categories, 0, this.categories, 0, categories.length);
    }
    
    public int compare(Integer v1, Integer v2) {
      return categories[v1] - categories[v2];
    }
  }
  
  private static class NaturalComparator<S extends Comparable<? super S>> implements Comparator<S> {
    public NaturalComparator() {}
    public int compare(S s1, S s2) {
      return s1.compareTo(s2);
    }
  }

  public final Comparator<? super T> comparator;
  private ArrayList<T> elements;
  private ArrayList<Block> blocks;

  private Partition(Comparator<? super T> comparator, ArrayList<T> elements) {
    this.comparator = comparator;
    this.elements = elements;
    this.blocks = new ArrayList<Block>();
    
    if (elements.size() == 0) return;

    int start = 0;
    T prototype = elements.get(0);
    for (int i = 1; i < elements.size(); i++) {
      if (comparator.compare(prototype, elements.get(i)) != 0) {
        blocks.add(new Block(start, i-1));
        start = i;
        prototype = elements.get(i);
      }
    }
    blocks.add(new Block(start, elements.size()-1));
  }

  public int getNumElts() { return elements.size(); }
  public T getElt(int idx) { return elements.get(idx); }

  public int getNumBlocks() { return blocks.size(); }
  public Block getBlock(int idx) { return blocks.get(idx); }
  
  public Iterator<Partition<T>.Block> iterator() {
    return new util.UnmodifiableIterator<Partition<T>.Block>(blocks.iterator());
  }

  public Partition<T> removeBlock(int idx) {
    if (idx < 0 || idx >= getNumBlocks()) {
      throw new IndexOutOfBoundsException();
    }
    Block target = blocks.get(idx);
    ArrayList<T> newElts = new ArrayList<T>();
    for (int i = 0; i < elements.size(); i++) {
      if (i >= target.start && i <= target.end) continue;
      newElts.add(elements.get(i));
    }
    return new Partition<T>(comparator, newElts);
  }

  public class Block implements Iterable<T> {
    public final int start;  // index of first element
    public final int end;  // index of last element
    
    private Block(int start, int end) {
      this.start = start;
      this.end = end;
    }
    
    public int size() { return 1 + end - start; }
    public T get(int i) { return elements.get(i+start); }
    
    public Iterator<T> iterator() {
      return new RangeIterator();
    }
    private class RangeIterator implements Iterator<T> {
      int next = start;

      public boolean hasNext() { return next <= end; }
      public T next() { return elements.get(next++); }
      public void remove() { throw new UnsupportedOperationException(); }
    }
    public String toString() {
      return "{" + Joiner.join(this) + "}";
    }
  }
  
  public String toString() {
    return Joiner.join(this);
  }
  
  public static <T extends Comparable<? super T>> Builder<T> naturalBuilder() {
    return new Builder<T>(new NaturalComparator<T>());
  }
  
  public static Partition<Integer> createFromCategories(int ... categories) {
    Builder<Integer> builder = new Builder<Integer>(new CategoryComparator(categories));
    for (int i = 0; i < categories.length; i++) {
      builder.add(i);
    }
    return builder.build();
  }
  
  public static class Builder<T> {
    final Comparator<? super T> comparator;
    ArrayList<T> elements = new ArrayList<T>();
    boolean isBuilt = false;
    
    public Builder(Comparator<? super T> comparator) {
      this.comparator = comparator;
    }
    
    public void add(T elt) {
      if (isBuilt) throw new RuntimeException();
      elements.add(elt);
    }
    
    public Partition<T> build() {
      if (isBuilt) throw new RuntimeException();
      isBuilt = true;
      
      Collections.sort(elements, comparator);
      return new Partition<T>(comparator, elements);
    }
    
    public int size() { return elements.size(); }
  }
}
