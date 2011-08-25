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
    public boolean isDominant(T value);
  }

  public static <T extends Comparable<? super T>> Romdd<T> apply(Op<T> op, Romdd<T> f, Romdd<T> g) {
    return new Application<T>(op, f, g).apply();
  }

  // TODO implement apply
  public static class Application<T extends Comparable<? super T>> {
    public final Op<T> operation;
    public final Romdd<T> f;
    public final Romdd<T> g;
    
    TreeMap<RomddPair<T>, Romdd<T>> pairCache;
    TreeMap<Romdd<T>, Romdd<T>> nodeCache;
    
    public Application(Op<T> operation, Romdd<T> f, Romdd<T> g) {
      this.operation = operation;
      this.f = f;
      this.g = g;
      this.pairCache = new TreeMap<RomddPair<T>, Romdd<T>>();
      this.nodeCache = new TreeMap<Romdd<T>, Romdd<T>>();
    }
    
    public Romdd<T> apply() {
      return recurse(f, g);
    }
    
    public Romdd<T> recurse(final Romdd<T> left, final Romdd<T> right) {
      RomddPair<T> pair = new RomddPair<T>(left, right);
      Romdd<T> rv = pairCache.get(pair);
      if (rv == null) {
        final RomddPtr<T> rvPtr = new RomddPtr<T>();
        left.accept(new Visitor<T>() {
          public void visitNode(final Node<T> leftNode) {
            right.accept(new Visitor<T>() {
              public void visitNode(final Node<T> rightNode) {
                int comp = leftNode.alpha.compareTo(rightNode.alpha);
                if (comp == 0) {
                  rvPtr.value = expandBoth(leftNode, rightNode);
                } else {
                  rvPtr.value = comp < 0 ? expandLeft(leftNode, rightNode) : expandRight(leftNode, rightNode);
                }
              }
              public void visitTerminal(final Terminal<T> rightTerm) {
                rvPtr.value = operation.isDominant(rightTerm.output) ? getTerm(rightTerm.output) : expandLeft(leftNode, rightTerm);
              }
            });
          }
          public void visitTerminal(final Terminal<T> leftTerm) {
            right.accept(new Visitor<T>() {
              public void visitNode(final Node<T> rightNode) {
                rvPtr.value = operation.isDominant(leftTerm.output) ? getTerm(leftTerm.output) : expandRight(leftTerm, rightNode); 
              }
              public void visitTerminal(final Terminal<T> rightTerm) {
                rvPtr.value = getTerm(operation.apply(leftTerm.output, rightTerm.output));
              }
            });
          }
        });
        rv = rvPtr.value;
        pairCache.put(pair, rv);
      }
      return rv;
    }
    
    public Romdd<T> getTerm(T output) {
      Terminal<T> newTerminal = new Terminal<T>(output);
      Romdd<T> terminal = nodeCache.get(newTerminal);
      if (terminal == null) {
        nodeCache.put(newTerminal, newTerminal);
        terminal = newTerminal;
      }
      return terminal;
    }
    
    public Romdd<T> expandLeft(final Node<T> left, final Romdd<T> right) {
      return expandOne(left, right, true);
    }
    public Romdd<T> expandRight(final Romdd<T> left, final Node<T> right) {
      return expandOne(right, left, false);
    }
    // make sure to check the return value in cache before return
    // Also check that the children are not all equal.
    public Romdd<T> expandOne(final Node<T> splitter, final Romdd<T> node, boolean splitterLeft) {
      ArrayList<Romdd<T>> children = new ArrayList<Romdd<T>>();
      Romdd<T> protoChild = null;
      boolean foundDiff = false;
      for (Romdd<T> child : splitter) {
        Romdd<T> mix = splitterLeft ? recurse(child, node) : recurse(node, child);
        children.add(mix);
        if (!foundDiff) {
          if (protoChild == null) {
            protoChild = mix;
          } else {
            foundDiff = protoChild.compareTo(mix) != 0;
          }
        }
      }
      if (foundDiff) {
        Romdd<T> newNode = new Node<T>(splitter.alpha, children);  // checking cache
        Romdd<T> rv = nodeCache.get(newNode);
        if (rv == null) {
          rv = newNode;
          nodeCache.put(rv, rv);
        }
        return rv;
      } else {
        return protoChild;  // anything returned by recurse has been checked already
      }
    }

    // make sure to check the return value in cache before return
    // Also check that the children are not all equal.
    public Romdd<T> expandBoth(final Node<T> left, final Node<T> right) {
      assert(left.alpha.compareTo(right.alpha) == 0);
      ArrayList<Romdd<T>> children = new ArrayList<Romdd<T>>();
      Romdd<T> protoChild = null;
      boolean foundDiff = false;
      for (int i = 0; i < left.getSize(); i++) {
        Romdd<T> mix = recurse(left.getChild(i), right.getChild(i));
        children.add(mix);
        if (!foundDiff) {
          if (protoChild == null) {
            protoChild = mix;
          } else {
            foundDiff = protoChild.compareTo(mix) != 0;
          }
        }
      }
      if (foundDiff) {
        Romdd<T> newNode = new Node<T>(left.alpha, children);
        Romdd<T> rv = nodeCache.get(newNode);
        if (rv == null) {
          rv = newNode;
          nodeCache.put(rv, rv);
        }
        return rv;
      } else {
        return protoChild;
      }
    }
  }
  
  /** This class exists solely so that we can use it in closures. */
  static class RomddPtr<T extends Comparable<? super T>> {
    public Romdd<T> value; 
  }
  
  static class RomddPair<T extends Comparable<? super T>> implements Comparable<RomddPair<T>>{
    public final Romdd<T> first;
    public final Romdd<T> second;
    
    public RomddPair(Romdd<T> first, Romdd<T> second) {
      this.first = first;
      this.second = second;
    }
    
    public int compareTo(RomddPair<T> pair) {
      int rv = first.compareTo(pair.first);
      if (rv == 0) {
        rv = second.compareTo(pair.second);
      }
      return rv;
    }
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