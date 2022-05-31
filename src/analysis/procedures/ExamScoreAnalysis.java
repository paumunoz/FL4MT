package analysis.procedures;

import analysis.elements.Case;
import analysis.elements.Constraint;
import analysis.elements.Mutant;
import analysis.elements.Rule;
import csv.spectrum.SpectrumParser;
import csv.util.CSVUtil;

import java.io.File;
import java.util.*;

public class ExamScoreAnalysis {

    private static final String[] FIRST_AUX_HEADERS = new String[]{"Buggy Rule", "Rule"};
    private static final String[] SPECTRUM_HEADERS = new String[]{"Mountford", "Kulcynski2", "Zoltar", "Ochiai"};
    private static final String[] STATIC_HEADERS = new String[]{"CC", "RC", "RCR"};

    private static final String[] EXAM_SCORES = new String[]{"EXAM-BC", "EXAM-WC", "EXAM-AVG"};

    public static void buildExamScoreFile(List<Mutant> mutants, Case cs, String outputFolder) throws Exception {
        for(Mutant mutant : mutants){
            List<String[]> outputFile = new ArrayList<>();

            for(Constraint c : mutant.getAppliedConstraints()) {
                outputFile.add(new String[]{c.getName(), c.getOclConstraint()});

                // 1. Add the headers row to the file
                List<String> headers = new ArrayList<>(Arrays.asList(FIRST_AUX_HEADERS));
                for (int i = 0; i < SPECTRUM_HEADERS.length; i++) {
                    headers.add(SPECTRUM_HEADERS[i]);
                    headers.add("Original Ranking");
                    headers.add("Inheritance Ranking");
                    for (int j = 0; j < STATIC_HEADERS.length; j++) {
                        headers.add(STATIC_HEADERS[j]);
                        headers.add(STATIC_HEADERS[j] + " Static Ranking");
                    }
                }
                outputFile.add(headers.toArray(new String[0]));

                Map<String, Map<String, Integer>> rankings = new HashMap<>();
                for(String suspiciousFormula : SPECTRUM_HEADERS){
                    Map<String, Integer> originalRanking = c.getRanking(suspiciousFormula);
                    Rule highestBuggy = c.getHighestBuggyRule(originalRanking);
                    rankings.put(suspiciousFormula + "_original", originalRanking);
                    rankings.put(suspiciousFormula + "_inheritance",
                            c.getSolvedTieByInheritanceRanking(suspiciousFormula,
                                    cs.getRuleInheritance()));
                    for(String staticTechnique : STATIC_HEADERS){
                        rankings.put(suspiciousFormula + "_" + staticTechnique + "_static",
                                c.getSolvedTieByStaticRanking(staticTechnique, suspiciousFormula));
                    }
                }

                for(Rule r : c.getRulesApplied()){
                    List<String> row = new ArrayList<>();
                    row.add(r.isBuggy() ? "1" : "0");
                    row.add(r.getName());
                    for(String suspiciousFormula : SPECTRUM_HEADERS){
                        row.add(String.valueOf(r.getAnalysisValues().get(suspiciousFormula)));
                        row.add(String.valueOf(rankings.get(suspiciousFormula + "_original").get(r.getName())));
                        row.add(String.valueOf(rankings.get(suspiciousFormula + "_inheritance").get(r.getName())));
                        for(String staticTechnique : STATIC_HEADERS){
                            row.add(String.valueOf(r.getAnalysisValues().get(staticTechnique)));
                            row.add(String.valueOf(rankings.get(suspiciousFormula + "_" + staticTechnique + "_static").get(r.getName())));
                        }
                    }
                    outputFile.add(row.toArray(new String[0]));
                }

                Map<String, double[]> examScores = new HashMap<>();
                for(String suspiciousFormula : SPECTRUM_HEADERS){
                    examScores.put(suspiciousFormula + "_original",
                            c.calculateExamScore(rankings.get(suspiciousFormula + "_original")));
                    examScores.put(suspiciousFormula + "_inheritance",
                            c.calculateExamScore(rankings.get(suspiciousFormula + "_inheritance")));
                    for(String staticTechnique : STATIC_HEADERS){
                        examScores.put(suspiciousFormula + "_" + staticTechnique + "_static",
                                c.calculateExamScore(rankings.get(suspiciousFormula + "_" + staticTechnique +
                                        "_static")));
                    }
                }

                List<String> row;
                for(int i = 0; i < 3; i++) {
                    row = new ArrayList<>();
                    row.add(" ");
                    row.add(EXAM_SCORES[i]);
                    for (String suspiciousFormula : SPECTRUM_HEADERS) {
                        row.add(" ");
                        row.add(String.valueOf(examScores.get(suspiciousFormula + "_original")[i]));
                        row.add(String.valueOf(examScores.get(suspiciousFormula + "_inheritance")[i]));
                        for (String staticTechnique : STATIC_HEADERS) {
                            row.add(" ");
                            row.add(String.valueOf(examScores.get(suspiciousFormula + "_" + staticTechnique +
                                    "_static")[i]));
                        }
                    }
                    outputFile.add(row.toArray(new String[0]));
                }

                // Empty row
                outputFile.add(new String[20]);
            }
            String pathCSVFolder = cs.getOutputTiesExamScore() + outputFolder;
            boolean res = new File(pathCSVFolder).mkdirs();
            CSVUtil.writeAll(outputFile, pathCSVFolder + "/" + "UML2ER_ " + mutant.getName() + "--suspiciousness.csv");

        }


    }
}
