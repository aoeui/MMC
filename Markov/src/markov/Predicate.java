package markov;

import java.util.ArrayList;
import java.util.Iterator;
import util.UnmodifiableIterator;

public abstract class Predicate {
  public enum CollectionType {
    AND("/\\"), OR("\\/");
    
    public final String str;
    CollectionType(String str) {
      this.str = str;
    }
    public String toString() { return str; }
    public CollectionType opposite() {
      switch (this) {
      case AND: return OR;
      case OR: return AND;
      default: throw new RuntimeException();
      }
    }
  }  

  public abstract void accept(Visitor visitor);

  /** Turns everything into alternating AND / OR and eliminates NEG */
  public Resolver reduce(Dictionary dictionary) {
    return new Resolver.Builder(this, dictionary).build();
  }

  public static class CollectionBuilder {
    ArrayList<Predicate> terms;
    final CollectionType type;
    
    public CollectionBuilder(CollectionType t) {
      this.type = t;
      terms = new ArrayList<Predicate>();
    }
    public void add(Predicate p) {
      terms.add(p);
    }
    public Predicate build() {
      if (terms.size() == 0) throw new RuntimeException();
      if (terms.size() == 1) return terms.get(0);
      switch (type) {
      case AND:
        return new And(terms);
      case OR:
        return new Or(terms);
      default: throw new RuntimeException();
      }
    }
  }
  
  public static abstract class CollectionPredicate extends Predicate implements Iterable<Predicate> {
    public final CollectionType type;
    ArrayList<Predicate> terms;
    CollectionPredicate(CollectionType type, ArrayList<Predicate> terms) {
      if (terms.size() < 2) throw new RuntimeException();
      
      this.type = type;
      this.terms = new ArrayList<Predicate>(terms);
    }
    public void accept(Visitor v) { v.visitCollection(this); }
    
    public Iterator<Predicate> iterator() {
      return new UnmodifiableIterator<Predicate>(terms.iterator());
    }
    public String toString() {
      StringBuilder b = new StringBuilder();
      boolean isFirst = true;
      for (Predicate term : terms) {
        if (isFirst) isFirst = false;
        else b.append(' ').append(type).append(' ');
        b.append('(').append(term).append(')');
      }
      return b.toString();
    }
  }

  public static class And extends CollectionPredicate {
    And(ArrayList<Predicate> clauses) {
      super(CollectionType.AND, clauses);
    }
    public void accept(Visitor v) { v.visitAnd(this); }
  }
  public static class Or extends CollectionPredicate {
    Or(ArrayList<Predicate> terms) {
      super(CollectionType.OR, terms);
    }
    public void accept(Visitor v) { v.visitOr(this); }
  }

  public static class Implies extends Predicate {
    public final Predicate antecedent;
    public final Predicate consequent;
    
    public Implies(Predicate antecedent, Predicate consequent) {
      this.antecedent = antecedent;
      this.consequent = consequent;
    }
    public void accept(Visitor v) { v.visitImplies(this); }
    
    // Converts to an Or
    public Predicate convert() {
      CollectionBuilder builder = new CollectionBuilder(CollectionType.OR);
      builder.add(new Neg(antecedent));
      builder.add(consequent);
      return builder.build();
    }

    public String toString() {
      return "(" + antecedent + ") -> (" + consequent + ")";
    }
  }
  public static class Neg extends Predicate {
    public final Predicate subject;
    
    public Neg(Predicate subject) {
      this.subject = subject;
    }
    public void accept(Visitor v) { v.visitNeg(this); }
    
    public String toString() {
      return "-(" + subject + ")";
    }
  }
  public static class Atom extends Predicate implements Comparable<Atom> {
    public final String machineName;
    public final String labelName;
    public final String character;
    
    public final String varName;
    
    public Atom(String machineName, String labelName, String instance) {
      this.machineName = machineName;
      this.labelName = labelName;
      this.character = instance;
      
      this.varName = machineName + "." + labelName;
    }
    public void accept(Visitor v) { v.visitAtom(this); }

    public boolean isSameVariable(Atom atom) {
      return varName.equals(atom.varName);
    }
    
    public String toString() {
      return varName + "=" + character;
    }
    
    public int compareTo(Atom other) {
      int rv = varName.compareTo(other.varName);
      if (rv == 0) return character.compareTo(other.character);
      return rv;
    }
    
    public boolean equals(Object o) {
      try {
        Atom other = (Atom)o;
        return varName.equals(other.varName) && character.equals(other.character);
      } catch (Exception e) { return false; }
    }
  }

  public static interface Visitor {
    public void visitCollection(CollectionPredicate predicate);
    public void visitAnd(And predicate);
    public void visitOr(Or predicate);
    public void visitNeg(Neg predicate);
    public void visitImplies(Implies predicate);
    public void visitAtom(Atom predicate);
  }

  public abstract static class VisitorAdapter implements Visitor {
    public abstract void visit(Predicate p);
    public void visitCollection(CollectionPredicate p) { visit(p); }
    public void visitAnd(And p) { visitCollection(p); }
    public void visitOr(Or p) { visitCollection(p); }
    public void visitNeg(Neg p) { visit(p); }
    public void visitImplies(Implies p) { visit(p); }
    public void visitAtom(Atom p) { visit(p); }
  }

}