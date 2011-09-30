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

import markov.DecisionTree.Terminal;
import markov.TransitionMatrix.RandomBuilder;
import parser.XmlParser;
import util.Ptr;
import util.Stack;

public class Simulation {
  
  public TreeMap<String,Integer> headlist;
  public TransitionMatrix<DoubleProbability> matrix;
  public final static String STATE_MULTIPLY_STRING="::";
  
  public static void main(String[] args) {
    System.out.println(new Simulation("xml/umlVersion4.xml"));
  }

  public Simulation(String xmlFileName){
      int numOfPatient=2;
      
      Net<DoubleProbability> net=XmlParser.XmlInput(xmlFileName,numOfPatient);

      Net.Builder<DoubleProbability> netBuild=new Net.Builder<DoubleProbability>();
      Machine<DoubleProbability> dischargeModel=constructDischargeModel(numOfPatient,net);
      Machine<DoubleProbability> arrivalModel=constructArrivalModel(numOfPatient, new DoubleProbability(1,4));
      
      Iterator<Machine<DoubleProbability>> itr=net.iterator();
      while(itr.hasNext()){
        Machine<DoubleProbability> temp=itr.next();
        if (temp.name.contains("ICP")){
          Iterator<State<DoubleProbability>> states = temp.iterator();
          Machine.Builder<DoubleProbability> machineBuild = new Machine.Builder<DoubleProbability>(temp.name);
          while(states.hasNext()){
            State<DoubleProbability> state = states.next();
            if (state.name.contains("Exit") || state.name.contains("Dead")){

              DecisionTree<TransitionVector<DoubleProbability>> decisionTree = state.transitionFunction;
              if (decisionTree.isBranch()) System.err.println("Exit or Dead can only have p[E] p[D]=1");
              else{
                String[] patient=temp.name.split(":");
                String patientNum=patient[0].substring(patient[0].indexOf("Patient")+7);
                Predicate.Atom atom=new Predicate.Atom("DischargeModel",patientNum,"Occupied");
                
                TransitionVector.Builder<DoubleProbability> b=new TransitionVector.Builder<DoubleProbability>(temp.name);
                b.setProbability("InitLow", new DoubleProbability(1, 1));
                TransitionVector<DoubleProbability> initLow=b.build();
                Terminal<TransitionVector<DoubleProbability>> conseq = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(initLow) ;
                b=new TransitionVector.Builder<DoubleProbability>(temp.name);
                b.setProbability(state.name, new DoubleProbability(1, 1));
                Terminal<TransitionVector<DoubleProbability>> alter = new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(b.build());

                State.Builder<DoubleProbability> stateBuild=new State.Builder<DoubleProbability>(temp.name,state.name,new DecisionTree.Branch<TransitionVector<DoubleProbability>>(atom, conseq, alter));
                Iterator<Entry<String, String>> labelItr = state.labelIterator();
                while (labelItr.hasNext()){
                  Entry<String, String> tempEntry = labelItr.next();
                  stateBuild.setLabel(tempEntry.getKey(),tempEntry.getValue());
                }
                machineBuild.addState(stateBuild.build());
              }
            }else{
              machineBuild.addState(state);
            }
          }
          netBuild.addMachine(machineBuild.build());
        }else{
          netBuild.addMachine(temp);
        }

      }
      netBuild.addMachine(dischargeModel);
      netBuild.addMachine(arrivalModel);
      net=netBuild.build();

      itr=net.iterator();
      
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
      
      TransitionMatrix.RandomBuilder<DoubleProbability> builder =new TransitionMatrix.RandomBuilder<DoubleProbability>(stateNameList.size(), DoubleProbability.ZERO);
      
      
      Iterator<ArrayList<State<DoubleProbability>>> itrStates=accu.iterator();
      Long[] costTable=new Long[stateNameList.keySet().size()];
      while(itrStates.hasNext()){
        ArrayList<State<DoubleProbability>> states=itrStates.next();
        
        String combinedStateName="empty";
        int sum=0;
        for (State<DoubleProbability> s:states){
          if (s.getLabel("Cost")!=null) sum+=Integer.parseInt(s.getLabel("Cost"));
          combinedStateName=(combinedStateName.equals("empty")) ? s.name : combinedStateName+Machine.MULTIPLY_STRING+s.name;
        }
        costTable[stateNameList.get(combinedStateName)]= (long) sum;
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
//      System.out.println(matrix.toString());
      System.out.println(Arrays.toString(costTable));
      
      double[][] matOut=this.matrixToDouble();
      MLInt64 mlint=new MLInt64("costVector",costTable,1);
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

  public static Machine<DoubleProbability> constructArrivalModel(int numOfPatient,DoubleProbability p){
    final String modelName="ArrivalModel";
    TransitionVector.Builder<DoubleProbability> b=new TransitionVector.Builder<DoubleProbability>(modelName);
    b.setProbability("NoArrival", new DoubleProbability(1-p.p));
    b.setProbability("Arrival", p);
    TransitionVector<DoubleProbability> pVector=b.build();
    DecisionTree.Terminal<TransitionVector<DoubleProbability>> term= new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(pVector);
    Machine.Builder<DoubleProbability> machineBuild = new Machine.Builder<DoubleProbability>(modelName);
    State.Builder<DoubleProbability> stateBuild=new State.Builder<DoubleProbability>(modelName,"Arrival",term);
    stateBuild.setLabel("Arrival", "T");
    stateBuild.setLabel("Cost", "0");
    State<DoubleProbability> stateA=stateBuild.build();

    stateBuild=new State.Builder<DoubleProbability>(modelName,"NoArrival",term);
    stateBuild.setLabel("Arrival", "F");
    stateBuild.setLabel("Cost", "0");
    State<DoubleProbability> stateB=stateBuild.build();
    
    machineBuild.addState(stateA);
    machineBuild.addState(stateB);
    Machine<DoubleProbability> out=machineBuild.build();
    return out;
    
  }
  
  
  public static Machine<DoubleProbability> constructDischargeModel(int numOfPatient, Net<DoubleProbability> net) {

    final String modelName="DischargeModel";
    int numOfState=(int) Math.pow(2,numOfPatient);
    DecisionTree<TransitionVector<DoubleProbability>> alternative=null;
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
        stateName=(stateName.equals(""))? nextStateName : stateName+STATE_MULTIPLY_STRING+nextStateName;
        
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
      code.add(i);
    }


    
    Machine.Builder<DoubleProbability> machineBuild = new Machine.Builder<DoubleProbability>(modelName);
    Iterator<Integer> itrTemp=code.iterator();

    for(String s:stateNames){
/*      Net.Builder<DoubleProbability> netUnused=new Net.Builder<DoubleProbability>();
      Builder<DoubleProbability> stateUnused=new State.Builder<DoubleProbability>(modelName,s,alternative);
      Machine.Builder<DoubleProbability> machineUnused = new Machine.Builder<DoubleProbability>(modelName);
      machineUnused.addState(stateUnused.build());
      Iterator<Machine<DoubleProbability>> tempItr=net.iterator();
      while(tempItr.hasNext()){
        netUnused.addMachine(tempItr.next());
      }
      netUnused.addMachine(machineUnused.build());
      */
      DecisionTree<TransitionVector<DoubleProbability>> head=replaceTerminals(alternative,s); 
      
      State.Builder<DoubleProbability> stateBuild=new State.Builder<DoubleProbability>(modelName,s,head);

      String[] machines=s.split(STATE_MULTIPLY_STRING);
      int value=0;
      int occupancy=0;
      for (String sM:machines){
        stateBuild.setLabel(sM.substring(0, sM.indexOf("_")),sM.substring(sM.indexOf("_")+1));
        if (sM.contains("Occupied")){
          value+=1000;
          occupancy++;
        }
      }
      
      stateBuild.setLabel("Cost", Integer.toString(value));
      stateBuild.setLabel("Code", itrTemp.next().toString());
      stateBuild.setLabel("Occupancy", Integer.toString(occupancy));
      machineBuild.addState(stateBuild.build());
    }
    return machineBuild.build();
  }

  private static DecisionTree<TransitionVector<DoubleProbability>> replaceTerminals(
      DecisionTree<TransitionVector<DoubleProbability>> alternative, final String s) {
    
    final String modelName="DischargeModel";
    DecisionTreeVisitor dv=new DecisionTreeVisitor(s,modelName,alternative);
    alternative.accept(dv);
    return dv.alternative;
  }
  
  static class DecisionTreeVisitor implements DecisionTree.Visitor<TransitionVector<DoubleProbability>>{
    String s;
    String modelName;
    DecisionTree<TransitionVector<DoubleProbability>> alternative;
    int kickOutPatientNum;
    
    DecisionTreeVisitor(String s,String model,DecisionTree<TransitionVector<DoubleProbability>> alternative2){
      this.s=s;
      this.modelName=model;
      this.alternative=alternative2;
      this.kickOutPatientNum=0;
    }
    
    public void visitTerminal(DecisionTree.Terminal<TransitionVector<DoubleProbability>> term) {
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
      if (numOfEmpty==0) {
        //TODO kick the patient out if state is Arrival
      }else{
        DoubleProbability p=new DoubleProbability(1,numOfEmpty);
        TransitionVector.Builder<DoubleProbability> tb=new TransitionVector.Builder<DoubleProbability>(modelName);
        for (int s:stateNum){
          String stateName="";
          for(int j=0;j<state.length;j++){
            byte stateCode = (byte) ((s & (int)(Math.pow(2, j)))>>j);
            String nextStateName=(stateCode==0)? state[j]:(j+"_Occupied");
            stateName=(stateName.equals(""))? nextStateName : stateName+STATE_MULTIPLY_STRING+nextStateName;
          }
          tb.setProbability(stateName, p);
        }
        TransitionVector<DoubleProbability> pVector = tb.build();
        Predicate.Atom atom=new Predicate.Atom("ArrivalModel", "Arrival", "T");
        DecisionTree.Terminal<TransitionVector<DoubleProbability>> conseq=new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(pVector);
        DecisionTree.Terminal<TransitionVector<DoubleProbability>> alter=new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(term.output);
        alternative=new DecisionTree.Branch<TransitionVector<DoubleProbability>>(atom, conseq, alter);
      }
    }

    public void visitBranch(DecisionTree.Branch<TransitionVector<DoubleProbability>> walker) {
      DecisionTree<TransitionVector<DoubleProbability>> conseq = replaceTerminals(walker.consequent,s);
      DecisionTree<TransitionVector<DoubleProbability>> alter = replaceTerminals(walker.alternative,s);
      Predicate pred=walker.predicate;
      alternative=new DecisionTree.Branch<TransitionVector<DoubleProbability>>(pred, conseq, alter);
    }
  
    
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
          root=root.restrict(Stack.<String>emptyInstance().push(labelName).push(machineNameCopy.get(i)), instance);
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
