package markov;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import parser.XmlParser;

public class Simulation {
  
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
    
      return stateNameList;
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
