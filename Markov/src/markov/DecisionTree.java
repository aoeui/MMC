package markov;

import markov.Predicate.And;
import markov.Predicate.Atom;
import markov.Predicate.CollectionPredicate;
import markov.Predicate.Implies;
import markov.Predicate.Neg;
import markov.Predicate.Or;
import util.Closure;
import util.Indenter;
import util.TerminatedIterator;

public abstract class DecisionTree<T extends Probability<T>> {
  public final boolean isBranch() { return !isTerminal(); }
  public abstract boolean isTerminal();
  
  public abstract void accept(Visitor<T> visitor);
  public abstract <S> S accept(VisitorRv<T,S> visitor);
  
  public TerminatedIterator<Predicate.Atom> atomIterator() {
    return new AtomIterator<T>(this).iterator();
  }
  
  private static class AtomIterator<T extends Probability<T>> extends Closure<Predicate.Atom> {
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

  public static class Branch<T extends Probability<T>> extends DecisionTree<T> {
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
    
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("if (").append(predicate).append(") {\n");
      builder.append(consequent);
      builder.append("\n} else {");
      builder.append(alternative);
      builder.append("}\n");
      return builder.toString();
    }
    
    public void indent(Indenter indenter) {
      indenter.print("if (").print(predicate).print(") {\n");
      indenter.indent();
      consequent.indent(indenter);
      indenter.deindent();
      indenter.print("} else {\n").indent();
      alternative.indent(indenter);
      indenter.deindent().print("}\n");
    }
  }  
  
  public static class Terminal<T extends Probability<T>> extends DecisionTree<T> {
    public final TransitionVector<T> vector;
    
    public Terminal(TransitionVector<T> vector) {
      this.vector = vector;
    }
    
    public void accept(Visitor<T> visitor) {
      visitor.visitTerminal(this);
    }
    
    public<S> S accept(VisitorRv<T,S> visitor) {
      return visitor.visitTerminal(this);
    }

    public boolean isTerminal() { return true; }
    public String toString() { return vector.toString(); }
    public void indent(Indenter indenter) { indenter.println(vector); }
  }
  
  public static interface Visitor<T extends Probability<T>> {
    void visitTerminal(Terminal<T> t);
    void visitBranch(Branch<T> t);
  }
  
  public static interface VisitorRv<T extends Probability<T>, S> {
    public S visitTerminal(Terminal<T> t);
    public S visitBranch(Branch<T> t);
  }

  public abstract String toString();
  public abstract void indent(Indenter indenter);
}
