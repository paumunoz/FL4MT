package analysis.procedures;


import analysis.elements.Constraint;
import analysis.elements.Mutant;
import analysis.elements.Rule;

import java.io.*;
import java.util.*;

public class TiesAnalysis {

    private static StringBuilder result;

    public static String analyzeInheritance(List<Mutant> mutants, Map<String, List<String>> rulesInheritance, String filepath) throws FileNotFoundException {
        int ties = 0;
        int solvedTiedByInheritance = 0;
        int unsolvableTiesByInheritance = 0;
        int solvedTiesByCC = 0;
        int solvedTiesByRC = 0;
        int solvedTiesByRCR = 0;
        int unsolvableTiesByStatic = 0;
        int unsolvableTies = 0;
        for (Mutant mutant : mutants) {
            // 1a. Traverse all constraints applied to that mutant
            for (Constraint constraint : mutant.getAppliedConstraints()) {
                // 2a. Find duplicates between buggy and no buggy rules
                for (Rule buggyRule : constraint.getBuggy()) {
                    for (Rule rule : constraint.getNonBuggy()) {
                        if (buggyRule.duplicateRule(rule)) {
                            System.out.println("Duplicate rules: ");
                            System.out.println("Buggy: " + buggyRule.getName());
                            System.out.println(buggyRule.getAnalysisValues());
                            System.out.println("Not Buggy: " + rule.getName());
                            System.out.println(rule.getAnalysisValues());
                            ties++;

                            if (rulesInheritance.get(buggyRule.getName()) != null && rulesInheritance.get(buggyRule.getName()).contains(rule.getName()) ||
                                    rulesInheritance.get(rule.getName()) != null && rulesInheritance.get(rule.getName()).contains(buggyRule.getName())) {
                                //System.out.println("There is inheritance between the rules.");
                                //System.out.println("Tie solved! The Buggy rule is " + rule.getName());
                                solvedTiedByInheritance++;
                            } else {
                                unsolvableTiesByInheritance++;
                            }

                            if(buggyRule.getAnalysisValues().get("CC") > rule.getAnalysisValues().get("CC")){
                                solvedTiesByCC++;
                            }

                            if(buggyRule.getAnalysisValues().get("RC") > rule.getAnalysisValues().get("RC")){
                                solvedTiesByRC++;
                            }

                            if(buggyRule.getAnalysisValues().get("RCR") > rule.getAnalysisValues().get("RCR")){
                                solvedTiesByRCR++;
                            }

                            if(!(buggyRule.getAnalysisValues().get("CC") > rule.getAnalysisValues().get("CC")) &&
                                    !(buggyRule.getAnalysisValues().get("RC") > rule.getAnalysisValues().get("RC")) &&
                            !(buggyRule.getAnalysisValues().get("RCR") > rule.getAnalysisValues().get("RCR"))){
                                unsolvableTiesByStatic++;
                            }

                            if(!(buggyRule.getAnalysisValues().get("CC") > rule.getAnalysisValues().get("CC")) &&
                                    !(buggyRule.getAnalysisValues().get("RC") > rule.getAnalysisValues().get("RC")) &&
                                    !(buggyRule.getAnalysisValues().get("RCR") > rule.getAnalysisValues().get("RCR")) &&
                                    !(rulesInheritance.get(buggyRule.getName()) != null && rulesInheritance.get(buggyRule.getName()).contains(rule.getName()) ||
                                            rulesInheritance.get(rule.getName()) != null && rulesInheritance.get(rule.getName()).contains(buggyRule.getName()))){
                                unsolvableTies++;
                            }

                        }
                    }
                }
            }
        }

        String res = "Ties: " + ties + "\n";
        res += "Solved by inheritance: " + solvedTiedByInheritance + "\n";
        res += "Unsolvable by inheritance: " + unsolvableTiesByInheritance + "\n";
        res += "Solved by CC: " + solvedTiesByCC + "\n";
        res += "Solved by RC: " + solvedTiesByRC + "\n";
        res += "Solved by RCR: " + solvedTiesByRCR + "\n";
        res += "Unsolvable by static: " + unsolvableTiesByStatic + "\n";
        res += "Unsolvable by both methods: " + unsolvableTies + "\n";

        try (PrintWriter out = new PrintWriter(filepath + "tiesAnalysis.txt")) {
            out.println(res);
        }

        return res;
    }

    private static void printExamScores(Map<String, List<String>> ruleInheritance, Constraint tieSolved,
                                        String staticTechnique, String suspiciousFormula) {
        result.append("Constraint: ").append(tieSolved.getName()).append("\n");
        result.append("ORIGINAL EXAM SCORE: ")
                .append(tieSolved.getExamScore(suspiciousFormula)[0]).append(" ")
                .append(tieSolved.getExamScore(suspiciousFormula)[1]).append(" ")
                .append(tieSolved.getExamScore(suspiciousFormula)[2]).append("\n");
        result.append("STATIC IMPROVED EXAM SCORE: ")
                .append(tieSolved.getStaticImprovedExamScore(suspiciousFormula, staticTechnique)[0]).append(" ")
                .append(tieSolved.getStaticImprovedExamScore(suspiciousFormula, staticTechnique)[1]).append(" ")
                .append(tieSolved.getStaticImprovedExamScore(suspiciousFormula, staticTechnique)[2]).append("\n");
        result.append("RULES INHERITANCE IMPROVED EXAM SCORE: ")
                .append(tieSolved.getInheritanceImprovedExamScore(suspiciousFormula, ruleInheritance)[0]).append(" ")
                .append(tieSolved.getInheritanceImprovedExamScore(suspiciousFormula, ruleInheritance)[1]).append(" ")
                .append(tieSolved.getInheritanceImprovedExamScore(suspiciousFormula, ruleInheritance)[2]).append("\n");
    }

}
