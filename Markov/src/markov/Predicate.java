package markov;

import java.util.ArrayList;
import java.util.Iterator;

import util.UnmodifiableIterator;

public abstract class Predicate {
  public final static Predicate TRUE = Value.TRUE;
  public final static Predicate FALSE = Value.FALSE;

  public abstract void accept(Visitor visitor);

  public abstract Predicate resolve(Atom atom, Dictionary dictionary);

  public abstract Predicate reduceNegations(Dictionary dictionary);

  public static class Builder {
    public enum Type { AND, OR };

    ArrayList<Predicate> terms;
    ArrayList<Atom> atoms;  // Could do more given the alphabets
    final Type type;
    
    public Builder(Type t) {
      this.type = t;
      terms = new ArrayList<Predicate>();
      atoms = new ArrayList<Atom>();
    }
    public void add(Predicate p) {
      if (terms == null) return;
      
      p.accept(new VisitorAdapter() {
        public void visit(Predicate p) {
          terms.add(p);
        }
        public void visitAnd(And p) {
          if (type.equals(Type.AND)) {
            for (Predicate term : p.terms) {
              add(term);
            }
          } else {
            terms.add(p);
          }
        }
        public void visitOr(Or p) {
          if (type.equals(Type.OR)) {
            for (Predicate term : p.terms) {
              add(term);
            }
          } else {
            terms.add(p);
          }
        }
        public void visitAtom(Atom atom) {
          for (Atom a : atoms) {
            if (a.isSameInstance(atom)) {
              if (a.isNeg != atom.isNeg) terms = null;
              return;
            }
          }
          atoms.add(atom);
        }
        public void visitValue(Value v) {
          if (type.equals(Type.AND) ^ v.isTrue) terms = null;
        }
      });
    }
    public Predicate build() {
      if (type.equals(Type.AND)) {
        if (terms == null) return FALSE;
        if (terms.size() == 0) throw new RuntimeException();
        if (terms.size() == 1) return terms.get(0);
        return new And(terms);
      } else {
        if (terms == null) return TRUE;
        if (terms.size() == 0) throw new RuntimeException();
        if (terms.size() == 1) return terms.get(0);
        return new Or(terms);
      }
    }
  }

  public boolean isValue() { return false; }
  public boolean isTrue() { return false; }

  public static class And extends Predicate implements Iterable<Predicate> {
    ArrayList<Predicate> terms;
    And(ArrayList<Predicate> clauses) {
      if (clauses.size() < 2) throw new RuntimeException();
      
      this.terms = new ArrayList<Predicate>(clauses);
    }
    public void accept(Visitor v) { v.visitAnd(this); }
    public Predicate reduceNegations(Dictionary dictionary) {
      Builder b = new Builder(Builder.Type.OR);
      for (Predicate p : terms) {
        b.add(p.reduceNegations(dictionary));
      }
      return b.build();
    }
    public Predicate resolve(Atom atom, Dictionary dictionary) {
      Builder b = new Builder(Builder.Type.AND);
      for (Predicate term : terms) {
        b.add(term.resolve(atom, dictionary));
      }
      return b.build();
    }
    public String toString() {
      StringBuilder b = new StringBuilder();
      boolean isFirst = true;
      for (Predicate term : terms) {
        if (isFirst) isFirst = false;
        else b.append(" /\\ ");
        b.append('(').append(term).append(')');
      }
      return b.toString();
    }
    public Iterator<Predicate> iterator() {
      return new UnmodifiableIterator<Predicate>(terms.iterator());
    }
  }
  public static class Or extends Predicate implements Iterable<Predicate> {
    ArrayList<Predicate> terms;
    Or(ArrayList<Predicate> clauses) {
      if (clauses.size() < 2) throw new RuntimeException();
      this.terms = new ArrayList<Predicate>(clauses);
    }
    public void accept(Visitor v) { v.visitOr(this); }
    public Predicate reduceNegations(Dictionary dictionary) {
      Builder b = new Builder(Builder.Type.AND);
      for (Predicate p : terms) {
        b.add(p.reduceNegations(dictionary));
      }
      return b.build();
    }
    public Predicate resolve(Atom atom, Dictionary dictionary) {
      Builder b = new Builder(Builder.Type.OR);
      for (Predicate p : terms) {
        b.add(p.resolve(atom, dictionary));
      }
      return b.build();
    }
    
    public String toString() {
      StringBuilder b = new StringBuilder();
      boolean isFirst = true;
      for (Predicate term : terms) {
        if (isFirst) isFirst = false;
        else b.append(" \\/ ");
        b.append('(').append(term).append(')');
      }
      return b.toString();
    }
    public Iterator<Predicate> iterator() {
      return new UnmodifiableIterator<Predicate>(terms.iterator());
    }
  }
  public static class Neg extends Predicate {
    public final Predicate subject;
    
