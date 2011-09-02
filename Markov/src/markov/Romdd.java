package markov;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import util.LexicalCompare;
import util.Pair;
import util.Partition;
import util.Ptr;

/* T corresponds to terminal values. */
public abstract class Romdd<T extends Comparable<? super T>> implements Comparable<Romdd<T>> {
  public final static Romdd.Terminal<Boolean> TRUE = new Romdd.Terminal<Boolean>(Boolean.TRUE);
  public final static Romdd.Terminal<Boolean> FALSE = new Romdd.Terminal<Boolean>(Boolean.FALSE);
  
  public final static Op<Boolean> AND = AndOp.INSTANCE;
  public final static Op<Boolean> OR = OrOp.INSTANCE;
  public final static Mapping<Boolean,Boolean> INVERT = Invert.INSTANCE;

  private Romdd() { }
  public abstract void accept(Visitor<T> visitor);

  // these break the visitor pattern and should be used sparingly
  abstract Alphabet getAlphabet();
  abstract T getOutput(); 
  
  public static interface Op<T extends Comparable<? super T>> {
    public T apply(T v1, T v2);
    public boolean isDominant(T value);
  }
  
  public static interface Mapping<T extends Comparable<? super T>, S extends Comparable<? super S>> {
    public S transform(T input);
  }
  
  public static <T extends Comparable<? super T>> Romdd<T> branch(final Romdd<Boolean> condition, final Romdd<T> consequent, final Romdd<T> alternative) {
    final class Brancher extends RomddCacher<T> {
      Romdd<T> branch() {
        return recurse(condition, consequent, alternative);
      }
      Romdd<T> recurse(Romdd<Boolean> pred, Romdd<T> cons, Romdd<T> alt) {
        Alphabet alpha = findMinAlpha(pred, cons, alt);
        if (alpha == null) {  // implies all searches at terminal
          return checkCache(pred.getOutput() ? cons : alt); 
        } else {
          DiffTrackingArrayList<Romdd<T>> newChildren = new DiffTrackingArrayList<Romdd<T>>();
          for (int i = 0; i < alpha.size(); i++) {
            newChildren.add(recurse(choose(alpha, i, pred), choose(alpha, i, cons), choose(alpha, i, alt)));
          }
          return newChildren.isAllSame() ? newChildren.get(0) : checkCache(new Node<T>(alpha, newChildren));
        }
      }
      <S extends Comparable<? super S>> Romdd<S> choose(final Alphabet alpha, final int idx, Romdd<S> romdd) {
        final Ptr<Romdd<S>> rvPtr = new Ptr<Romdd<S>>();
        romdd.accept(new Visitor<S>() {
          public void visitTerminal(Terminal<S> term) {
            rvPtr.value = term;
          }
          public void visitNode(Node<S> node) {
            rvPtr.value = alpha.name.equals(node.alpha.name) ? node.getChild(idx) : node;
          }
        });
        return rvPtr.value;
      }
      Alphabet findMinAlpha(Romdd<?> ... romdds) {
        Alphabet rv = null;
        for (Romdd<?> romdd : romdds) {
          Alphabet test = romdd.getAlphabet();
          if (test != null && (rv == null || test.compareTo(rv) < 0)) {
            rv = test;
          }
        }
        return rv;
      }
    }
    return new Brancher().branch();
  }
  
  public TreeSet<String> listVarNames() {
    TreeSet<String> rv = new TreeSet<String>();
    recurseVarNames(rv);
    return rv;
  }
  
  public <S extends Comparable<? super S>> Romdd<S> remap(final Mapping<T,S> mapper) { 
    final class Remapper extends RomddCacher<S >{
      Romdd<S> remap() {
        return recurse(Romdd.this);
      }
      Romdd<S> recurse(Romdd<T> input) {
        final Ptr<Romdd<S>> rvPtr = new Ptr<Romdd<S>>();
        input.accept(new Visitor<T>() {
          public void visitTerminal(Terminal<T> term) {
            rvPtr.value = checkCache(new Terminal<S>(mapper.transform(term.output)));
          }
          public void visitNode(Node<T> node) {
            DiffTrackingArrayList<Romdd<S>> newChildren = new DiffTrackingArrayList<Romdd<S>>();
            for (Romdd<T> child : node) {
              newChildren.add(recurse(child));
            }
            rvPtr.value = newChildren.isAllSame() ? newChildren.get(0) : checkCache(new Node<S>(node.alpha, newChildren));
          }
        });
        return rvPtr.value;
      }
    }
    return new Remapper().remap();
  }
  
