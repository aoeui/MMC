package markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;

public class Constraint implements Iterable<String> {
  public final Romdd<Boolean> constraint;
  ArrayList<String> vars;
  HashSet<String> varSet;
  
  public Constraint(Romdd<Boolean> constraint) {
    this.constraint = constraint;
    this.vars = new ArrayList<String>(constraint.listVarNames());
    this.varSet = new HashSet<String>(vars);
  }
  
  public Iterator<String> iterator() {
    return new util.UnmodifiableIterator<String>(vars.iterator());
  }
  
  public int size() {
    return vars.size();
  }
  
  public String getVar(int idx) {
    return vars.get(idx);
  }
  
  public boolean contains(String str) {
    return varSet.contains(str);
  }
}
