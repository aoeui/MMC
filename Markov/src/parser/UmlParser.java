package parser;

/*import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;*/


import java.io.IOException;
import java.io.StringWriter;


public class UmlParser {
  
  /****** main method is not used anymore********/  
/*public static void main (String args[]) {
    System.out.println(new UmlParser());
    
  }
  public UmlParser() {
    
    UmlToXml("uml/Input.out.xml","xml/umlVersion.xml");
    TestXmlInput.XmlInput("xml/umlVersion.xml");
   
  }
  
  private void UmlToXml(String fileName, String outputFile) {
    try {

      //Read in XML file
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      Document doc = docBuilder.parse(fileName);

      doc.getDocumentElement().normalize();
      FileWriter fstream = new FileWriter(outputFile);
      BufferedWriter outFile = new BufferedWriter(fstream);
      StringWriter out=new StringWriter();
      
      out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE machines SYSTEM \"Machine.dtd\">\n");
      FileReader finput = new FileReader(fileName);
      BufferedReader in = new BufferedReader(finput);
      // escape the first line xml version
      in.readLine();
      String temp;
      Boolean withInDecisionTree=false;
      int decisionTreeCounter=0;

      while ((temp=in.readLine())!=null){
        if (!temp.contains("decisionTree") && !withInDecisionTree){
          out.write(temp);
        }else if(temp.contains("<decisionTree>") && !temp.contains("</decisionTree>")){
          withInDecisionTree=true;
        }else if((temp.contains("</decisionTree>")&& withInDecisionTree) || (temp.contains("<decisionTree>") && temp.contains("</decisionTree>"))){
          withInDecisionTree=false;
          Element decisionTreeXml;
          out.write("<decisionTree>");
          decisionTreeXml=(Element)doc.getElementsByTagName("decisionTree").item(decisionTreeCounter);
          String decisionTree=decisionTreeXml.getChildNodes().item(0).getNodeValue();
          parseDecisionTree(decisionTree,out);
          out.write("</decisionTree>");
          
          decisionTreeCounter++;
        }else{ //skip the lines
        }
        
      }
      outFile.write(out.toString());

      outFile.close();
    }catch(Exception e) {
      e.printStackTrace();
    }
  }*/
  
  public static String decisionTreeXslParser(String input) throws IOException{
    StringWriter out=new StringWriter();
    parseDecisionTree(input,out);
    return out.toString();
  }
  

  private static void parseDecisionTree(String decisionTree, StringWriter out) throws IOException {
    if(decisionTree.contains("if")){
      out.write("<branch>");
      int startIfIdx=decisionTree.indexOf("if");
      int ifNum=1,elseNum=1;
      int endBranchIndex=findEndOfBranchIdx(decisionTree,ifNum, startIfIdx);
      int elseIndex=findElseIdx(decisionTree,elseNum,startIfIdx);
      
      int startBranchIndex=decisionTree.indexOf("then");
      String predicate=decisionTree.substring(2,startBranchIndex);
      out.write("<predicate>");
      parsePredicate(predicate,out);
      out.write("</predicate>");
//      System.out.println("Predicate: "+predicate+ " "+decisionTree.substring(startBranchIndex+4, endBranchIndex));
      
      out.write("<consequent>");
      String consequent=decisionTree.substring(startBranchIndex+4,elseIndex);
      
      if (consequent.contains("if")){
        out.write("<decisionTree>");
        consequent=consequent.substring(consequent.indexOf("if"));
        parseDecisionTree(consequent,out);
        out.write("</decisionTree>");
      }else if(consequent.contains("p[")){
        out.write("<decisionTree>");
        out.write("<probability>");
        parseProbability(consequent,out);
        out.write("</probability>");
        out.write("</decisionTree>");
      }
      out.write("</consequent>");
      
      out.write("<alternative>");
      String alternative=decisionTree.substring(elseIndex+4, endBranchIndex);
      
      if (alternative.contains("if")){
        out.write("<decisionTree>");
        alternative=alternative.substring(alternative.indexOf("if"));
        parseDecisionTree(alternative,out);
        out.write("</decisionTree>");
      }else if(alternative.contains("p[")){
        out.write("<decisionTree>");
        out.write("<probability>");
        parseProbability(alternative,out);
        out.write("</probability>");
        out.write("</decisionTree>");
      }
      out.write("</alternative>");
      
      out.write("</branch>");
    }else if(decisionTree.contains("p[")){
      out.write("<probability>");
      parseProbability(decisionTree,out);
      out.write("</probability>");
    }else{
      System.err.println("Error in DecisionTree!");
    }
    
      
    
  }
  
