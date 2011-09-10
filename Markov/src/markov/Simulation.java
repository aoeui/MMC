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


import markov.DecisionTree.Branch;
import markov.DecisionTree.Terminal;
import markov.TransitionMatrix.RandomBuilder;
import parser.XmlParser;
import util.Ptr;
import util.Stack;

public class Simulation {
  
  public TreeMap<String,Integer> headlist;
  public TransitionMatrix<FractionProbability> matrix;
  private final String STATE_MULTIPLY_STRING="::";
  
  public static void main(String[] args) {
    System.out.println(new Simulation("xml/umlVersion4.xml"));
  }

  public Simulation(String xmlFileName){
      int numOfPatient=2;
      
      Net<FractionProbability> net=XmlParser.XmlInput(xmlFileName,numOfPatient);

      Net.Builder<FractionProbability> netBuild=new Net.Builder<FractionProbability>();

      Machine<FractionProbability> dischargeModel=constructDischargeModel(numOfPatient,net);
      Machine<FractionProbability> arrivalModel=constructArrivalModel(numOfPatient, new FractionProbability(1,4));
      
      Iterator<Machine<FractionProbability>> tempItr=net.iterator();
      while(tempItr.hasNext()){
        netBuild.addMachine(tempItr.next());
      }
      netBuild.addMachine(dischargeModel);
      netBuild.addMachine(arrivalModel);
      net=netBuild.build();

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
        //create list of machineName
        machineName.add(tempB.name);
        tempA=tempB;
      }
      //order the stack so that stack.head is the same as the first one that get pushed on the the stack
      stack=stack.reverse();
      
      //get combinedState and return them in accu
      constructStateList(stateArray,stack,accu);
      
