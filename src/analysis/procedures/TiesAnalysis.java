package analysis.procedures;


import analysis.elements.Constraint;
import analysis.elements.Mutant;
import analysis.elements.Rule;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/***
 * It counts the number of ties between the buggy rule and any other and how many of those ties could be
 * broken using inheritance relationships or static information.
 */
public class TiesAnalysis {

    public static String analyzeTies(List<Mutant> mutants, Map<String, List<String>> rulesInheritance,
                                     String filepath, String suspiciousnessFormula) throws FileNotFoundException {
        // COUNTERS
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
                Rule buggyRule = constraint.getHighestBuggyRule(constraint.getRanking(suspiciousnessFormula));
                for (Rule rule : constraint.getNonBuggyRules()) {
                    if (buggyRule.getAnalysisValues().get(suspiciousnessFormula).equals(rule.getAnalysisValues().get(suspiciousnessFormula))) {
                        ties++;

                        if (rulesInheritance.get(buggyRule.getName()) != null && rulesInheritance.get(buggyRule.getName()).contains(rule.getName()) || rulesInheritance.get(rule.getName()) != null && rulesInheritance.get(rule.getName()).contains(buggyRule.getName())) {
                            solvedTiedByInheritance++;
                        } else {
                            unsolvableTiesByInheritance++;
                        }

                        if (buggyRule.getAnalysisValues().get("CC") > rule.getAnalysisValues().get("CC")) {
                            solvedTiesByCC++;
                        }

                        if (buggyRule.getAnalysisValues().get("RC") > rule.getAnalysisValues().get("RC")) {
                            solvedTiesByRC++;
                        }

                        if (buggyRule.getAnalysisValues().get("RCR") > rule.getAnalysisValues().get("RCR")) {
                            solvedTiesByRCR++;
                        }

                        if (!(buggyRule.getAnalysisValues().get("CC") > rule.getAnalysisValues().get("CC")) && !(buggyRule.getAnalysisValues().get("RC") > rule.getAnalysisValues().get("RC")) && !(buggyRule.getAnalysisValues().get("RCR") > rule.getAnalysisValues().get("RCR"))) {
                            unsolvableTiesByStatic++;
                        }

                        if (!(buggyRule.getAnalysisValues().get("CC") > rule.getAnalysisValues().get("CC")) && !(buggyRule.getAnalysisValues().get("RC") > rule.getAnalysisValues().get("RC")) && !(buggyRule.getAnalysisValues().get("RCR") > rule.getAnalysisValues().get("RCR")) && !(rulesInheritance.get(buggyRule.getName()) != null && rulesInheritance.get(buggyRule.getName()).contains(rule.getName()) || rulesInheritance.get(rule.getName()) != null && rulesInheritance.get(rule.getName()).contains(buggyRule.getName()))) {
                            unsolvableTies++;
                        }

                    }
                }
            }

        }

        // PRINT RESULTS
        String res = "Ties: " + ties + "\n";
        res += "Solved by inheritance: " + solvedTiedByInheritance + "\n";
        res += "Unsolvable by inheritance: " + unsolvableTiesByInheritance + "\n";
        res += "Solved by CC: " + solvedTiesByCC + "\n";
        res += "Solved by RC: " + solvedTiesByRC + "\n";
        res += "Solved by RCR: " + solvedTiesByRCR + "\n";
        res += "Unsolvable by static: " + unsolvableTiesByStatic + "\n";
        res += "Unsolvable by both methods: " + unsolvableTies + "\n";

        try (PrintWriter out = new PrintWriter(filepath + "_" + suspiciousnessFormula + "_TiesAnalysis.txt")) {
            out.println(res);
        }

        return res;
    }

}
