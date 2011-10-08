package parser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import markov.Machine;
import markov.Net;
import markov.Predicate;
import markov.DoubleProbability;
import markov.State;
import markov.TransitionVector;
import markov.DecisionTree;

import org.w3c.dom.*;

import javax.xml.parsers.*;


public class XmlParser {
  
      public Net<DoubleProbability> net;
      private static int NUM_PATIENTS;
      public final static double P_ARRIVAL = 0.2;

      public static void main (String args[]) {
          XmlParser temp = new XmlParser("xml/umlVersion6.xml",2);
          System.out.println(temp.net.toString());
      }
      
/*    No longer used
      public XmlParser() {
        
        Net<DoubleProbability> net = XmlInput("xml/umlVersion5.xml",1);
        System.out.println(net.toString());
       
      }*/
      
      public XmlParser(String fileName, int numOfPatient){
        XmlParser.NUM_PATIENTS=numOfPatient;
        Net.Builder<DoubleProbability> netBuild=new Net.Builder<DoubleProbability>();
        netBuild = XmlPartialParse(fileName,numOfPatient,netBuild);
        //TODO need make compute dispatch in the parser so that numOfPatient is the same
        netBuild.addMachine(computeDispatch());
        net=netBuild.build();
      }
      public static DecisionTree<TransitionVector<DoubleProbability>> computeTree(int state, BitSet bedState, int nextBit) {
        if (nextBit >= NUM_PATIENTS) return nextState(bedState);
        
        if (nextBit == state) {
          BitSet set = (BitSet)bedState.clone();
          set.set(nextBit);
          return computeTree(state, set, nextBit+1);
        }
        Predicate pred = new Predicate.Atom("Patient" + nextBit + ":ICP", "Patient", "NA");
        BitSet unset = (BitSet)bedState.clone();
        unset.clear(nextBit);
        BitSet set = (BitSet)bedState.clone();
        set.set(nextBit);
        return new DecisionTree.Branch<TransitionVector<DoubleProbability>>(pred, computeTree(state, unset, nextBit+1), computeTree(state, set, nextBit+1));
      }
      
      public static DecisionTree<TransitionVector<DoubleProbability>> nextState(BitSet set) {
        TransitionVector.Builder<DoubleProbability> builder = new TransitionVector.Builder<DoubleProbability>("Dispatch");
        if (set.cardinality() < NUM_PATIENTS) {
          builder.setProbability("None", new DoubleProbability(1-P_ARRIVAL));
          for (int i = 0; i < NUM_PATIENTS; i++) {
            if (!set.get(i)) {
              builder.setProbability(Integer.toString(i), new DoubleProbability(P_ARRIVAL/(NUM_PATIENTS-set.cardinality())));
            }
          }
        } else {
          builder.setProbability("None", DoubleProbability.ONE);
        }
        return new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(builder.build());
      }
      
      public static Machine<DoubleProbability> computeDispatch() {
        Machine.Builder<DoubleProbability> builder = new Machine.Builder<DoubleProbability>("Dispatch");
        State.Builder<DoubleProbability> sBuilder = new State.Builder<DoubleProbability>("Dispatch", "None", computeTree(-1, new BitSet(), 0));
        // sBuilder.setLabel("next", "none");
        for (int i = 0; i < NUM_PATIENTS; i++) {
          sBuilder.setLabel("p" + i + "arriving", Integer.toString(0));
        }
        sBuilder.setLabel("arriving", "0");
        builder.addState(sBuilder.build());
        for (int i = 0; i < NUM_PATIENTS; i++) {
          sBuilder =  new State.Builder<DoubleProbability>("Dispatch", Integer.toString(i), computeTree(i, new BitSet(), 0));
          // sBuilder.setLabel("next", Integer.toString(i));
          sBuilder.setLabel("arriving", "1");
          for (int j = 0; j < NUM_PATIENTS; j++) {
            sBuilder.setLabel("p" + j + "arriving", i==j ? "1" : "0");
          }
          builder.addState(sBuilder.build());
        }
        return builder.build();
      }

/*    No longer used
      public static Net<DoubleProbability> XmlInput(String fileName){
        return XmlInput(fileName,1);
      }*/
      
