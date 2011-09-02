package markov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.jmatio.io.*;
import com.jmatio.types.*;


import markov.TransitionMatrix.RandomBuilder;
import parser.XmlParser;
import util.Ptr;
import util.Stack;

public class Simulation {
  
  public TreeMap<String,Integer> headlist;
  public TransitionMatrix<FractionProbability> matrix;
  
  public static void main(String[] args) {
    System.out.println(new Simulation("xml/umlVersion3.xml"));
  }

  public Simulation(String xmlFileName){
      Net<FractionProbability> net=XmlParser.XmlInput(xmlFileName);

      Iterator<Machine<FractionProbability>> itr=net.iterator();
      
      /****** Initialization ***********************************************************
       * 1. Order Machines by machineName and create iterator based on ordered machines
       * 2. Retrieve stateNameList to assign combined state a count up number to be used in transition Matrix mapping
       * 3. Initialize stack with every machines to be used in recursive combined state retrieval
       * 4. Get a list of machine names
       * **************************************************************************/
      //sort machines
      TreeMap<String,Machine<FractionProbability>> sortList=new TreeMap<String,Machine<FractionProbability>>();
      while (itr.hasNext()){
        Machine<FractionProbability> temp=itr.next();
        sortList.put(temp.name,temp);
      }
      itr=sortList.values().iterator();
      
      HashMap<String,Integer> stateNameList=new HashMap<String, Integer>();
      Stack<Machine<FractionProbability>> stack= Stack.<Machine<FractionProbability>>emptyInstance();
      ArrayList<State<FractionProbability>> stateArray=new ArrayList<State<FractionProbability>>();
      HashSet<ArrayList<State<FractionProbability>>> accu=new HashSet<ArrayList<State<FractionProbability>>>();
      ArrayList<String> machineName=new ArrayList<String>();
      
      Machine<FractionProbability>tempB=null;
      Machine<FractionProbability>tempA=itr.next();
      stack=stack.push(tempA);
      machineName.add(tempA.name);
      
      while (itr.hasNext()){
        tempB=itr.next();
        //create stateNameList
        initializeStates(tempA,tempB,stateNameList);
        //create stack of machines for use in constructStateList
        stack=stack.push(tempB);
        //create machineName
        machineName.add(tempB.name);
        tempA=tempB;
      }
      //order the stack so that stack.head is the same one as the first one that get pushed on the the stack
      stack=stack.reverse();
      
      //get combinedState and return them in accu
      constructStateList(stateArray,stack,accu);
      
      TransitionMatrix.RandomBuilder<FractionProbability> builder =new TransitionMatrix.RandomBuilder<FractionProbability>(stateNameList.size());
      
      Iterator<ArrayList<State<FractionProbability>>> itrStates=accu.iterator();
      while(itrStates.hasNext()){
        //now get probability and save it to the correct row of transition Matrix based on ordering of stateNameList
        retrieveProbability(machineName,(itrStates.next()),net.dictionary, stateNameList, builder);
      }

      this.matrix = builder.build();
      
      //sort the head to be printed.
      ValueComparator bvc =  new ValueComparator(stateNameList);
      this.headlist = new TreeMap<String, Integer>(bvc);
      headlist.putAll(stateNameList);

      System.out.println(headlist.keySet().toString());
      System.out.println(matrix.toString()); 
      
      int[] test={1,2,3,4};
      double[][] matOut=this.matrixToDouble();
      MLDouble mldouble=new MLDouble("pTransition",matOut);
      String[] heads=headlist.keySet().toString().split(", ");
      MLCell mlcell=new MLCell("heads", new int[]{headlist.keySet().size(), 1} );
      
      for(int colNum=0;colNum<headlist.keySet().size();colNum++){
        MLChar temp=new MLChar("text",heads[colNum]);
        mlcell.set(temp, colNum);
      }
      
      //write arrays to file
      ArrayList list = new ArrayList();
      list.add( mldouble );
      list.add( mlcell );
      
      try {
        new MatFileWriter( "mat_file.mat", list );
      } catch (IOException e) {
        e.printStackTrace();
      }
   

  }
  
  private double[][] matrixToDouble() {
    
    double[][] out=new double[this.matrix.N][this.matrix.N];
    
    for (int i=0;i<this.matrix.N;i++){
      for (int j=0;j<this.matrix.N;j++){
        out[i][j]=(double)this.matrix.get(i, j).p.num / (double)this.matrix.get(i, j).p.den;
      }
    }
    return out;
  }