  private static void parseProbability(String probability, StringWriter out) throws IOException {
    String[] temp=probability.split("p");
    for (int i=0;i<temp.length;i++){
      if(temp[i].contains("[")){
        out.write("<stateName>");
        int idx1=temp[i].indexOf('[');
        int idx2=temp[i].indexOf(']');
        int idx3=temp[i].indexOf('=');
        int idx4=temp[i].indexOf('/');
        int idxTmp=(temp[i].indexOf(',')==-1)? temp[i].length():temp[i].indexOf(',');
        int idxTmp2=(temp[i].indexOf('\n')==-1)? temp[i].length():temp[i].indexOf('\n');
        int idx5=Math.min(idxTmp,idxTmp2);
        out.write(temp[i].substring(idx1+1, idx2));
        out.write("</stateName>");
        out.write("<pValue>");
        int den=(idx4!=-1)? Integer.parseInt(temp[i].substring(idx4+1,idx5)):1;
        int num=(idx4!=-1)? Integer.parseInt(temp[i].substring(idx3+1,idx4)):Integer.parseInt(temp[i].substring(idx3+1,idx5));
        out.write(num+","+den);
        out.write("</pValue>");
      }else{
      }
    }
  }
  private static void parsePredicate(String predicate, StringWriter out) throws IOException {
    if (predicate.contains("OR")||predicate.contains("AND")||predicate.contains("NEG")||predicate.contains("IMPLIES")){    
      if (predicate.contains("(")){
        String pattern="empty";
        
        int startIdx=predicate.indexOf("(");
        int bracketNum=1;
        int endIdx=findPredicate(predicate,bracketNum, startIdx); 
//        System.out.println(pattern);
        //find top logic
        pattern=findTopLogic(predicate,startIdx,"IMPLIES",pattern);
        pattern=findTopLogic(predicate,startIdx,"OR",pattern);
        pattern=findTopLogic(predicate,startIdx,"AND",pattern);
        pattern=findTopLogic(predicate,startIdx,"NEG",pattern);
        
        if (pattern.equals("NEG")) {
          int negIdx=predicate.indexOf("NEG");
          String negPredicate=predicate.substring(negIdx+3,endIdx);
   //       System.out.println("NEG:"+negPredicate);
          out.write("<neg>");
          out.write("<predicate>");
          parsePredicate(negPredicate,out);
          out.write("</predicate>");
          out.write("</neg>");
        }
        else if (pattern.equals("IMPLIES")){
          int impliesIdx=predicate.indexOf("IMPLIES");
          out.write("<implies>");
          String antecedent=predicate.substring(startIdx+1,impliesIdx);
          out.write("<predicate>");
          parsePredicate(antecedent,out);
          out.write("</predicate>");
          String consequent=predicate.substring(impliesIdx+7,endIdx);
          out.write("<predicate>");
          parsePredicate(consequent,out);
          out.write("</predicate>");
          out.write("</implies>");
          
  //        System.out.println("IMPLIES:" +antecedent +"=>"+consequent);
        }
        else if (pattern.equals("OR")){
          out.write("<or>");
          
          String temp=predicate.substring(startIdx+1,endIdx);
          
          while (temp.contains("OR")){
            int orIdx=temp.indexOf("OR");
            String predicateFirst=temp.substring(0,orIdx);
            out.write("<predicate>");
            temp=temp.substring(orIdx+3,temp.length());
//            System.out.println(predicateFirst+" OR "+temp+";");          
            parsePredicate(predicateFirst,out);
            out.write("</predicate>");
          }
          out.write("<predicate>");
          parsePredicate(temp,out);
          out.write("</predicate>");
          
          out.write("</or>");

        }
        else if (pattern.equals("AND")){
          out.write("<and>");
          
          String temp=predicate.substring(startIdx+1,endIdx);
          
          while (temp.contains("AND")){
            int andIdx=temp.indexOf("AND");
            String predicateFirst=temp.substring(0,andIdx);
            out.write("<predicate>");
            temp=temp.substring(andIdx+3,temp.length());
//            System.out.println(predicateFirst+" AND "+temp+";");          
            parsePredicate(predicateFirst,out);
            out.write("</predicate>");
          }
          out.write("<predicate>");
          parsePredicate(temp,out);
          out.write("</predicate>");
          
          out.write("</and>");
        }
        
      }
    }else if (predicate.contains("M")){
      int idx1=predicate.indexOf('M');
      int idx2=predicate.indexOf('.');
      int idx3=predicate.indexOf('=');
      int idx4=(predicate.lastIndexOf(' ')>0)? predicate.lastIndexOf(' '):predicate.length();
      out.write("<atom machineName=\""+predicate.substring(idx1,idx2)+"\">");
      out.write("<labelPair name=\""+predicate.substring(idx2+1,idx3)+"\">");
      out.write("<instance>"+predicate.substring(idx3+1,idx4)+"</instance>");
      out.write("</labelPair></atom>");
    }else{
      System.err.println("Predicate missmatch!"+predicate);
    }
//    System.out.println(predicate); 
    
  }
    
  
  private static String findTopLogic(String predicate,int startIdx, String string, String pattern) {
    if(predicate.contains(string) && pattern.equals("empty")){
      int idx=predicate.indexOf(string);
      String temp=predicate.substring(startIdx+1,idx);
      int leftBracketNum=temp.length();
      temp=temp.replace("(", "");
      leftBracketNum=leftBracketNum-temp.length();
      int rightBracketNum=temp.length();
      temp=temp.replace(")", "");
      rightBracketNum=rightBracketNum-temp.length();
      if (leftBracketNum==rightBracketNum) pattern=string;
    }
    return pattern;
  }
  
