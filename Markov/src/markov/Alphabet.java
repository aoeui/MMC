package markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import util.LexicalCompare;

public class Alphabet implements Iterable<Resolver.Atom>, Comparable<Alphabet> {
  public final String machineName;  // needed for generating fully qualified labels
  public final String domainName;  // domain name

  public final String name;  // unique identifier for this alphabet

  private final ArrayList<String> characters;  // this is ordered, unique
  
  private Alphabet(String machineName, String domainName, SortedSet<? extends String> chars) {
    if (machineName.contains(Machine.SCOPE_OPERATOR) || domainName.contains(Machine.SCOPE_OPERATOR)) throw new RuntimeException();
	  this.machineName = machineName;
	  this.domainName = domainName;
	  this.name = machineName + "." + domainName;
	  this.characters = new ArrayList<String>(chars);
  }
  
  public int compareTo(Alphabet b) {
    int rv = machineName.compareTo(b.machineName);
    if (rv == 0) rv = domainName.compareTo(b.domainName);
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
  
  public Iterator<Resolver.Atom> iterator() {
    return new IteratorHelper();
  }
  
  class IteratorHelper implements Iterator<Resolver.Atom> {
    int idx;

    IteratorHelper() {
      idx = 0;
    }
    
    public Resolver.Atom next() {
      return new Resolver.Atom(Alphabet.this, idx++);
    }
    
    public boolean hasNext() { return idx < size(); }
    public void remove() { throw new UnsupportedOperationException(); }
  }
  
  public static class Builder {
    Machine<?> machine;
    String domainName;
    TreeSet<String> chars;
    
    public Builder(Machine<?> machine, String name) {
      this.machine = machine;
      this.domainName = name;
      this.chars=new TreeSet<String>();
    }
    
    private void addCharacter(String character) {
      chars.add(character);
    }
    
    public Alphabet build() {
      for (State<?> state : machine) {
        addCharacter(state.getLabel(domainName));
      }
      return new Alphabet(machine.name, domainName, chars);
    }
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(machineName + "." + domainName + "->{");
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
