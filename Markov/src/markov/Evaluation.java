package markov;

import java.util.HashMap;
import java.util.Iterator;

import util.Stack;

/** This class evaluates a DecisionTree using restrictions */
public class Evaluation<T extends Probability<T>> {
  final Dictionary dictionary;
  final HashMap<Predicate,Resolver> resolverCache;

  public final DecisionTree<T> tree;
  public final Evaluator root;

  public Evaluation(final Dictionary dictionary, DecisionTree<T> tree) {
    this.dictionary = dictionary;
    this.resolverCache = new HashMap<Predicate,Resolver>();

    this.tree = tree;
    root = tree.accept(new DecisionTree.VisitorRv<T,Evaluator>() {
      public Evaluator visitTerminal(markov.DecisionTree.Terminal<T> t) {
        return new Terminal(t.vector);
      }
      public Evaluator visitBranch(DecisionTree.Branch<T> t) {
        return new Walker(t, Stack.<Resolver.Atom>emptyInstance(), getResolver(t.predicate));
      }
    });
  }
  
  public Resolver getResolver(Predicate p) {
    Resolver rv = resolverCache.get(p);
    if (rv == null) {
      rv = p.reduce(dictionary);
      resolverCache.put(p, rv);
    }
    return rv;
  }
    
  public abstract class Evaluator {
    public abstract boolean isTerminal();
    public abstract TransitionVector<T> getTransitionVector();
    public abstract Evaluator restrict(Predicate.Atom newRestriction);
    public abstract Evaluator restrict(Resolver.Atom newRestriction);
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
          return new Terminal(t.vector);
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
    
    public TransitionVector<T> getTransitionVector() { throw new UnsupportedOperationException(); }
  }
  
  public class Terminal extends Evaluator {
    public final TransitionVector<T> vector;
    
    public Terminal(TransitionVector<T> vector) {
      this.vector = vector;
    }

    public boolean isTerminal() { return true; }
    public TransitionVector<T> getTransitionVector() { return vector; }

    public Evaluator restrict(Predicate.Atom newRestriction) { return this; }

    public Evaluator restrict(Resolver.Atom newRestriction) { return this; }
  }
}
