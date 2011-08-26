package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import markov.DecisionTree;
import markov.Evaluation;
import markov.FractionProbability;
import markov.Machine;
import markov.Net;
import markov.Predicate;
import markov.State;
import markov.TransitionMatrix;
import markov.TransitionVector;
import parser.XmlParser;
import util.Ptr;

public class TestSimulate {
  
  public static void main(String args[]) {
    
    Net<FractionProbability> net=XmlParser.XmlInput("xml/umlVersion2.xml");

    Iterator<Machine<FractionProbability>> itr=net.iterator();
    
    Machine<FractionProbability> machineA= itr.next();
    String machineAName=machineA.name;    
    Machine<FractionProbability> machineB= itr.next();
    String machineBName=machineB.name;    
    HashMap<String,Integer> stateNameList=new HashMap<String, Integer>();
    
    int count=0;
    Iterator<State<FractionProbability>> itrAState= machineA.iterator();
   
    while (itrAState.hasNext()){
      State<FractionProbability> temp = itrAState.next();
      Iterator<State<FractionProbability>> itrBState= machineB.iterator();
      while(itrBState.hasNext()){
        stateNameList.put((temp.name+Machine.MULTIPLY_STRING+itrBState.next().name),new Integer(count));
        count++;
      }
    }
    
    TransitionMatrix.RandomBuilder<FractionProbability> builder =new TransitionMatrix.RandomBuilder<FractionProbability>(machineA.getStateNum()*machineB.getStateNum());

    
    itrAState= machineA.iterator();
    while (itrAState.hasNext()){
      State<FractionProbability> Astate =itrAState.next();
      DecisionTree<TransitionVector<FractionProbability>> decisionTreeA=Astate.getTransitionFunction();
      Evaluation<TransitionVector<FractionProbability>> eva=new Evaluation<TransitionVector<FractionProbability>>(net.dictionary,decisionTreeA);
      Evaluation<TransitionVector<FractionProbability>>.Evaluator root=eva.root;

      
      Iterator<State<FractionProbability>> itrBState= machineB.iterator();
      while(itrBState.hasNext()){
        count--;
        State<FractionProbability> Bstate =itrBState.next();
        Iterator<String> itrLabel=Bstate.labelNameIterator();
        while(itrLabel.hasNext()){
          String labelName=itrLabel.next();
          String instance=Bstate.getLabel(labelName);
          Predicate.Atom atom=new Predicate.Atom(machineBName,labelName,instance);
          root=root.restrict(atom);      
        }
        
        final Ptr<TransitionVector<FractionProbability>> ptrA=new Ptr<TransitionVector<FractionProbability>>();
        root.accept(new Evaluation.Visitor<TransitionVector<FractionProbability>>() {

          public void visitTerminal(Evaluation<TransitionVector<FractionProbability>>.Terminal term) {
            ptrA.value=term.output;
          }

          public void visitWalker(Evaluation<TransitionVector<FractionProbability>>.Walker walker) {
            ptrA.value=null;
            System.err.println("End up in walker!!");
          }
        });
        
        Evaluation<TransitionVector<FractionProbability>>.Evaluator rootB=(new Evaluation<TransitionVector<FractionProbability>>(net.dictionary,Bstate.getTransitionFunction())).root;
        itrLabel=Astate.labelNameIterator();
        while(itrLabel.hasNext()){
          String labelName=itrLabel.next();
          String instance=Astate.getLabel(labelName);
          Predicate.Atom atom=new Predicate.Atom(machineAName,labelName,instance);
          rootB=rootB.restrict(atom);      
        }
        final Ptr<TransitionVector<FractionProbability>> ptrB=new Ptr<TransitionVector<FractionProbability>>();
        rootB.accept(new Evaluation.Visitor<TransitionVector<FractionProbability>>() {

          public void visitTerminal(Evaluation<TransitionVector<FractionProbability>>.Terminal term) {
            ptrB.value=term.output;
          }

          public void visitWalker(Evaluation<TransitionVector<FractionProbability>>.Walker walker) {
            ptrB.value=null;
            System.err.println("End up in walker!!");
          }
        });
        
        int rowNum=stateNameList.get(Astate.name+Machine.MULTIPLY_STRING+Bstate.name);
        TransitionVector<FractionProbability> out=ptrA.value.times(ptrB.value);
        Iterator<Map.Entry<String, FractionProbability>> itrMergedLabel=out.iterator();
        ArrayList<FractionProbability> row=new ArrayList<FractionProbability>(machineA.getStateNum()*machineB.getStateNum());
        
        for (int i=0;i<machineA.getStateNum()*machineB.getStateNum();i++){
          row.add(i, FractionProbability.ZERO);
        }
        
        while(itrMergedLabel.hasNext()){
          Entry<String, FractionProbability> temp = itrMergedLabel.next();
          int colNum=(stateNameList.get(temp.getKey())!=null)?stateNameList.get(temp.getKey()):-1;
          if (colNum>-1) row.set(colNum, temp.getValue());
        }
        builder.set(rowNum, row);

      }

    }
    
    TransitionMatrix<FractionProbability> matrix = builder.build();
    System.out.println(matrix.toString());
    System.out.println(stateNameList.toString());
    
  }
  


}



