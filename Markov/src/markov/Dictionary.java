package markov;

import java.util.Iterator;
import java.util.LinkedHashMap;
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
  
  public Iterator<Domain> iterator() {
    return new util.UnmodifiableIterator<Domain>(data.values().iterator());
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
