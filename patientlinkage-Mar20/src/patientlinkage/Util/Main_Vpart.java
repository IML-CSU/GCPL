            /*
             * To change this license header, choose License Headers in Project Properties.
             * To change this template file, choose Tools | Templates
             * and open the template in the editor.
 */
package patientlinkage.Util;

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
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import patientlinkage.DataType.BFHelper;
import patientlinkage.DataType.Helper;
import patientlinkage.DataType.PatientLinkage;
import patientlinkage.DataType.PatientLinkage2;
import patientlinkage.GarbledCircuit.PatientLinkageGadget;
import static patientlinkage.GarbledCircuit.PatientLinkageGadget.getIntArrayFromStrs;
import static patientlinkage.Util.Util.fromInt;
import patientlinkage.parties.Env;
import patientlinkage.parties.EnvWss;
import patientlinkage.parties.EnvWssBFs;
import patientlinkage.parties.EnvWssBFsWith1scnt;
import patientlinkage.parties.EnvWssBFsWith1scntAndChkDCi;
import patientlinkage.parties.EnvWssBFsWithCLR1scntAndChkDCi;
import patientlinkage.parties.EnvWssBFsWithCLR1scntAndChkDCi_Vpart;
import patientlinkage.parties.EnvWssF;
import patientlinkage.parties.Gen;
import patientlinkage.parties.GenWss;
import patientlinkage.parties.GenWssBFs;
import patientlinkage.parties.GenWssBFsWith1scnt;
import patientlinkage.parties.GenWssBFsWith1scntAndChkDCi;
import patientlinkage.parties.GenWssBFsWithCLR1scntAndChkDCi;
import patientlinkage.parties.GenWssBFsWithCLR1scntAndChkDCi_Vpart;
import patientlinkage.parties.GenWssF;

/**
 *
 * @author cf
 */
public class Main_Vpart {

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
        int hTasks = 1;
        int vTasks=1;
        int numOfParts=4;
        int records = 0;
        boolean filter = false;
        int data_len = 32;// 16;
        String results_save_path = null;
        boolean useBFs = false;
        boolean useCombBF = false;// BFfilter: property, True:  Use combinations of the BFs , False: use  BFs one by one and filter results by removing
        // those ones matched by the previous BF
        boolean PassNumOfOnes = true; //Send BF's number of ones with each record BFs instead of securely computing it.
        boolean useXMLconfigf = false;
        ArrayList<int[]> prop_array = new ArrayList<>();
        ArrayList<Integer> ws = new ArrayList<>();
        ArrayList<int[]> BFprop_array = new ArrayList<>();
        ArrayList<Integer> BFws = new ArrayList<>();
        ArrayList<PatientLinkage> res = null;
        ArrayList<PatientLinkage> tres = null;
        ArrayList<PatientLinkage2> tres2 = null;
        ArrayList<PatientLinkage2> TotBFPartsRes = null;
        ArrayList<PatientLinkage2>[] AllBfsTotBFPartsRes = null;
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
            hTasks = Integer.parseInt(configs.getProperty("hTasks"));
            vTasks = Integer.parseInt(configs.getProperty("vTasks"));
             numOfParts=Integer.parseInt(configs.getProperty("numOfParts"));
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
                        case "vTasks":
                            vTasks = Integer.parseInt(strs2[1].trim());
                            break;  
                        case "hTasks":
                            hTasks = Integer.parseInt(strs2[1].trim());
                            break;  
                        case "numOfParts":
                            numOfParts = Integer.parseInt(strs2[1].trim());
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
        boolean[][] chkPartDCi;
        boolean[][] chkDCi;
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
            AllBFsRes = (ArrayList<PatientLinkage>[]) new ArrayList[numOfUsedBFs];
            AllBfsTotBFPartsRes = (ArrayList<PatientLinkage2>[]) new ArrayList[numOfUsedBFs];

