/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.GarbledCircuit;

import flexsc.CompEnv;

/**
 *
 * @author cf
 * @param <T>
 */
public class PatientLinkageBFGCLib<T> extends circuits.IntegerLib<T> {

    static final int S = 0;
    static final int COUT = 1;
    static final int dataSize = 32;//16;//
    
    public PatientLinkageBFGCLib(CompEnv<T> e) {
        super(e);
    }

    public T[] getWs(T[] a, T[] b, T[] w) throws Exception {
        T[] ret = env.newTArray(w.length);
        T flag = eq(a, b);
        
        for (int i = 0; i < ret.length; i++) {
            ret[i] = and(w[i], flag);
        }
        return ret;
    }

    public T[] matchRcvsWs(T[][] a, T[][] b, T[][] w) throws Exception {
        T[] ret = getWs(a[0], b[0], w[0]);
        for (int i = 1; i < a.length; i++) {
            T[] tmp = getWs(a[i], b[i], w[i]);
            ret = add(ret, tmp);
        }
        return ret;
    }
    
    //-----------------------------------------------------------------------
    public T[] getInters(T[] a, T[] b) throws Exception {
        T[] temp;//=env.newTArray(a.length);
         T[] ret0;//=env.newTArray(dataSize);
        temp=and(a,b);//padSignal( and(a,b),a.length);
        ret0=padSignal(numberOfOnes(temp),dataSize);
         return(ret0); 
       
    }
    
     public T[] getUnion(T[] a, T[] b) throws Exception {
         T[] ret1;//=env.newTArray(dataSize);
         T[] ret;//=env.newTArray(dataSize);
         ret=padSignal(numberOfOnes(a),dataSize);
         ret1=padSignal(numberOfOnes(b),dataSize);
         ret=padSignal(add(ret,ret1),dataSize);
        // System.out.println("result of add in UAB-length="+add(ret,ret1).length+"  ret len="+ret.length);
       return(ret);
    }
     
    public T[] getDiceCo(T[] i, T[] u) throws Exception {
       return(sub(u,i));
    }
    
    public T[][] matchRcvsBF(T[][] a, T[][] b, T[] TR) throws Exception {
        T[] IntersectionAB ;//= env.newTArray(dataSize);//TR.length);       
        T[] UniounAB ;//= env.newTArray(dataSize);
        
        
        //..UniounAB = getUnion(a[0], b[0]);
        //..IntersectionAB = getInters(a[0], b[0]);
        //T[] Res= add(IntersectionAB,IntersectionAB);
        //Res = leftPublicShift(Res , 7) ;
        //System.out.println("UAB-length="+UniounAB.length+"   TR-len="+TR.length);
        //..  T[][] ret = env.newTArray(2*a.length, UniounAB.length);// getDiceCo(IntersectionAB,UniounAB);
         T[][] ret = env.newTArray(2*a.length, dataSize);
           // ret has each DC num and Denum of each BF (a.length =#BFs)
           //..ret[0]=padSignal(add(IntersectionAB,IntersectionAB),dataSize);
           //..ret[1]=UniounAB;
         for (int i = 0,j=0; i < a.length; i++,j+=2) {
            UniounAB = getUnion(a[i], b[i]);
            IntersectionAB = getInters(a[i], b[i]);
            ret[j]=padSignal(add(IntersectionAB,IntersectionAB),dataSize);
            ret[j+1]=UniounAB;
           }   
        
        return ret;
    }

public T[][] matchRcvsBF(T[][] a, T[][] b , T[][] a1s, T[][] b1s ) throws Exception {
        T[] IntersectionAB ;//= env.newTArray(dataSize);//TR.length);       
        T[] UnionAB ;//= env.newTArray(dataSize);
        
        
        //..UnionAB = getUnion(a[0], b[0]);
        //..IntersectionAB = getInters(a[0], b[0]);
        //T[] Res= add(IntersectionAB,IntersectionAB);
        //Res = leftPublicShift(Res , 7) ;
        //System.out.println("UAB-length="+UnionAB.length+"   TR-len="+TR.length);
        //..  T[][] ret = env.newTArray(2*a.length, UnionAB.length);// getDiceCo(IntersectionAB,UnionAB);
         T[][] ret = env.newTArray(2*a.length, dataSize);
           // ret has each DC num and Denum of each BF (a.length =#BFs)
           //..ret[0]=padSignal(add(IntersectionAB,IntersectionAB),dataSize);
           //..ret[1]=UnionAB;
         for (int i = 0,j=0; i < a.length; i++,j+=2) {
            //UnionAB = getUnion(a[i], b[i]);
            UnionAB=padSignal(add(a1s[i],b1s[i]),dataSize);
            IntersectionAB = getInters(a[i], b[i]);
            ret[j]=padSignal(add(IntersectionAB,IntersectionAB),dataSize);
            ret[j+1]=UnionAB;
           }   
        
        return ret;
    }
    
    
}
