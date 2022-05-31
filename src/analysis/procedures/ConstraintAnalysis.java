package analysis.procedures;

import analysis.elements.Constraint;
import analysis.elements.Mutant;
import analysis.elements.Rule;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConstraintAnalysis {

    private static List<String> checkedConstraints;
    private static StringBuilder result;
    private static int ties;
    private static int solvedTiedByInheritance;
    private static int unsolvableTiesByInheritance;
    private static int solvedTiesByCC;
    private static int solvedTiesByRC;
    private static int solvedTiesByRCR;
    private static int unsolvableTiesByStatic;
    private static int unsolvableTies;

    public static String findTheBugUsingStaticInformation(List<Mutant> mutants,
                                                          Map<String, List<String>> ruleInheritance, Map<String,
            List<String>> constraintInheritance, String staticTechnique, String suspiciousFormula, String filepath) throws FileNotFoundException {
        result = new StringBuilder();
        resetTieCounters();
        for (Mutant mutant : mutants) {
            result.append("----------------\n");
            result.append("Analysis for: ").append(mutant.getName()).append("\n");
            checkedConstraints = new ArrayList<>();
            // 1a. Traverse all constraints applied to that mutant
            for (int i = 1; i <= 14; i++) {
                int finalI = i;
                Constraint constraint =
                        mutant.getAppliedConstraints().stream().filter(ic -> finalI == ic.getOrder()).findAny().orElse(null);
                // 2a. If the constraint is not included, it means that all the tests passed
                if (constraint == null) {
                    result.append("OCL").append(i).append(" passed.").append("\n");
                    continue;
                }
                // 2b. If the constraint exists, and it was not checked before, we check it now
                if (!checkedConstraints.contains(constraint.getName())) {
                    Constraint tieSolved = checkConstraintStatic(constraintInheritance, constraint,
                            mutant.getAppliedConstraints(), ruleInheritance, staticTechnique, suspiciousFormula);
                    if (tieSolved != null) {
                        result.append("Buggy rule found.").append("\n");
                        printExamScores(tieSolved, staticTechnique, suspiciousFormula, true);
                    } else {
                        result.append("Tie wasn't solved.").append("\n");
                        printExamScores(constraint, staticTechnique, suspiciousFormula, false);
                    }
                }
            }
        }
        printTiesInformation();

        try (PrintWriter out = new PrintWriter(filepath + "constraintAnalysis.txt")) {
            out.println(result.toString());
        }

        return result.toString();
    }


    private static Constraint checkConstraintStatic(Map<String, List<String>> constraintInheritance,
                                                    Constraint constraint, List<Constraint> appliedConstraints,
                                                    Map<String, List<String>> ruleInheritance, String staticTechnique
            , String suspiciousFormula) {
        Constraint tieSolved = constraint;
        result.append("Checking constraint: ").append(constraint.getName()).append("\n");
                //.append(" - ").append(constraint.getOclConstraint());
        // 2a. Find duplicates between buggy and no buggy rules
        Rule buggyRule = constraint.getHighestBuggyRule(constraint.getRanking(suspiciousFormula));
        for (Rule rule : constraint.getNonBuggy()) {
            if (buggyRule.duplicateRule(rule)) {
                // 3a. We print que duplicated information for the output file
                printDuplicatedInformation(buggyRule, rule);
                // 3b. Count the type of tie
                countTies(buggyRule, rule, ruleInheritance);
                if (buggyRule.getAnalysisValues().get(staticTechnique) <= rule.getAnalysisValues().get(staticTechnique)) {
                    tieSolved = null;
                    List<String> inheritedConstraints = constraintInheritance.get(constraint.getName());
                    // 2b. Let's try to break the ties with the inherited constraints
                    if (inheritedConstraints != null) {
                        for (String inheritingConstraintName : inheritedConstraints) {
                            if (!checkedConstraints.contains(inheritingConstraintName)) {
                                Constraint inheritingConstraint =
                                        appliedConstraints.stream().filter(ic -> inheritingConstraintName.equals(ic.getName())).findAny().orElse(null);
                                if (inheritingConstraint != null) {
                                    tieSolved = checkConstraintStatic(constraintInheritance, inheritingConstraint,
                                            appliedConstraints, ruleInheritance, staticTechnique, suspiciousFormula);
                                    if (tieSolved != null) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    result.append("Tie solved using static technique: ").append(staticTechnique).append("\n");
                }

            }
        }
        checkedConstraints.add(constraint.getName());
        if (tieSolved != null) {
            addCheckedConstraints(constraintInheritance, constraint.getName());
        }
        return tieSolved;
    }

    private static void printDuplicatedInformation(Rule buggyRule, Rule rule) {
        result.append("Duplicated rules: ").append("\n");
        result.append("- Buggy: ").append(buggyRule.getName()).append("\n");
        //result.append(buggyRule.getAnalysisValues()).append("\n");
        result.append("- Not Buggy: ").append(rule.getName()).append("\n");
        //result.append(rule.getAnalysisValues()).append("\n");
    }

    private static void addCheckedConstraints(Map<String, List<String>> constraintInheritance, String constraint) {
        if (constraintInheritance.get(constraint) != null) {
            for (String c : constraintInheritance.get(constraint)) {
                checkedConstraints.add(c);
                addCheckedConstraints(constraintInheritance, c);
            }
        }
    }

    private static void resetTieCounters() {
        ties = 0;
        solvedTiesByCC = 0;
        solvedTiesByRC = 0;
        solvedTiesByRCR = 0;
        unsolvableTiesByStatic = 0;
    }

    private static void countTies(Rule buggyRule, Rule rule, Map<String, List<String>> rulesInheritance) {
        ties++;

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
    }

    private static void printTiesInformation() {
        result.append("----------------\n");
        result.append("Ties: ").append(ties).append("\n");
        result.append("Solved by CC: ").append(solvedTiesByCC).append("\n");
        result.append("Solved by RC: ").append(solvedTiesByRC).append("\n");
        result.append("Solved by RCR: ").append(solvedTiesByRCR).append("\n");
        result.append("Unsolvable by static: ").append(unsolvableTiesByStatic).append("\n");
    }

    private static void printExamScores(Constraint tieSolved, String staticTechnique, String suspiciousFormula,
                                        boolean staticImproved) {
        //result.append("Constraint: ").append(tieSolved.getName()).append("\n");
        result.append("ORIGINAL EXAM SCORE: ")
                .append(tieSolved.getExamScore(suspiciousFormula)[0]).append(" ")
                .append(tieSolved.getExamScore(suspiciousFormula)[1]).append(" ")
                .append(tieSolved.getExamScore(suspiciousFormula)[2]).append("\n");
        if(staticImproved) {
            result.append("STATIC IMPROVED EXAM SCORE: ").append(tieSolved.getStaticImprovedExamScore(suspiciousFormula,
                    staticTechnique)[0]).append(" ").append(tieSolved.getStaticImprovedExamScore(suspiciousFormula,
                    staticTechnique)[1]).append(" ").append(tieSolved.getStaticImprovedExamScore(suspiciousFormula,
                    staticTechnique)[2]).append("\n");
        }
    }
}