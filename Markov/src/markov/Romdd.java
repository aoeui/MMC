package markov;

import java.util.ArrayList;
import java.util.Iterator;

import util.LexicalCompare;

/* Type parameters
 * T corresponds to terminal values.
 */
public abstract class Romdd<T extends Comparable<? super T>> implements Comparable<Romdd<T>> {
  private Romdd() { }

  public static interface Op<T> {
    public T apply(T f, T g);
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
    
    public Node(Alphabet alpha) {
      this.alpha = alpha;
      this.name = alpha.name;
      children = new ArrayList<Romdd<T>>(alpha.size());
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
  
  // TODO implement Builder
  public static class Builder<T extends Comparable<? super T>> {
    public Builder() {
    }
  }
}