      /****** This method can be used in other class to retrieve machines **********/
      public static Net.Builder<DoubleProbability> XmlPartialParse(String fileName, int numOfPatient,Net.Builder<DoubleProbability> netBuild){
        
        try {
          

          //Read in XML file
          DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
          DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
          Document doc = docBuilder.parse(fileName);

          doc.getDocumentElement().normalize();
          for (int patientNum=0;patientNum<numOfPatient;patientNum++){  
//            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            NodeList machineList = doc.getElementsByTagName("machine");
      
            for (int i=0; i<machineList.getLength(); i++){
              
              Element machineXml = (Element) machineList.item(i);
              
//              System.out.println("-----------------------");
//              System.out.println("MachineeName : " + machineXml.getAttribute("name") );
              String machineNames[]=machineXml.getAttribute("name").split(":");
              machineNames[0]=machineNames[0].substring(1);
              String machineName=machineNames[0]+patientNum+":"+machineNames[1];
              Machine.Builder<DoubleProbability> machineBuild = new Machine.Builder<DoubleProbability>(machineName);
//              ArrayList<String> labelName=new ArrayList<String>();
              
              NodeList stateList = machineXml.getElementsByTagName("state");
              
              for (int j=0; j<stateList.getLength(); j++){
                 
                Element stateXml = (Element) stateList.item(j);
//                System.out.println("  StateName : " + stateXml.getAttribute("name") );
                            
                NodeList labels=stateXml.getChildNodes().item(0).getChildNodes();

                
/*                  for (int temp=0; temp<labels.getLength();temp++)
                {
                  if (labels.item(temp).getNodeName().equals("labelPair")) {
                    Element labelVector=(Element) labels.item(temp);
                    System.out.println("    LabelVector: [" + labelVector.getAttribute("name") + ":"+ labelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue()+"]" );
                  }
                }*/

            
                //getDecisionTreeInfo  under state using 3rd under alternative or consequence use 1st of the Elements's childrenNode
                DecisionTree<TransitionVector<DoubleProbability>> decisionTree=getDecisionTreeInfo(machineBuild.name, stateXml,patientNum);
                State.Builder<DoubleProbability> stateBuild;
                if (!stateXml.getAttribute("name").contains("Empty")){
                  stateBuild=new State.Builder<DoubleProbability>(machineName,stateXml.getAttribute("name"),decisionTree);
                }else{
                  Predicate temp=new Predicate.Atom("Dispatch", "p" + patientNum + "arriving", "1");
                  TransitionVector.Builder<DoubleProbability> b=new TransitionVector.Builder<DoubleProbability>(machineName);
                  b.setProbability("InitLow", DoubleProbability.ONE);                  
                  DecisionTree<TransitionVector<DoubleProbability>> consequent=new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(b.build());
                  decisionTree=new DecisionTree.Branch<TransitionVector<DoubleProbability>>(temp, consequent, decisionTree);
                  stateBuild=new State.Builder<DoubleProbability>(machineName,stateXml.getAttribute("name"),decisionTree);
                }
                
                for (int temp=0; temp<labels.getLength();temp++)
                {
                  if (labels.item(temp).getNodeName().equals("labelPair")) {
                    Element labelVector=(Element) labels.item(temp);
                    stateBuild.setLabel(labelVector.getAttribute("name"), labelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue());
//                    labelName.add(labelVector.getAttribute("name"));
                  }
                }
                State<DoubleProbability> state=stateBuild.build();
                machineBuild.addState(state);
              }
              Machine<DoubleProbability> machine=machineBuild.build();
              netBuild.addMachine(machine);
                                                             
            }
          }
          return netBuild;
          
        } catch (Exception e) {
          e.printStackTrace();
        }        
        return null;
      }
      
