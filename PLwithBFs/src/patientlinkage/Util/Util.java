/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.Util;

import patientlinkage.GarbledCircuit.PatientLinkageGadget;
import com.opencsv.CSVReader;
import flexsc.CompEnv;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang.StringUtils;
import patientlinkage.DataType.Helper;
import patientlinkage.DataType.PatientLinkage;
import static java.util.Arrays.copyOf;
import patientlinkage.DataType.PatientLinkageWssBFOutput;

/**
 *
 * @author cf
 */
public class Util {

    public static final int BYTE_BITS = 8;
    public static final int  DATA_LEN = 32;// 16;

    public static boolean[][][] generateDummyArray(boolean[][][] src) {
        boolean[][][] retArr = new boolean[src.length][][];

        for (int i = 0; i < src.length; i++) {
            retArr[i] = new boolean[src[i].length][];
            for (int j = 0; j < src[i].length; j++) {
                retArr[i][j] = new boolean[src[i][j].length];
            }
        }
        return retArr;
    }

    public static boolean[][][] generateDummyArray(boolean[][][] src, int len) {
        boolean[][][] retArr = new boolean[len][][];
        int width = src[0].length;

        for (int i = 0; i < retArr.length; i++) {
            retArr[i] = new boolean[width][];
            for (int j = 0; j < width; j++) {
                retArr[i][j] = new boolean[src[0][j].length];
            }
        }
        return retArr;
    }

    public static int[][] linspace(int pt0, int pt1, int num_of_intervals) {
        assert num_of_intervals > 0 : "math1.linspace: num of intervals > 0";
        int[] ret = new int[num_of_intervals + 1];

        int[][] ret1 = new int[num_of_intervals][2];

        ret[0] = pt0;
        ret[num_of_intervals] = pt1;

        ret1[0][0] = pt0;
        ret1[num_of_intervals - 1][1] = pt1;

        int int_len = (pt1 - pt0) / num_of_intervals;

        for (int i = 1; i < num_of_intervals; i++) {
            ret[i] = ret[i - 1] + int_len;
            ret1[i - 1][1] = ret[i];
            ret1[i][0] = ret[i];
        }

        return ret1;
    }
    
    public static int getPtLnkCnts(int[][] ranges1, int opp_num){
        int ptLnkCnts = 0;
        
        for (int[] ranges11 : ranges1) {
            ptLnkCnts += (ranges11[1] - ranges11[0]) * opp_num;
        }
        
        return ptLnkCnts;
    }

    public static <T> T[][] unifyArray(Object[] input, CompEnv<T> eva, int len) {
        T[][] ret = eva.newTArray(len, 0);
        int index = 0;

        for (int i = 0; i < input.length; i++) {
            T[][] tmp = ((T[][]) input[i]);
            for (int j = 0; j < tmp.length; j++) {
                ret[index++] = tmp[j];
            }
        }
        return ret;
    }
    
    
    
    
    public static <T> Object VunifyObjArray(Object[] input, CompEnv<T> eva, int len_b) {
        
        int rows=((PatientLinkageWssBFOutput)input[0]).getA().length;
        
        int cols=len_b;
        T[][] Ha= eva.newTArray(rows,cols);
        T[][][] Hb= eva.newTArray(rows, cols, 0);
        //Object Hobj=new PatientLinkageWssBFOutput( eva,  rows,  cols);
        int hn=0;
        for (int i = 0; i < input.length; i++) {
            T[][] a=(T[][])((PatientLinkageWssBFOutput)input[i]).getA();
            T[][][] b=(T[][][])((PatientLinkageWssBFOutput)input[i]).getB();
            int cols0=a[0].length;
            
            for(int k=0;k<rows;k++)
            {
              for(int n=0;n<cols0;n++){
                 Ha[k][n+hn]=a[k][n];
                 Hb[k][n+hn]=b[k][n];
                }
           
            }
             hn=hn+cols0;
        }    
        
        return (new PatientLinkageWssBFOutput(Ha,Hb ));
    }

    public static <T> T[][][] unifyArray1(Object[] input, CompEnv<T> eva, int len) {

        T[][][] ret = eva.newTArray(len, 0, 0);
        int index = 0;

        for (Object input1 : input) {
            T[][][] tmp = (T[][][]) input1;
            for (T[][] tmp1 : tmp) {
                ret[index++] = tmp1;
            }
        }
        return ret;
    }
    
