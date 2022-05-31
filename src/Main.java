import analysis.elements.Case;
import analysis.elements.Mutant;
import analysis.procedures.ExamScoreAnalysis;
import analysis.procedures.MatchingTablesBuilder;
import analysis.procedures.SpectrumAnalysis;
import csv.util.CSVUtil;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static analysis.procedures.ConstraintAnalysis.findTheBugUsingStaticInformation;

public class Main {

    private static final List<Case> cases = new ArrayList<>();

    private static final String CASE_STUDY = "UML2ER";

    public static void main(String[] args) throws Exception {
        // 0. We create the cases that we want to study
        // Original case with no embedding and the hierarchy between rules
        //cases.add(new Case(false, true, CASE_STUDY));
        // Case with embedding between rules and constraints keeping the hierarchy between rules
        cases.add(new Case(true, true, CASE_STUDY));
        // Case without hierarchy between rules
        //cases.add(new Case(true, false, CASE_STUDY));

        // 1. We get the current time to generate a unique name for the folders
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String folderPrefix = dateFormat.format(date);

        for(Case c : cases) {
            // Opt. We store the generated tables in a map to avoid reading the CSV file in the following steps
            Map<String, double[][]> matchingTables = new HashMap();

            // 2. We generate the Matching tables for the footprints, taking into account mutants in the rules
            for(String filename : CSVUtil.listFileNamesInDirectory(c.getRulesFootprintPath(), ".csv")){
                String mutant = filename.substring(filename.indexOf("Mutant"), filename.indexOf(".csv"));
                Map<String, double[][]> mutantTables = generateMatchingTable(c, mutant, folderPrefix + "/");
                for (String technique : mutantTables.keySet()) {
                    matchingTables.put(mutant + "_" + technique, mutantTables.get(technique));
                }
            }

            // 3. We analyze the Spectrum files and build the dataFile
            List<Mutant> mutants = SpectrumAnalysis.buildDataFile(matchingTables, folderPrefix, c);

            // 4. Analyze the inheritance present in the resulting file
            //String inheritance = InheritanceAnalysis.analyzeInheritance(mutants, c.getRuleInheritance(), c.getOutputTiesPath()+folderPrefix+c.getFolder());
            //System.out.println(inheritance);

            // 5. OCL Constraints analysis by inheritance
            /*String constraintInheritance = findTheBugUsingStaticInformation(mutants, c.getRuleInheritance(),
                    c.getConstraintInheritance(), "RC", "Mountford",c.getOutputConstraintsPath()+folderPrefix+c.getFolder());
            System.out.println(constraintInheritance);*/

            // 6. ExamScore Analysis
            ExamScoreAnalysis.buildExamScoreFile(mutants, c, folderPrefix);

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
        t2a.generateCSV(pathCSVFolder + fileName);

        Map<String, double[][]> tables = new HashMap<>();
        tables.put("CC", t2a.getCc());
        tables.put("RC", t2a.getRc());
        tables.put("RCR", t2a.getRcr());

        return tables;
    }
}