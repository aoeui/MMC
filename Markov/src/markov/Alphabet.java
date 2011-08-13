package markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import util.LexicalCompare;

public class Alphabet implements Iterable<String>, Comparable<Alphabet> {
  public final String machineName;  // needed for generating fully qualified labels
  public final String name;  // "machine.name" should uniquely identify an alphabet

  public final String varName;

  private final String prefix;  // "<machine>.<name>." convenience attribute
  private final ArrayList<String> characters;  // this is ordered, unique
  
  private Alphabet(String machineName, String name, SortedSet<? extends String> chars) {
	  this.machineName = machineName;
	  this.name = name;
	  this.prefix = machineName + "." + name + ".";
	  this.varName = machineName + "." + name;
	  this.characters = new ArrayList<String>(chars);
  }
  
  public int compareTo(Alphabet b) {
    int rv = machineName.compareTo(b.machineName);
    if (rv == 0) rv = name.compareTo(b.name);
    // Alphabets should be equal if machine.name is equal
    assert(rv == 0 ? compareAlphabets(characters, b.characters) == 0 : true);
    return rv;
  }
  
  public static int compareAlphabets(ArrayList<String> a1, ArrayList<String> a2) {
    for (int i = 0; i < Math.min(a1.size(), a2.size()); i++) {
      int comp = a1.get(i).compareTo(a2.get(i));
      if (comp != 0) return comp;
    }
    return a2.size() - a1.size();
  }
  
  public boolean isCharSetEqual(SortedSet<String> charSet) {
    return LexicalCompare.areEqual(characters.iterator(), charSet.iterator());
  }
  
  /** Accept only regular name. */
  public boolean hasCharacter(String str) {
    int idx = Collections.<String>binarySearch(characters, str);
    return idx >= 0 && idx < characters.size() && characters.get(idx).equals(str);
  }

  public String get(int idx) { return characters.get(idx); }
  /** If character not contained, output is not specified */
  public int indexOf(String str) {
    return Collections.<String>binarySearch(characters, str);
  }
  public int size() { return characters.size(); }
  
  /** Returns an iterator over the fully qualified label instances */
  public Iterator<String> iterator() {
    return new IteratorHelper(characters.iterator());
  }
  
  class IteratorHelper implements Iterator<String> {
    Iterator<String> orig;
    IteratorHelper(Iterator<String> orig) {
      this.orig = orig;
    }
    
    public String next() {
      return prefix + orig.next();
    }
    
    public boolean hasNext() { return orig.hasNext(); }
    public void remove() { throw new UnsupportedOperationException(); }
  }
  
  public static class Builder {
    Machine<?> machine;
    String name;
    TreeSet<String> chars;
    
    public Builder(Machine<?> machine, String name) {
      this.machine = machine;
      this.name = name;
      this.chars=new TreeSet<String>();
    }
    
    private void addCharacter(String character) {
      chars.add(character);
    }
    
    public Alphabet build() {
      for (State<?> state : machine) {
        addCharacter(state.getLabel(name));
      }
      return new Alphabet(machine.name, name, chars);
    }
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(machineName + "." + name + "->{");
    boolean isFirst = true;
    for (String str : characters) {
      if (isFirst) { isFirst = false; }
      else { builder.append(", "); }
      builder.append(str);
    }
    builder.append('}');
    return builder.toString();
  }
}