      /*******This one will not be able to do net Build after adding discharge in*******/
      public static Net<DoubleProbability> XmlInput(String fileName, int numOfPatient) {
        Net.Builder<DoubleProbability> netBuild=new Net.Builder<DoubleProbability>();
        netBuild=XmlPartialParse(fileName,numOfPatient,netBuild);
        if(netBuild==null){
          System.err.println("NetBuild Null");
          return null;
        }else{
          return netBuild.build();
        }

      }
      
      private static DecisionTree<TransitionVector<DoubleProbability>> getDecisionTreeInfo(String machineName, Element parent, int patientNum){
        
        Element decisionTreeXml=null;
        //if DecisionTree is under state use the 4th item of childNodes 
        if (parent.getNodeName().equals("state")){
          decisionTreeXml=(Element)parent.getChildNodes().item(1);
//          System.out.println(parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(0).getNodeName()+ ": ");

        }//else if DecisionTree is under consequent or alternative use 2nd item of childNodes
        else if(parent.getNodeName().equals("consequent")||parent.getNodeName().equals("alternative")){
         decisionTreeXml=(Element)parent.getChildNodes().item(0);
//         System.out.println("  "+parent.getNodeName()+": DecisionTree: "+ decisionTreeXml.getChildNodes().item(0).getNodeName()+ ": ");

        }
        
        if (decisionTreeXml==null){
          System.err.println("DecisionTree parent is not valid");
        }
        
        
        
        if (decisionTreeXml.getChildNodes().item(0).getNodeName().equals("branch")){
          Element predicateXml=(Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(0);
//        System.out.println("  "+predicateXml.getNodeName()+": ");
            
          //get atom or ... info from predicate
          Predicate predicate=null;
          predicate=getInfoFromPredicate(predicateXml,predicate,patientNum);
          
          if (predicate==null){
            System.err.println("!!Predicate not parsed!");
          }
          
          DecisionTree<TransitionVector<DoubleProbability>> consequent=null;
          DecisionTree<TransitionVector<DoubleProbability>> alternative=null;         
          Element consequentXml = (Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(1);
          consequent = getDecisionTreeInfo (machineName, consequentXml,patientNum);
          if (decisionTreeXml.getChildNodes().item(0).getChildNodes().getLength()<3){
            System.err.println("!Missing consequence or alternative");
          } //no alternative
          else {
            Element alternativeXml = (Element) decisionTreeXml.getChildNodes().item(0).getChildNodes().item(2);
            alternative=getDecisionTreeInfo(machineName, alternativeXml,patientNum);
          }
          if (consequent==null || alternative==null)
            System.err.println("!Consequence or alternative not parsed!");
          
          DecisionTree.Branch<TransitionVector<DoubleProbability>> branch=new DecisionTree.Branch<TransitionVector<DoubleProbability>>(predicate,consequent,alternative);
          
          return branch;  
        }
        else if(decisionTreeXml.getChildNodes().item(0).getNodeName().equals("probability")){  
          Element probabilityXml = (Element)decisionTreeXml.getChildNodes().item(0);
          NodeList stateNameXml = probabilityXml.getElementsByTagName("stateName");
          NodeList pValueXml = probabilityXml.getElementsByTagName("pValue");
          TransitionVector.Builder<DoubleProbability> b=new TransitionVector.Builder<DoubleProbability>(machineName);

          for (int temp=0; temp<stateNameXml.getLength();temp++){
//           System.out.println("  stateName:"+stateNameXml.item(temp).getChildNodes().item(0).getNodeValue()+",pValue:"+pValueXml.item(temp).getChildNodes().item(0).getNodeValue());
            String[] pValue=pValueXml.item(temp).getChildNodes().item(0).getNodeValue().split(",");
            String stateName=stateNameXml.item(temp).getChildNodes().item(0).getNodeValue();

            if (pValue.length!=2)
              System.err.println("pValue input error! Should have format <num>,<den>");
            Long num=Long.parseLong(pValue[0]);
            Long den=Long.parseLong(pValue[1]);
            DoubleProbability probability=new DoubleProbability(num,den);
            b.setProbability(stateName, probability);

          }
        
          TransitionVector<DoubleProbability> transitionVector=b.build();
          DecisionTree.Terminal<TransitionVector<DoubleProbability>> terminal= new DecisionTree.Terminal<TransitionVector<DoubleProbability>>(transitionVector);
          return terminal;
          
        }else{
          return null;
        }
        
       
      }
      
      private static Predicate getInfoFromPredicate(Element predicate, Predicate Input, int patientNum ){
        //get atom info
        if (predicate.getChildNodes().item(0).getNodeName().equals("atom")){
          Element atomXml=(Element) predicate.getChildNodes().item(0);
//          System.out.println("    " + atomXml.getNodeName()+": machineName: "+atomXml.getAttribute("machineName"));   
          Element atomLabelVector=(Element) atomXml.getChildNodes().item(0);
//          System.out.println("      atom labelVector: [" + atomLabelVector.getAttribute("name")+":"+atomLabelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue()+"]");
          // create atom
          String machineNameTemp[]=atomXml.getAttribute("machineName").split(":");
          machineNameTemp[0]=machineNameTemp[0].substring(1);
          String machineName=(machineNameTemp.length==2) ? machineNameTemp[0]+patientNum+":"+machineNameTemp[1] : atomXml.getAttribute("machineName");
          Predicate.Atom atom=new Predicate.Atom(machineName, atomLabelVector.getAttribute("name"), atomLabelVector.getChildNodes().item(0).getChildNodes().item(0).getNodeValue());
          Predicate output=(Predicate)atom;

          return output;
        }
        
        else if (predicate.getChildNodes().item(0).getNodeName().equals("or")||predicate.getChildNodes().item(0).getNodeName().equals("and")){
          Element orAnd=(Element) predicate.getChildNodes().item(0);
          ArrayList<Element> predicates=new ArrayList<Element>();

          for (int temp=0; temp<orAnd.getChildNodes().getLength();temp++){
            if (orAnd.getChildNodes().item(temp).getNodeName().equals("predicate")){
              predicates.add((Element)orAnd.getChildNodes().item(temp));
            }
          }
//          System.out.println("  "+orAnd.getNodeName()+": predicates #: "+predicates.size());

          Predicate.CollectionBuilder cBuild=null;
          if (predicate.getChildNodes().item(0).getNodeName().equals("or"))
            cBuild=new Predicate.CollectionBuilder(Predicate.CollectionType.OR);
          else if (predicate.getChildNodes().item(0).getNodeName().equals("and"))
            cBuild=new Predicate.CollectionBuilder(Predicate.CollectionType.AND);
          else
            System.err.println("Error cBuild!");
          
          // call itself to retrieve the predicate info
          Iterator<Element> itr = predicates.iterator();
          while(itr.hasNext()){
            Element pred=itr.next();
            Predicate temp=getInfoFromPredicate(pred,Input,patientNum);
            cBuild.add(temp);
          }
          Predicate output=cBuild.build();

          return output;
        }
        else if (predicate.getChildNodes().item(0).getNodeName().equals("neg")){
          Element neg=(Element) predicate.getChildNodes().item(0);
          Element pred=(Element) neg.getChildNodes().item(0);
          // call itself to retrieve the predicate info
          Predicate temp=getInfoFromPredicate(pred,Input,patientNum);
          Predicate output=new Predicate.Neg(temp);

          return output;

        }else if (predicate.getChildNodes().item(0).getNodeName().equals("implies")){
          Element implies=(Element) predicate.getChildNodes().item(0);
          Element antecedentXml=(Element) implies.getChildNodes().item(0);
          Element consequentXml=(Element) implies.getChildNodes().item(1);
          // call itself to retrieve the predicate info
          Predicate antecedent=getInfoFromPredicate(antecedentXml,Input,patientNum);
          Predicate consequent=getInfoFromPredicate(consequentXml,Input,patientNum);
          Predicate output=new Predicate.Implies(antecedent,consequent);
          return output;
        }else{
          return null;
        }
       
      }


}
