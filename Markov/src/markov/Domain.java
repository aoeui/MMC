package markov;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/** Determined by its Alphabet instances, which are determined by the labels occurring in state machines.
 */
public class Domain implements Comparable<Domain> {
  public final String machineName;
  private HashMap<String,Alphabet> data; 
  
  private Domain(String machineName, Collection<Alphabet> alphabets) {
    this.machineName = machineName;
    data = new HashMap<String,Alphabet>();
    for (Alphabet alpha : alphabets) {
      data.put(alpha.domainName, alpha);
    }
  }
  
  public int compareTo(Domain other) {
    return machineName.compareTo(other.machineName);
  }
  
  public boolean equals(Object o) {
    try {
      return ((Domain)o).machineName.equals(machineName);
    } catch (Exception e) { return false; }
  }

  public Alphabet get(String name) {
    return data.get(name);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean isFirst = true;
    for (Alphabet alpha : data.values()) {
      if (isFirst) isFirst = false;
      else builder.append('\n');
      builder.append(alpha);
    }
    return builder.toString();
  } 
  
  public static class Builder {
    Machine<?> machine;

    public Builder(Machine<?> machine) {
      this.machine = machine;
    }
        
    public Domain build() {
      // Since Machine.Builder forces all states to have the same alphabets, we can use one to get the alphabet names
      State<?> example = machine.iterator().next();
      Iterator<String> it = example.labelNameIterator();
      ArrayList<Alphabet> alphabets = new ArrayList<Alphabet>();
      while (it.hasNext()) {
        Alphabet.Builder alphaBuilder = new Alphabet.Builder(machine, it.next());
        alphabets.add(alphaBuilder.build());
      }
      return new Domain(machine.name, alphabets);
    }
  }
}