      TransitionMatrix.RandomBuilder<FractionProbability> builder =new TransitionMatrix.RandomBuilder<FractionProbability>(stateNameList.size());
      
      
      Iterator<ArrayList<State<FractionProbability>>> itrStates=accu.iterator();
      int[] costTable=new int[stateNameList.keySet().size()];
      while(itrStates.hasNext()){
        ArrayList<State<FractionProbability>> states=itrStates.next();
        
        String combinedStateName="empty";
        int sum=0;
        for (State<FractionProbability> s:states){
          if (s.getLabel("Cost")!=null) sum+=Integer.parseInt(s.getLabel("Cost"));
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
  
  private Machine<FractionProbability> constructArrivalModel(int numOfPatient,FractionProbability p){
    final String modelName="ArrivalModel";
    TransitionVector.Builder<FractionProbability> b=new TransitionVector.Builder<FractionProbability>(modelName);
    b.setProbability("NoArrival", p);
    b.setProbability("Arrival",new FractionProbability(p.p.den-p.p.num,p.p.den));
    TransitionVector<FractionProbability> pVector=b.build();
    DecisionTree.Terminal<TransitionVector<FractionProbability>> term= new DecisionTree.Terminal<TransitionVector<FractionProbability>>(pVector);
    Machine.Builder<FractionProbability> machineBuild = new Machine.Builder<FractionProbability>(modelName);
    State.Builder<FractionProbability> stateBuild=new State.Builder<FractionProbability>(modelName,"Arrival",term);
    stateBuild.setLabel("Arrival", "T");
    State<FractionProbability> stateA=stateBuild.build();

    stateBuild=new State.Builder<FractionProbability>(modelName,"NoArrival",term);
    stateBuild.setLabel("Arrival", "F");
    State<FractionProbability> stateB=stateBuild.build();
    
    machineBuild.addState(stateA);
    machineBuild.addState(stateB);
    Machine<FractionProbability> out=machineBuild.build();
    return out;
    
  }
  
  
  private Machine<FractionProbability> constructDischargeModel(int numOfPatient, Net<FractionProbability> net) {
    final String modelName="DischargeModel";
    int numOfState=(int) Math.pow(2,numOfPatient);
    DecisionTree<TransitionVector<FractionProbability>> alternative=null;
    ArrayList<String> stateNames=new ArrayList<String>();
    ArrayList<Integer> code=new ArrayList<Integer>();
    //with no arrivals
    for (int i=0;i<numOfState;i++){
      String stateName="";
      ArrayList<Predicate> predArray=new ArrayList<Predicate>();
      for(int j=0;j<numOfPatient;j++){
        ArrayList<Predicate> tempArray=new ArrayList<Predicate>();
        tempArray.add(new Predicate.Atom("Patient"+j+":ICP", "ICP", "E"));
        tempArray.add(new Predicate.Atom("Patient"+j+":ICP", "ICP", "D"));
        Predicate pred=new Predicate.Or(tempArray);
        
        byte stateCode = (byte) ((i & (int)(Math.pow(2, j)))>>j);
        if (stateCode!=0) pred=new Predicate.Neg(pred);
        predArray.add(pred);
        
        String nextStateName=(stateCode!=0)? (j+"_Occupied"):(j+"_Empty");
        stateName=(stateName.equals(""))? nextStateName : stateName+this.STATE_MULTIPLY_STRING+nextStateName;
        
      }
      
      Predicate predicate=new Predicate.And(predArray);
      stateNames.add(stateName);
      
      if (alternative==null){
        TransitionVector.Builder<FractionProbability> b=new TransitionVector.Builder<FractionProbability>(modelName);
        b.setProbability(stateName, new FractionProbability(1, 1));
        TransitionVector<FractionProbability> unused=b.build();
        alternative= new DecisionTree.Terminal<TransitionVector<FractionProbability>>(unused) ;
      }
      TransitionVector.Builder<FractionProbability>b=new TransitionVector.Builder<FractionProbability>(modelName);
      b.setProbability(stateName, new FractionProbability(1, 1));
      TransitionVector<FractionProbability> prob=b.build();
      DecisionTree.Terminal<TransitionVector<FractionProbability>> consequent=new DecisionTree.Terminal<TransitionVector<FractionProbability>>(prob);
      
      DecisionTree.Branch<TransitionVector<FractionProbability>> branch=new DecisionTree.Branch<TransitionVector<FractionProbability>>(predicate, consequent, alternative);
      
      alternative=branch;
      code.add(i);
    }
    
    Machine.Builder<FractionProbability> machineBuild = new Machine.Builder<FractionProbability>(modelName);
    Iterator<Integer> itrTemp=code.iterator();
    for(String s:stateNames){
      Net.Builder<FractionProbability> netUnused=new Net.Builder<FractionProbability>();
      State.Builder<FractionProbability> stateUnused=new State.Builder<FractionProbability>(modelName,s,alternative);
      Machine.Builder<FractionProbability> machineUnused = new Machine.Builder<FractionProbability>(modelName);
      machineUnused.addState(stateUnused.build());
      Iterator<Machine<FractionProbability>> tempItr=net.iterator();
      while(tempItr.hasNext()){
        netUnused.addMachine(tempItr.next());
      }
      netUnused.addMachine(machineUnused.build());
      
      DecisionTree<TransitionVector<FractionProbability>> head=replaceTerminals(alternative,s,netUnused.build().dictionary); 
      
      State.Builder<FractionProbability> stateBuild=new State.Builder<FractionProbability>(modelName,s,head);
      String[] machines=s.split(this.STATE_MULTIPLY_STRING);
      int value=0;
      for (String sM:machines){
        if (sM.contains("Occupied")) value+=1000;
      }
      
      stateBuild.setLabel("Cost", Integer.toString(value));
      stateBuild.setLabel("Code", itrTemp.next().toString());
      machineBuild.addState(stateBuild.build());
    }
    return machineBuild.build();
  }

  private DecisionTree<TransitionVector<FractionProbability>> replaceTerminals(
      DecisionTree<TransitionVector<FractionProbability>> alternative, final String s, final Dictionary dictionary) {
    
    final String modelName="DischargeModel";
    DecisionTreeVisitor dv=new DecisionTreeVisitor(s,modelName,dictionary,alternative);
    alternative.accept(dv);
    return dv.alternative;
  }
  
  class DecisionTreeVisitor implements DecisionTree.Visitor<TransitionVector<FractionProbability>>{
    String s;
    String modelName;
    Dictionary dictionary;
    DecisionTree<TransitionVector<FractionProbability>> alternative;
    int kickOutPatientNum;
    
    DecisionTreeVisitor(String s,String model,Dictionary dict,DecisionTree<TransitionVector<FractionProbability>> alt){
      this.s=s;
      this.modelName=model;
      this.dictionary=dict;
      this.alternative=alt;
      this.kickOutPatientNum=0;
    }
    
    public void visitTerminal(DecisionTree.Terminal<TransitionVector<FractionProbability>> term) {
      String[] state=s.split("::");
      int numOfEmpty=0;
      ArrayList<Integer> stateNum=new ArrayList<Integer>();
      stateNum.add(0);
      ArrayList<Integer> temp=new ArrayList<Integer>();
      for(int i=0;i<state.length;i++){
        if (state[i].contains("Empty")) {
          numOfEmpty++;
          for (int s:stateNum){
            temp.add((int) (s+Math.pow(2, i)));
          }
        }
      }
      stateNum=temp;
      TransitionVector.Builder<FractionProbability> b=new TransitionVector.Builder<FractionProbability>(modelName);
      FractionProbability p=new FractionProbability(1,numOfEmpty);
      if (numOfEmpty==0) {
        //kick the patient out if state is Arrival
      }else{
        TransitionVector.Builder<FractionProbability> tb=new TransitionVector.Builder<FractionProbability>(modelName);
        for (int s:stateNum){
          String stateName="";
          for(int j=0;j<state.length;j++){
            byte stateCode = (byte) ((s & (int)(Math.pow(2, j)))>>j);
            String nextStateName=(stateCode==0)? state[j]:(j+"_Occupied");
            stateName=(stateName.equals(""))? nextStateName : stateName+STATE_MULTIPLY_STRING+nextStateName;
          }
          tb.setProbability(stateName, p);
        }
        TransitionVector<FractionProbability> pVector = tb.build();
        Predicate.Atom atom=new Predicate.Atom("ArrivalModel", "Arrival", "T");
        DecisionTree.Terminal<TransitionVector<FractionProbability>> conseq=new DecisionTree.Terminal<TransitionVector<FractionProbability>>(pVector);
        DecisionTree.Terminal<TransitionVector<FractionProbability>> alter=new DecisionTree.Terminal<TransitionVector<FractionProbability>>(term.output);
        alternative=new DecisionTree.Branch<TransitionVector<FractionProbability>>(atom, conseq, alter);
      }
    }

    public void visitBranch(DecisionTree.Branch<TransitionVector<FractionProbability>> walker) {
      DecisionTree<TransitionVector<FractionProbability>> conseq = replaceTerminals(walker.consequent,s,dictionary);
      DecisionTree<TransitionVector<FractionProbability>> alter = replaceTerminals(walker.alternative,s,dictionary);
      Predicate pred=walker.predicate;
      alternative=new DecisionTree.Branch<TransitionVector<FractionProbability>>(pred, conseq, alter);
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