  void recurseVarNames(TreeSet<String> accu) {}
  
  public TreeSet<String> findChildrenReaching(final String varName, final T value) {
    final TreeSet<String> rv = new TreeSet<String>();
    final class Finder {
      final TreeSet<Node<T>> cache = new TreeSet<Node<T>>();
      void find() {
        findVar(Romdd.this);
      }
      void findVar(Romdd<T> romdd) {
        romdd.accept(new Visitor<T>() {
          public void visitTerminal(Terminal<T> term) { }
          public void visitNode(Node<T> node) {
            if (cache.contains(node)) return;
            cache.add(node);

            if (node.alpha.name.equals(varName)) {
              for (int i = 0; i < node.alpha.size(); i++) {
                isValueReachable(node.alpha.get(i), node.getChild(i));
              }
            } else {
              for (Romdd<T> child : node) {
                findVar(child);
              }
            }
          }
        });
      }
      void isValueReachable(final String choice, Romdd<T> next) {
        next.accept(new Visitor<T>() {
          public void visitTerminal(Terminal<T> term) {
            if (term.output.equals(value)) {
              rv.add(choice);
            }
          }
          public void visitNode(Node<T> node) {
            for (Romdd<T> child : node) {
              isValueReachable(choice, child);
            }
          }
        });
      }
    }
    new Finder().find();
    return rv;
  }

  public static <T extends Comparable<? super T>> Romdd<T> apply(Op<T> op, Romdd<T> f, Romdd<T> g) {
    class Application extends RomddCacher<T> {
      public final Op<T> operation;
      public final Romdd<T> f;
      public final Romdd<T> g;
      
      TreeMap<Pair<Romdd<T>>, Romdd<T>> pairCache;
      
      public Application(Op<T> operation, Romdd<T> f, Romdd<T> g) {
        this.operation = operation;
        this.f = f;
        this.g = g;
        this.pairCache = new TreeMap<Pair<Romdd<T>>, Romdd<T>>();
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
    return new Application(op, f, g).apply();
  }
  
  public Romdd<T> sum(Op<T> op, String varName) {
    final class Summation extends RomddCacher<T> {
      public final Op<T> operation;
      public final String varName;
      
      public Summation(Op<T> operation, String varName) {
        this.operation = operation;
        this.varName = varName;        
      }

      public Romdd<T> compute() {
        final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
        accept(new Visitor<T>() {
          public void visitTerminal(Terminal<T> term) {
            rvPtr.value = term;
          }
          public void visitNode(Node<T> node) {
            rvPtr.value = recurse(node);
          }
        });
        return rvPtr.value;
      }
      
      public Romdd<T> recurse(Node<T> node) {
        if (node.alpha.name.equals(varName)) {
          final Partition.Builder<Node<T>> builder = new Partition.Builder<Node<T>>(NodeAlphaComparator.INSTANCE);
          final Ptr<T> wPtr = new Ptr<T>();
          for (Romdd<T> child : node) {
            child.accept(new Visitor<T>() {
              public void visitTerminal(Terminal<T> term) {
                wPtr.value = wPtr.value == null ? term.output : operation.apply(wPtr.value, term.output);
              }
              public void visitNode(Node<T> node) {
                builder.add(node);
              }
            });
          }
          return recurse(builder.build(), wPtr.value);
        } else {
          final DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
          for (Romdd<T> child : node) {
            child.accept(new Visitor<T>() {
              public void visitTerminal(Terminal<T> term) {
                children.add(term);
              }
              public void visitNode(Node<T> node) {
                children.add(recurse(node));
              }
            });
          }
          return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(node.alpha, children));
        }
      }

      // Must deal with possibly null weight, size 0 partitions.
      // Each of the nodes reaching this part of the recursion are summed together, though branching is permitted.
      public Romdd<T> recurse(Partition<Node<T>> nodes, T weight) {
        if (nodes.size() == 0 || ((weight != null) && operation.isDominant(weight))) {
          if (weight == null) throw new RuntimeException();
          return checkCache(new Terminal<T>(weight));
        }
        Partition<Node<T>>.Block block = nodes.getBlock(0);
        
        final DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
        Alphabet alpha = nodes.get(0).alpha;
        for (int i = 0; i < alpha.size(); i++) {
          final Ptr<T> wPtr = new Ptr<T>(weight);
          final Partition.Builder<Node<T>> builder = new Partition.Builder<Node<T>>(NodeAlphaComparator.INSTANCE);
          for (int j = block.start; j <= block.end; j++) {
            nodes.get(j).getChild(i).accept(new Visitor<T>() {
              public void visitTerminal(Terminal<T> term) {
                wPtr.value = wPtr.value == null ? term.output : operation.apply(wPtr.value, term.output);
              }
              public void visitNode(Node<T> node) {
                builder.add(node);
              }
            });
          }
          for (int j = block.end+1; j < nodes.size(); j++) {
            builder.add(nodes.get(j));
          }
          children.add(recurse(builder.build(), wPtr.value));
        }
        return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(alpha, children));
      }
    }
    return new Summation(op, varName).compute();
  }