  private static int findPredicate(String predicate, int num, int startIdx) {
    int idx=predicate.indexOf(")",startIdx);
    if(idx==-1){
      System.err.println("Predicate is not valid in UML!"+predicate);
      return -1;
    }
    num=num-1;    
    int temp=0;
//   System.out.print("Num:"+num+" startIdx:"+startIdx+" idx:"+idx+" length:"+predicate.length()+"->");
    
    while((temp=predicate.indexOf("(", startIdx+1))<idx && temp!=-1){
      startIdx=temp;
      num++;
    }
    
    if (num!=0)
      return idx=findPredicate(predicate,num,idx+1);
    else {
//      System.out.println("Num: "+num+" endIdx: "+idx+" length: "+predicate.length());
      return idx;
    }

  }
  
  /****** Find the "end if" corresponding to the "if" at the start  *********/
  private static int findEndOfBranchIdx(String decisionTree, int ifNum, int startIfIdx) {
    int endIfIdx=decisionTree.indexOf("end if",startIfIdx);
    if(endIfIdx==-1){
      System.err.println("DecisionTree is not valid in UML!");
      return -1;
    }
    ifNum=ifNum-1;    
    int temp=0;
    //    System.out.print("ifNum:"+ifNum+" startIfIdx:"+startIfIdx+" endIfIdx:"+endIfIdx+" length:"+decisionTree.length()+"->");
    
    while((temp=decisionTree.indexOf("if", startIfIdx+2))<endIfIdx && temp!=-1){
      startIfIdx=temp;
      ifNum++;
    }
    
    if (ifNum!=0)
      return endIfIdx=findEndOfBranchIdx(decisionTree,ifNum,endIfIdx+6);
    else{
//      System.out.println("ifNum: "+ifNum+" endIfIdx: "+endIfIdx+" length: "+decisionTree.length());
      return endIfIdx;
    }
   
  }
  
  /****This is almost same as finding "end if", but when compute ifNum, one needs to take take care of "end if".******/
  private static int findElseIdx(String decisionTree, int ifNum, int startIfIdx){
    int elseIdx=decisionTree.indexOf("else",startIfIdx);
//    System.out.print("ifNum:"+ifNum+" startIfIdx:"+startIfIdx+" elseIdx:"+elseIdx+" length:"+decisionTree.length()+"->");
    if(elseIdx==-1){
      System.err.println("DecisionTree is not valid in UML!");
      return -1;
    }
    ifNum=ifNum-1;
    int temp=0;
  
    while((temp=decisionTree.indexOf("if", startIfIdx+2))<elseIdx && temp!=-1 && decisionTree.charAt(temp-2)!='d'){
      startIfIdx=temp;
      ifNum++;
    }
  
    if (ifNum!=0)
      return elseIdx=findElseIdx(decisionTree,ifNum,elseIdx+4);
    else {
//      System.out.println("ifNum: "+ifNum+" elseIdx: "+elseIdx+" length: "+decisionTree.length());
      return elseIdx;
    }
    
  }
  
  
}
