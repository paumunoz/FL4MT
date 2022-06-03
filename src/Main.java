import analysis.elements.Case;
import analysis.elements.Mutant;
import analysis.procedures.ExamScoreAnalysis;
import analysis.procedures.MatchingTablesBuilder;
import analysis.procedures.SpectrumAnalysis;
import analysis.procedures.TiesAnalysis;
import csv.util.CSVUtil;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    private static final List<Case> cases = new ArrayList<>();

    private static final String CASE_STUDY = "UML2ER";

    private static final String[] SUSPICIOUSNESS_FORMULAS = new String[]{"Mountford", "Kulcynski2", "Zoltar", "Ochiai"};

    public static void main(String[] args) throws Exception {
        // 0. We create the cases that we want to study
        // Note: the Case objects include the input and output paths for the data files

        // Original case: no embedding and hierarchy between rules
        cases.add(new Case(false, true, CASE_STUDY));
        // Embedding case: embedding between rules and constraints and the hierarchy between rules
        cases.add(new Case(true, true, CASE_STUDY));
        // No hierarchy: no embedding and no hierarchy between rules
        cases.add(new Case(true, false, CASE_STUDY));

        String folderPrefix = "";

        // 1. If you want unique names for the files in every program execution, uncomment the following lines
        // 1a. We get the current time to generate a unique name for the folders
//        Date date = Calendar.getInstance().getTime();
//        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
//        folderPrefix = dateFormat.format(date);


        for(Case c : cases) {
            // 2. MATCHING TABLES GENERATION
            // 2a. We store the generated tables in a map to avoid reading the CSV file in the following steps
            Map<String, double[][]> matchingTables = new HashMap();

            // 2b. We generate the Matching tables for the footprints, taking into account mutants in the rules
            for(String filename : CSVUtil.listFileNamesInDirectory(c.getRulesFootprintPath(), ".csv")){
                String mutant = filename.substring(filename.indexOf("Mutant"), filename.indexOf(".csv"));
                Map<String, double[][]> mutantTables = generateMatchingTable(c, mutant, folderPrefix + "/");
                for (String technique : mutantTables.keySet()) {
                    matchingTables.put(mutant + "_" + technique, mutantTables.get(technique));
                }
            }

            // 3. SPECTRUM BASED FAULT LOCALIZATION RESULTS ANALYSIS
            // 3a. We analyze the Spectrum files and build the dataFile
            List<Mutant> mutants = SpectrumAnalysis.buildDataFile(matchingTables, folderPrefix, c);


            // 4. TIES ANALYSIS
            // 4a. We get the number of ties between the buggy rule and any other
            for(String suspiciousnessFormula : SUSPICIOUSNESS_FORMULAS){
                TiesAnalysis.analyzeTies(mutants, c.getRuleInheritance(),
                        c.getOutputTiesPath() + c.getFolder(), suspiciousnessFormula);
            }

            // 5. EXAM SCORE
            // 5a. We create files with the optimized rankings and the EXAM Scores for each mutant
            ExamScoreAnalysis.printMutantEXAMScoreFile(mutants, c, folderPrefix);
            // 5b. We create a file for each case with the aggregated values for the BEST, WORST and AVG case<s
            ExamScoreAnalysis.printExamScoreAggregatedValues(mutants, c, folderPrefix);

        }
    }

    private static Map<String, double[][]> generateMatchingTable(Case c, String fileName, String folderPrefix) throws Exception {
        String datafilesInputPath = "/datafiles/input";
        MatchingTablesBuilder t2a = new MatchingTablesBuilder(
                c.getConstraintsFootprintPath() + CASE_STUDY + "_Static_Constraints.csv",
                c.getRulesFootprintPath() + CASE_STUDY + "_Static_Rules_" + fileName + ".csv",
                c.getModelsPath() + "SimpleUML.ecore",
                c.getModelsPath() + "ER.ecore"
        );

        String pathCSVFolder = c.getOutputMatchingTablesPath() + folderPrefix;
        boolean res = new File(pathCSVFolder).mkdirs();
        t2a.generateCSV(pathCSVFolder + c.getCaseStudy() + "_" + fileName);

        Map<String, double[][]> tables = new HashMap<>();
        tables.put("CC", t2a.getCc());
        tables.put("RC", t2a.getRc());
        tables.put("RCR", t2a.getRcr());

        return tables;
    }
}