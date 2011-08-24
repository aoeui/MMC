package markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import util.LexicalCompare;
import util.Stack;

/* Type parameters
 * T corresponds to terminal values.
 */
public abstract class Romdd<T extends Comparable<? super T>> implements Comparable<Romdd<T>> {
  private Romdd() { }

  public static interface Op<T extends Comparable<? super T>> {
    public T apply(T v1, T v2);
  }

  public static <T extends Comparable<? super T>> Romdd<T> apply(Op<T> op, Romdd<T> f, Romdd<T> g) {
    // TODO implement apply
    return null;
  }

  public abstract Romdd<T> restrict(String name, String value);
  
  public static class Node<T extends Comparable<? super T>> extends Romdd<T> implements Iterable<Romdd<T>> {
    public final Alphabet alpha;
    public final String name;  // Name of alphabet

    final ArrayList<Romdd<T>> children;  // These are ordered the same as the alphabet
    
    public Node(Alphabet alpha, ArrayList<Romdd<T>> children) {
      this.alpha = alpha;
      this.name = alpha.name;
      this.children = new ArrayList<Romdd<T>>(children);
    }
    
    public Romdd<T> restrict(String name, String value) {
      // TODO implement restrict
      if (this.name.equals(name)) {
        
      }
      return null;
    }
    
    public Romdd<T> getChild(int i) { return children.get(i); }
    public int getSize() { return alpha.size(); }
    public Iterator<Romdd<T>> iterator() { return children.iterator(); }
    
    public void accept(Visitor<T> visitor) { visitor.visitNode(this); }

    public int compareTo(Romdd<T> o) {
      if (this == o) return 0;
      final int[] rvPtr = new int[] { 0 };
      o.accept(new Visitor<T>() {
        public void visitTerminal(Terminal<T> term) {
          rvPtr[0] = 1;         
        }
        public void visitNode(Node<T> node) {
          rvPtr[0] = name.compareTo(node.name);
          if (rvPtr[0] != 0) return;
          rvPtr[0] = LexicalCompare.compare(Node.this, node);
        }
      });
      return rvPtr[0];
    }
  }

  public static class Terminal<T extends Comparable<? super T>> extends Romdd<T>{
    public final T output;
    
    public Terminal(T output) {
      this.output = output;
    }
    
    public Romdd<T> restrict(String name, String value) { return this; }
    
    public void accept(Visitor<T> visitor) { visitor.visitTerminal(this); }

    public int compareTo(Romdd<T> o) {
      if (this == o) return 0;
      final int[] rv = new int[] { 0 };
      o.accept(new Visitor<T>() {
        public void visitTerminal(Terminal<T> term) {
          rv[0] = output.compareTo(term.output);
        }
        public void visitNode(Node<T> node) {
          rv[0] = -1;
        }
      });
      return rv[0];
    }
  }
  
  public abstract void accept(Visitor<T> visitor);
  public static interface Visitor<T extends Comparable<? super T>> {
    public void visitTerminal(Terminal<T> term);
    public void visitNode(Node<T> node);
  }
  
  public static class Builder<T extends Probability<T>> {
    public final Evaluation<T> eval;
    private TreeMap<Romdd<T>,Romdd<T>> nodes;

    public Builder(Evaluation<T> eval) {
      this.eval = eval;
      this.nodes = new TreeMap<Romdd<T>,Romdd<T>>();
    }
    
    public Romdd<T> build() {
      Stack<Alphabet> alphabets = Stack.<Alphabet>emptyInstance();
      for (Alphabet alpha : eval) {
        alphabets = alphabets.push(alpha);
      }
      alphabets = alphabets.reverse();  // top of stack is now least element
      return recurse(alphabets, eval.root);
    }
    
    private Romdd<T> recurse(Stack<Alphabet> stack, Evaluation<T>.Evaluator state) {
      if (state.isTerminal()) {
        Terminal<T> newTerm = new Terminal<T>(state.getOutput());
        Romdd<T> term = nodes.get(newTerm);
        if (term == null) {
          term = newTerm;
          nodes.put(term, term);
        }
        return term;
      }
      if (stack.isEmpty()) throw new RuntimeException();

      // State is not terminal -> recurse its children
      Alphabet next = stack.head();
      assert(next.size() > 0);
      ArrayList<Romdd<T>> children = new ArrayList<Romdd<T>>();
      for (Resolver.Atom nextRestriction : next) { 
        children.add(recurse(stack.tail(), state.restrict(nextRestriction)));
      }
      Romdd<T> child = children.get(0);
      boolean foundDiff = false;
      int i = 1;
      while (!foundDiff && i < children.size()) {
        foundDiff = children.get(i++).compareTo(child) != 0;
      }
      if (foundDiff) {
        Romdd<T> newNode = new Node<T>(next, children);
        Romdd<T> rv = nodes.get(newNode);
        if (rv == null) {
          rv = newNode;
          nodes.put(rv, rv);
        }
        return rv;
      } else {
        return child;
      }
    }
  }
}