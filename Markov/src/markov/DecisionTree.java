package markov;

import markov.Predicate.And;
import markov.Predicate.Atom;
import markov.Predicate.CollectionPredicate;
import markov.Predicate.Implies;
import markov.Predicate.Neg;
import markov.Predicate.Or;
import util.Coroutine;
import util.Indenter;
import util.TerminatedIterator;

public abstract class DecisionTree<T extends Comparable<? super T>> {
  public final boolean isBranch() { return !isTerminal(); }
  public abstract boolean isTerminal();
  
  public abstract void accept(Visitor<T> visitor);
  public abstract <S> S accept(VisitorRv<T,S> visitor);
  
  public abstract Romdd<T> toRomdd(Dictionary dict);
  
  public TerminatedIterator<T> outputIterator() {
    class TerminalIterator extends Coroutine<T> {
      public void init() {
        recurse(DecisionTree.this);
      }
      void recurse(DecisionTree<T> tree) {
        tree.accept(new Visitor<T>() {
          public void visitTerminal(Terminal<T> term) {
            yield(term.output);
          }
          public void visitBranch(Branch<T> branch) {
            recurse(branch.consequent);
            recurse(branch.alternative);
          }
        });
      }
    }
    return new TerminalIterator().iterator();
  }
  
  public TerminatedIterator<Predicate.Atom> atomIterator() {
    class AtomIterator extends Coroutine<Predicate.Atom> {
      public final DecisionTree<T> root;
      
      public AtomIterator(DecisionTree<T> root) {
        this.root = root;
      }
      public void init() {
        recurseDecision(root);
      }
      private void recurseDecision(DecisionTree<T> tree) {
        tree.accept(new Visitor<T>() {
          public void visitTerminal(Terminal<T> t) { }
          public void visitBranch(Branch<T> t) {
            recursePredicate(t.predicate);
            recurseDecision(t.alternative);
            recurseDecision(t.consequent);
          }
        });      
      }
      public void recursePredicate(Predicate pred) {
        pred.accept(new Predicate.Visitor() {
          public void visitCollection(CollectionPredicate predicate) {
            for (Predicate child : predicate) {
              recursePredicate(child);
            }
          }
          public void visitAnd(And predicate) {
            visitCollection(predicate);
          }
          public void visitOr(Or predicate) {
            visitCollection(predicate);
          }
          public void visitNeg(Neg predicate) {
            recursePredicate(predicate.subject);
          }
          public void visitImplies(Implies predicate) {
            recursePredicate(predicate.antecedent);
            recursePredicate(predicate.consequent);
          }
          public void visitAtom(Atom predicate) {
            yield(predicate);
          }
        });
      }
    }
    return new AtomIterator(this).iterator();
  }
  
  public static class Branch<T extends Comparable<? super T>> extends DecisionTree<T> {
    public final Predicate predicate;
    public final DecisionTree<T> consequent;
    public final DecisionTree<T> alternative;
    
    public Branch(Predicate predicate, DecisionTree<T> consequent, DecisionTree<T> alternative) {
      this.predicate = predicate;
      this.consequent = consequent;
      this.alternative = alternative;
    }
    public boolean isTerminal() { return false; }
    
    public void accept(Visitor<T> visitor) {
      visitor.visitBranch(this);
    }
    
    public<S> S accept(VisitorRv<T,S> visitor) {
      return visitor.visitBranch(this);
    }
    
    public Romdd<T> toRomdd(Dictionary dict) {
      return Romdd.<T>branch(predicate.toRomdd(dict), consequent.toRomdd(dict), alternative.toRomdd(dict)); 
    }
    
    public String toString() {
      Indenter indenter = new Indenter();
      indent(indenter);
      return indenter.toString();
    }
    
    public void indent(Indenter indenter) {
      indenter.print("if ").print(predicate).print("\n");
      indenter.indent();
      consequent.indent(indenter);
      indenter.deindent();
      indenter.print("else ");
      alternative.indent(indenter);
    }
  }
  
  public static class Terminal<T extends Comparable<? super T>> extends DecisionTree<T> {
    public final T output;
    
    public Terminal(T output) {
      this.output = output;
    }
    
    public Romdd<T> toRomdd(Dictionary dict) {
      return new Romdd.Terminal<T>(output);
    }
    
    public void accept(Visitor<T> visitor) {
      visitor.visitTerminal(this);
    }
    
    public<S> S accept(VisitorRv<T,S> visitor) {
      return visitor.visitTerminal(this);
    }

    public boolean isTerminal() { return true; }
    public String toString() { return output.toString(); }
    public void indent(Indenter indenter) { indenter.println(output); }
  }
  
  public static interface Visitor<T extends Comparable<? super T>> {
    void visitTerminal(Terminal<T> t);
    void visitBranch(Branch<T> t);
  }
  
  public static interface VisitorRv<T extends Comparable<? super T>, S> {
    public S visitTerminal(Terminal<T> t);
    public S visitBranch(Branch<T> t);
  }

  public abstract String toString();
  public abstract void indent(Indenter indenter);
}