            chkPartDCi = new boolean[data_bin.length][records];
            chkDCi = new boolean[data_bin.length][records];
            boolean useChkDci = true;
            boolean useBfsInSeq = true;

            for (int ci = 0; ci < data_bin.length; ci++) {
                for (int cj = 0; cj < records; cj++) {
                    chkPartDCi[ci][cj] = true;
                    chkDCi[ci][cj] = true;

                }
            }

            switch (party) {
                case "generator":
                    PartyA_IDs = help1.IDs;
                    if (useChkDci) {

                        System.out.println((new Date())+" :\n Gen.((V3)) starts patientlinkage algorithm with:\n -Partitioned Bfs \n-Vpartitioning \n -filtring using chk DC for those matched by any filter ...");

                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            int[] intNumOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed in the Clr...");

                            boolean[][][] BFpartData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);
                            BFHlp1.setnumOfParts(numOfParts);
                            int BRecsToProcess = records;
                            //int ArecsToProcess = data_bin.length;
                            tot_t = 0;
                            for (int i = 0; i < numOfUsedBFs; i++) {
                                /*if (!useBfsInSeq) {
                                    for (int ci = 0; ci < data_bin.length; ci++) {
                                        for (int cj = 0; cj < records; cj++) {
                                            chkPartDCi[ci][cj] = true;
                                        }
                                    }
                                }*/
                                t0 = System.currentTimeMillis();
                                
                                int[][] AllParts1sCnt = BFHlp1.getBFAllPartitions1sCnt(i);
                                numOf1s = BFHlp1.getBF1sCnt(i);
                                intNumOf1s=help1.getIntBF1sCnt(i);
                                if (TotBFPartsRes != null) {
                                    TotBFPartsRes.clear();
                                }
                                for (int Part = 1; Part <= BFHlp1.getnumOfParts(); Part++) {
                                    System.out.println("Start processing BF part# "+Part +" out of "+BFHlp1.getnumOfParts());
                                    t1 = System.currentTimeMillis();
                                    BFpartData = BFHlp1.getRecsBFPartition(i, Part);
                                    //GenWssBFsWith1scntAndChkDCi<GCSignal> gen = new GenWssBFsWith1scntAndChkDCi<>(port, Mode.REAL, threads, BFpartData, numOf1s, ws_bin, threshold_bin, chkPartDCi, BRecsToProcess, PartyA_IDs);
                                    GenWssBFsWithCLR1scntAndChkDCi_Vpart<GCSignal> gen = new GenWssBFsWithCLR1scntAndChkDCi_Vpart<>(port, Mode.REAL, threads,hTasks,vTasks, BFpartData, intNumOf1s, chkPartDCi, BRecsToProcess, PartyA_IDs);
                                    gen.implement();
                                    tres2 = gen.getLinkage();
                                    //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                    if (tres2 != null) {

                                        TotBFPartsRes = UpdateBFPartsRes(TotBFPartsRes, tres2);
                                        chkPartDCi = UpdateChkPartDCi(chkPartDCi, TotBFPartsRes, AllParts1sCnt, Part, threshold, party);
                                    }
                                    if(Part==1)
                                    PartyB_IDs = gen.getPartyB_IDs();
                                    
                                    System.out.println("BF part# "+Part +" Completed");
                                    t1 = System.currentTimeMillis() - t1;
                                    System.out.println("The running time of part# " + Part + " is " + t1 / 1e3 + " seconds!");
                                }
                                if (AllBfsTotBFPartsRes[i] == null) {
                                    AllBfsTotBFPartsRes[i] = new ArrayList<>();
                                    
                                    //gen.getLinkage();
                                    //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } //else {
                                
                                
                                for (int n = 0; n < TotBFPartsRes.size(); n++) {
                                    PatientLinkage2 l = TotBFPartsRes.get(n);
                                    if (l.getScore() >= (double) threshold / 128.0) {
                                        if (useBfsInSeq) {
                                            int indi = l.getI();
                                            int indj = l.getJ();
                                            chkDCi[indi][indj] = false;
                                        }

                                        AllBfsTotBFPartsRes[i].add(l);
                                    }
                                }
                                chkPartDCi=chkDCi;
                                //}
                                //TODO: add check DCi for the entire filter
                                //chkDCi= UpdateChkDCi(chkDCi,AllBfsTotBFPartsRes[i]);
                                t1 = System.currentTimeMillis() - t0;
                                tot_t += t1;
                                //System.out.println("The running time of patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");

                                //PartyB_IDs.addAll(B_IDs);
                                //BFData= BFHlp1.getData_bin(i);
                                //ArecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + AllBfsTotBFPartsRes[i].size() + " Matches");

                            } //for i
                            System.out.println("The Total running time of  patientlinkage algorithm is " + tot_t / 1e3 + " seconds!");
                        } //if PassNumOfOnes

                    }// if UseChckDCi
                    else //====================================
                    if (useCombBF) {

                        System.out.println((new Date())+" :\n Gen. starts patientlinkage algorithm with Comb. of BFs ...");

                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);
                            int BRecsToProcess = records;
                            //int ArecsToProcess = data_bin.length;
                            tot_t = 0;
                            for (int i = 0; i < numOfUsedBFs; i++) {
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
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches");
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
                            for (int i = 0; i < numOfUsedBFs; i++) {
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
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches");
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
                        System.out.println((new Date())+" :\n Gen. starts patientlinkage algorithm single BFs ...");
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
                    } // else if Comb// else  !useChkDci
                    break;

                case "evaluator":
                    PartyB_IDs = help1.IDs;

                    if (useChkDci) {
                           System.out.println((new Date())+" :\n Eval.((V3)) starts patientlinkage algorithm with:\n -Partitioned Bfs \n-Vpartitioning \n -filtring using chk DC for those matched by any filter ...");

                        //System.out.println("Eval. starts patientlinkage algorithm with filtring using chk DC for those matched by any filter ...");
                        t0 = System.currentTimeMillis();
                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;
                            int[] intNumOf1s;
                            System.out.println(" patientlinkage With BF #1s passedin the Clr...");

                            boolean[][][] BFpartData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);
                             BFHlp1.setnumOfParts(numOfParts);

                            //int BRecsToProcess = data_bin.length;
                            int ARecsToProcess = records;
                            for (int i = 0; i < numOfUsedBFs; i++) {
                                /*if (!useBfsInSeq) {
                                    for (int ci = 0; ci < data_bin.length; ci++) {
                                        for (int cj = 0; cj < records; cj++) {
                                            chkPartDCi[ci][cj] = true;
                                        }
                                    }
                                }*/

                                t0 = System.currentTimeMillis();

                                int[][] AllParts1sCnt = BFHlp1.getBFAllPartitions1sCnt(i);
                                numOf1s = BFHlp1.getBF1sCnt(i);
                                intNumOf1s=help1.getIntBF1sCnt(i);
                                
                                if (TotBFPartsRes != null) {
                                    TotBFPartsRes.clear();
                                }
                                for (int Part = 1; Part <= BFHlp1.getnumOfParts(); Part++) {
                                    BFpartData = BFHlp1.getRecsBFPartition(i, Part);

                                    //EnvWssBFsWith1scntAndChkDCi<GCSignal> env = new EnvWssBFsWith1scntAndChkDCi<>(addr, port, Mode.REAL, threads, BFpartData, numOf1s, ws_bin, threshold_bin, chkPartDCi, ARecsToProcess, PartyB_IDs);
                                    EnvWssBFsWithCLR1scntAndChkDCi_Vpart<GCSignal> env = new EnvWssBFsWithCLR1scntAndChkDCi_Vpart<>(addr, port, Mode.REAL, threads,hTasks,vTasks, BFpartData, intNumOf1s, chkPartDCi, ARecsToProcess, PartyB_IDs);
                                    env.implement();
                                    tres2 = env.getLinkage();
                                    //tres = BFHlp1.getLinkageOrgIndexes(tres, party);
                                    // get the linkages based on the org. indexes
                                    if (tres2 != null) {
                                        System.out.println("EVA Tres2 size=" + tres2.size());
                                        TotBFPartsRes = UpdateBFPartsRes(TotBFPartsRes, tres2);
                                        chkPartDCi = UpdateChkPartDCi(chkPartDCi, TotBFPartsRes, AllParts1sCnt, Part, threshold, party);
                                    }
                                     if(Part==1)
                                    PartyA_IDs = env.getPartyA_IDs();
                                } // for part

                                if (AllBfsTotBFPartsRes[i] == null) {
                                    AllBfsTotBFPartsRes[i] = new ArrayList<>();
                                    //gen.getLinkage();
                                    //BRecsToProcess = BFHlp1.FilterOutMatchedRecs(tres, party);

                                } //else {
                                
//                                for (int ci = 0; ci < data_bin.length; ci++) {
//                                        for (int cj = 0; cj < records; cj++) {
//                                            chkPartDCi[ci][cj] = true;
//                                        }
//                                    }
                                for (int n = 0; n < TotBFPartsRes.size(); n++) {
                                    PatientLinkage2 l = TotBFPartsRes.get(n);
                                    // System.out.print(l.getScore()+" | ");

                                    if (l.getScore() >= (double) threshold / 128.0) {
                                        if (useBfsInSeq) {
                                            int indi = l.getI();
                                            int indj = l.getJ();
                                            chkDCi[indi][indj] = false;
                                        }
                                        AllBfsTotBFPartsRes[i].add(l);
                                    }

                                    //if(l.getScore()>=(double)threshold/128.0)
                                    //  AllBfsTotBFPartsRes[i].add(l);
                                }
                                System.out.println();
                                 chkPartDCi=chkDCi;
                                //}
                                //chkDCi= UpdateChkDCi(chkDCi,AllBfsTotBFPartsRes[i]);
                                t1 = System.currentTimeMillis() - t0;

                                System.out.println("The running time of round (" + i + ") patientlinkage algorithm is " + t1 / 1e3 + " seconds!");
                                //if (i == 0) {
                                //PartyA_IDs = env.getPartyA_IDs();
                                //}
                                //PartyB_IDs.addAll(B_IDs);
                                //BRecsToProcess = BFHlp1.getDataSize();
                                System.out.println("Round (" + i + ") found " + AllBfsTotBFPartsRes[i].size() + " Matches");
                            } //for i #BFs
                        }
                    }// useChkDDi
                    else //============================
                    if (useCombBF) {
                        System.out.println((new Date())+" :\n Eval. starts patientlinkage algorithm with Combs of BFs ...");
                        t0 = System.currentTimeMillis();
                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);

                            //int BRecsToProcess = data_bin.length;
                            int ARecsToProcess = records;
                            for (int i = 0; i < numOfUsedBFs; i++) {
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
                            for (int i = 0; i < numOfUsedBFs; i++) {
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
                                System.out.println("Round (" + i + ") found " + tres.size() + " Matches ");
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
                        System.out.println((new Date())+" :\n Eval. starts patientlinkage algorithm single BFs ...");
                        t0 = System.currentTimeMillis();
                        if (PassNumOfOnes) {
                            boolean[][][] numOf1s;
                            numOf1s = help1.numOfOnesInBFs;

                            System.out.println(" patientlinkage With BF #1s passed...");

                            boolean[][][] BFData;
                            BFHelper BFHlp1 = new BFHelper(data_bin, numOf1s, records);

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

                    }// else useChkDci
                    break;
                default:
                    throw new AssertionError();
            }

            String str = "";

            if (useChkDci) {

                for (int i = 0; i < AllBfsTotBFPartsRes.length; i++) {

                    str = (new Date())+" :\n Matched by BF# " + i + "\n";
                    if (AllBfsTotBFPartsRes[i] != null) {
                        str += "----------------------------------\n";
                        for (int m = 0; m < help1.rules.length; m++) {
                            str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
                        }
                        str += String.format("\nThe threshold is %f\n", threshold / 128.0);
                        str += "----------------------------------\n";
                        str += "linkage " + "\t\t\tscore\n";
                        str += "ID A(index)  ID B(index)\n\n";
                        for (int n = 0; n < AllBfsTotBFPartsRes[i].size(); n++) {
                            int[] link0 = AllBfsTotBFPartsRes[i].get(n).getLinkage();
                            str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1], AllBfsTotBFPartsRes[i].get(n).getScore());
                        }
                        str += String.format("\nThe number of matches records: %d\n", AllBfsTotBFPartsRes[i].size());
                        str += "-----------------------------------\n";
                    }
                    System.out.println(str);

                    if (results_save_path != null) {
                        try (FileWriter writer = new FileWriter(results_save_path,true)) {
                            writer.write(str);
                            writer.flush();
                        } catch (IOException ex) {
                            Logger.getLogger(Main_BFpartsEnc1s.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }//for
            } //chkDCi

            if (!useCombBF) {

                if (res != null) {
                    str += (new Date())+" :\n ----------------------------------\n";
                    for (int m = 0; m < help1.rules.length; m++) {
                        str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
                    }
                    str += String.format("\nThe threshold is %d\n", threshold);
                    str += "----------------------------------\n";
                    str += "linkage " + "\t\t\tscore\n";
                    str += "ID A(index)  ID B(index)\n\n";
                    for (int n = 0; n < res.size(); n++) {
                        int[] link0 = res.get(n).getLinkage();
                        str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1], res.get(n).getScore());
                    }
                    str += String.format("\nThe number of matches records: %d\n", res.size());
                    str += "-----------------------------------\n";
                }
                System.out.println(str);

                if (results_save_path != null) {
                    try (FileWriter writer = new FileWriter(results_save_path,true)) {
                        writer.write(str);
                        writer.flush();
                    } catch (IOException ex) {
                        Logger.getLogger(Main_BFpartsEnc1s.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            } else { //useCombBFs
                for (int i = 0; i < AllBFsRes.length; i++) {

                    str = (new Date())+" :\n Matched by BF# " + i + "\n";
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
                            str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1], AllBFsRes[i].get(n).getScore());
                        }
                        str += String.format("\nThe number of matches records: %d\n", AllBFsRes[i].size());
                        str += "-----------------------------------\n";
                    }
                    System.out.println(str);

                    if (results_save_path != null) {
                        try (FileWriter writer = new FileWriter(results_save_path,true)) {
                            writer.write(str);
                            writer.flush();
                        } catch (IOException ex) {
                            Logger.getLogger(Main_BFpartsEnc1s.class.getName()).log(Level.SEVERE, null, ex);
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
                str += (new Date())+" :\n ----------------------------------\n";
                for (int m = 0; m < help1.rules.length; m++) {
                    str += String.format("Rule %d is %s, and the weight is %d\n", m + 1, help1.rules[m], ws.get(m));
                }
                str += String.format("\nThe threshold is %d\n", threshold);
                str += "----------------------------------\n";
                str += "linkage " + "\t\t\tscore\n";
                str += "ID A(index)  ID B(index)\n\n";
                for (int n = 0; n < res.size(); n++) {
                    int[] link0 = res.get(n).getLinkage();
                    str += String.format("%s(%d) <--> %s(%d) \t\t%3.3f\n", PartyA_IDs.get(link0[0]), link0[0], PartyB_IDs.get(link0[1]), link0[1], res.get(n).getScore());
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
                    Logger.getLogger(Main_BFpartsEnc1s.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Main_BFpartsEnc1s.class.getName()).log(Level.SEVERE, null, ex);
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

    public static boolean[][] UpdateChkDCi(boolean[][] chkDCi, ArrayList<PatientLinkage> tres) {
        int ind, Otherindx;
        for (int n = 0; n < tres.size(); n++) {
            ind = tres.get(n).getI();
            Otherindx = tres.get(n).getJ();
            chkDCi[ind][Otherindx] = false;
        }
        return chkDCi;
    }
    // add party to change ind to J instead of I

    public static boolean[][] UpdateChkPartDCi(boolean[][] chkDCi, ArrayList<PatientLinkage2> totPartRes,
            int[][] numOf1sInParts, int Part, int DCt, String role) { //Part starts from 1
        int ind = 0, Otherindx = 0, myInd = 0;
        int nOfParts = numOf1sInParts[0].length;
        int t1s, sh1s, tEstMaxSh1s;
        double DCtr = (double) DCt / 128.0;

        switch (role) {
            case "generator":
                for (int n = 0; n < totPartRes.size(); n++) {

                    ind = totPartRes.get(n).getI();
                    myInd = totPartRes.get(n).getI();
                    Otherindx = totPartRes.get(n).getJ();
                    sh1s = totPartRes.get(n).getShared1s();
                    t1s = totPartRes.get(n).getTotal1s();
                    tEstMaxSh1s = 0;
                    for (int i = Part; i < nOfParts; i++) {
                        tEstMaxSh1s += numOf1sInParts[myInd][i];
                    }
                    int remSh1s = (int) (DCtr * t1s) - 2 * tEstMaxSh1s;
                    if (sh1s * 2 < remSh1s) {
                        chkDCi[ind][Otherindx] = false;
                    }
                }
                break;
            case "evaluator":
                for (int n = 0; n < totPartRes.size(); n++) {
                    ind = totPartRes.get(n).getI();
                    myInd = totPartRes.get(n).getJ();
                    Otherindx = totPartRes.get(n).getJ();
                    sh1s = totPartRes.get(n).getShared1s();
                    t1s = totPartRes.get(n).getTotal1s();
                    tEstMaxSh1s = 0;
                    for (int i = Part; i < nOfParts; i++) {
                        tEstMaxSh1s += numOf1sInParts[myInd][i];
                    }
                    int remSh1s = (int) (DCtr * t1s) - 2 * tEstMaxSh1s;
                    if (sh1s * 2 < remSh1s) {
                        chkDCi[ind][Otherindx] = false;
                    }
                }
                break;
        }//case

        return chkDCi;
    }

    // I assumed tres have all linkages, where nonchecked items has score 0
    public static ArrayList<PatientLinkage2> UpdateBFPartsRes(ArrayList<PatientLinkage2> TotBFPartsRes, ArrayList<PatientLinkage2> tres) {
        int ind, Otherindx;
        PatientLinkage2 l;
        if (tres.isEmpty()) {
            return TotBFPartsRes;
        }
        if (TotBFPartsRes == null || TotBFPartsRes.isEmpty()) {
            //TotBFPartsRes=new ArrayList<>();
            TotBFPartsRes = tres;
        } else {
            for (int n = 0; n < tres.size(); n++) {

                l = TotBFPartsRes.get(n);
                assert (tres.get(n).getI() == l.getI() && tres.get(n).getJ() == l.getJ());
                l.setShared1s(l.getShared1s() + tres.get(n).getShared1s());
                l.setScore((double) l.getShared1s() * 2.0 / l.getTotal1s());
                //l.setScore(l.getScore()+tres.get(n).getScore());

                //check if score=shared1s/tot1s
                TotBFPartsRes.set(n, l);
            }
        }

        return TotBFPartsRes;
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
            Logger.getLogger(Main_BFpartsEnc1s.class.getName()).log(Level.SEVERE, null, ex);
        }

        return (configs);
    }

}
