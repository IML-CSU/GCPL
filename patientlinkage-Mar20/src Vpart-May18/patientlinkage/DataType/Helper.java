/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;

import java.util.ArrayList;

/**
 *
 * @author cf
 */
public class Helper {
    public ArrayList<String> IDs = new ArrayList<>();
    public String[] pros;
    public boolean[][][] data_bin;
    public boolean[][][] numOfOnesInBFs;
    public int[][] intNumOfOnesInBFs;
    public String[] rules;

    public ArrayList<String> getIDs() {
        return IDs;
    }
    public int[] getIntBF1sCnt(int BFindex) { //
        
        int[] retdata=new int[intNumOfOnesInBFs.length];
        for(int i=0;i<intNumOfOnesInBFs.length;i++){
            retdata[i]=intNumOfOnesInBFs[i][BFindex];
            
        }
    
        return retdata;
    }
    public String[] getPros() {
        return pros;
    }

    public boolean[][][] getData_bin() {
        return data_bin;
    }
    
     public boolean[][][] getNumOfOnesInBFs() {
        return numOfOnesInBFs;
    }

     
    public void setNumOfOnesInBFs(boolean[][][] numOfOnes) {
        this.numOfOnesInBFs = numOfOnes;
    }
    
    
    public void setIDs(ArrayList<String> IDs) {
        this.IDs = IDs;
    }

    public void setPros(String[] pros) {
        this.pros = pros;
    }

    public void setData_bin(boolean[][][] data_bin) {
        this.data_bin = data_bin;
    }
  
    public void updatingrules(int[][] ind){
        this.rules = new String[ind.length];
        
        for(int i = 0; i < ind.length; i++){
            this.rules[i] = "";
            for(int j = 0; j < ind[i].length; j++){
                if(ind[i][j] > 0){
                    if(this.rules[i].length() > 0){
                        this.rules[i] += "+";
                    }
                    if(ind[i][j] > 65536){
                        this.rules[i] += String.format("Soundex(%s)", this.pros[j]);
                    }else{
                        this.rules[i] += this.pros[j];
                    }
                }
            }
        }
    }
}
