package markov;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import util.Joiner;
import util.LexicalCompare;
import util.Stack;

public class Alphabet implements Comparable<Alphabet> {
  public final Stack<String> name;  // every alphabet has a name

  private final ArrayList<String> characters;  // this is ordered, unique
  
  private Alphabet(SortedSet<? extends String> chars, String ... nameArr) {
    Stack<String> stack = Stack.<String>emptyInstance();
    for (int i = nameArr.length-1; i >= 0; i--) {
      stack = stack.push(nameArr[i]);
    }
    this.name = stack;  // empty name is actually OK
    this.characters = new ArrayList<String>(chars);
  }
  
  public int compareTo(Alphabet b) {
    int rv = Stack.STRING_COMP.compare(name, b.name);
    // Alphabets are equal if names are equal
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
    
  public static class Builder extends AltBuilder {
    Machine<?> machine;
    
    public Builder(Machine<?> machine, String name) {
      super(machine.name, name);
      this.machine = machine;
    }
        
    public Alphabet build() {
      for (State<?> state : machine) {
        addCharacter(state.getLabel(args[1]));
      }
      return super.build();
    }
  }
  
  public static class AltBuilder {
    public final String[] args;
    TreeSet<String> chars;
    
    public AltBuilder(String ... args) {
      this.args = args;
      this.chars = new TreeSet<String>();
    }
    
    public void addCharacter(String character) {
      chars.add(character);
    }
    
    public Alphabet build() {
      return new Alphabet(chars, args);
    }
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(name.toString("::"));
    builder.append("->{");
    Joiner.appendJoin(builder, characters, ", ");
    builder.append('}');
    return builder.toString();
  }
}
