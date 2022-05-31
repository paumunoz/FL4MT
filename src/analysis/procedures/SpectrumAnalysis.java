package analysis.procedures;


import analysis.elements.Case;
import analysis.elements.Constraint;
import analysis.elements.Mutant;
import analysis.elements.Rule;
import csv.spectrum.SpectrumParser;
import csv.util.CSVUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpectrumAnalysis {

    // HEADERS
    private static final String[] FIRST_AUX_HEADERS = new String[]{"Case"};
    private static final String[] SPECTRUM_HEADERS = new String[]{"Buggy Rule", "Ncf", "Nuf", "Ncs", "Nus", "Nc", "Nu"
            , "Ns", "Nf", "Mountford", "Kulcynski2", "Zoltar", "Ochiai"};
    private static final String[] STATIC_HEADERS = new String[]{"CC", "RC", "RCR"};
    private static final String[] LAST_AUX_HEADERS = new String[]{"Constraint", "Rule", "Mutant"};

    // CONSTRAINTS
    private static final String CONSTRAINT_NAME = "OCL";

    public static List<Mutant> buildDataFile(Map<String, double[][]> matchingTables, String outputFolder, Case c) throws Exception {
        String[] mutantFiles = CSVUtil.listFileNamesInDirectory(c.getMutantsPath(), ".csv");
        List<String[]> outputFile = new ArrayList<>();
        List<Mutant> result = new ArrayList<>();

        // 1. Add the headers row to the file
        String[] fileHeader = SpectrumParser.getRow(FIRST_AUX_HEADERS, SPECTRUM_HEADERS, STATIC_HEADERS,
                LAST_AUX_HEADERS);
        outputFile.add(fileHeader);

        // 1a. Index to identify mutant case
        int index = 0;

        // 2. Process each mutant file
        for (String mutantFilename : mutantFiles) {
            //System.out.println("Analysing file " + mutantFilename);
            List<String[]> mutantCSV = CSVUtil.readAll(c.getMutantsPath() + mutantFilename, ';');

            String mutantName = mutantFilename.substring(mutantFilename.indexOf('_') + 1, mutantFilename.indexOf(
                    "--OCL"));
            // 2a. Create the mutant object checking if it was previously created
            Mutant mutant = new Mutant(mutantName);
            if (!result.contains(mutant)) {
                result.add(mutant);
            } else {
                mutant = result.get(result.indexOf(mutant));
            }

            List<String> rules = SpectrumParser.getRules(c.getRulesFootprintPath() + c.getCaseStudy() +
                    "_Static_Rules_" + mutantName + ".csv");

            int headersRow = SpectrumParser.getHeadersRow(mutantCSV, SPECTRUM_HEADERS[0]);
            int lastRuleRow = SpectrumParser.getNumberOfRules(mutantCSV, headersRow);
            List<Integer> selectedColumns = SpectrumParser.getSelectedColumnsByHeader(mutantCSV, SPECTRUM_HEADERS);
            int constraintId = SpectrumParser.getConstraintId(CONSTRAINT_NAME, mutantFilename);

            // 2b. Create the constraint object checking if it was previously stored
            Constraint constraint = new Constraint(CONSTRAINT_NAME + constraintId, mutantCSV.get(headersRow - 2)[2]);
            if (!mutant.getAppliedConstraints().contains(constraint)) {
                mutant.addConstraint(constraint);
            } else {
                constraint = mutant.getAppliedConstraints().get(mutant.getAppliedConstraints().indexOf(constraint));
            }

            // 2a. Analyse each line  in the csv mutant file
            for (int i = headersRow + 1; i < lastRuleRow; i++) {
                String[] row = mutantCSV.get(i);
                List<String> rowOutput = new ArrayList<>();

                // 2a. Get the rule id of the row (it is always located in column 1)
                String ruleId = row[1];

                // 2a1. Add the index to the new row.
                rowOutput.add(Integer.toString(index));

                // 2a2. Add the selected columns to the row
                for (int j : selectedColumns) {
                    if (j == 0) {
                        rowOutput.add(row[j].isEmpty() ? "0" : "1");
                    } else {
                        rowOutput.add(row[j]);
                    }
                }

                // 2a3. Add the corresponding value from the MatchingTable
                for (String metric : STATIC_HEADERS) {
                    int ruleIndex = rules.indexOf(ruleId);
                    String matchingValue =
                            String.valueOf(matchingTables.get(mutantName + "_" + metric)[constraintId - 1][ruleIndex]);
                    rowOutput.add(matchingValue);
                }
                rowOutput.add(String.valueOf(constraintId));
                rowOutput.add(ruleId);
                rowOutput.add(mutantName);

                // 2a1. Create the rule object
                Rule rule = new Rule(ruleId, rowOutput.subList(1,
                        SPECTRUM_HEADERS.length + STATIC_HEADERS.length + 1), SpectrumParser.getRow(SPECTRUM_HEADERS,
                        STATIC_HEADERS));
                constraint.addRule(rule);

                outputFile.add(rowOutput.toArray(new String[0]));
            }

            index++;
        }
        String pathCSVFolder = c.getOutputSpectrumPath() + outputFolder;
        boolean res = new File(pathCSVFolder).mkdirs();
        CSVUtil.writeAll(outputFile, pathCSVFolder + "/" + "UML2ER_Data.csv");
        return result;
    }
}
