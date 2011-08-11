package markov;

import util.Indenter;

public abstract class DecisionTree<T extends Probability<T>> {
  public final boolean isBranch() { return !isTerminal(); }
  public abstract boolean isTerminal();
  
  public abstract void accept(Visitor<T> visitor);

  static class Branch<T extends Probability<T>> extends DecisionTree<T> {
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
      indenter.print("if (").print(predicate).println(") {\n");
      indenter.indent();
      indenter.println(consequent);
      indenter.deindent();
      indenter.println("} else {").indent();
      indenter.println(alternative);
      indenter.deindent().println("}");
    }
  }  
  
  static class Terminal<T extends Probability<T>> extends DecisionTree<T> {
    TransitionVector<T> vector;
    
    public Terminal(TransitionVector<T> vector) {
      this.vector = vector;
    }
    
    public void accept(Visitor<T> visitor) {
      visitor.visitTerminal(this);
    }

    public boolean isTerminal() { return true; }
    public TransitionVector<T> getTransitionVector() { return vector; }
    public String toString() { return vector.toString(); }
    public void indent(Indenter indenter) { indenter.print(vector); }
  }
  
  public static interface Visitor<T extends Probability<T>> {
    void visitTerminal(Terminal<T> t);
    void visitBranch(Branch<T> t);
  }

  public abstract String toString();
  public abstract void indent(Indenter indenter);
}
