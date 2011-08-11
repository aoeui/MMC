package markov;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class Dictionary implements Iterable<Domain> {
  private LinkedHashMap<String, Domain> data;
  
  private Dictionary(TreeSet<Domain> codomains) {
    data = new LinkedHashMap<String, Domain>();
    for (Domain l : codomains) {
      data.put(l.machineName, l);
    }
  }
  
  public Domain get(String machineName) {
    return data.get(machineName);
  }
  
  public boolean isCompleteSet(String machineName, String labelName, SortedSet<String> characters) {
    return get(machineName).get(labelName).isCharSetEqual(characters); 
  }
  
  public Iterator<Domain> iterator() {
    return new util.UnmodifiableIterator<Domain>(data.values().iterator());
  }
  
  public Resolver.Atom convert(Predicate.Atom atom) {
    return new Resolver.Atom(atom.machineName, atom.labelName, get(atom.machineName).get(atom.labelName).indexOf(atom.character));
  }
  
  public static class Builder {
    TreeSet<Domain> list;
    public Builder() {
      this.list = new TreeSet<Domain>();
    }
    
    public void add(Domain label) {
      list.add(label);
    }
    
    public Dictionary build() {
      return new Dictionary(list);
    }
  }
}
