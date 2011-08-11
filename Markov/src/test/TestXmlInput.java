package test;

import java.util.ArrayList;
import java.util.Iterator;

import markov.Predicate;
import markov.Predicate.*;
import markov.Probability;
import markov.FractionProbability;

import org.w3c.dom.*;
import javax.xml.parsers.*;


public class TestXmlInput {

      /**
       * Our goal is to create a DOM XML tree and then print the XML.
       */
      public static void main (String args[]) {
          new TestXmlInput();
      }

      public TestXmlInput() {
          try {
              /////////////////////////////
              //Creating an empty XML Document

              //We need a Document
              DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
              DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
              Document doc = docBuilder.parse("xml/Machine.xml");

              doc.getDocumentElement().normalize();
              
              System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
              NodeList nList = doc.getElementsByTagName("machine");
              System.out.println("-----------------------");
              
              for (int i=0; i<nList.getLength(); i++){
                Element machine = (Element) nList.item(i);
                Element state = (Element) machine.getElementsByTagName("state").item(0);
                System.out.println("MachineeName : " + machine.getAttribute("name") );
                System.out.println("  StateName : " + state.getAttribute("name") );
              
                NodeList labels=state.getChildNodes().item(1).getChildNodes();
                for (int temp=0; temp<labels.getLength();temp++)
                {
                  if (labels.item(temp).getNodeName().equals("labelPair")) {
                    Element labelVector=(Element) labels.item(temp);
                    System.out.println("    LabelVector: [" + labelVector.getAttribute("name") + ":"+ labelVector.getChildNodes().item(1).getChildNodes().item(0).getNodeValue()+"]" );
                  }
                }
              
                //getDecisionTreeInfo  under state using 3rd under alternative or consequence use 1st of the Elements's childrenNode
                getDecisionTreeInfo(state);
              }              
          } catch (Exception e) {
            e.printStackTrace();
          }
          

      }
      
      private static void getDecisionTreeInfo(Element parent){
        
        Element decisionTreeXml=null;
        //if DecisionTree is under state use the 4th item of childNodes 
        if (parent.getNodeName().equals("state")){
          decisionTreeXml=(Element)parent.getChildNodes().item(3);
        }//else if DecisionTree is under consequent or alternative use 2nd item of childNodes
        else if(parent.getNodeName().equals("consequent")||parent.getNodeName().equals("alternative")){
         decisionTreeXml=(Element)parent.getChildNodes().item(1);
        }
        
        if (decisionTreeXml==null){
          System.err.println("DecisionTree parent is not valid");
        }
        
        System.out.print("  DecisionTree: "+ decisionTreeXml.getChildNodes().item(1).getNodeName()+ ": ");
        
        
        if (decisionTreeXml.getChildNodes().item(1).getNodeName().equals("branch")){
          Element predicateXml=(Element) decisionTreeXml.getChildNodes().item(1).getChildNodes().item(1);
          System.out.print(predicateXml.getNodeName());
            
          //get atom or ... info from predicate
          Predicate predicate=null;
          getInfoFromPredicate(predicateXml,predicate);
            
          Element consequent=(Element) decisionTreeXml.getChildNodes().item(1).getChildNodes().item(3);
          getDecisionTreeInfo(consequent);
          if (decisionTreeXml.getChildNodes().item(1).getChildNodes().getLength()<6){} //no alternative
          else {
            Element alternative=(Element) decisionTreeXml.getChildNodes().item(1).getChildNodes().item(5);
            getDecisionTreeInfo(alternative);
          }
            
        }
        else if(decisionTreeXml.getChildNodes().item(1).getNodeName().equals("probability")){
          Element probabilityXml=(Element)decisionTreeXml.getChildNodes().item(1);
          System.out.println("instance:"+probabilityXml.getElementsByTagName("instance").item(0).getChildNodes().item(0).getNodeValue() + " ,pValue:" +probabilityXml.getElementsByTagName("pValue").item(0).getChildNodes().item(0).getNodeValue());
          String[] pValue=probabilityXml.getElementsByTagName("pValue").item(0).getChildNodes().item(0).getNodeValue().split(",");
          if (pValue.length!=2)
            System.err.println("pValue input wrong");
          Long num=Long.parseLong(pValue[0]);
          Long den=Long.parseLong(pValue[1]);
          Probability<FractionProbability> probability=new FractionProbability(num,den);
        }
        
       
      }
      
      private static void getInfoFromPredicate(Element predicate, Predicate Input ){
        //get atom info
        if (predicate.getChildNodes().item(1).getNodeName().equals("atom")){
          Element atomXml=(Element) predicate.getChildNodes().item(1);
          System.out.println(": "+ atomXml.getNodeName()+": machineName: "+atomXml.getAttribute("machineName"));   
          Element atomLabelVector=(Element) atomXml.getChildNodes().item(1);
          System.out.println("    atom labelVector: [" + atomLabelVector.getAttribute("name")+":"+atomLabelVector.getChildNodes().item(1).getChildNodes().item(0).getNodeValue()+"]");
          // create atom
          Atom atom=new Atom(atomXml.getAttribute("machineName"),atomLabelVector.getAttribute("name"),atomLabelVector.getChildNodes().item(1).getChildNodes().item(0).getNodeValue());
          Input=(Predicate)atom;
        }
        
        else if (predicate.getChildNodes().item(1).getNodeName().equals("or")||predicate.getChildNodes().item(1).getNodeName().equals("and")){
          Element orAnd=(Element) predicate.getChildNodes().item(1);
          int orAndPredicateCounter=0;
          ArrayList<Element> predicates=new ArrayList<Element>();

          for (int temp=0; temp<orAnd.getChildNodes().getLength();temp++){
            if (orAnd.getChildNodes().item(temp).getNodeName().equals("predicate")){
              orAndPredicateCounter++;
              predicates.add((Element)orAnd.getChildNodes().item(temp));
            }
          }
          System.out.println(": "+orAnd.getNodeName()+": predicates #: "+predicates.size());

          CollectionBuilder cBuild=null;
          if (predicate.getChildNodes().item(1).getNodeName().equals("or"))
            cBuild=new CollectionBuilder(Predicate.CollectionType.OR);
          else cBuild=new CollectionBuilder(Predicate.CollectionType.AND);
          
          // call itself to retrieve the predicate info
          Iterator<Element> itr = predicates.iterator();
          while(itr.hasNext()){
            Element pred=itr.next();
            getInfoFromPredicate(pred,Input);
            cBuild.add(Input);
          }
          Input=cBuild.build();

        }
        else if (predicate.getChildNodes().item(1).getNodeName().equals("neg")){
          Element neg=(Element) predicate.getChildNodes().item(1);
          Element pred=(Element) neg.getChildNodes().item(1);
          // call itself to retrieve the predicate info
          getInfoFromPredicate(pred,Input);
          Input=new Neg(Input);

        }else if (predicate.getChildNodes().item(1).getNodeName().equals("implies")){
          Element antecedentXml=(Element) predicate.getChildNodes().item(1);
          Element consequentXml=(Element) predicate.getChildNodes().item(3);
          // call itself to retrieve the predicate info
          getInfoFromPredicate(antecedentXml,Input);
          Predicate antecedent=Input;
          Input=null;
          getInfoFromPredicate(consequentXml,Input);
          Predicate consequent=Input;
          Input=new Implies(antecedent,consequent);
        }
       
      }



}
