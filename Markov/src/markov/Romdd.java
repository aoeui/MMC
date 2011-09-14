package markov;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import util.LexicalCompare;
import util.Pair;
import util.Partition;
import util.Ptr;
import util.Stack;

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
  public abstract boolean isTerminal();
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
        final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
        condition.accept(new Visitor<Boolean>() {
          public void visitTerminal(Terminal<Boolean> term) {
            rvPtr.value = term.output ? consequent : alternative;
          }
          public void visitNode(Node<Boolean> node) {
            rvPtr.value = recurse(node.dict, condition, consequent, alternative);
          }
        });
        return rvPtr.value;
      }
      Romdd<T> recurse(Dictionary dict, Romdd<Boolean> pred, Romdd<T> cons, Romdd<T> alt) {
        Integer alpha = findMinAlpha(pred, cons, alt);
        if (alpha == null) {  // implies all searches at terminal
          return checkCache(pred.getOutput() ? cons : alt); 
        } else {
          DiffTrackingArrayList<Romdd<T>> newChildren = new DiffTrackingArrayList<Romdd<T>>();
          for (int i = 0; i < dict.getAlpha(alpha).size(); i++) {
            newChildren.add(recurse(dict, choose(alpha, i, pred), choose(alpha, i, cons), choose(alpha, i, alt)));
          }
          return newChildren.isAllSame() ? newChildren.get(0) : checkCache(new Node<T>(dict, alpha, newChildren));
        }
      }
      <S extends Comparable<? super S>> Romdd<S> choose(final int varId, final int idx, Romdd<S> romdd) {
        final Ptr<Romdd<S>> rvPtr = new Ptr<Romdd<S>>();
        romdd.accept(new Visitor<S>() {
          public void visitTerminal(Terminal<S> term) {
            rvPtr.value = term;
          }
          public void visitNode(Node<S> node) {
            rvPtr.value = varId == node.varId ? node.getChild(idx) : node;
          }
        });
        return rvPtr.value;
      }
      @SuppressWarnings({ "rawtypes", "unchecked" })
      Integer findMinAlpha(Romdd ... romdds) {
        final Ptr<Integer> rvPtr = new Ptr<Integer>();
        for (Romdd romdd : romdds) {
          romdd.accept(new Visitor() {
            public void visitTerminal(Terminal term) { }
            public void visitNode(Node node) {
              if (rvPtr.value == null) {
                rvPtr.value = node.varId;
              } else {
                if (rvPtr.value > node.varId) {
                  rvPtr.value = node.varId;
                }
              }
            }
          });
        }
        return rvPtr.value;
      }
    }
    return new Brancher().branch();
  }
  
  /** Returns a sorted array of the alphabets used by this Romdd */
  public List<Alphabet> listVarNames() {
    final Ptr<List<Alphabet>> rvPtr = new Ptr<List<Alphabet>>();
    accept(new Visitor<T>() {
      public void visitTerminal(Terminal<T> term) {
        rvPtr.value = new ArrayList<Alphabet>();
      }
      public void visitNode(Node<T> node) {
        SortedSet<Integer> vars = listVarIdx();
        rvPtr.value = new ArrayList<Alphabet>(vars.size());
        for (int id : vars) {
          rvPtr.value.add(node.dict.getAlpha(id));
        }
      }
    });
    return rvPtr.value;
  }
  
  public SortedSet<Integer> listVarIdx() {
    final TreeSet<Integer> rvIdx = new TreeSet<Integer>();
    class Lister implements Visitor<T> {
      TreeSet<Romdd<T>> visited = new TreeSet<Romdd<T>>();
      void doList(Romdd<T> romdd) {
        romdd.accept(this);
      }
      public void visitTerminal(Terminal<T> term) { }
      public void visitNode(Node<T> node) {
        if (visited.contains(node)) return;
        visited.add(node);
        rvIdx.add(node.varId);
        for (Romdd<T> child : node) {
          doList(child);
        }
      }
    }
    new Lister().doList(this);
    return rvIdx;
  }
  
  public <S extends Comparable<? super S>> Romdd<S> remap(final Mapping<T,S> mapper) { 
    final class Remapper extends RomddCacher<S >{
      TreeMap<Node<T>,Romdd<S>> mapCache = new TreeMap<Node<T>, Romdd<S>>();
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
            rvPtr.value = mapCache.get(node);
            if (rvPtr.value != null) return;

            DiffTrackingArrayList<Romdd<S>> newChildren = new DiffTrackingArrayList<Romdd<S>>();
            for (Romdd<T> child : node) {
              newChildren.add(recurse(child));
            }
            rvPtr.value = newChildren.isAllSame() ? newChildren.get(0) : checkCache(new Node<S>(node.dict, node.varId, newChildren));
            mapCache.put(node, rvPtr.value);
          }
        });
        return rvPtr.value;
      }
    }
    return new Remapper().remap();
  }
  
  public TreeSet<String> findChildrenReaching(final Stack<String> varName, final T value) {
    final Ptr<TreeSet<String>> rvPtr = new Ptr<TreeSet<String>>();
    accept(new Visitor<T>() {
      public void visitTerminal(Terminal<T> term) {
        rvPtr.value = new TreeSet<String>();
      }
      public void visitNode(Node<T> node) {
        rvPtr.value = findChildrenReaching(node.dict.getId(varName), value);
      }
    });
    return rvPtr.value;
  }
  
  public TreeSet<String> findChildrenReaching(final int varId, final T value) {
    final TreeSet<String> rv = new TreeSet<String>();
    final class Finder {
      final TreeSet<Node<T>> cache1 = new TreeSet<Node<T>>();
      final HashMap<String,TreeSet<Node<T>>> cache2 = new HashMap<String,TreeSet<Node<T>>>();
      void find() {
        findVar(Romdd.this);
      }
      void findVar(Romdd<T> romdd) {
        romdd.accept(new Visitor<T>() {
          public void visitTerminal(Terminal<T> term) { }
          public void visitNode(Node<T> node) {
            if (cache1.contains(node)) return;
            cache1.add(node);

            if (node.varId == varId) {
              for (int i = 0; i < node.getSize(); i++) {
                isValueReachable(node.getAlphabet().get(i), node.getChild(i));
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
            TreeSet<Node<T>> set = cache2.get(choice);
            if (set == null) {
              set = new TreeSet<Node<T>>();
              cache2.put(choice, set);
            }
            if (!set.contains(node)) {
              set.add(node);
              for (Romdd<T> child : node) {
                isValueReachable(choice, child);
              }
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
                  int comp = leftNode.varId - rightNode.varId;
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
        return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(splitter.dict, splitter.varId, children));
      }

      public Romdd<T> expandBoth(final Node<T> left, final Node<T> right) {
        assert(left.varId == right.varId);
        DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
        for (int i = 0; i < left.getSize(); i++) {
          children.add(recurse(left.getChild(i), right.getChild(i)));
        }
        return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(left.dict, left.varId, children));
      }
    }    
    return new Application(op, f, g).apply();
  }

  public Romdd<T> sum(final Op<T> operation, final Stack<String> varName) {
    final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
    accept(new Visitor<T>() {
      public void visitTerminal(Terminal<T> term) {
        rvPtr.value = term;
      }
      public void visitNode(Node<T> node) {
        rvPtr.value = sum(operation, node.dict.getId(varName));
      }
    });
    return rvPtr.value;
  }
  
  public Romdd<T> sum(final Op<T> operation, final int varId) {
    final class Summation extends RomddCacher<T> {
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
        if (node.varId == varId) {
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
          return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(node.dict, node.varId, children));
        }
      }

      // Must deal with possibly null weight, size 0 partitions.
      // Each of the nodes reaching this part of the recursion are summed together, though branching is permitted.
      // This is an implementation of N-way-apply
      // We may  need to add caching for this query
      public Romdd<T> recurse(Partition<Node<T>> nodes, T weight) {
        if (nodes.size() == 0 || ((weight != null) && operation.isDominant(weight))) {
          if (weight == null) throw new RuntimeException();
          return checkCache(new Terminal<T>(weight));
        }
        Partition<Node<T>>.Block block = nodes.getBlock(0);  // the first block has least alpha, it gets branched first
        
        final DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
        final int size = nodes.get(0).getSize();
        for (int i = 0; i < size; i++) {
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
            builder.add(nodes.get(j));  // non-branching nodes copied over
          }
          children.add(recurse(builder.build(), wPtr.value));
        }
        return children.isAllSame() ? children.get(0) : checkCache(new Node<T>(nodes.get(0).dict, nodes.get(0).varId, children));
      }
    }
    return new Summation().compute();
  }

  public Romdd<T> restrict(final Stack<String> varName, final String value) {
    final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
    accept(new Visitor<T>() {
      public void visitTerminal(Terminal<T> term) {
        rvPtr.value = Romdd.this;
      }
      public void visitNode(Node<T> node) {
        rvPtr.value = restrict(node.dict.getId(varName), value);
      }
    });
    return rvPtr.value;
  }
  
  public Romdd<T> restrict(final int varId, final String value) {
    class Restriction extends RomddCacher<T> {      
      TreeMap<Node<T>, Romdd<T>> map = new TreeMap<Node<T>, Romdd<T>>();
      
      private Romdd<T> recurse(Romdd<T> next) {
        final Ptr<Romdd<T>> rvPtr = new Ptr<Romdd<T>>();
        next.accept(new Visitor<T>() {
          public void visitNode(Node<T> node) {
            rvPtr.value = map.get(node);
            if (rvPtr.value != null) return;
            
            if (node.varId == varId) {
              rvPtr.value = recurse(node.getChild(node.getAlphabet().indexOf(value)));
            } else {
              DiffTrackingArrayList<Romdd<T>> children = new DiffTrackingArrayList<Romdd<T>>();
              for (Romdd<T> child : node) {
                children.add(recurse(child));
              }
              rvPtr.value = children.isAllSame() ? children.get(0) : checkCache(new Node<T>(node.dict, node.varId, children));
            }
            map.put(node, rvPtr.value);
          }
          public void visitTerminal(Terminal<T> term) {
            rvPtr.value = checkCache(term);
          }
        });
        return rvPtr.value;
      }
    }
    return new Restriction().recurse(this);
  }

  public static class Node<T extends Comparable<? super T>> extends Romdd<T> implements Iterable<Romdd<T>> {
    public final Dictionary dict;
    public final int varId;

    final ArrayList<Romdd<T>> children;  // These are ordered the same as the alphabet
    
    public Node(Dictionary dict, Stack<String> name, Collection<? extends Romdd<T>> children) {
      this(dict, dict.getId(name), children);
    }
    
    public Node(Dictionary dict, int varId, Collection<? extends Romdd<T>> children) {
      this.dict = dict;
      this.varId = varId;
      if (children.size() != dict.getAlpha(varId).size()) throw new RuntimeException();
      this.children = new ArrayList<Romdd<T>>(children);
    }

    // cheat methods to avoid visitor
    public boolean isTerminal() { return false; }
    T getOutput() { throw new UnsupportedOperationException(); }
    public Alphabet getAlphabet() { return dict.getAlpha(varId); }

    public Romdd<T> getChild(int i) { return children.get(i); }
    public int getSize() { return children.size(); }
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
          rvPtr[0] = varId - node.varId;
          if (rvPtr[0] != 0) return;
          rvPtr[0] = LexicalCompare.compare(Node.this, node);
        }
      });
      return rvPtr[0];
    }    
    public Stack<String> getName() { return getAlphabet().name; }
  }

  public static class Terminal<T extends Comparable<? super T>> extends Romdd<T> {
    public final T output;
    
    public Terminal(T output) {
      this.output = output;
    }
    
    public boolean isTerminal() { return true; }
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
      return first.varId - second.varId;
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