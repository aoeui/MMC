package test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import markov.DecisionTree;
import markov.FractionProbability;
import markov.Machine;
import markov.Net;
import markov.Romdd;
import markov.State;
import markov.TransitionMatrix;
import markov.TransitionVector;
import parser.XmlParser;
import util.Ptr;

public class TestSimulate {
  public TreeMap<String,Integer> headlist;
  public TransitionMatrix<FractionProbability> matrix;
  
  
  public TestSimulate(String xmlFileName){
    Net<FractionProbability> net=XmlParser.XmlInput(xmlFileName);

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
      Romdd<TransitionVector<FractionProbability>> eva=decisionTreeA.toRomdd(net.dictionary);
      
      Iterator<State<FractionProbability>> itrBState= machineB.iterator();
      while(itrBState.hasNext()){
        count--;
        State<FractionProbability> Bstate =itrBState.next();
        Iterator<String> itrLabel=Bstate.labelNameIterator();
        while(itrLabel.hasNext()){
          String labelName=itrLabel.next();
          String instance=Bstate.getLabel(labelName);
          eva=eva.restrict(machineBName + "." + labelName, instance);      
        }

        final Ptr<TransitionVector<FractionProbability>> ptrA=new Ptr<TransitionVector<FractionProbability>>();
        eva.accept(new Romdd.Visitor<TransitionVector<FractionProbability>>() {

          public void visitTerminal(Romdd.Terminal<TransitionVector<FractionProbability>> term) {
            ptrA.value=term.output;
          }

          public void visitNode(Romdd.Node<TransitionVector<FractionProbability>> walker) {
            ptrA.value=null;
            System.err.println("End up in walker!!");
          }
        });
        
        Romdd<TransitionVector<FractionProbability>> rootB=Bstate.getTransitionFunction().toRomdd(net.dictionary);
        itrLabel=Astate.labelNameIterator();
        while(itrLabel.hasNext()){
          String labelName=itrLabel.next();
          String instance=Astate.getLabel(labelName);
          rootB=rootB.restrict(machineAName +"." + labelName, instance);      
        }
        final Ptr<TransitionVector<FractionProbability>> ptrB=new Ptr<TransitionVector<FractionProbability>>();
        rootB.accept(new Romdd.Visitor<TransitionVector<FractionProbability>>() {

          public void visitTerminal(Romdd.Terminal<TransitionVector<FractionProbability>> term) {
            ptrB.value=term.output;
          }

          public void visitNode(Romdd.Node<TransitionVector<FractionProbability>> walker) {
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
    this.matrix = builder.build();

    
    ValueComparator bvc =  new ValueComparator(stateNameList);
    this.headlist = new TreeMap<String, Integer>(bvc);
    headlist.putAll(stateNameList);

    System.out.println(headlist.keySet().toString());
    System.out.println(matrix.toString()); 
  }
  
  
    public static void main(String args[]) {
      System.out.println(new TestSimulate("xml/umlVersion2.xml"));
    }
    
  class ValueComparator implements Comparator<String>{

    Map<String, Integer> base;
    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    public int compare(String key1, String key2) {
      if((Integer)base.get(key1) < (Integer)base.get(key2)) {
        return -1;
      } else if((Integer)base.get(key1) == (Integer)base.get(key2)) {
        return 0;
      } else {
        return 1;
      }

    }
    
  }
  


}



