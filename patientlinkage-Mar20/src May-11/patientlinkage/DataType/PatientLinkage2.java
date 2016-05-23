/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;

/**
 *
 * @author cf
 */
public class PatientLinkage2<T> extends PatientLinkage{
    int shared1s;
    int tot1s;

    public PatientLinkage2(int i, int j) {
        super(i,j);
    }

    public PatientLinkage2(int i, int j, int sh1s,int t1s) {
        super(i,j);
        shared1s=sh1s;
        tot1s=t1s;
        this.score = (double)sh1s*2/t1s;
    }

    
   
    
    public void setLinkage(int i, int j,int sh1s,int t1s){
        this.i = i;
        this.j = j;
        shared1s=sh1s;
        tot1s=t1s;
        this.score = (double)sh1s/t1s;
    }
    
    
    
    public int getShared1s(){
        return this.shared1s;
    }
    
    public int getTotal1s(){
        return this.tot1s;
    }
    public void setShared1s(int sh1s){
         this.shared1s=sh1s;
    }
    
    public void setTotal1s(int t1s){
         this.tot1s=t1s;
    }
}
