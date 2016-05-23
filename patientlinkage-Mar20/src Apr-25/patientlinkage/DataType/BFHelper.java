/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cf
 */
public class BFHelper {
    //public ArrayList<String> IDs = new ArrayList<>();
    //public String[] pros;
    //public boolean[][][] BFdata;
    ArrayList<boolean[][]> BFdata = new ArrayList<>();
    ArrayList<boolean[][]> BFnumOf1s = new ArrayList<>();
    //public int[] OrgIndxes; 
    ArrayList<Integer> OrgIndxes = new ArrayList<>();
    ArrayList<Integer> OtherPartyOrgIndxes = new ArrayList<>();
    //public String[] rules;

public BFHelper(boolean[][][] data, int otherpartyRecsCnt)
{   //BFdata=new boolean[data.length][1][];
    //OrgIndxes= new int[data.length];
    for(int i=0;i<data.length;i++)
    {
        BFdata.add(data[i]);
        
        OrgIndxes.add(i);
      //BFdata[i][0]=data[i][BFindex];
      //OrgIndxes[i]=i;
    }
    for(int i=0;i<otherpartyRecsCnt;i++)
         OtherPartyOrgIndxes.add(i);
}

public BFHelper(boolean[][][] data, boolean[][][] BF1s, int otherpartyRecsCnt)
{   //BFdata=new boolean[data.length][1][];
    //OrgIndxes= new int[data.length];
    for(int i=0;i<data.length;i++)
    {
        BFdata.add(data[i]);
        BFnumOf1s.add(BF1s[i]);
        OrgIndxes.add(i);
      //BFdata[i][0]=data[i][BFindex];
      //OrgIndxes[i]=i;
    }
    for(int i=0;i<otherpartyRecsCnt;i++)
         OtherPartyOrgIndxes.add(i);
}

public int getDataSize(){
    return(BFdata.size());
}


public ArrayList<PatientLinkage> getLinkageOrgIndexes(ArrayList<PatientLinkage> linkages,String role){
    
int Myind,MyOrgind,Otherindx, OpOrgind;
ArrayList<PatientLinkage> NewlinkagesWithOrgIndxes= new ArrayList<>(); 
switch (role) {
            case "generator":
                for (int n = 0; n < linkages.size(); n++) {
                    Myind = linkages.get(n).getI();
                    Otherindx=linkages.get(n).getJ();
                    
                    OpOrgind=OtherPartyOrgIndxes.get(Otherindx);
                    
                    MyOrgind=OrgIndxes.get(Myind);
                    NewlinkagesWithOrgIndxes.add(new   PatientLinkage(MyOrgind, OpOrgind,linkages.get(n).getScore() )  );
                    }  
                
                break;
            case "evaluator":
                for (int n = 0; n < linkages.size(); n++) {
                    Myind = linkages.get(n).getJ();
                    Otherindx=linkages.get(n).getI();
                    
                    OpOrgind=OtherPartyOrgIndxes.get(Otherindx);
                    
                    MyOrgind=OrgIndxes.get(Myind);
                    NewlinkagesWithOrgIndxes.add(new   PatientLinkage( OpOrgind,MyOrgind,linkages.get(n).getScore() )  );
                    }  
                
                 break;
    }
  return  NewlinkagesWithOrgIndxes;             
}



    public boolean[][][] getData_bin() { // this casting will not work : runtime error
        boolean[][][] retdata=(boolean [][][])BFdata.toArray();   //new boolean[BFdata.size()][1][];
    
        return retdata;
    }

   public boolean[][][] getData_bin(int BFindex) { //
        //boolean[][][] data=new boolean[BFdata.size()][][]; 
        //data = (boolean[][][]) BFdata.toArray();
        boolean[][][] retdata=new boolean[BFdata.size()][1][];
        for(int i=0;i<BFdata.size();i++){
            retdata[i][0]=BFdata.get(i)[BFindex];
        }
    
        return retdata;
    }

   
   public boolean[][][] getBF1sCnt(int BFindex) { //
        //boolean[][][] data=new boolean[BFdata.size()][][]; 
        //data = (boolean[][][]) BFdata.toArray();
        boolean[][][] retdata=new boolean[BFnumOf1s.size()][1][];
        for(int i=0;i<BFnumOf1s.size();i++){
            retdata[i][0]=BFnumOf1s.get(i)[BFindex];
        }
    
        return retdata;
    }

  
//---------------------------------------------
    // returns other party # of  recs to update its records count 
    public int FilterOutMatchedRecs( ArrayList<PatientLinkage> ptl_arr, String role) {
        //boolean[][][] res = null; //new boolean[ptl_arr.size()][][];
        //ArrayList<boolean[][]> OrgData = new ArrayList<>();
        //for(int i=0;i<arr1.length;i++)
          //  OrgData.add(arr1[i]);
          ArrayList<Integer> uniqueIdxCnt=new ArrayList<>();
        int ind,ind2,cnt=0,Otherindx; // uniqueIdxCnt and Otherindx to count the unique indexes of the other party(B/A) 
        int OPind;
        switch (role) {
            case "generator":
                
                for (int n = 0; n < ptl_arr.size(); n++) {
                    ind = ptl_arr.get(n).getI();
                    Otherindx=ptl_arr.get(n).getJ();
                    /*if(!uniqueIdxCnt.contains(Otherindx)){
                        uniqueIdxCnt.add(Otherindx);
                        cnt++;
                        int OPind=OtherPartyOrgIndxes.indexOf(Otherindx);
                        OtherPartyOrgIndxes.remove(OPind);
                    }*/
                    OPind=OtherPartyOrgIndxes.indexOf(Otherindx);
                    if (OPind!=-1)
                        OtherPartyOrgIndxes.remove(OPind);
                    ind2=OrgIndxes.indexOf(ind);
                    if(ind2!=-1 && BFdata.size()!=0){
                        BFdata.remove(ind2);
                        if(BFnumOf1s.size()>0)
                            BFnumOf1s.remove(ind2);
                        OrgIndxes.remove(ind2);
                    }
                    //res[n] = arr1[ind];
                }
                cnt=OtherPartyOrgIndxes.size();
                break;
            case "evaluator":
                
                for (int n = 0; n < ptl_arr.size(); n++) {
                    ind = ptl_arr.get(n).getJ();
                    Otherindx=ptl_arr.get(n).getI();
                    /*if(!uniqueIdxCnt.contains(Otherindx)){
                        uniqueIdxCnt.add(Otherindx);
                        cnt++;
                    }*/
                    
                    OPind=OtherPartyOrgIndxes.indexOf(Otherindx);
                    if (OPind!=-1)
                        OtherPartyOrgIndxes.remove(OPind);
                    ind2=OrgIndxes.indexOf(ind);
                    if(ind2!=-1 ){
                        BFdata.remove(ind2);
                        if(BFnumOf1s.size()>0)
                            BFnumOf1s.remove(ind2);
                        OrgIndxes.remove(ind2);
                    } 
                    //res[n] = arr1[ind];
                }
                cnt=OtherPartyOrgIndxes.size();
                break;
        }

     return cnt;   
    }
        
    //----------------------------------------------    
    
}
