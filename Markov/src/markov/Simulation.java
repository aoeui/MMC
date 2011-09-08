package markov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
  public TransitionMatrix<DoubleProbability> matrix;
  private final String STATE_MULTIPLY_STRING="::";
  
  public static void main(String[] args) {
    System.out.println(new Simulation("xml/umlVersion4.xml"));
  }

  public Simulation(String xmlFileName){
      int numOfPatient=2;
      
      Net<DoubleProbability> net=XmlParser.XmlInput(xmlFileName,numOfPatient);

      Net.Builder<DoubleProbability> netBuild=new Net.Builder<DoubleProbability>();

      Machine<DoubleProbability> dischargeModel=constructDischargeModel(numOfPatient);
      
      Iterator<Machine<DoubleProbability>> tempItr=net.iterator();
      while(tempItr.hasNext()){
        netBuild.addMachine(tempItr.next());
      }
      netBuild.addMachine(dischargeModel);
      net=netBuild.build();

      Iterator<Machine<DoubleProbability>> itr=net.iterator();
      
      /****** Initialization ***********************************************************
       * 1. Order Machines by machineName and create iterator based on ordered machines
       * 2. Retrieve stateNameList to assign combined state a count up number to be used in transition Matrix mapping
       * 3. Initialize stack with every machines to be used in recursive combined state retrieval
       * 4. Get a list of machine names
       * **************************************************************************/
      //sort machines
      TreeMap<String,Machine<DoubleProbability>> sortList=new TreeMap<String,Machine<DoubleProbability>>();
      while (itr.hasNext()){
        Machine<DoubleProbability> temp=itr.next();
        sortList.put(temp.name,temp);
      }
      itr=sortList.values().iterator();
      
      HashMap<String,Integer> stateNameList=new HashMap<String, Integer>();
      Stack<Machine<DoubleProbability>> stack= Stack.<Machine<DoubleProbability>>emptyInstance();
      ArrayList<State<DoubleProbability>> stateArray=new ArrayList<State<DoubleProbability>>();
      HashSet<ArrayList<State<DoubleProbability>>> accu=new HashSet<ArrayList<State<DoubleProbability>>>();
      ArrayList<String> machineName=new ArrayList<String>();
      
      Machine<DoubleProbability>tempB=null;
      Machine<DoubleProbability>tempA=itr.next();
      stack=stack.push(tempA);
      machineName.add(tempA.name);
      
      while (itr.hasNext()){
        tempB=itr.next();
        //create stateNameList
        initializeStates(tempA,tempB,stateNameList);
        //create stack of machines for use in constructStateList
        stack=stack.push(tempB);
        //create list of machineName
        machineName.add(tempB.name);
        tempA=tempB;
      }
      //order the stack so that stack.head is the same as the first one that get pushed on the the stack
      stack=stack.reverse();
      
      //get combinedState and return them in accu
      constructStateList(stateArray,stack,accu);
      
      TransitionMatrix.RandomBuilder<DoubleProbability> builder =new TransitionMatrix.RandomBuilder<DoubleProbability>(stateNameList.size());
      
      
      Iterator<ArrayList<State<DoubleProbability>>> itrStates=accu.iterator();
      int[] costTable=new int[stateNameList.keySet().size()];
      while(itrStates.hasNext()){
        ArrayList<State<DoubleProbability>> states=itrStates.next();
        
        String combinedStateName="empty";
        int sum=0;
        for (State<DoubleProbability> s:states){
          sum+=Integer.parseInt(s.getLabel("Cost"));
          combinedStateName=(combinedStateName.equals("empty")) ? s.name : combinedStateName+Machine.MULTIPLY_STRING+s.name;
        }
        costTable[stateNameList.get(combinedStateName)]=sum;
        //now get probability and save it to the correct row of transition Matrix based on ordering of stateNameList
        retrieveProbability(machineName,states,net.dictionary, stateNameList, builder);
      }

      this.matrix = builder.build();
      
      //sort the head to be printed.
      ValueComparator bvc =  new ValueComparator(stateNameList);
      this.headlist = new TreeMap<String, Integer>(bvc);
      headlist.putAll(stateNameList);

      System.out.println(machineName.toString());
      System.out.println(headlist.keySet().toString());
      System.out.println(matrix.toString());
      
      System.out.println(Arrays.toString(costTable));
      System.out.println(machineName.toString());
      System.out.println(headlist.keySet().toString());
      
      double[][] matOut=this.matrixToDouble();
      MLInt8 mlint=new MLInt8("costVector",costTable);
      MLDouble mldouble=new MLDouble("pTransition",matOut);
      String[] heads=headlist.keySet().toString().split(", ");
      MLCell mlcell=new MLCell("heads", new int[]{headlist.keySet().size(), 1} );
      
      for(int colNum=0;colNum<headlist.keySet().size();colNum++){
        MLChar temp=new MLChar("text",heads[colNum]);
        mlcell.set(temp, colNum);
      }
      
      //write arrays to file
      ArrayList<MLArray> list = new ArrayList<MLArray>();
      list.add( mldouble );
      list.add( mlcell );
      list.add(mlint);
      
      try {
        new MatFileWriter( "mat_file.mat", list );
      } catch (IOException e) {
        e.printStackTrace();
      }
   

  }
  
  private Machine<DoubleProbability> constructDischargeModel(int numOfPatient) {
    final String modelName="DischargeModel";
    int numOfState=(int) Math.pow(2,numOfPatient);
    DecisionTree<TransitionVector<DoubleProbability>> alternative=null;
    ArrayList<String> stateNames=new ArrayList<String>();
    for (int i=0;i<numOfState;i++){
      String stateName="";
      ArrayList<Predicate> predArray=new ArrayList<Predicate>();
      for(int j=0;j<numOfPatient;j++){
        ArrayList<Predicate> tempArray=new ArrayList<Predicate>();
        tempArray.add(new Predicate.Atom("Patient"+j+":ICP", "ICP", "E"));
        tempArray.add(new Predicate.Atom("Patient"+j+":ICP", "ICP", "D"));
        Predicate pred=new Predicate.Or(tempArray);
        if (Math.floor(i/Math.pow(2, j))==0) pred=new Predicate.Neg(pred);
        predArray.add(pred);
        
        String nextStateName=(Math.floor(i/Math.pow(2, j))==0)? (j+"_Occupied"):(j+"_Empty");
        stateName=(stateName.equals(""))? nextStateName : stateName+this.STATE_MULTIPLY_STRING+nextStateName;
        
      }

      Predicate predicate=new Predicate.And(predArray);
      stateNames.add(stateName);
      
      if (alternative==null){
        TransitionVector.Builder<DoubleProbability> b=new TransitionVector.Builder<DoubleProbability>(modelName);
        b.setProbability(stateName, new DoubleProbability(1, 1));
        TransitionVector<DoubleProbability> unused=b.build();
        alternative= new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(unused) ;
      }
      TransitionVector.Builder<DoubleProbability>b=new TransitionVector.Builder<DoubleProbability>(modelName);
      b.setProbability(stateName, new DoubleProbability(1, 1));
      TransitionVector<DoubleProbability> prob=b.build();
      DecisionTree.Terminal<TransitionVector<DoubleProbability>> consequent=new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(prob);
      
      DecisionTree.Branch<TransitionVector<DoubleProbability>> branch=new DecisionTree.Branch<TransitionVector<DoubleProbability>>(predicate, consequent, alternative);
      
      alternative=branch;
    }

    Machine.Builder<DoubleProbability> machineBuild = new Machine.Builder<DoubleProbability>(modelName);
    for(String s:stateNames){
      State.Builder<DoubleProbability> stateBuild=new State.Builder<DoubleProbability>(modelName,s,alternative);
      String[] machines=s.split(this.STATE_MULTIPLY_STRING);
      int value=0;
      for (String sM:machines){
        if (sM.contains("Occupied")) value+=1000;
      }
      
      stateBuild.setLabel("Cost", Integer.toString(value));
      machineBuild.addState(stateBuild.build());
    }
    return machineBuild.build();
  }

  private double[][] matrixToDouble() {
    
    double[][] out=new double[this.matrix.N][this.matrix.N];
    
    for (int i=0;i<this.matrix.N;i++){
      for (int j=0;j<this.matrix.N;j++){
        out[i][j]= this.matrix.get(i, j).p;
      }
    }
    return out;
  }

  private void retrieveProbability(
      ArrayList<String> machineName,
      ArrayList<State<DoubleProbability>> states, Dictionary dictionary, HashMap<String, Integer> stateNameList, RandomBuilder<DoubleProbability> builder) {
    
    ArrayList<Ptr<TransitionVector<DoubleProbability>>> ptrs=new ArrayList<Ptr<TransitionVector<DoubleProbability>>>();
    TransitionVector<DoubleProbability> out=null;
    String combinedStateName="empty";
    
    for (int machineCounter=0;machineCounter<machineName.size();machineCounter++){
      ArrayList<State<DoubleProbability>> stateCopy= new ArrayList<State<DoubleProbability>>(states);
      ArrayList<String> machineNameCopy=new ArrayList<String>(machineName);
      
      State<DoubleProbability> Astate =states.get(machineCounter);
      DecisionTree<TransitionVector<DoubleProbability>> decisionTreeA=Astate.getTransitionFunction();
      Romdd<TransitionVector<DoubleProbability>> eva = decisionTreeA.toRomdd(dictionary);
      Romdd<TransitionVector<DoubleProbability>> root = eva;
      
      stateCopy.remove(machineCounter);
      machineNameCopy.remove(machineCounter);
    
      for(int i=0;i<stateCopy.size();i++){
        State<DoubleProbability> s=stateCopy.get(i);
        Iterator<String> itrLabel=s.labelNameIterator();
        while(itrLabel.hasNext()){
          String labelName=itrLabel.next();
          String instance=s.getLabel(labelName);
          root=root.restrict(machineNameCopy.get(i) + "." + labelName, instance);
        }
      }
      
      final Ptr<TransitionVector<DoubleProbability>> ptr=new Ptr<TransitionVector<DoubleProbability>>();
      root.accept(new Romdd.Visitor<TransitionVector<DoubleProbability>>() {

        public void visitTerminal(Romdd.Terminal<TransitionVector<DoubleProbability>> term) {
          ptr.value=term.output;
        }

        public void visitNode(Romdd.Node<TransitionVector<DoubleProbability>> walker) {
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
    
    Iterator<Map.Entry<String, DoubleProbability>> itrMergedLabel=out.iterator();
    ArrayList<DoubleProbability> row=new ArrayList<DoubleProbability>();
    
    for (int i=0;i<stateNameList.size();i++){
      row.add(i, DoubleProbability.ZERO);
    }
    DoubleProbability sum=DoubleProbability.ZERO;
    while(itrMergedLabel.hasNext()){
      Entry<String, DoubleProbability> temp = itrMergedLabel.next();
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
      ArrayList<State<DoubleProbability>> stateArray,
      Stack<Machine<DoubleProbability>> stack, HashSet<ArrayList<State<DoubleProbability>>> accu) {
    //output: accu is HashSet which every element is ArrayList of stateArray
    if (stack.isEmpty()){
      accu.add(stateArray);
      return;
    }
    Iterator<State<DoubleProbability>> itr=stack.head().iterator();
    while(itr.hasNext()){
      State<DoubleProbability> s=itr.next();
      ArrayList<State<DoubleProbability>> stateArrayTemp = new ArrayList<State<DoubleProbability>>(stateArray);
      stateArrayTemp.add(s);
      constructStateList(stateArrayTemp,stack.tail(),accu);
    }

  }
  
  private void initializeStates(
      Machine<DoubleProbability> A,
      Machine<DoubleProbability> B,
      HashMap<String, Integer> stateNameList) {
    HashMap<String, Integer> out=new HashMap<String, Integer>();
    
    if (!stateNameList.isEmpty()){

      Iterator<State<DoubleProbability>> itrBState= B.iterator();
      int count=0;

      while (itrBState.hasNext()){
        State<DoubleProbability> tempB = itrBState.next();
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

      Iterator<State<DoubleProbability>> itrBState= B.iterator();
      int count=0;

      while (itrBState.hasNext()){
        State<DoubleProbability> tempB = itrBState.next();
        Iterator<State<DoubleProbability>> itrAState=A.iterator();
      
        while(itrAState.hasNext()){
          
          State<DoubleProbability> tempA = itrAState.next();
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
