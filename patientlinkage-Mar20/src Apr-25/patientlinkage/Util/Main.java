/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.Util;

import com.sun.org.apache.xalan.internal.xsltc.util.IntegerArray;
import flexsc.CompPool;
import flexsc.Mode;
import gc.GCSignal;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import patientlinkage.DataType.BFHelper;
import patientlinkage.DataType.Helper;
import patientlinkage.DataType.PatientLinkage;
import patientlinkage.GarbledCircuit.PatientLinkageGadget;
import static patientlinkage.GarbledCircuit.PatientLinkageGadget.getIntArrayFromStrs;
import static patientlinkage.Util.Util.fromInt;
import patientlinkage.parties.Env;
import patientlinkage.parties.EnvWss;
import patientlinkage.parties.EnvWssBF;
import patientlinkage.parties.EnvWssBFs;
import patientlinkage.parties.EnvWssBFsWith1scnt;
import patientlinkage.parties.EnvWssF;
import patientlinkage.parties.Gen;
import patientlinkage.parties.GenWss;
import patientlinkage.parties.GenWssBF;
import patientlinkage.parties.GenWssBFs;
import patientlinkage.parties.GenWssBFsWith1scnt;
import patientlinkage.parties.GenWssF;

/**
 *
 * @author cf
 */
public class Main {

    /**
     * starting linkage algorithm
     *
     * @param args the passing parameters
     */
    public static void startLinkage(String[] args) {
        String file_config = null;
        String file_data = null;
        Properties configs;
        String party = "nobody";
        String addr = null;
        int port = -1;
        int threshold = 1;
        int threads = 1;
        int records = 0;
        boolean filter = false;
        int data_len = 32;// 16;
        String results_save_path = null;
        boolean useBFs = false;
        boolean useCombBF = false;// BFfilter: property, True:  Use combinations of the BFs , False: use  BFs one by one and filter results by removing
        // those ones matched by the previous BF
        boolean PassNumOfOnes =   true; //Send BF's number of ones with each record BFs instead of securely computing it.
        boolean useXMLconfigf = false;
        ArrayList<int[]> prop_array = new ArrayList<>();
        ArrayList<Integer> ws = new ArrayList<>();
        ArrayList<int[]> BFprop_array = new ArrayList<>();
        ArrayList<Integer> BFws = new ArrayList<>();
        ArrayList<PatientLinkage> res = null;
        ArrayList<PatientLinkage> tres = null;
        ArrayList<PatientLinkage>[] AllBFsRes = null; //(ArrayList<PatientLinkage>[])new ArrayList[4];

        boolean[][][] data_bin;
        String[] tmp = null;
        ArrayList<String> PartyA_IDs = null;
        ArrayList<String> PartyB_IDs = null;

        if (args.length < 1) {
            usagemain();
            return;
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) != '-') {
                usagemain();
                return;
            }
            try {
                switch (args[i].replaceFirst("-", "")) {
                    case "config":
                        file_config = args[++i];
                        break;
                    case "data":
                        file_data = args[++i];
                        break;
                    case "help":
                        usagemain();
                        break;
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println("please input the configure file or data file!");
            } catch (IllegalArgumentException e) {
                System.out.println(args[i] + " is illegal input");
            }
        }

        if (useXMLconfigf) {
            configs = getConfigsFromXML(file_config);
            party = configs.getProperty("party");
            addr = configs.getProperty("address");
            port = Integer.parseInt(configs.getProperty("port"));
            threshold = Integer.parseInt(configs.getProperty("threshold"));
            threads = Integer.parseInt(configs.getProperty("threads"));
            records = Integer.parseInt(configs.getProperty("records"));
            filter = Integer.parseInt(configs.getProperty("filter")) == 1; //false;

            results_save_path = configs.getProperty("results_save_path");
            useBFs = Integer.parseInt(configs.getProperty("method")) == 2;
            useCombBF = Integer.parseInt(configs.getProperty("BFfilter")) == 0; //1 means use single BFs and do filter out matched recs 
            
            if (configs.containsKey("comBF")) {
                String BFs = configs.getProperty("comBF");
                tmp = BFs.trim().split("->");
                BFprop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                // BFws.add(Integer.parseInt(tmp[1].trim()));

            }
            if (configs.containsKey("BFweights")) {
                String strBFsw = configs.getProperty("BFweights");
                int[] tmpint;
                tmpint = getIntArrayFromStrs(strBFsw.trim());
                for (int el : tmpint) {
                    BFws.add(el);
                }

            }
            for (int ki = 1; configs.containsKey("com." + ki); ki++) {
                String combs = configs.getProperty("com." + ki);
                tmp = combs.trim().split("->");
                prop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                ws.add(Integer.parseInt(tmp[1].trim()));

            }

        }

