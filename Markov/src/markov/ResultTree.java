package markov;

import java.util.HashMap;

import util.Stack;

public abstract class ResultTree {
  public static ResultTree create(Stack<Alphabet> alphas) {
    return new Node(alphas);
  }
  public static ResultTree create(Dictionary dict, Stack<Integer> alphas) {
    return new Node(dict, alphas);
  }
  private ResultTree() {}
  public abstract void increment(Stack<String> choices, double amount);
  public abstract void accept(Visitor v);
  public String toCountString(int total) {
    StringBuilder builder = new StringBuilder();
    computeCountString(builder, Stack.<Choice>emptyInstance(), total);
    return builder.toString();
  }
  public String toString() {
    StringBuilder builder = new StringBuilder();
    computeString(builder, Stack.<Choice>emptyInstance());
    return builder.toString();
  }
  protected abstract void computeString(StringBuilder builder, Stack<Choice> alphas);
  protected abstract void computeCountString(StringBuilder builder, Stack<Choice> alphas, int total);
  
  public static class Node extends ResultTree {
    public final Alphabet alpha;
    HashMap<String, ResultTree> children;
    
    public Node(Stack<Alphabet> alphas) {
      if (alphas.isEmpty()) throw new RuntimeException();
      
      alpha = alphas.head();
      Stack<Alphabet> tail = alphas.tail();
      children = new HashMap<String,ResultTree>();
      for (int i = 0; i < alpha.size(); i++) {
        children.put(alpha.get(i), tail.isEmpty() ? new Terminal() : new Node(tail));
      }
    }
    
    public Node(Dictionary dict, Stack<Integer> alphas) {
      if (alphas.isEmpty()) throw new RuntimeException();
      
      alpha = dict.getAlpha(alphas.head());
      Stack<Integer> tail = alphas.tail();
      children = new HashMap<String,ResultTree>();
      for (int i = 0; i < alpha.size(); i++) {
        children.put(alpha.get(i), tail.isEmpty() ? new Terminal() : new Node(dict, tail));
      }
    }
    
    public void accept(Visitor v) { v.visitNode(this); }
    
    public void increment(Stack<String> choices, double amount) {
      // this can throw Null if choices is not according to the initial alphabet stack
      children.get(choices.head()).increment(choices.tail(), amount);
    }
    
    public void computeCountString(StringBuilder builder, Stack<Choice> alphas, int total) {
      for (int i = 0; i < alpha.size(); i++) {
        children.get(alpha.get(i)).computeCountString(builder, alphas.push(new Choice(alpha, i)), total);
      }
    }
    public void computeString(StringBuilder builder, Stack<Choice> alphas) {
      for (int i = 0; i < alpha.size(); i++) {
        children.get(alpha.get(i)).computeString(builder, alphas.push(new Choice(alpha, i)));
      }
    }
  }
  
  public static class Terminal extends ResultTree {
    private int count;
    private double value;
    
    public void increment(Stack<String> choices, double amount) {
      if (!choices.isEmpty()) throw new RuntimeException();
      count++;
      value += amount;
    }
    
    public void accept(Visitor v) { v.visitTerminal(this); }
    
    public void computeCountString(StringBuilder builder, Stack<Choice> alphas, int total) {
      builder.append(alphas).append(' ').append(count).append('/').append(total).append(' ').append(100.*((double)count/(double)total)).append("%\n");
    }
    public void computeString(StringBuilder builder, Stack<Choice> alphas) {
      builder.append(alphas).append(' ').append(100*value).append("%\n");
    }
  }
  
  public static interface Visitor {
    public void visitNode(Node node);
    public void visitTerminal(Terminal term);
  }

  private static class Choice {
    public final Alphabet alpha;
    public final int choice;
    
    public Choice(Alphabet alpha, int choice) {
      this.alpha = alpha;
      this.choice = choice;
    }
    
    public String toString() {
      return alpha.name.toString("::") + "=" + alpha.get(choice);
    }
  }
}
