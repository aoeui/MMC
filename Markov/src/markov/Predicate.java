package markov;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import util.Joiner;
import util.Stack;
import util.UnmodifiableIterator;

public abstract class Predicate {
  protected abstract String computeString();
  
  public abstract Romdd<Boolean> toRomdd(Dictionary dict);

  public enum CollectionType {
    AND("/\\", Romdd.AND), OR("\\/", Romdd.OR);
    
    public final String str;
    public final Romdd.Op<Boolean> op;
    
    CollectionType(String str, Romdd.Op<Boolean> op) {
      this.str = str;
      this.op = op;
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
  
  private String stringRepresentation;
  private boolean hashCodeInitialized = false;
  private int hashCode;

  public final int hashCode() {
    if (!hashCodeInitialized) {
      hashCode = toString().hashCode();
      hashCodeInitialized = true;
    }
    return hashCode;
  }
  public final String toString() {
    if (stringRepresentation == null) {
      stringRepresentation = computeString();
    }
    return stringRepresentation;
  }
  // Just a superficial test based on the string representation
  public boolean equals(Object o) {
    try {
      return ((Predicate)o).stringRepresentation.equals(stringRepresentation);
    } catch (Exception e) {
      return false;
    }
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
    public int size() { return terms.size(); }
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
    CollectionPredicate(CollectionType type, Predicate p1, Predicate p2, Predicate ... remainder) {
      this.type = type;
      this.terms = new ArrayList<Predicate>();
      terms.add(p1);
      terms.add(p2);
      terms.addAll(Arrays.asList(remainder));
    }
    CollectionPredicate(CollectionType type, List<Predicate> terms) {
      if (terms.size() < 2) throw new RuntimeException();
      
      this.type = type;
      this.terms = new ArrayList<Predicate>(terms);
    }
    public void accept(Visitor v) { v.visitCollection(this); }
    
    public Iterator<Predicate> iterator() {
      return new UnmodifiableIterator<Predicate>(terms.iterator());
    }
    protected String computeString() {
      StringBuilder b = new StringBuilder();
      boolean isFirst = true;
      for (Predicate term : terms) {
        if (isFirst) isFirst = false;
        else b.append(' ').append(type).append(' ');
        b.append('(').append(term).append(')');
      }
      return b.toString();
    }
    public Romdd<Boolean> toRomdd(Dictionary dict) {
      Romdd<Boolean> accu = terms.get(0).toRomdd(dict);
      for (int i = 1; i < terms.size(); i++) {
        accu = Romdd.<Boolean>apply(type.op, accu, terms.get(i).toRomdd(dict));
      }
      return accu;
    }
  }

  public static class And extends CollectionPredicate {
    public And(Predicate p1, Predicate p2, Predicate... remainder) {
      super(CollectionType.AND, p1, p2, remainder);
    }
    And(ArrayList<Predicate> clauses) {
      super(CollectionType.AND, clauses);
    }
    public void accept(Visitor v) { v.visitAnd(this); }
  }
  public static class Or extends CollectionPredicate {
    public Or(Predicate p1, Predicate p2, Predicate... remainder) {
      super(CollectionType.OR, p1, p2, remainder);
    }
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

    protected String computeString() {
      return "(" + antecedent + ") -> (" + consequent + ")";
    }
    
    public Romdd<Boolean> toRomdd(Dictionary dict) {
      return Romdd.<Boolean>apply(Romdd.OR, antecedent.toRomdd(dict).remap(Romdd.INVERT), consequent.toRomdd(dict));
    }
  }
  public static class Neg extends Predicate {
    public final Predicate subject;
    
    public Neg(Predicate subject) {
      this.subject = subject;
    }
    public void accept(Visitor v) { v.visitNeg(this); }
    
    protected String computeString() {
      return "-(" + subject + ")";
    }
    
    public Romdd<Boolean> toRomdd(Dictionary dict) {
      return subject.toRomdd(dict).remap(Romdd.INVERT);
    }
  }

  public static class Atom extends Predicate implements Comparable<Atom> {
    public final Stack<String> name;

    public final String value;
    
    // Last string is considered instance
    public Atom(String name1, String name2, String ... spec) {
      Stack<String> stack = Stack.<String>emptyInstance();
      for (int i = spec.length-2; i >= 0; i--) {
        stack = stack.push(spec[i]);
      }
      if (spec.length == 0) {
        this.name = stack.push(name1);
        this.value = name2;
      } else {
        this.name = stack.push(name2).push(name1);
        this.value = spec[spec.length-1];
      }
    }

    public void accept(Visitor v) { v.visitAtom(this); }

    public boolean isSameVariable(Atom atom) {
      return Stack.STRING_COMP.compare(name, atom.name) == 0;
    }
    
    protected String computeString() {
      return  Joiner.join(name, "::") + "=" + value;
    }
    
    public int compareTo(Atom other) {
      int rv = Stack.STRING_COMP.compare(name, other.name);
      if (rv == 0) return value.compareTo(other.value);
      return rv;
    }
    
    public boolean equals(Object o) {
      try {
        return compareTo((Atom)o) == 0;
      } catch (Exception e) { return false; }
    }
    
    public Romdd<Boolean> toRomdd(Dictionary dict) {
      int varId = dict.getId(name);
      Alphabet alpha = dict.getAlpha(varId);
      ArrayList<Romdd<Boolean>> children = new ArrayList<Romdd<Boolean>>();
      boolean found = false;
      for (int i = 0; i < alpha.size(); i++) {
        if (alpha.get(i).equals(value)) {
          if (found) throw new RuntimeException();
          children.add(Romdd.TRUE);
          found = true;
        } else {
          children.add(Romdd.FALSE);
        }
      }
      if (!found) throw new RuntimeException();
      
      return new Romdd.Node<Boolean>(dict, varId, children);
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
}