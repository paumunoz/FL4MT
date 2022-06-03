package analysis.procedures;

import analysis.elements.Case;
import analysis.elements.Constraint;
import analysis.elements.Mutant;
import analysis.elements.Rule;
import csv.util.CSVUtil;

import java.io.File;
import java.util.*;

public class ExamScoreAnalysis {
    private static final String[] FIRST_AUX_HEADERS = new String[]{"Buggy Rule", "Rule"};
    private static final String[] SPECTRUM_HEADERS = new String[]{"Mountford", "Kulcynski2", "Zoltar", "Ochiai"};
    private static final String[] STATIC_HEADERS = new String[]{"CC", "RC", "RCR"};
    private static final String[] EXAM_SCORES = new String[]{"EXAM-BC", "EXAM-WC", "EXAM-AVG"};
    private static final String ORIGINAL = "original";
    private static final String INHERITANCE = "inheritance";
    private static final String STATIC = "static";

    public static void printMutantEXAMScoreFile(List<Mutant> mutants, Case cs, String outputFolder) throws Exception {
        // 0. For each mutant, we create an output file with its EXAM Score values
        for (Mutant mutant : mutants) {
            List<String[]> outputFile = new ArrayList<>();

            // 1. Calculate the EXAM Score for each constraint
            for (Constraint c : mutant.getAppliedConstraints()) {
                outputFile.add(new String[]{c.getName(), c.getOclConstraint()});

                // 1a. Add the headers row to the file
                List<String> headers = new ArrayList<>(Arrays.asList(FIRST_AUX_HEADERS));
                for (String spectrumHeader : SPECTRUM_HEADERS) {
                    headers.add(spectrumHeader);
                    headers.add("Original Ranking");
                    headers.add("Inheritance Ranking");
                    for (String staticHeader : STATIC_HEADERS) {
                        headers.add(staticHeader);
                        headers.add(staticHeader + " Static Ranking");
                    }
                }
                outputFile.add(headers.toArray(new String[0]));

                // 1b. Get the rankings for each of the tie-breaking methods: original, inheritance and static
                Map<String, Map<Rule, Integer>> rankings = new HashMap<>();
                for (String suspiciousFormula : SPECTRUM_HEADERS) {
                    Map<Rule, Integer> originalRanking = c.getRanking(suspiciousFormula);
                    rankings.put(suspiciousFormula + "_original", originalRanking);
                    rankings.put(suspiciousFormula + "_inheritance", c.getInheritanceRanking(suspiciousFormula,
                            cs.getRuleInheritance()));
                    for (String staticTechnique : STATIC_HEADERS) {
                        rankings.put(suspiciousFormula + "_" + staticTechnique + "_static",
                                c.getStaticRanking(staticTechnique, suspiciousFormula));
                    }
                }

                // 1c. Print the ranking of each of the rules for every Spectrum formulae and tie-breaking method
                for (Rule r : c.getRulesApplied()) {
                    List<String> row = new ArrayList<>();
                    row.add(r.isBuggy() ? "1" : "0");
                    row.add(r.getName());
                    for (String suspiciousFormula : SPECTRUM_HEADERS) {
                        row.add(String.valueOf(r.getAnalysisValues().get(suspiciousFormula)));
                        row.add(String.valueOf(rankings.get(suspiciousFormula + "_original").get(r)));
                        row.add(String.valueOf(rankings.get(suspiciousFormula + "_inheritance").get(r)));
                        for (String staticTechnique : STATIC_HEADERS) {
                            row.add(String.valueOf(r.getAnalysisValues().get(staticTechnique)));
                            row.add(String.valueOf(rankings.get(suspiciousFormula + "_" + staticTechnique + "_static").get(r)));
                        }
                    }
                    outputFile.add(row.toArray(new String[0]));
                }

                // 1e. Calculate and store the EXAM Score for every Spectrum formulae and tie-breaking method
                Map<String, double[]> examScores = new HashMap<>();
                for (String suspiciousFormula : SPECTRUM_HEADERS) {
                    examScores.put(suspiciousFormula + "_original",
                            c.calculateExamScore(rankings.get(suspiciousFormula + "_original")));
                    examScores.put(suspiciousFormula + "_inheritance",
                            c.calculateExamScore(rankings.get(suspiciousFormula + "_inheritance")));
                    for (String staticTechnique : STATIC_HEADERS) {
                        examScores.put(suspiciousFormula + "_" + staticTechnique + "_static",
                                c.calculateExamScore(rankings.get(suspiciousFormula + "_" + staticTechnique +
                                        "_static")));
                    }
                }

                // 1f. Print the EXAM Scores in the corresponding columns
                List<String> row;
                for (int i = 0; i < 3; i++) {
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
            }

            String pathCSVFolder = cs.getOutputTiesExamScore() + cs.getFolder();
            boolean res = new File(pathCSVFolder).mkdirs();
            CSVUtil.writeAll(outputFile,
                    pathCSVFolder + "/" + outputFolder + cs.getCaseStudy() + "_" + mutant.getName() +
                    "--suspiciousness" + ".csv");


        }
    }

    public static void printExamScoreAggregatedValues(List<Mutant> mutants, Case cs, String outputFolder) throws Exception {
        // 0. It creates a file with the aggregated values for the EXAM Scores for all mutants
        List<String[]> outputFile = new ArrayList<>();
        for (String suspiciousFormula : SPECTRUM_HEADERS) {
            Map<String, List<Double>> bestCase = new HashMap<>();
            initializeAggregationMaps(bestCase);
            Map<String, List<Double>> worstCase = new HashMap<>();
            initializeAggregationMaps(worstCase);
            Map<String, List<Double>> avgCase = new HashMap<>();
            initializeAggregationMaps(avgCase);

            for (Mutant mutant : mutants) {
                for (Constraint c : mutant.getAppliedConstraints()) {
                    // ORIGINAL
                    Map<Rule, Integer> originalRanking = c.getRanking(suspiciousFormula);
                    double[] originalExamScore = c.calculateExamScore(originalRanking);
                    bestCase.get(ORIGINAL).add(originalExamScore[0]);
                    worstCase.get(ORIGINAL).add(originalExamScore[1]);
                    avgCase.get(ORIGINAL).add(originalExamScore[2]);

                    // INHERITANCE
                    Map<Rule, Integer> inheritanceRanking = c.getInheritanceRanking(suspiciousFormula,
                            cs.getRuleInheritance());
                    double[] inheritanceExamScore = c.calculateExamScore(inheritanceRanking);
                    bestCase.get(INHERITANCE).add(inheritanceExamScore[0]);
                    worstCase.get(INHERITANCE).add(inheritanceExamScore[1]);
                    avgCase.get(INHERITANCE).add(inheritanceExamScore[2]);

                    // STATIC
                    for (String staticTechnique : STATIC_HEADERS) {
                        Map<Rule, Integer> staticRanking = c.getStaticRanking(staticTechnique, suspiciousFormula);
                        double[] staticExamScore = c.calculateExamScore(staticRanking);
                        bestCase.get(staticTechnique + STATIC).add(staticExamScore[0]);
                        worstCase.get(staticTechnique + STATIC).add(staticExamScore[1]);
                        avgCase.get(staticTechnique + STATIC).add(staticExamScore[2]);
                    }
                }
            }
            printResultsAggregation("BC" + suspiciousFormula, bestCase, outputFile);
            printResultsAggregation("WC" + suspiciousFormula, worstCase, outputFile);
            printResultsAggregation("AC" + suspiciousFormula, avgCase, outputFile);
        }

        String pathCSVFolder = cs.getOutputTiesExamScore() + cs.getFolder();
        boolean res = new File(pathCSVFolder).mkdirs();
        CSVUtil.writeAll(outputFile, pathCSVFolder + "/" + outputFolder + "SimpleUML2ER " + "--EXAMSCORE" + ".csv");

    }

    private static void initializeAggregationMaps(Map<String, List<Double>> worstCase) {
        worstCase.put(ORIGINAL, new ArrayList<>());
        worstCase.put(INHERITANCE, new ArrayList<>());
        for (String staticTechnique : STATIC_HEADERS) {
            worstCase.put(staticTechnique + STATIC, new ArrayList<>());
        }
    }

    private static void printResultsAggregation(String typeCase, Map<String, List<Double>> aggregatedCase,
                                                List<String[]> outputFile) {
        outputFile.add(new String[]{typeCase + ORIGINAL, String.valueOf(calculateMDN(aggregatedCase.get(ORIGINAL))),
                String.valueOf(calculateAVG(aggregatedCase.get(ORIGINAL))),
                String.valueOf(calculateSD(aggregatedCase.get(ORIGINAL)))});

        outputFile.add(new String[]{typeCase + INHERITANCE,
                String.valueOf(calculateMDN(aggregatedCase.get(INHERITANCE))),
                String.valueOf(calculateAVG(aggregatedCase.get(INHERITANCE))),
                String.valueOf(calculateSD(aggregatedCase.get(INHERITANCE)))});

        for (String staticTechnique : STATIC_HEADERS) {
            outputFile.add(new String[]{typeCase + staticTechnique,
                    String.valueOf(calculateMDN(aggregatedCase.get(staticTechnique + STATIC))),
                    String.valueOf(calculateAVG(aggregatedCase.get(staticTechnique + STATIC))),
                    String.valueOf(calculateSD(aggregatedCase.get(staticTechnique + STATIC)))});
        }
    }

    public static double calculateSD(List<Double> values) {
        double standardDeviation = 0.0;
        double mean = calculateAVG(values);
        for (double num : values) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation / values.size());
    }

    public static double calculateAVG(List<Double> values) {
        double sum = 0.0;
        for (double num : values) {
            sum += num;
        }
        return sum / values.size();
    }

    public static double calculateMDN(List<Double> values) {
        Collections.sort(values);
        int numberOfElements = values.size();
        double median;
        if (numberOfElements % 2 == 0) {
            double sumOfMiddleElements = values.get(numberOfElements / 2) + values.get(numberOfElements / 2 - 1);
            median = sumOfMiddleElements / 2;
        } else {
            median = values.get(numberOfElements / 2);
        }
        return median;
    }
}