        if (!useXMLconfigf) {
            try (FileReader fid_config = new FileReader(file_config); BufferedReader br_config = new BufferedReader(fid_config)) {
                String line;
                while ((line = br_config.readLine()) != null) {
                    String[] strs1 = line.split("\\|");
                    if (strs1.length < 1) {
                        continue;
                    }

                    String str = strs1[0].trim();

                    if (str.equals("") || !str.contains(":")) {
                        continue;
                    }

                    String[] strs2 = str.split(":");

                    switch (strs2[0].trim()) {
                        case "party":
                            party = strs2[1].trim();
                            break;
                        case "address":
                            addr = strs2[1].trim();
                            break;
                        case "port":
                            port = Integer.parseInt(strs2[1].trim());
                            break;
                        case "threshold":
                            threshold = Integer.parseInt(strs2[1].trim());
                            //threshold = 96;//89~=70%, 96~=75%
                            break;
                        case "threads":
                            threads = Integer.parseInt(strs2[1].trim());
                            break;
                        case "filter":
                            filter = Integer.parseInt(strs2[1].trim()) == 1;
                            break;
                        case "records":
                            records = Integer.parseInt(strs2[1].trim());
                            break;
                        case "method":

                            useBFs = Integer.parseInt(strs2[1].trim()) == 2;
                            break;
                        case "BFfilter":

                            useCombBF = Integer.parseInt(strs2[1].trim()) == 0; // 0: means no BF filtering
                            break;    
                        case "results_save_path":
                            results_save_path = strs2[1].trim();
                            break;
                        case "com":
                            tmp = strs2[1].trim().split("->");
                            prop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                            ws.add(Integer.parseInt(tmp[1].trim()));
                            break;

                        case "comBF":
                            tmp = strs2[1].trim().split("->");
                            BFprop_array.add(getIntArrayFromStrs(tmp[0].trim()));
                            break;

                        case "BFweights":

                            int[] tmpint;
                            tmpint = getIntArrayFromStrs(strs2[1].trim());
                            for (int el : tmpint) {
                                BFws.add(el);
                            }
                            break;
                        default:
                            System.out.println("no property" + strs2[0].trim() + ", please check the configure file!");
                            throw new AssertionError();
                    }

                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
            }

        } //!useXMLconfigf  

        int[][] array_int1;
        boolean[][] ws_bin;

        Helper help1;

        if (useBFs) {
            System.out.println("start encoding BF data ...");
            array_int1 = new int[BFprop_array.size()][];
            for (int i = 0; i < array_int1.length; i++) {
                array_int1[i] = BFprop_array.get(i);
            }

            help1 = Util.readBFsWithProps(file_data, array_int1);
            data_bin = help1.data_bin;

            //if(PassNumOfOnes)
            //      numOf1s= help1.numOfOnesInBFs;
            int[] usedBFws = new int[array_int1[0].length];
            int numOfUsedBFs = 0;  //I need to know how many BFs with properties !=0 (used)
            // Which is also the 2nd dim of data_bin
            for (int i = 0; i < array_int1[0].length; i++) {
                if (array_int1[0][i] > 0) {
                    usedBFws[numOfUsedBFs] = BFws.get(i);
                    numOfUsedBFs++;
                }
            }

            ws_bin = new boolean[numOfUsedBFs][];
            for (int i = 0; i < ws_bin.length; i++) {
                ws_bin[i] = fromInt(usedBFws[i], data_len);
            }

            boolean[] threshold_bin = fromInt(threshold, data_len);
            CompPool.MaxNumberTask = threads;
            System.out.println(" encoding data ...done");
            long t0, t1, tot_t = 0;
            //ArrayList<String> B_IDs;
            //ArrayList<String> A_IDs;
            AllBFsRes=(ArrayList<PatientLinkage>[])new ArrayList[numOfUsedBFs];
            switch (party) {
                case "generator":
                    PartyA_IDs = help1.IDs;
                    
                    if (useCombBF) {
                        
                        System.out.println("Gen. starts patientlinkage algorithm with Comb. of BFs ...");
                        
                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);
                            int BRecsToProcess = records;
                            //int ArecsToProcess = data_bin.length;
                            tot_t = 0;
                            for (int i = 0; i < numOfUsedBFs ; i++) {
                                t0 = System.currentTimeMillis();

                                BFData = BFHlp1.getData_bin(i);
                                numOf1s = BFHlp1.getBF1sCnt(i);
                                GenWssBFsWith1scnt<GCSignal> gen = new GenWssBFsWith1scnt<>(port, Mode.REAL, threads, BFData, numOf1s, ws_bin, threshold_bin, BRecsToProcess, PartyA_IDs);
                                gen.implement();
                                tres = gen.getLinkage();
                                //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                if (AllBFsRes[i] == null) {
                                    AllBFsRes[i] = gen.getLinkage();
                                    //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    AllBFsRes[i].addAll(tres);

                                    //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;
                                tot_t += t1;
                                //System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                PartyB_IDs = gen.getPartyB_IDs();

                                //PartyB_IDs.addAll(B_IDs);
                                //BFData= BFHlp1.getData_bin(i);
                                //ArecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches" );
                            } //for i
                            System.out.println("The Total running time of  patientlinkage algorithm is " + tot_t / 1e3 + " seconds!");
                        } //if PassNumOfOnes
                        else {
                            //System.out.println("Gen. starts patientlinkage algorithm single BFs ...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, records);
                            int BRecsToProcess = records;
                            //int ArecsToProcess = data_bin.length;
                            tot_t = 0;
                            for (int i = 0; i < numOfUsedBFs ; i++) {
                                t0 = System.currentTimeMillis();

                                BFData = BFHlp1.getData_bin(i);
                                GenWssBFs<GCSignal> gen = new GenWssBFs<>(port, Mode.REAL, threads, BFData, ws_bin, threshold_bin, BRecsToProcess, PartyA_IDs);
                                gen.implement();
                                tres = gen.getLinkage();
                                //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                if (AllBFsRes[i] == null) {
                                    AllBFsRes[i] = gen.getLinkage();
                                    //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    AllBFsRes[i].addAll(tres);

                                    //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;
                                tot_t += t1;
                                //System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                PartyB_IDs = gen.getPartyB_IDs();

                                //PartyB_IDs.addAll(B_IDs);
                                //BFData= BFHlp1.getData_bin(i);
                                //ArecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches" );
                            } //for i

                            System.out.println("The Total running time of  patientlinkage algorithm is " + tot_t / 1e3 + " seconds!");
                        }
                        
                        
                        /*==================================Original before Apr 18
                        //PartyA_IDs = help1.IDs;
                        System.out.println("Gen. starts patientlinkage algorithm BFs Combined...");
                        t0 = System.currentTimeMillis();
                        if (PassNumOfOnes) {
                            System.out.println(" patientlinkage With BF #1s passed...");
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;
                            GenWssBFsWith1scnt<GCSignal> gen = new GenWssBFsWith1scnt<>(port, Mode.REAL, threads, data_bin, numOf1s, ws_bin, threshold_bin, records, PartyA_IDs);
                            gen.implement();
                            res = gen.getLinkage();
                            t1 = System.currentTimeMillis() - t0;
                            System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                            PartyB_IDs = gen.getPartyB_IDs();
                        } else {
                            GenWssBF<GCSignal> gen = new GenWssBF<>(port, Mode.REAL, threads, data_bin, ws_bin, threshold_bin, records, PartyA_IDs);
                            gen.implement();

                            res = gen.getLinkage();
                            t1 = System.currentTimeMillis() - t0;
                            System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                            PartyB_IDs = gen.getPartyB_IDs();
                        }
                ========================================*/
                    } else { //single BFs Gen

                        // TODO: I think we need to supply the other party with the IDs of those matched records only
                        //PartyA_IDs= help1.IDs; //help1.getIDs();
                        //PartyB_IDs=null;
                        //A_IDs=null;
                        //A_IDs.addAll(PartyA_IDs);
                        System.out.println("Gen. starts patientlinkage algorithm single BFs ...");
                        // t0 = System.currentTimeMillis();
                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);
                            int BRecsToProcess = records;
                            int ArecsToProcess = data_bin.length;
                            tot_t = 0;
                            for (int i = 0; i < numOfUsedBFs && BRecsToProcess > 0 && ArecsToProcess > 0; i++) {
                                t0 = System.currentTimeMillis();

                                BFData = BFHlp1.getData_bin(i);
                                numOf1s = BFHlp1.getBF1sCnt(i);
                                GenWssBFsWith1scnt<GCSignal> gen = new GenWssBFsWith1scnt<>(port, Mode.REAL, threads, BFData, numOf1s, ws_bin, threshold_bin, BRecsToProcess, PartyA_IDs);
                                gen.implement();
                                tres = gen.getLinkage();
                                tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                if (res == null) {
                                    res = gen.getLinkage();
                                    BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    res.addAll(tres);

                                    BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;
                                tot_t += t1;
                                //System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                PartyB_IDs = gen.getPartyB_IDs();

                                //PartyB_IDs.addAll(B_IDs);
                                //BFData= BFHlp1.getData_bin(i);
                                ArecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches, A new set size:  " + ArecsToProcess + " And B's :" + BRecsToProcess);
                            } //for i
                            System.out.println("The Total running time of  patientlinkage algorithm is " + tot_t / 1e3 + " seconds!");
                        } //if PassNumOfOnes
                        else {
                            //System.out.println("Gen. starts patientlinkage algorithm single BFs ...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, records);
                            int BRecsToProcess = records;
                            int ArecsToProcess = data_bin.length;
                            tot_t = 0;
                            for (int i = 0; i < numOfUsedBFs && BRecsToProcess > 0 && ArecsToProcess > 0; i++) {
                                t0 = System.currentTimeMillis();

                                BFData = BFHlp1.getData_bin(i);
                                GenWssBFs<GCSignal> gen = new GenWssBFs<>(port, Mode.REAL, threads, BFData, ws_bin, threshold_bin, BRecsToProcess, PartyA_IDs);
                                gen.implement();
                                tres = gen.getLinkage();
                                tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                if (res == null) {
                                    res = gen.getLinkage();
                                    BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    res.addAll(tres);

                                    BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;
                                tot_t += t1;
                                //System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                PartyB_IDs = gen.getPartyB_IDs();

                                //PartyB_IDs.addAll(B_IDs);
                                //BFData= BFHlp1.getData_bin(i);
                                ArecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches, A new set size:  " + ArecsToProcess + " And B's :" + BRecsToProcess);
                            } //for i

                            System.out.println("The Total running time of  patientlinkage algorithm is " + tot_t / 1e3 + " seconds!");
                        } //if PassNumOfOnes
                    } // else if Comb
                    break;
                    
                    
                    
                case "evaluator":
                    PartyB_IDs = help1.IDs;
                    if (useCombBF) {
                        System.out.println("Eval. starts patientlinkage algorithm with Combs of BFs ...");
                        t0 = System.currentTimeMillis();
                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin,numOf1s, records);

                            //int BRecsToProcess = data_bin.length;
                            int ARecsToProcess = records;
                            for (int i = 0; i < numOfUsedBFs ; i++) {
                                BFData = BFHlp1.getData_bin(i);
                                numOf1s = BFHlp1.getBF1sCnt(i);
                                EnvWssBFsWith1scnt<GCSignal> env = new EnvWssBFsWith1scnt<>(addr, port, Mode.REAL, threads, BFData, numOf1s, ws_bin, threshold_bin, ARecsToProcess, PartyB_IDs);
                                env.implement();
                                tres = env.getLinkage();
                                //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                // get the linkages based on the org. indexes

                                if (AllBFsRes[i] == null) {
                                    AllBFsRes[i] = env.getLinkage();
                                    //ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    AllBFsRes[i].addAll(tres);

                                    //ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;

                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                //if (i == 0) {
                                PartyA_IDs = env.getPartyA_IDs();
                                //}
                                //PartyB_IDs.addAll(B_IDs);
                                //BRecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches");
                            }
                        } else { // if ! PassNumOfOnes
                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, records);

                            //int BRecsToProcess = data_bin.length;
                            int ARecsToProcess = records;
                            for (int i = 0; i < numOfUsedBFs ; i++) {
                                BFData = BFHlp1.getData_bin(i);

                                EnvWssBFs<GCSignal> env = new EnvWssBFs<>(addr, port, Mode.REAL, threads, BFData, ws_bin, threshold_bin, ARecsToProcess, PartyB_IDs);
                                env.implement();
                                tres = env.getLinkage();
                                //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                // get the linkages based on the org. indexes

                                if (AllBFsRes[i] == null) {
                                    AllBFsRes[i] = env.getLinkage();
                                    //ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    AllBFsRes[i].addAll(tres);

                                    //ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;

                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                //if (i == 0) {
                                PartyA_IDs = env.getPartyA_IDs();
                                //}
                                //PartyB_IDs.addAll(B_IDs);
                                //BRecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches " );
                            }

                        }
                        
                        
                        
                        /*==================================Original before Apr 18
                        System.out.println("Eval. starts patientlinkage algorithm BFs Combined...");
                        t0 = System.currentTimeMillis();

                        if (PassNumOfOnes) {
                            System.out.println(" patientlinkage With BF #1s passed...");
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;
                            EnvWssBFsWith1scnt<GCSignal> eva = new EnvWssBFsWith1scnt<>(addr, port, Mode.REAL, threads, data_bin, numOf1s, ws_bin, threshold_bin, records, PartyB_IDs);
                            eva.implement();
                            res = eva.getLinkage();
                            t1 = System.currentTimeMillis() - t0;
                            System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                            PartyA_IDs = eva.getPartyA_IDs();
                        } else {
                            EnvWssBF<GCSignal> eva = new EnvWssBF<>(addr, port, Mode.REAL, threads, data_bin, ws_bin, threshold_bin, records, PartyB_IDs);
                            eva.implement();
                            res = eva.getLinkage();
                            t1 = System.currentTimeMillis() - t0;
                            System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                            PartyA_IDs = eva.getPartyA_IDs();
                        }
                        ===========================*/
                    } else { // single BFs
                        System.out.println("Eval. starts patientlinkage algorithm single BFs ...");
                        t0 = System.currentTimeMillis();
                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin,numOf1s, records);

