package markov;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public abstract class Resolver {
  public final static Resolver TRUE = Value.TRUE;
  public final static Resolver FALSE = Value.FALSE;
  
  public abstract void accept(Visitor v);

  public abstract Resolver resolve(Dictionary dict, Atom atom);
  public boolean isValue() { return false; }
  public boolean getValue() { throw new UnsupportedOperationException(); }

  public static class And extends Resolver {
    ArrayList<Resolver> terms;
    
    public And(Collection<Resolver> terms) {
      terms = new ArrayList<Resolver>(terms);
    }
    
    public void accept(Visitor v) { v.visitAnd(this); }
    
    public Resolver resolve(Dictionary dict, Atom atom) {
      return null;
    }
  }

  public static class Or extends Resolver {
    ArrayList<Resolver> terms;
    
    public Or(Collection<Resolver> terms) {
      terms = new ArrayList<Resolver>(terms);
    }
    
    public void accept(Visitor v) { v.visitOr(this); }
    
    public Resolver resolve(final Dictionary dict, Atom atom) {
      final HashMap<String,BitSet> atoms = new HashMap<String,BitSet>();
      final Resolver[] shortCircuit = new Resolver[] { null };
      Iterator<Resolver> termIt = terms.iterator();
      while (termIt.hasNext() && shortCircuit[0] == null) {
        Resolver term = termIt.next().resolve(dict, atom);
        term.accept(new Visitor() {
          public void visitAnd(And p) {
            
          }
          public void visitOr(Or p) {
            
          }
          public void visitAtom(Atom p) {
            BitSet bits = atoms.get(p.varName);
            if (bits == null) {
              bits = new BitSet(dict.get(p.machineName).get(p.labelName).size());
            }
          }
          public void visitValue(Value p) {
            if (p.value) shortCircuit[0] = TRUE;
          }
        }); 
      }
      if (shortCircuit[0] != null) return shortCircuit[0];
      return null;
    }
  }
  
  public static class Atom extends Resolver {
    public final String machineName;
    public final String labelName;
    public final int instance;
    
    public final String varName;
    
    public Atom(String machineName, String labelName, int instance) {
      this.machineName = machineName;
      this.labelName = labelName;
      this.instance = instance;
      
      this.varName = machineName + "." + labelName;
    }
    
    public void accept(Visitor v) { v.visitAtom(this); }

    public Resolver resolve(Dictionary dict, Atom atom) {
      if (atom.varName.equals(varName)) {
        return instance == atom.instance ? TRUE : FALSE;
      }
      return this;
    }
  }
  
  public static class Value extends Resolver {
    public final static Value TRUE = new Value(true);
    public final static Value FALSE = new Value(false);

    public final boolean value;

    private Value(boolean val) {
      this.value = val;
    }
    
    public void accept(Visitor v) { v.visitValue(this); }
    
    public Resolver resolve(Dictionary dict, Atom atom) { return this; }
    public boolean isValue() { return true; }
    public boolean getValue() { return value; }
  }

  public static interface Visitor {
    public void visitAnd(And p);
    public void visitOr(Or p);
    public void visitAtom(Atom p);
    public void visitValue(Value p);
  }
}
