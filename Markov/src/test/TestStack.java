package test;

import util.Stack;

public class TestStack {
  public static void main(String[] args) {
    Stack<Integer> stack = Stack.<Integer>emptyInstance();
    for (int i = 0; i < 25; i++) {
      stack = stack.push(i);
    }
    System.out.println(stack);
    stack = stack.reverse();
    System.out.println(stack);
  }
}