  private void retrieveProbability(
      ArrayList<String> machineName,
      ArrayList<State<FractionProbability>> states, Dictionary dictionary, HashMap<String, Integer> stateNameList, RandomBuilder<FractionProbability> builder) {
    
    ArrayList<Ptr<TransitionVector<FractionProbability>>> ptrs=new ArrayList<Ptr<TransitionVector<FractionProbability>>>();
    TransitionVector<FractionProbability> out=null;
    String combinedStateName="empty";
    
    for (int machineCounter=0;machineCounter<machineName.size();machineCounter++){
      ArrayList<State<FractionProbability>> stateCopy= new ArrayList<State<FractionProbability>>(states);
      ArrayList<String> machineNameCopy=new ArrayList<String>(machineName);
      
      State<FractionProbability> Astate =states.get(machineCounter);
      DecisionTree<TransitionVector<FractionProbability>> decisionTreeA=Astate.getTransitionFunction();
      Romdd<TransitionVector<FractionProbability>> eva = decisionTreeA.toRomdd(dictionary);
      Romdd<TransitionVector<FractionProbability>> root = eva;
      
      stateCopy.remove(machineCounter);
      machineNameCopy.remove(machineCounter);
    
      for(int i=0;i<stateCopy.size();i++){
        State<FractionProbability> s=stateCopy.get(i);
        Iterator<String> itrLabel=s.labelNameIterator();
        while(itrLabel.hasNext()){
          String labelName=itrLabel.next();
          String instance=s.getLabel(labelName);
          root=root.restrict(machineNameCopy.get(i) + "." + labelName, instance);
        }
      }
      
      final Ptr<TransitionVector<FractionProbability>> ptr=new Ptr<TransitionVector<FractionProbability>>();
      root.accept(new Romdd.Visitor<TransitionVector<FractionProbability>>() {

        public void visitTerminal(Romdd.Terminal<TransitionVector<FractionProbability>> term) {
          ptr.value=term.output;
        }

        public void visitNode(Romdd.Node<TransitionVector<FractionProbability>> walker) {
          ptr.value=null;
          System.err.println("End up in walker!!");
        }
      });
      // assign TransitionVector 
      out=(out==null) ? ptr.value : out.times(ptr.value); 
      ptrs.add(ptr);
    }
    //assign combined state name
    String machineTemp=out.machineName;
    String[] orderedMachine=machineTemp.split(" X ");
    for (String s:orderedMachine){
      combinedStateName=(combinedStateName.equals("empty")) ? states.get(machineName.indexOf(s)).name : combinedStateName+Machine.MULTIPLY_STRING+states.get(machineName.indexOf(s)).name;
    }
    
    int rowNum=stateNameList.get(combinedStateName);
    
    Iterator<Map.Entry<String, FractionProbability>> itrMergedLabel=out.iterator();
    ArrayList<FractionProbability> row=new ArrayList<FractionProbability>();
    
    for (int i=0;i<stateNameList.size();i++){
      row.add(i, FractionProbability.ZERO);
    }
    FractionProbability sum=FractionProbability.ZERO;
    while(itrMergedLabel.hasNext()){
      Entry<String, FractionProbability> temp = itrMergedLabel.next();
      int colNum=(stateNameList.get(temp.getKey())!=null)?stateNameList.get(temp.getKey()):-1;
      if (colNum>-1){
        row.set(colNum, temp.getValue());
        sum=sum.sum(temp.getValue());
      }else{
        System.err.println("Typo in the transitionTree"+temp.getKey());
      }
    }
    if(!sum.isOne()) System.err.println("Row "+rowNum+"sum to "+ sum.toString());
    builder.set(rowNum, row);
    
  }

  private void constructStateList(
      ArrayList<State<FractionProbability>> stateArray,
      Stack<Machine<FractionProbability>> stack, HashSet<ArrayList<State<FractionProbability>>> accu) {
    //output: accu is HashSet which every element is ArrayList of stateArray
    if (stack.isEmpty()){
      accu.add(stateArray);
      return;
    }
    Iterator<State<FractionProbability>> itr=stack.head().iterator();
    while(itr.hasNext()){
      State<FractionProbability> s=itr.next();
      ArrayList<State<FractionProbability>> stateArrayTemp = new ArrayList<State<FractionProbability>>(stateArray);
      stateArrayTemp.add(s);
      constructStateList(stateArrayTemp,stack.tail(),accu);
    }

  }
  
  private void initializeStates(
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
    
      stateNameList.clear();
      stateNameList.putAll(out);
      return;
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
    return;
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
