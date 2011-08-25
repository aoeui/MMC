package markov;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import util.Stack;
import util.TerminatedIterator;

/** This class evaluates a DecisionTree using restrictions */
public class Evaluation<T extends Comparable<? super T>> implements Iterable<Alphabet> {
  public final Dictionary dictionary;
  final HashMap<Predicate,Resolver> resolverCache;

  public final DecisionTree<T> tree;
  public final Evaluator root;
  
  private final TreeSet<Alphabet> alphabets;  // ordered alphabets

  public Evaluation(final Dictionary dictionary, DecisionTree<T> tree) {
    this.dictionary = dictionary;
    this.resolverCache = new HashMap<Predicate,Resolver>();

    this.tree = tree;
    root = tree.accept(new DecisionTree.VisitorRv<T,Evaluator>() {
      public Evaluator visitTerminal(markov.DecisionTree.Terminal<T> t) {
        return new Terminal(t.output);
      }
      public Evaluator visitBranch(DecisionTree.Branch<T> t) {
        return new Walker(t, Stack.<Resolver.Atom>emptyInstance(), getResolver(t.predicate));
      }
    });
    alphabets = new TreeSet<Alphabet>();
    TerminatedIterator<Predicate.Atom> it = tree.atomIterator();
    while (it.hasNext()) {
      Predicate.Atom atom = it.next();
      alphabets.add(dictionary.get(atom.machineName).get(atom.labelName));
    }    
  }
  
  public Iterator<Alphabet> iterator() {
    return alphabets.iterator();
  }
  
  private Resolver getResolver(Predicate p) {
    Resolver rv = resolverCache.get(p);
    if (rv == null) {
      rv = p.reduce(dictionary);
      resolverCache.put(p, rv);
    }
    return rv;
  }
    
  public abstract class Evaluator {
    public abstract boolean isTerminal();
    public abstract Evaluator restrict(Predicate.Atom newRestriction);
    public abstract Evaluator restrict(Resolver.Atom newRestriction);
    public abstract void accept(Visitor<T> visitor);
  }

  /** Note that this is not a static class, it has implicit access to a root Evaluator */
  public class Walker extends Evaluator {
    public final DecisionTree.Branch<T> position;  // Current DecisionTree node.
    public final Stack<Resolver.Atom> restrictions;  // Current restrictions enforced.
    public final Resolver state;  // Current Resolver with restrictions applied

    public Walker(DecisionTree.Branch<T> position, Stack<Resolver.Atom> restrictions, Resolver state) {
      this.position = position;
      this.restrictions = restrictions;
      this.state = state;
    }
    
    public boolean isTerminal() { return false; }
    
    public Evaluator restrict(Predicate.Atom newRestriction) {
      return restrict(dictionary.convert(newRestriction));
    }

    public Evaluator restrict(Resolver.Atom newRestriction) {
      if (restrictions.contains(newRestriction)) throw new RuntimeException();

      Resolver next = state.restrict(newRestriction);
      Stack<Resolver.Atom> newRestrictions = restrictions.push(newRestriction);
      if (next.isValue()) {
        return decide(next.getValue() ? position.consequent : position.alternative, newRestrictions);
      } else {
        return new Walker(position, restrictions.push(newRestriction), next);
      }
    }
    
    public Evaluator decide(DecisionTree<T> decision, final Stack<Resolver.Atom> restrictions) {
      return decision.accept(new DecisionTree.VisitorRv<T,Evaluator>() {
        public Evaluator visitTerminal(DecisionTree.Terminal<T> t) {
          return new Terminal(t.output);
        }
        public Evaluator visitBranch(DecisionTree.Branch<T> t) {
          Resolver resolver = getResolver(t.predicate);
          Iterator<Resolver.Atom> it = restrictions.iterator();
          while (it.hasNext() && !resolver.isValue()) {
            resolver = resolver.restrict(it.next());
          }
          if (resolver.isValue()) {
            return decide(resolver.getValue() ? t.consequent : t.alternative, restrictions);
          } else {
            return new Walker(t, restrictions, resolver);
          }
        }
      });
    }
    
    public void accept(Visitor<T> visitor) { visitor.visitWalker(this); }
  }
  
  public class Terminal extends Evaluator {
    public final T output;
    
    public Terminal(T output) {
      this.output = output;
    }

    public boolean isTerminal() { return true; }

    public Evaluator restrict(Predicate.Atom newRestriction) { return this; }

    public Evaluator restrict(Resolver.Atom newRestriction) { return this; }
    
    public void accept(Visitor<T> visitor) { visitor.visitTerminal(this); }
  }
  
  public static interface Visitor<T extends Comparable<? super T>> {
    public void visitTerminal(Evaluation<T>.Terminal term);
    public void visitWalker(Evaluation<T>.Walker walker);
  }
}
