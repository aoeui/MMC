package markov;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import parser.XmlParser;
import util.Ptr;

public class Simulation {
  
  public TreeMap<String,Integer> headlist;
  public TransitionMatrix<FractionProbability> matrix;
  
  public static void main(String[] args) {
    System.out.println(new Simulation("xml/umlVersion2.xml"));
  }

  public Simulation(String xmlFileName){
      Net<FractionProbability> net=XmlParser.XmlInput(xmlFileName);

      Iterator<Machine<FractionProbability>> itr=net.iterator();
      
      HashMap<String,Integer> stateNameList=new HashMap<String, Integer>();
      
      Machine<FractionProbability>tempB=null;
      Machine<FractionProbability>tempA=itr.next();
      while (itr.hasNext()){
        tempB=itr.next();
        stateNameList=initializeStates(tempA,tempB,stateNameList);
        tempA=tempB;
      }
      
      TransitionMatrix.RandomBuilder<FractionProbability> builder =new TransitionMatrix.RandomBuilder<FractionProbability>(stateNameList.size());

      itr=net.iterator();
      tempA=itr.next();
      tempB=null;
      while (itr.hasNext()){
        tempB=itr.next();
        String machineAName=tempA.name;
        String machineBName=tempB.name;

        
        
        
        tempA=tempB;
      }
      
      
      String machineAName=tempA.name;
      String machineBName=tempB.name;
      Iterator<State<FractionProbability>> itrAState = tempA.iterator();
      while (itrAState.hasNext()){
        State<FractionProbability> Astate =itrAState.next();
        DecisionTree<TransitionVector<FractionProbability>> decisionTreeA=Astate.getTransitionFunction();
        Evaluation<TransitionVector<FractionProbability>> eva=new Evaluation<TransitionVector<FractionProbability>>(net.dictionary,decisionTreeA);
        Evaluation<TransitionVector<FractionProbability>>.Evaluator root=eva.root;

        
        Iterator<State<FractionProbability>> itrBState= tempB.iterator();
        while(itrBState.hasNext()){
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
          ArrayList<FractionProbability> row=new ArrayList<FractionProbability>(tempA.getStateNum()*tempB.getStateNum());
          
          for (int i=0;i<tempA.getStateNum()*tempB.getStateNum();i++){
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
      
      System.out.println(stateNameList.toString());
      

  }

  
  private HashMap<String, Integer> initializeStates(
      Machine<FractionProbability> A,
      Machine<FractionProbability> B,
      HashMap<String, Integer> stateNameList) {
    HashMap<String, Integer> out=new HashMap<String, Integer>();
    
    if (!stateNameList.isEmpty()){

      Iterator<State<FractionProbability>> itrBState= B.iterator();
      int count=0;

      while (itrBState.hasNext()){
        State<FractionProbability> tempB = itrBState.next();
        Iterator<String> itrAState=stateNameList.keySet().iterator();
        while(itrAState.hasNext()){
          String tempAName = itrAState.next();
          out.put((tempAName+Machine.MULTIPLY_STRING+tempB.name),new Integer(count));
          count++;
        }       
      }
    
      return out;
    }
    else {

      Iterator<State<FractionProbability>> itrBState= B.iterator();
      int count=0;

      while (itrBState.hasNext()){
        State<FractionProbability> tempB = itrBState.next();
        Iterator<State<FractionProbability>> itrAState=A.iterator();
      
        while(itrAState.hasNext()){
          
          State<FractionProbability> tempA = itrAState.next();
          stateNameList.put((tempA.name+Machine.MULTIPLY_STRING+tempB.name),new Integer(count));
          count++;
        }       
      }
    
      return stateNameList;
    }
  
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