    public static <T> T[] unifyArrayWithF(Object[] input, CompEnv<T> eva, int len) {
        T[] ret = eva.newTArray(len);
        int index = 0;
        
        for (Object input1 : input) {
            T[] tmp = (T[]) input1;
            for (T tmp1 : tmp) {
                ret[index++] = tmp1;
            }
        }

        return ret;
    }
   
          
    public static boolean[][][] extractArray(boolean[][][] arr1, ArrayList<PatientLinkage> ptl_arr, String role) {
        boolean[][][] res = new boolean[ptl_arr.size()][][];
        switch (role) {
            case "generator":
                int ind;
                for (int n = 0; n < ptl_arr.size(); n++) {
                    ind = ptl_arr.get(n).getI();
                    res[n] = arr1[ind];
                }
                break;
            case "evaluator":
                for (int n = 0; n < ptl_arr.size(); n++) {
                    ind = ptl_arr.get(n).getJ();
                    res[n] = arr1[ind];
                }
                break;
        }

        return res;
    }

    public static boolean[][][] encodeCobinationAsJAMIA4Criteria(String[][] data1, int[][] properties_bytes) {
        //12, 11, 9, 8
        assert data1[0].length == properties_bytes[0].length;
        boolean[][][] ret = new boolean[data1.length][properties_bytes.length][];

        for (int i = 0; i < data1.length; i++) {
            for (int j = 0; j < properties_bytes.length; j++) {
                String temp = "";
                for (int k = 0; k < properties_bytes[j].length; k++) {
                    temp += resizeString(data1[i][k], properties_bytes[j][k]);
                }
                ret[i][j] = bytes2boolean(temp.getBytes(StandardCharsets.US_ASCII));
            }
        }

        return ret;
    }

    public static String resizeString(String str, int len) {
        if (str.length() < len) {
            return StringUtils.rightPad(str, len);
        } else if (str.length() > len) {
            return StringUtils.substring(str, 0, len);
        } else {
            return str;
        }
    }

    public static boolean[] bytes2boolean(byte[] vals) {
        boolean[] ret = new boolean[BYTE_BITS * vals.length];

        for (int i = 0; i < vals.length; i++) {
            System.arraycopy(fromByte(vals[i]), 0, ret, i * BYTE_BITS, BYTE_BITS);
        }

        return ret;
    }

    
    public static boolean[] fromByte(byte value) {

        boolean[] res = new boolean[BYTE_BITS];
        for (int i = 0; i < BYTE_BITS; i++) {
            res[i] = (((value >> i) & 1) != 0);
        }
        return res;
    }

    public static boolean[] fromInt(int value, int width) {
        boolean[] res = new boolean[width];
        for (int i = 0; i < width; i++) {
            res[i] = (((value >> i) & 1) != 0);
        }

        return res;
    }

    public static int toInt(boolean[] value) {
        int res = 0;
        for (int i = 0; i < value.length; i++) {
            res = (value[i]) ? (res | (1 << i)) : res;
        }

        return res;
    }

    public static String[][] readAndProcessCSV(String FileName, int records_num) {
        String[][] data1 = null;
        int properties_num = 6;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] nextLine;
            data1 = new String[records_num][properties_num];
            reader.readNext();
            int ind = 0;
            while ((nextLine = reader.readNext()) != null && ind < records_num) {
                data1[ind][0] = nextLine[1].toLowerCase();
                data1[ind][1] = nextLine[2].toLowerCase();
                data1[ind][2] = sdx.encode(nextLine[1]).toLowerCase();
                data1[ind][3] = sdx.encode(nextLine[2]).toLowerCase();
                data1[ind][4] = nextLine[6].replaceAll("-", "");
                data1[ind][5] = nextLine[11].replaceAll("-", "");

                ind++;

            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        return data1;
    }