    public Neg(Predicate subject) {
      this.subject = subject;
    }
    public Predicate reduceNegations(Dictionary dictionary) { return subject; }
    public void accept(Visitor v) { v.visitNeg(this); }
    public Predicate resolve(Atom atom, Dictionary dictionary) {
      final Predicate newSubject = subject.resolve(atom, dictionary);
      if (newSubject.isValue()) {
        return newSubject.isTrue() ? FALSE : TRUE;
      } else {
        return newSubject == subject ? this : new Neg(newSubject);
      }
    }
    
    public String toString() {
      return "-(" + subject + ")";
    }
  }
  public static class Implies extends Predicate {
    public final Predicate antecedent;
    public final Predicate consequent;
    
    public Implies(Predicate antecedent, Predicate consequent) {
      this.antecedent = antecedent;
      this.consequent = consequent;
    }
    public void accept(Visitor v) { v.visitImplies(this); }
    public Predicate reduceNegations(Dictionary dictionary) {
      Builder builder = new Builder(Builder.Type.AND);
      builder.add(antecedent);
      builder.add(consequent.reduceNegations(dictionary));
      return builder.build();
    }
    public Predicate resolve(Atom atom, Dictionary dictionary) {
      final Predicate newAntecedent = antecedent.resolve(atom, dictionary);
      final Predicate newConsequent = consequent.resolve(atom, dictionary);
      
      if (newAntecedent.isValue()) {
        return newAntecedent.isTrue() ? newConsequent : TRUE;
      } else {
        if (newConsequent.isValue()) {
          return newConsequent.isTrue() ? TRUE : newAntecedent.reduceNegations(dictionary);
        } else {
          return (newAntecedent == antecedent && newConsequent == consequent) ? this
              : new Implies(newAntecedent, newConsequent);
        }
      }      
    }
    
    public String toString() {
      return "(" + antecedent + ") -> (" + consequent + ")";
    }
  }
  public static class Atom extends Predicate {
    public final String machineName;
    public final String labelName;
    public final String character;
    public final boolean isNeg;
    
    public Atom(String machineName, String labelName, String instance, boolean isNeg) {
      this.machineName = machineName;
      this.labelName = labelName;
      this.character = instance;
      this.isNeg = isNeg;
    }
    
    public Atom(String machineName, String labelName, String instance) {
      this(machineName, labelName, instance, false);
    }
    
    public Predicate reduceNegations(Dictionary dictionary) {
      return new Atom(machineName, labelName, character, !isNeg);
    }
    public void accept(Visitor v) { v.visitAtom(this); }
    public Predicate resolve(Atom atom, Dictionary dictionary) {
      if (isSameInstance(atom)) {
        return isNeg == atom.isNeg ? TRUE : FALSE;
      } else return this;
    }
    public boolean isSameInstance(Atom atom) {
      return machineName.equals(atom.machineName) && labelName.equals(atom.labelName)
          && character.equals(atom.character);
    }
    
    public String toString() {
      return (isNeg ? "-" : "") + machineName + "." + labelName + "." + character;
    }
  }

  public static class Value extends Predicate {
    public final static Value TRUE = new Value(true);
    public final static Value FALSE = new Value(false);
    
    public final boolean isTrue;
    private Value(boolean isTrue) {
      this.isTrue = isTrue;
    }
    
    public Predicate resolve(Atom atom, Dictionary d) { return this; }    
    public Predicate reduceNegations(Dictionary dictionary) { return isTrue ? FALSE : TRUE; }
    public boolean isValue() { return true; }    
    public boolean isTrue() { return isTrue; }
    
    public void accept(Visitor visitor) { visitor.visitValue(this); }
    
    public String toString() { return isTrue ? "TRUE" : "FALSE"; }
  }

  public static interface Visitor {
    public void visitAnd(And predicate);
    public void visitOr(Or predicate);
    public void visitNeg(Neg predicate);
    public void visitImplies(Implies predicate);
    public void visitAtom(Atom predicate);
    public void visitValue(Value predicate);
  }

  public abstract static class VisitorAdapter implements Visitor {
    public abstract void visit(Predicate p);
    public void visitAnd(And p) { visit(p); }
    public void visitOr(Or p) { visit(p); }
    public void visitNeg(Neg p) { visit(p); }
    public void visitImplies(Implies p) { visit(p); }
    public void visitAtom(Atom p) { visit(p); }
    public void visitValue(Value p) { visit(p); }
  }
}