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

  public abstract Resolver resolve(Atom atom);
  public boolean isValue() { return false; }
  public boolean getValue() { throw new UnsupportedOperationException(); }

  public static class And extends Resolver {
    ArrayList<Resolver> terms;
    
    public And(Collection<Resolver> terms) {
      terms = new ArrayList<Resolver>(terms);
    }
    
    public void accept(Visitor v) { v.visitAnd(this); }
    
    public Resolver resolve(Atom atom) {
      return null;
    }
  }

  public static class Or extends Resolver {
    ArrayList<Resolver> terms;
    
    public Or(Collection<Resolver> terms) {
      terms = new ArrayList<Resolver>(terms);
    }
    
    public void accept(Visitor v) { v.visitOr(this); }
    
    public Resolver resolve(Atom atom) {
      final ArrayList<Resolver> newTerms = new ArrayList<Resolver>();
      final HashMap<String,AtomState> atoms = new HashMap<String,AtomState>();
      final Resolver[] shortCircuit = new Resolver[] { null };
      Iterator<Resolver> termIt = terms.iterator();
      while (termIt.hasNext() && shortCircuit[0] == null) {
        Resolver term = termIt.next().resolve(atom);
        term.accept(new Visitor() {
          public void visitAnd(And p) {
            
          }
          public void visitOr(Or p) {
            
          }
          public void visitAtom(Atom p) {
            AtomState state = atoms.get(p.alpha.varName);
            if (state == null) {
              state = new AtomState(p.alpha);
            }
            state.add(p.instance);
            if (state.isComplete()) {
              shortCircuit[0] = TRUE;
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
  
  static class AtomState implements Iterable<Atom> {
    public final Alphabet alpha;
    public final int N;
    
    BitSet set;
    
    // Even though this is created from an Atom, it really only 
    public AtomState(Alphabet alpha) {
      this.alpha = alpha;
      this.N = alpha.size();
      
      set = new BitSet(N);
    }
    
    public boolean isComplete() {
      return set.cardinality() == N;
    }
    
    public void add(int instance) {
      set.set(instance);
    }
    
    public Iterator<Atom> iterator() {
      return new Iterator<Atom>() {
        int next = set.nextSetBit(0);
        public boolean hasNext() {
          return next >= 0;
        }
        public Atom next() {
          Atom rv = new Atom(alpha, next);
          next = set.nextSetBit(next+1);
          return rv;
        }
        public void remove() { throw new UnsupportedOperationException(); }
      };
    }
  }
  
  public static class Atom extends Resolver {
    public final Alphabet alpha;
    public final int instance;
    
    public Atom(Alphabet alpha, int instance) {
      this.alpha = alpha;
      this.instance = instance;
    }
    
    public void accept(Visitor v) { v.visitAtom(this); }

    public Resolver resolve(Atom atom) {
      if (atom.alpha.varName.equals(alpha.varName)) {
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
    
    public Resolver resolve(Atom atom) { return this; }
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