    public static boolean[][][] readAndEncode(String FileName, int[][] lens) {
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        int properties_num = lens[0].length;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            reader.readNext();
            while ((strs = reader.readNext()) != null) {

                String[] coms_strs = new String[lens.length];
                Arrays.fill(coms_strs, "");
                for (int i = 0; i < properties_num; i++) {
                    String temp = strs[i].replace("-", "").toLowerCase();
                    for (int j = 0; j < coms_strs.length; j++) {
                        if (lens[j][i] > 65536) {
                            coms_strs[j] += sdx.soundex(temp);
                        } else {
                            coms_strs[j] += resizeString(temp, lens[j][i]);
                        }
                    }
                }
                boolean[][] bool_arr = new boolean[coms_strs.length][];
                for (int j = 0; j < coms_strs.length; j++) {
                    bool_arr[j] = bytes2boolean(coms_strs[j].getBytes(StandardCharsets.US_ASCII));
                }
                retArrList.add(bool_arr);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean[][][] bool_ret = new boolean[retArrList.size()][][];
        for (int i = 0; i < bool_ret.length; i++) {
            bool_ret[i] = retArrList.get(i);
        }

        return bool_ret;
    }

    public static boolean[][][] readAndEncode(String FileName, int[][] lens, int hash_len) {
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        int properties_num = lens[0].length;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            String[] strs;
            reader.readNext();
            while ((strs = reader.readNext()) != null) {

                String[] coms_strs = new String[lens.length];
                Arrays.fill(coms_strs, "");
                for (int i = 0; i < properties_num; i++) {
                    String temp = strs[i].replace("-", "").toLowerCase();
                    for (int j = 0; j < coms_strs.length; j++) {
                        if (lens[j][i] > 65536) {
                            coms_strs[j] += sdx.soundex(temp);
                        } else {
                            coms_strs[j] += resizeString(temp, lens[j][i]);
                        }
                    }
                }
                boolean[][] bool_arr = new boolean[coms_strs.length][];
                for (int j = 0; j < coms_strs.length; j++) {
 //                   bool_arr[j] = bytes2boolean(coms_strs[j].getBytes(StandardCharsets.US_ASCII));
                    bool_arr[j] = bytes2boolean(copyOf(digest.digest(coms_strs[j].getBytes(StandardCharsets.UTF_8)), hash_len));
                }
                retArrList.add(bool_arr);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean[][][] bool_ret = new boolean[retArrList.size()][][];
        for (int i = 0; i < bool_ret.length; i++) {
            bool_ret[i] = retArrList.get(i);
        }

        return bool_ret;
    }
    
    public static Helper readAndEncodeWithProps(String FileName, int[][] lens) {
        Helper ret = new Helper();
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        int properties_num = lens[0].length;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            ret.pros = reader.readNext();
            ret.updatingrules(lens);
            while ((strs = reader.readNext()) != null) {
                ret.IDs.add(strs[0]);
                String[] coms_strs = new String[lens.length];
                Arrays.fill(coms_strs, "");
                for (int i = 0; i < properties_num; i++) {
                    String temp = strs[i].replace("-", "").toLowerCase();
                    for (int j = 0; j < coms_strs.length; j++) {
                        if (lens[j][i] > (Integer.MAX_VALUE/2)) {
                            coms_strs[j] += sdx.soundex(temp) + resizeString(temp, Integer.MAX_VALUE - lens[j][i]);
                        } else {
                            coms_strs[j] += resizeString(temp, lens[j][i]);
                        }
                    }
                }
                boolean[][] bool_arr = new boolean[coms_strs.length][];
                for (int j = 0; j < coms_strs.length; j++) {
                    bool_arr[j] = bytes2boolean(coms_strs[j].getBytes(StandardCharsets.US_ASCII));
                }
                retArrList.add(bool_arr);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        ret.data_bin = new boolean[retArrList.size()][][];
        for (int i = 0; i < ret.data_bin.length; i++) {
            ret.data_bin[i] = retArrList.get(i);
        }

        return ret;
    }
//-----------------------------------
    public static boolean[] BFstr2boolean(char[] vals) {
        boolean[] ret = new boolean[vals.length];

        for (int i = 0; i < vals.length; i++) {
            ret[i]= (vals[i]=='1');    
        //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
        }

        return ret;
    }
//-----------------------------------
 public static  int[] BFstrNumOfOnesInBFparts(char[] vals,int nOfParts) {
        
        
        int partSize=vals.length/ nOfParts;
        int partStart=0, partEnd=partSize-1;
        int[] Rets=new int[nOfParts];
        //assert(partEnd< vals.length && partStart< vals.length);
        for(int k=0;k<nOfParts;k++){
            Rets[k]=0;
            if(k==(nOfParts-1)) partEnd=vals.length-1;
            for (int i = partStart; i <=partEnd; i++) {
                Rets[k]+= (vals[i]=='1')? 1:0;    
        //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
            }
           partStart=partEnd+1;
           partEnd=partEnd+partSize-1; 
           
            
        }
       
        
        return Rets;
    }   
 //-----------------------------------    
 public static  boolean[] BFstrNumOfOnes(char[] vals) {
        int ret = 0;
        boolean[] retBool ;
        for (int i = 0; i < vals.length; i++) {
            ret+= (vals[i]=='1')? 1:0;    
        //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
        }
        retBool=fromInt(ret, DATA_LEN );
        //retBool=fromInt(ret, 16 );
        return retBool;
    }  
//-----------------------------------    
 public static  int BFstrIntNumOfOnes(char[] vals) {
        int ret = 0;
        
        for (int i = 0; i < vals.length; i++) {
            ret+= (vals[i]=='1')? 1:0;    
        //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
        }
       
        return ret;
    }   
 
 
 //-------------------------------------
 public static boolean[][] copyOfRange2d(boolean[][] chkDCi, int[] hRange, int[] vRange){
     boolean[][] retArr=new boolean[hRange[1]-hRange[0]][vRange[1]-vRange[0]];
     int i0=0,j0=0;
        for(int i=hRange[0];i<hRange[1];i++){
            j0=0;
            for(int j=vRange[0];j<vRange[1];j++,j0++)
                retArr[i0][j0]=chkDCi[i][j];
            i0++;
        }
        return retArr;
 }
 
 //-----------------------------------
     public static Helper readBFsWithProps(String FileName, int[][] lens) {
        Helper ret = new Helper();
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        ArrayList<boolean[][]> retNumOfOnesArrList = new ArrayList<>();
        ArrayList<int[]> retIntNumOfOnesArrList = new ArrayList<>();
        int BFs_num = lens[0].length;
        //Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            ret.pros = reader.readNext();
            ret.updatingrules(lens);
            while ((strs = reader.readNext()) != null) {
                ret.IDs.add(strs[0]);
                String[] coms_strs = new String[lens[0].length];
                Arrays.fill(coms_strs, "");
                int j=0;
                for (int i = 0; i < BFs_num; i++) {
                    String temp = strs[i+1].replace("-", "").toLowerCase();
                    
                        if (lens[0][i] > 0) {
                            coms_strs[j] = temp;
                            j++;
                        }
                }
                boolean[][] bool_arr = new boolean[j][];
                
                int[] nOf1sInparts; 
                boolean[][] boolNumOfOnes = new boolean[j][];
                int[] intNumOfOnes=new int[j];
                for (int k = 0; k < j; k++) {
                    bool_arr[k] = BFstr2boolean(coms_strs[k].toCharArray());
                    intNumOfOnes[k]=BFstrIntNumOfOnes(coms_strs[k].toCharArray());
                    boolNumOfOnes[k]=fromInt(intNumOfOnes[k], DATA_LEN);
                    //boolNumOfOnes[k]=BFstrNumOfOnes(coms_strs[k].toCharArray());
                    /*nOf1sInparts=BFstrNumOfOnesInBFparts( coms_strs[k].toCharArray(),3);
                    System.out.print("#1s in each part=");
                    for(int hh=0;hh<3;hh++)
                        System.out.print("P"+hh+":"+nOf1sInparts[hh]+" , ");
                    System.out.println();*/
                }
                retArrList.add(bool_arr);
                retNumOfOnesArrList.add(boolNumOfOnes);
                retIntNumOfOnesArrList.add(intNumOfOnes);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        ret.data_bin = new boolean[retArrList.size()][][];
        ret.numOfOnesInBFs = new boolean[retNumOfOnesArrList.size()][][];
        ret.intNumOfOnesInBFs =new int[retIntNumOfOnesArrList.size()][];
        for (int i = 0; i < ret.data_bin.length; i++) {
            ret.data_bin[i] = retArrList.get(i);
            ret.numOfOnesInBFs[i] = retNumOfOnesArrList.get(i);
            ret.intNumOfOnesInBFs[i] =retIntNumOfOnesArrList.get(i);
        }

        return ret;
    }

    
    
    public static void usagemain() {
        String help_str
                = ""
                + String.format("     -config     <path>      : input configure file path\n")
                + String.format("     -data       <path>      : input data file path\n")
                + String.format("     -help                   : show help");
        System.out.println(help_str);
    }


}
