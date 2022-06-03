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

/***
 * It parses the information from the output .csv files from the original work:
 * Troya, J., Segura, S., Parejo, J., & Ruiz-Cortés, A. (2018).
 * Spectrum-based fault localization in model transformations.
 * ACM Transactions on Software Engineering and Methodology, 1–50.
 *
 * The tool to generate such files and the original input files are available at:
 * <a href="https://github.com/javitroya/SBFL_MT">...</a>
 */

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
            // 2a. Parse the .csv file
            List<String[]> mutantCSV = CSVUtil.readAll(c.getMutantsPath() + mutantFilename, ';');

            String mutantName = mutantFilename.substring(mutantFilename.indexOf('_') + 1, mutantFilename.indexOf(
                    "--OCL"));

            // 2b. Create the mutant object checking if it was previously created
            Mutant mutant = new Mutant(mutantName);
            if (!result.contains(mutant)) {
                result.add(mutant);
            } else {
                mutant = result.get(result.indexOf(mutant));
            }

            int headersRow = SpectrumParser.getHeadersRow(mutantCSV, SPECTRUM_HEADERS[0]);
            int lastRuleRow = SpectrumParser.getNumberOfRules(mutantCSV, headersRow);
            List<Integer> selectedColumns = SpectrumParser.getSelectedColumnsByHeader(mutantCSV, SPECTRUM_HEADERS);
            int constraintId = SpectrumParser.getConstraintId(CONSTRAINT_NAME, mutantFilename);

            // 2c. Create the constraint object checking if it was previously stored
            Constraint constraint = new Constraint(CONSTRAINT_NAME + constraintId, mutantCSV.get(headersRow - 2)[2]);
            if (!mutant.getAppliedConstraints().contains(constraint)) {
                mutant.addConstraint(constraint);
            } else {
                constraint = mutant.getAppliedConstraints().get(mutant.getAppliedConstraints().indexOf(constraint));
            }

            // 2d. Analyse each line  in the csv mutant file
            for (int i = headersRow + 1; i < lastRuleRow; i++) {
                String[] row = mutantCSV.get(i);
                List<String> rowOutput = new ArrayList<>();

                // 2d1. Get the rule id of the row (it is always located in column 1)
                String ruleId = row[1];

                // 2d2. Add the index to the new row.
                rowOutput.add(Integer.toString(index));

                // 2d3. Add the selected columns to the row
                for (int j : selectedColumns) {
                    if (j == 0) {
                        rowOutput.add(row[j].isEmpty() ? "0" : "1");
                    } else {
                        rowOutput.add(row[j]);
                    }
                }

                List<String> rules = SpectrumParser.getRules(c.getRulesFootprintPath() + c.getCaseStudy() +
                        "_Static_Rules_" + mutantName + ".csv");

                // 2d4. Add the corresponding value from the MatchingTable
                for (String metric : STATIC_HEADERS) {
                    int ruleIndex = rules.indexOf(ruleId);
                    String matchingValue =
                            String.valueOf(matchingTables.get(mutantName + "_" + metric)[constraintId - 1][ruleIndex]);
                    rowOutput.add(matchingValue);
                }
                rowOutput.add(String.valueOf(constraintId));
                rowOutput.add(ruleId);
                rowOutput.add(mutantName);

                // 2d5. Create the rule object
                Rule rule = new Rule(ruleId, rowOutput.subList(1,
                        SPECTRUM_HEADERS.length + STATIC_HEADERS.length + 1), SpectrumParser.getRow(SPECTRUM_HEADERS,
                        STATIC_HEADERS));
                constraint.addRule(rule);

                // 2d6. Print all the information to an output file for easier analysis
                outputFile.add(rowOutput.toArray(new String[0]));
            }

            index++;
        }
        String pathCSVFolder = c.getOutputSpectrumPath() + outputFolder;
        boolean res = new File(pathCSVFolder).mkdirs();
        CSVUtil.writeAll(outputFile, pathCSVFolder + "/" + "SBFL_UML2ER_Data.csv");
        return result;
    }
}
