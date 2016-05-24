/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.DataType;

import flexsc.CompEnv;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cf
 * @param <T>
 */
public class PatientLinkage4GadgetWsBF1sCntInputs<T> extends PatientLinkage4GadgetWsInputs<T> {

    
    private T[][][] BFs1sCnt;
    boolean[][] RecsToChkDC;  // records to compute their DCi. After filtering of each part of The BF

    public PatientLinkage4GadgetWsBF1sCntInputs(boolean[][][] Inputs,boolean[][][] BFs1sCnt, boolean[][] weights, boolean[] threshold, CompEnv<T> gen, String role, int th_ID) {
        super(Inputs,weights, threshold, gen, role, th_ID);
        
        
        this.BFs1sCnt = gen.newTArray(BFs1sCnt.length, BFs1sCnt[0].length, 0);
        try {
            switch (role) {
                case "Alice":
                    for (int i = 0; i < BFs1sCnt.length; i++) {
                       
                        for (int j = 0; j < BFs1sCnt[i].length; j++) {

                            this.BFs1sCnt[i][j] = gen.inputOfAlice(BFs1sCnt[i][j]);

                        }
                    }
                    break;
                case "Bob":
                    for (int i = 0; i < BFs1sCnt.length; i++) {
                       
                        for (int j = 0; j < BFs1sCnt[i].length; j++) {

                            this.BFs1sCnt[i][j] = gen.inputOfBob(BFs1sCnt[i][j]);
                        }
                    }
                    break;
                default:
                    System.exit(1);
            }
        } catch (Exception ex) {
            Logger.getLogger(PatientLinkage4GadgetInputs.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    
    
    public T[][][] getBFs1sCnt(){
        return this.BFs1sCnt;
    }
    

   //----------------------New const. with added chkDCi
    public PatientLinkage4GadgetWsBF1sCntInputs(boolean[][][] Inputs,boolean[][][] BFs1sCnt, boolean[][] weights, boolean[] threshold,boolean[][] chkDCi, CompEnv<T> gen, String role, int th_ID) {
        super(Inputs,weights, threshold, gen, role, th_ID);
        
        
        this.BFs1sCnt = gen.newTArray(BFs1sCnt.length, BFs1sCnt[0].length, 0);
        this.RecsToChkDC = chkDCi;
        try {
            switch (role) {
                case "Alice":
                    for (int i = 0; i < BFs1sCnt.length; i++) {
                       
                        for (int j = 0; j < BFs1sCnt[i].length; j++) {

                            this.BFs1sCnt[i][j] = gen.inputOfAlice(BFs1sCnt[i][j]);

                        }
                    }
                    break;
                case "Bob":
                    for (int i = 0; i < BFs1sCnt.length; i++) {
                       
                        for (int j = 0; j < BFs1sCnt[i].length; j++) {

                            this.BFs1sCnt[i][j] = gen.inputOfBob(BFs1sCnt[i][j]);
                        }
                    }
                    break;
                default:
                    System.exit(1);
            }
        } catch (Exception ex) {
            Logger.getLogger(PatientLinkage4GadgetInputs.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    
    
    public boolean [][] getBFs1schkDCi(){
        return this.RecsToChkDC;
    }
    

    
    
    
}
