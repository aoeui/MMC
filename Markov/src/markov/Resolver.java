package markov;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import util.UnmodifiableIterator;
import markov.Predicate.CollectionType;

public abstract class Resolver {
  public final static Resolver TRUE = Value.TRUE;
  public final static Resolver FALSE = Value.FALSE;
  
  public abstract void accept(Visitor v);

  public abstract Resolver restrict(Atom atom);

  public boolean isValue() { return false; }
  public boolean getValue() { throw new UnsupportedOperationException(); }

  static abstract class Aggregate extends Resolver implements Iterable<Resolver> {
    public final CollectionType type;
    final ArrayList<Resolver> terms;
    
    public Aggregate(CollectionType type, Collection<Resolver> terms) {
      this.type = type;
      this.terms = new ArrayList<Resolver>(terms);
    }

    public Iterator<Resolver> iterator() {
      return new UnmodifiableIterator<Resolver>(terms.iterator());
    }
    
    public Resolver restrict(Atom atom) {
      ArrayList<Resolver> newTerms = new ArrayList<Resolver>();
      boolean containsNew = false;
      for (Resolver term : terms) {
        Resolver newTerm = term.restrict(atom);
        newTerms.add(newTerm);
        if (newTerm != term) {
          containsNew = true;
        }
      }
      return containsNew ? buildCollection(newTerms, type) : this;
    }
  }
  
  public static class And extends Aggregate {
    public And(Collection<Resolver> terms) {
      super(CollectionType.AND, terms);
    }
    public void accept(Visitor v) { v.visitAnd(this); }
  }
    
  public static class Or extends Aggregate {
    public Or(Collection<Resolver> terms) {
      super(CollectionType.OR, terms);
    }
    
    public void accept(Visitor v) { v.visitOr(this); }
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
    
    public int cardinality() { return set.cardinality(); }
    
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

    public Resolver restrict(Atom atom) {
      if (atom.alpha.name.equals(alpha.name)) {
        return instance == atom.instance ? TRUE : FALSE;
      }
      return this;
    }
    
    public Resolver invert() {
      ArrayList<Resolver> inverses = new ArrayList<Resolver>();
      for (int i = 0; i < alpha.size(); i++) {
        if (i != instance) inverses.add(new Atom(alpha, i));
      }
      return new Or(inverses);
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
    
    public Resolver restrict(Atom atom) { return this; }
    public boolean isValue() { return true; }
    public boolean getValue() { return value; }
  }

  public static interface Visitor {
    public void visitAnd(And p);
    public void visitOr(Or p);
    public void visitAtom(Atom p);
    public void visitValue(Value p);
  }

  // Encapsulates behavior of CollectionBuilder in one method call.
  static Resolver buildCollection(ArrayList<Resolver> terms, CollectionType type) {
    return new CollectionBuilder(terms, type).build();
  }
  
  /** This class performs two functions in constructing AND / OR predicates.
   * 1. It short circuits AND when two Atom instances occur.
   *    It short circuits OR when all Atoms occur.
   * 2. When OR (or AND) is nested under OR (or AND respectively), they are combined.
   */
  static class CollectionBuilder {
    final CollectionType type;
    final ArrayList<Resolver> terms;
    
    final ArrayList<Resolver> newTerms = new ArrayList<Resolver>();
    final HashMap<String,AtomState> atoms = new HashMap<String,AtomState>();

    Resolver shortCircuit = null;

    public CollectionBuilder(ArrayList<Resolver> terms, CollectionType type) {
      this.type = type;
      this.terms = terms;
    }
    
    public Resolver build() {
      Iterator<Resolver> it = terms.iterator();
      while (shortCircuit == null && it.hasNext()) {
        processTerm(it.next());
      }
      if (shortCircuit != null) return shortCircuit;
      for (AtomState state : atoms.values()) {
        for (Atom a : state) {
          newTerms.add(a);
        }
      }
      if (newTerms.size() <= 0) {
        switch (type) {
        case AND: return TRUE;
        case OR: return FALSE;
        default: throw new RuntimeException();
        }
      } else if (newTerms.size() == 1) {
        return newTerms.get(0);
      } else {
        switch (type) {
        case AND: return new And(newTerms);
        case OR: return new Or(newTerms);
        default: throw new RuntimeException();
        }
      }
    }
    
    public void processTerm(Resolver term) {
      term.accept(new Visitor() {
        public void visitAnd(And p) {
          switch (type) {
          case AND:
            for (Resolver r : p) {
              processTerm(r);
            }
            break;
          case OR:
            newTerms.add(p);
            break;
          default: throw new RuntimeException();
          }
        }
        public void visitOr(Or p) {
          switch (type) {
          case AND:
            newTerms.add(p);
            break;
          case OR:
            for (Resolver r : p) {
              processTerm(r);
            }
            break;
          default: throw new RuntimeException();
          }
        }
        public void visitAtom(Atom p) {
          AtomState state = atoms.get(p.alpha.name);
          if (state == null) {
            state = new AtomState(p.alpha);
            atoms.put(p.alpha.name, state);
          }
          state.add(p.instance);
          switch (type) {
          case OR:
            if (state.isComplete()) shortCircuit = TRUE;
            break;
          case AND:
            if (state.cardinality() >= 2) shortCircuit = FALSE;
            break;
          default: throw new RuntimeException();
          }
        }
        public void visitValue(Value p) {
          switch (type) {
          case OR:
            if (p.value) shortCircuit = TRUE;
            break;
          case AND:
            if (!p.value) shortCircuit = FALSE;
            break;
          default: throw new RuntimeException();
          }
        }        
      });
    }
  }
  
  public static class Builder {
    final Predicate root;
    final Dictionary dict;

    public Builder(Predicate p, Dictionary dict) {
      this.root = p;
      this.dict = dict;
    }

    public Resolver build() {
      return recurse(root, false);
    }
    
    Resolver recurse(final Predicate pred, final boolean doNegate) {
      final Resolver[] rvPtr = new Resolver[] {null};
      pred.accept(new Predicate.Visitor() {
        public void visitAnd(Predicate.And predicate) {
          visitCollection(predicate);
        }
        public void visitOr(Predicate.Or predicate) {
          visitCollection(predicate);
        }
        public void visitImplies(Predicate.Implies predicate) {
          rvPtr[0] = recurse(predicate.convert(), doNegate);
        }
        public void visitNeg(Predicate.Neg predicate) {
          rvPtr[0] = recurse(predicate.subject, !doNegate);
        }
        public void visitAtom(Predicate.Atom predicate) {
          Resolver.Atom atom = dict.convert(predicate);
          rvPtr[0] = doNegate ? atom.invert() : atom;
        }
        public void visitCollection(Predicate.CollectionPredicate predicate) {
          rvPtr[0] = handleAggregate(predicate, doNegate);
        }
      });
      return rvPtr[0];
    }
    
    Resolver handleAggregate(Predicate.CollectionPredicate predicate, boolean doNegate) {
      ArrayList<Resolver> accu = new ArrayList<Resolver>();
      for (Predicate term : predicate) {
        accu.add(recurse(term, doNegate));
      }
      return buildCollection(accu, doNegate ? predicate.type.opposite() : predicate.type);
    }
  }
}
