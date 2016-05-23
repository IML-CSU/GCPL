/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.GarbledCircuit;

//import patientlinkage.DataType.PatientLinkage4GadgetWsInputs;
import patientlinkage.DataType.PatientLinkageWssBFOutput;
import flexsc.CompEnv;
import flexsc.Gadget;
import patientlinkage.DataType.PatientLinkage4GadgetWsBF1sCntInputs;
import static patientlinkage.GarbledCircuit.PatientLinkageBFGCLib.dataSize;

/**
 *
 * @author cf
 * @param <T>
 */
public class PatientLinkageWssBFw1sCntGadget<T> extends Gadget<T> {
    public static int all_progresses = 0;
    private static int progress = 0;
    private static final StringBuilder RES = new StringBuilder();

    public static void resetBar() {
        all_progresses = 0;
        progress = 0;
        RES.delete(0, RES.length());
    }

    @Override
    public Object secureCompute(CompEnv<T> e, Object[] o) throws Exception {
        T[][][] a = (T[][][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[0]).getInputs();
        T[][][] b = (T[][][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[1]).getInputs();
        
        T[][][] a1s = (T[][][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[0]).getBFs1sCnt();
        T[][][] b1s = (T[][][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[1]).getBFs1sCnt();
        
        boolean[][] aChkDci = (boolean[][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[0]).getBFs1schkDCi();
        boolean[][] bChkDci = (boolean[][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[1]).getBFs1schkDCi();
        


        T[][] weights_a = (T[][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[0]).getWs();
        T[][] weights_b = (T[][]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[1]).getWs();

        T[] threshold_a = (T[]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[0]).getThreshold();
        T[] threshold_b = (T[]) ((PatientLinkage4GadgetWsBF1sCntInputs) o[1]).getThreshold();

        int rows = a.length;
        int cols = b.length;

        T[][] ret_a = e.newTArray(rows, cols);
        T[][][] ret_b = e.newTArray(rows, cols, 0);
        T[][][] ret_c = e.newTArray(rows, cols,0);
        T[][] ret0 =e.newTArray(2*a.length, 32),ret00;//= e.newTArray(r, dl); //data_len
        T[] Rhs ,/*=e.newTArray(dl),*/ Lhs;//=e.newTArray(dl);
        
        //final T[][] ws = e.newTArray(a[0].length, 0);

        PatientLinkageBFGCLib<T> pt_lib = new PatientLinkageBFGCLib<>(e);

        //for (int k = 0; k < ws.length; k++) {
        //    ws[k] = pt_lib.add(weights_a[k], weights_b[k]);
        //}
        //T[] threshold = pt_lib.add(threshold_a, threshold_b);
        
        //ret00 = pt_lib.matchRcvsBF(a[0], b[0],a1s[0], b1s[0]);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
              /*  if (progress % 100 == 0 && all_progresses > 0) {
                    synchronized (RES) {
                        double tmp = (progress) * 100.0 / all_progresses;
                        System.out.print(String.format("[%s]%.2f%%\r", progress((int) tmp), tmp));
                    }
                }
                progress++;*/
                //synchronized (aChkDci) {
                    if(aChkDci[i][j] && bChkDci[i][j]){//&& bChkDci[i][j]){
                    //ret_b[i][j] = pt_lib.matchRcvsWs(a[i], b[j], ws);
                    //ret_a[i][j] = pt_lib.compare(ret_b[i][j], threshold);
                //...    ret0 = pt_lib.matchRcvsBF(a[i], b[j],a1s[i], b1s[j]);
                    //ret_b[i][j]=ret0[0];
                    //System.out.println("ret0-length="+ret0[0].length);
                 //...Lhs=pt_lib.leftPublicShift(ret0[0],7); //ret0[0];//
                    //System.out.println("LHS-length="+Lhs.length);
                    //RHS is the denom( UnionofBFs)*TR, LHS is numerator(2*IntersecBF )*128 
                //... Rhs=pt_lib.multiply(ret0[1],threshold_a);
                    ret_c[i][j] = pt_lib.padSignal(pt_lib.add(a1s[i][0],b1s[i][0]),dataSize);//pt_lib.getUnion(a[i][0], b[j][0]);
                    ret_b[i][j] = pt_lib.getInters(a[i][0], b[j][0]);
                //... ret_a[i][j] = pt_lib.geq(Lhs, Rhs);
                    ret_a[i][j] = e.newT(true);
                // ...ret_b[i][j]=ret0[0];//pt_lib.toSignals(100, 32);
                //... ret_c[i][j]=ret0[1];//pt_lib.toSignals(500, 32);//
                }
                else{
                    ret_a[i][j] = e.newT(false);
                    ret_b[i][j]=pt_lib.toSignals(0, 32); //e.newTArray(32) ;
                    ret_c[i][j]=pt_lib.toSignals(1, 32);//e.newTArray(32);//ret00[1];
                }
                //}
                
                
            }
        }
        
        return new PatientLinkageWssBFOutput<>(ret_a, ret_b,ret_c);
    }
    
    public static String progress(int pct) {
        RES.delete(0, RES.length());
        pct /= 10;
        for (int i = 0; i <= pct; i++) {
            RES.append('#');
        }
        while (RES.length() <= 10) {
            RES.append(' ');
        }
        return RES.toString();
    }
}