                            int BRecsToProcess = data_bin.length;
                            int ARecsToProcess = records;
                            for (int i = 0; i < numOfUsedBFs && BRecsToProcess > 0 && ARecsToProcess > 0; i++) {
                                BFData = BFHlp1.getData_bin(i);
                                numOf1s = BFHlp1.getBF1sCnt(i);
                                EnvWssBFsWith1scnt<GCSignal> env = new EnvWssBFsWith1scnt<>(addr, port, Mode.REAL, threads, BFData, numOf1s, ws_bin, threshold_bin, ARecsToProcess, PartyB_IDs);
                                env.implement();
                                tres = env.getLinkage();
                                tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                // get the linkages based on the org. indexes

                                if (res == null) {
                                    res = env.getLinkage();
                                    ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    res.addAll(tres);

                                    ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;

                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                //if (i == 0) {
                                PartyA_IDs = env.getPartyA_IDs();
                                //}
                                //PartyB_IDs.addAll(B_IDs);
                                BRecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches, A new set size:  " + ARecsToProcess + " And B's :" + BRecsToProcess);
                            }
                        } else { // if ! PassNumOfOnes
                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, records);

                            int BRecsToProcess = data_bin.length;
                            int ARecsToProcess = records;
                            for (int i = 0; i < numOfUsedBFs && BRecsToProcess > 0 && ARecsToProcess > 0; i++) {
                                BFData = BFHlp1.getData_bin(i);

                                EnvWssBFs<GCSignal> env = new EnvWssBFs<>(addr, port, Mode.REAL, threads, BFData, ws_bin, threshold_bin, ARecsToProcess, PartyB_IDs);
                                env.implement();
                                tres = env.getLinkage();
                                tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                // get the linkages based on the org. indexes

                                if (res == null) {
                                    res = env.getLinkage();
                                    ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } else {
                                    res.addAll(tres);

                                    ARecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);
                                }
                                t1 = System.currentTimeMillis() - t0;

                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                //if (i == 0) {
                                PartyA_IDs = env.getPartyA_IDs();
                                //}
                                //PartyB_IDs.addAll(B_IDs);
                                BRecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches, A new set size:  " + ARecsToProcess + " And B's :" + BRecsToProcess);
                            }

                        }

                    }
                    break;
                default:
                    throw new AssertionError();
            }

            String str = "";
            if(!useCombBF){
                
            
            if (res != null) {
                str += "----------------------------------\n";
                for (int m = 0; m < help1.rules.length; m++) {
                    str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
                }
                str += String.format("\nThe threshold is %d\n", threshold);
                str += "----------------------------------\n";
                str += "linkage " + "\t\t\tscore\n";
                str += "ID A(index)  ID B(index)\n\n";
                for (int n = 0; n < res.size(); n++) {
                    int[] link0 = res.get(n).getLinkage();
                    str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1],  res.get(n).getScore());
                }
                str += String.format("\nThe number of matches records: %d\n", res.size());
                str += "-----------------------------------\n";
            }
            System.out.println(str);
            
            if (results_save_path != null) {
                try (FileWriter writer = new FileWriter(results_save_path)) {
                    writer.write(str);
                    writer.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            }
            else{
                for(int i=0;i<AllBFsRes.length;i++)
                {
                    
                str = "Matched by BF# "+i+"\n";
              if (AllBFsRes[i] != null) {
                str += "----------------------------------\n";
                for (int m = 0; m < help1.rules.length; m++) {
                    str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
                }
                str += String.format("\nThe threshold is %d\n", threshold);
                str += "----------------------------------\n";
                str += "linkage " + "\t\t\tscore\n";
                str += "ID A(index)  ID B(index)\n\n";
                for (int n = 0; n < AllBFsRes[i].size(); n++) {
                    int[] link0 = AllBFsRes[i].get(n).getLinkage();
                    str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1],  AllBFsRes[i].get(n).getScore());
                }
                str += String.format("\nThe number of matches records: %d\n", AllBFsRes[i].size());
                str += "-----------------------------------\n";
            }
            System.out.println(str); 
            
            if (results_save_path != null) {
                try (FileWriter writer = new FileWriter(results_save_path)) {
                    writer.write(str);
                    writer.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
                }//for
             }


        } // if useBFs
        else {
            array_int1 = new int[prop_array.size()][];
            for (int i = 0; i < array_int1.length; i++) {
                array_int1[i] = prop_array.get(i);
            }

            help1 = Util.readAndEncodeWithProps(file_data, array_int1);
            data_bin = help1.data_bin;

            ws_bin = new boolean[ws.size()][];
            for (int i = 0; i < ws_bin.length; i++) {
                ws_bin[i] = fromInt(ws.get(i), data_len);
            }

            boolean[] threshold_bin = fromInt(threshold, data_len);
            CompPool.MaxNumberTask = threads;

            switch (party) {
                case "generator":
                    PartyA_IDs = help1.IDs;
                    if (filter) {
                        System.out.println("start filtering linkage ...");
                        long t0 = System.currentTimeMillis();
                        boolean[][][] f_data_bin = Util.readAndEncode(file_data, array_int1, 3);
                        Gen<GCSignal> gen = new Gen<>(port, Mode.REAL, threads, f_data_bin, records);
                        gen.implement();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of filtering is " + t1 / 1e3 + " seconds!");

                        t0 = System.currentTimeMillis();
                        f_data_bin = Util.extractArray(data_bin, gen.getRes(), party);
                        GenWssF gen_f = new GenWssF(port, Mode.REAL, threads, f_data_bin, ws_bin, threshold_bin, gen.getRes(), PartyA_IDs);
                        System.out.println("start patientlinkage algorithm ...");
                        gen_f.implement();
                        t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        res = gen_f.getLinkage();
                        PartyB_IDs = gen_f.getPartyB_IDs();

                    } else {
                        System.out.println("start patientlinkage algorithm ...");
                        long t0 = System.currentTimeMillis();
                        GenWss<GCSignal> gen = new GenWss<>(port, Mode.REAL, threads, data_bin, ws_bin, threshold_bin, records, PartyA_IDs);
                        gen.implement();
                        res = gen.getLinkage();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        PartyB_IDs = gen.getPartyB_IDs();
                    }
                    break;
                case "evaluator":
                    PartyB_IDs = help1.IDs;
                    if (filter) {
                        System.out.println("start filtering linkage ...");
                        long t0 = System.currentTimeMillis();
                        boolean[][][] f_data_bin = Util.readAndEncode(file_data, array_int1, 3);
                        Env<GCSignal> eva = new Env<>(addr, port, Mode.REAL, threads, f_data_bin, records);
                        eva.implement();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of filtering is " + t1 / 1e3 + " seconds!");

                        t0 = System.currentTimeMillis();
                        f_data_bin = Util.extractArray(data_bin, eva.getRes(), party);
                        EnvWssF eva_f = new EnvWssF(addr, port, Mode.REAL, threads, f_data_bin, ws_bin, threshold_bin, eva.getRes(), PartyB_IDs);
                        System.out.println("start patientlinkage algorithm ...");
                        eva_f.implement();
                        t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        res = eva_f.getLinkage();
                        PartyA_IDs = eva_f.getPartyA_IDs();
                    } else {
                        System.out.println("start patientlinkage algorithm ...");
                        long t0 = System.currentTimeMillis();
                        EnvWss<GCSignal> eva = new EnvWss<>(addr, port, Mode.REAL, threads, data_bin, ws_bin, threshold_bin, records, PartyB_IDs);
                        eva.implement();
                        res = eva.getLinkage();
                        long t1 = System.currentTimeMillis() - t0;
                        System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                        PartyA_IDs = eva.getPartyA_IDs();
                    }
                    break;
                default:
                    throw new AssertionError();
            }

            String str = "";

            if (res != null) {
                str += "----------------------------------\n";
                for (int m = 0; m < help1.rules.length; m++) {
                    str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
                }
                str += String.format("\nThe threshold is %d\n", threshold);
                str += "----------------------------------\n";
                str += "linkage " + "\t\t\tscore\n";
                str += "ID A(index)  ID B(index)\n\n";
                for (int n = 0; n < res.size(); n++) {
                    int[] link0 = res.get(n).getLinkage();
                    str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1],  res.get(n).getScore());
                }
                str += String.format("\nThe number of matches records: %d\n", res.size());
                str += "-----------------------------------\n";
            }
            System.out.println(str);

            if (results_save_path != null) {
                try (FileWriter writer = new FileWriter(results_save_path)) {
                    writer.write(str);
                    writer.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } //!useBFs
    }

    public static void usagemain() {
        String help_str
                = ""
                + String.format("     -config     <path>      : input configure file path\n")
                + String.format("     -data       <path>      : input data file path\n")
                + String.format("     -help                   : show help");
        System.out.println(help_str);
    }

    public static void simulation() {
        String[] args0 = {"-config", "./configs/config_gen_1K.txt", "-data", "./data/Source14k_a_1K.csv"};
        String[] args1 = {"-config", "./configs/config_eva_1K.txt", "-data", "./data/Source14k_b_1K.csv"};

        Thread t_gen = new Thread(() -> {
            startLinkage(args0);
        });
        Thread t_eva = new Thread(() -> {
            startLinkage(args1);
        });

        long t0 = System.currentTimeMillis();
        try {
            t_gen.start();
            t_eva.start();
            t_gen.join();
            t_eva.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        long t1 = System.currentTimeMillis() - t0;

        System.out.println("The total running time is " + t1 / 1e3 + " seconds!");
    }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        if ("sim".equals(args[0])) {
            simulation();
        } else {
            startLinkage(args);
        }
        long t1 = System.currentTimeMillis() - t0;
        System.out.println("The total running time is " + t1 / 1e3 + " seconds!");
    }

    public static Properties getConfigsFromXML(String configFile) {
        Properties defaultConf = new Properties();
        // sets default properties

        defaultConf.setProperty("party", "nobody");
        defaultConf.setProperty("address", "");
        defaultConf.setProperty("threshold", "1");
        defaultConf.setProperty("threads", "1");
        defaultConf.setProperty("records", "0");

        defaultConf.setProperty("filter", "0"); //false
        defaultConf.setProperty("port", "-1");
        defaultConf.setProperty("com", "");
        defaultConf.setProperty("results_save_path", "none.txt");

        Properties configs = new Properties(defaultConf);

        // loads properties from file
        try (InputStream inputStream = new FileInputStream(configFile)) {
            configs.loadFromXML(inputStream);

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return (configs);
    }

}