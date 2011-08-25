package markov;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import util.LexicalCompare;
import util.Pair;
import util.Ptr;
import util.Stack;

/* T corresponds to terminal values. */
public abstract class Romdd<T extends Comparable<? super T>> implements Comparable<Romdd<T>> {
  private Romdd() { }
  public abstract void accept(Visitor<T> visitor);

  public static interface Op<T extends Comparable<? super T>> {
    public T apply(T v1, T v2);
    public boolean isDominant(T value);
  }

  public static <T extends Comparable<? super T>> Romdd<T> apply(Op<T> op, Romdd<T> f, Romdd<T> g) {
    return new Application<T>(op, f, g).apply();
  }

  public Romdd<T> sum(Op<T> op, String varName) {
    return new Summation<T>(this, op, varName).compute();
  }

  // TODO Implement sum
  private static class Summation<T extends Comparable<? super T>> {
    public final Romdd<T> root;
    public final Op<T> operation;
    public final String varName;
    
    public Summation(Romdd<T> root, Op<T> operation, String varName) {
      this.root = root;
      this.operation = operation;
      this.varName = varName;
    }
    
    public Romdd<T> compute() {
      return null;
    }
  }
  
  public Romdd<T> restrict(String varName, String value) {
    return new Restriction<T>(this, varName, value).build();
  }

  public static class Node<T extends Comparable<? super T>> extends Romdd<T> implements Iterable<Romdd<T>> {
    public final Alphabet alpha;

    final ArrayList<Romdd<T>> children;  // These are ordered the same as the alphabet
    
    public Node(Alphabet alpha, ArrayList<Romdd<T>> children) {
      this.alpha = alpha;
      this.children = new ArrayList<Romdd<T>>(children);
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
          rvPtr[0] = alpha.name.compareTo(node.alpha.name);
          if (rvPtr[0] != 0) return;
          rvPtr[0] = LexicalCompare.compare(Node.this, node);
        }
      });
      return rvPtr[0];
    }
    
    public String getName() { return alpha.name; }
  }

  public static class Terminal<T extends Comparable<? super T>> extends Romdd<T> {
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
  
  public static class Builder<T extends Comparable<? super T>> {
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
    
    private Romdd<T> checkCache(Romdd<T> test) {
      Romdd<T> rv = nodes.get(test);
      if (rv == null) {
        nodes.put(test, test);
        rv = test;
      }
      return rv;
    }
    
    private Romdd<T> recurse(final Stack<Alphabet> stack, final Evaluation<T>.Evaluator state) {
      final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
      state.accept(new Evaluation.Visitor<T>() {
        public void visitTerminal(Evaluation<T>.Terminal term) {
          rvPtr.value = checkCache(new Terminal<T>(term.output));
        }
        public void visitWalker(Evaluation<T>.Walker walker) {
          if (stack.isEmpty()) throw new RuntimeException();
          
          Alphabet next = stack.head();
          assert(next.size() > 0);
          DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
          for (Resolver.Atom nextRestriction : next) { 
            children.add(recurse(stack.tail(), state.restrict(nextRestriction)));
          }
          rvPtr.value = children.isAllSame() ? children.get(0) : checkCache(new Node<T>(next, children));          
        }
      });
      return rvPtr.value;
    }
  }
  
  public static interface Visitor<T extends Comparable<? super T>> {
    public void visitTerminal(Terminal<T> term);
    public void visitNode(Node<T> node);
  }

  private static class Application<T extends Comparable<? super T>> {
    public final Op<T> operation;
    public final Romdd<T> f;
    public final Romdd<T> g;
    
    TreeMap<Pair<Romdd<T>>, Romdd<T>> pairCache;
    TreeMap<Romdd<T>, Romdd<T>> nodeCache;
    
    public Application(Op<T> operation, Romdd<T> f, Romdd<T> g) {
      this.operation = operation;
      this.f = f;
      this.g = g;
      this.pairCache = new TreeMap<Pair<Romdd<T>>, Romdd<T>>();
      this.nodeCache = new TreeMap<Romdd<T>, Romdd<T>>();
    }
    
    public Romdd<T> apply() {
      return recurse(f, g);
    }
    
    public Romdd<T> recurse(final Romdd<T> left, final Romdd<T> right) {
      Pair<Romdd<T>> pair = new Pair<Romdd<T>>(left, right);
      Romdd<T> rv = pairCache.get(pair);
      if (rv == null) {
        final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
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
    
    public Romdd<T> checkCache(Romdd<T> test) {
      Romdd<T> rv = nodeCache.get(test);
      if (rv == null) {
        nodeCache.put(test, test);
        rv = test;
      }
      return rv;
    }
    
    public Romdd<T> getTerm(T output) {
      return checkCache(new Terminal<T>(output));
    }
    
    public Romdd<T> expandLeft(final Node<T> left, final Romdd<T> right) {
      return expandOne(left, right, true);
    }
    public Romdd<T> expandRight(final Romdd<T> left, final Node<T> right) {
      return expandOne(right, left, false);
    }
    public Romdd<T> expandOne(final Node<T> splitter, final Romdd<T> node, boolean splitterLeft) {
      DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
      for (Romdd<T> child : splitter) {
        children.add(splitterLeft ? recurse(child, node) : recurse(node, child));
      }
      return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(splitter.alpha, children));
    }

    public Romdd<T> expandBoth(final Node<T> left, final Node<T> right) {
      assert(left.alpha.compareTo(right.alpha) == 0);
      DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
      for (int i = 0; i < left.getSize(); i++) {
        children.add(recurse(left.getChild(i), right.getChild(i)));
      }
      return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(left.alpha, children));
    }
  }

  private static class Restriction<T extends Comparable<? super T>> {
    public final Romdd<T> root;
    public final String varName;
    public final String value;
    
    final TreeMap<Romdd<T>, Romdd<T>> cache;
    
    public Restriction(Romdd<T> root, String varName, String value) {
      this.root = root;
      this.varName = varName;
      this.value = value;
      this.cache = new TreeMap<Romdd<T>,Romdd<T>>();
    }
    
    public Romdd<T> checkCache(Romdd<T> test) {
      Romdd<T> rv = cache.get(test);
      if (rv == null) {
        cache.put(test, test);
        rv = test;
      }
      return rv;
    }
    
    public Romdd<T> build() {
      return recurse(root);
    }
    
    private Romdd<T> recurse(Romdd<T> next) {
      final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
      next.accept(new Visitor<T>() {
        public void visitNode(Node<T> node) {
          if (node.getName().equals(varName)) {
            rvPtr.value = recurse(node.getChild(node.alpha.indexOf(value)));
          } else {
            DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
            for (Romdd<T> child : node) {
              children.add(recurse(child));
            }
            rvPtr.value = children.isAllSame() ? children.get(0) : checkCache(new Node<T>(node.alpha,children));
          }
        }
        public void visitTerminal(Terminal<T> term) {
          rvPtr.value = checkCache(term);
        }
      });
      return rvPtr.value;
    }
  }
  
  static class DiffTrackingArrayList<T extends Comparable<? super T>> extends ArrayList<T> {
    private static final long serialVersionUID = -4941485083660554847L;

    private boolean foundDifference = false;
    private T prototype = null;

    public boolean add(T elt) { 
      if (!foundDifference) {
        if (size() == 0) {
          prototype = elt;
        } else {
          if (prototype.compareTo(elt) != 0) foundDifference = true;
        }
      }
      return super.add(elt);
    }
    
    public boolean isAllSame() {
      return !foundDifference;
    }
  }
}