  public Romdd<T> restrict(String varName, String value) {
    class Restriction extends RomddCacher<T> {
      public final String varName;
      public final String value;
            
      public Restriction(String varName, String value) {
        this.varName = varName;
        this.value = value;
      }
      
      public Romdd<T> build() {
        return recurse(Romdd.this);
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
    return new Restriction(varName, value).build();
  }

  public static class Node<T extends Comparable<? super T>> extends Romdd<T> implements Iterable<Romdd<T>> {
    public final Alphabet alpha;

    final ArrayList<Romdd<T>> children;  // These are ordered the same as the alphabet
    
    public Node(Alphabet alpha, ArrayList<Romdd<T>> children) {
      this.alpha = alpha;
      this.children = new ArrayList<Romdd<T>>(children);
    }
    
    T getOutput() { return null; }
    Alphabet getAlphabet() { return alpha; }
    public Romdd<T> getChild(int i) { return children.get(i); }
    public int getSize() { return alpha.size(); }
    public Iterator<Romdd<T>> iterator() { return children.iterator(); }
    
    public void accept(Visitor<T> visitor) { visitor.visitNode(this); }
    
    void recurseVarNames(TreeSet<String> accu) {
      accu.add(alpha.name);
      for (Romdd<T> child : children) {
        child.recurseVarNames(accu);
      }
    }

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
    
    Alphabet getAlphabet() { return null; }
    T getOutput() { return output; }

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
    
    public String toString() { return output.toString(); }
  }

  public static interface Visitor<T extends Comparable<? super T>> {
    public void visitTerminal(Terminal<T> term);
    public void visitNode(Node<T> node);
  }

  public static class NodeAlphaComparator implements Comparator<Node<?>> {
    public final static NodeAlphaComparator INSTANCE = new NodeAlphaComparator();
    
    private NodeAlphaComparator() {}
    public int compare(Node<?> first, Node<?> second) {
      return first.alpha.compareTo(second.alpha);
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
 
  private static class AndOp implements Op<Boolean> {
    public final static AndOp INSTANCE = new AndOp();

    private AndOp() {}

    public Boolean apply(Boolean v1, Boolean v2) { return v1 && v2; }

    public boolean isDominant(Boolean value) { return !value; }
  }
  private static class OrOp implements Op<Boolean> {
    public final static OrOp INSTANCE = new OrOp();

    private OrOp() {}

    public Boolean apply(Boolean v1, Boolean v2) { return v1 || v2; }

    public boolean isDominant(Boolean value) { return value; }
  }

  private static class Invert implements Mapping<Boolean, Boolean> {
    public final static Invert INSTANCE = new Invert();
    
    private Invert() {}
    public Boolean transform(Boolean input) { return !input; }
  }
  
  private static class RomddCacher<T extends Comparable<? super T>> {
    private TreeMap<Romdd<T>, Romdd<T>> cache = new TreeMap<Romdd<T>, Romdd<T>>();
    
    public Romdd<T> checkCache(Romdd<T> test) {
      Romdd<T> rv = cache.get(test);
      if (rv == null) {
        rv = test;
        cache.put(test, test);
      }
      return rv;
    }
  }
}