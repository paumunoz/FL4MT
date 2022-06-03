package analysis.elements;

import java.util.*;

public class Constraint {
    private final String name;
    private final String oclConstraint;
    private final List<Rule> rulesApplied;

    public Constraint(String name, String oclConstraint) {
        this.name = name;
        this.oclConstraint = oclConstraint;
        this.rulesApplied = new ArrayList<>();
    }

    public void addRule(Rule r) {
        this.rulesApplied.add(r);
    }

    public String getName() {
        return name;
    }

    public List<Rule> getRulesApplied() {
        return rulesApplied;
    }

    public String getOclConstraint() {
        return oclConstraint;
    }

    private int getRuleHighestRanking(Map<Rule, Integer> ranking, List<Rule> rules) {
        int highestRanking = -1;
        for (Rule buggy : rules) {
            for (Rule r : ranking.keySet()) {
                if (r.equals(buggy) && (highestRanking < 0 || highestRanking > ranking.get(r))) {
                    highestRanking = ranking.get(r);
                    break;
                }
            }
        }
        return highestRanking;
    }

    public Rule getHighestBuggyRule(Map<Rule, Integer> ranking) {
        List<Rule> buggys = getBuggyRules();
        Rule highestBug = buggys.get(0);
        for (Rule r : buggys.subList(1, buggys.size())) {
            if (ranking.get(highestBug) > ranking.get(r)) {
                highestBug = r;
            }
        }
        return highestBug;
    }

    private int getLowestRanking(Map<Rule, Integer> ranking, int highestRanking) {
        int lowestRanking = highestRanking - 1;
        for (Rule r : ranking.keySet()) {
            if (ranking.get(r) == highestRanking) {
                lowestRanking++;
            }
        }
        return lowestRanking;
    }

    public Rule getDuplicatedRule(Rule rule, Map<Rule, Integer> ranking) {
        Rule duplicate = null;
        for (Rule r : getNonBuggyRules()) {
            if (rule.isDuplicate(r) && ranking.get(r).equals(ranking.get(rule))) {
                duplicate = r;
                break;
            }
        }
        return duplicate;
    }

    public List<Rule> getBuggyRules() {
        List<Rule> buggyRules = new ArrayList<>();
        for (Rule r : this.rulesApplied) {
            if (r.isBuggy()) {
                buggyRules.add(r);
            }
        }
        return buggyRules;
    }

    public List<Rule> getNonBuggyRules() {
        List<Rule> nonBuggyRules = new ArrayList<>();
        for (Rule r : this.rulesApplied) {
            if (!r.isBuggy()) {
                nonBuggyRules.add(r);
            }
        }
        return nonBuggyRules;
    }

    public Map<Rule, Integer> getInheritanceRanking(String method, Map<String, List<String>> rulesInheritance) {
        Map<Rule, Integer> ranking = getRanking(method);
        Rule buggy = getHighestBuggyRule(ranking);
        Rule duplicateRule = getDuplicatedRule(buggy, ranking);

        if (duplicateRule != null && rulesInheritance.get(duplicateRule.getName()) != null && rulesInheritance.get(duplicateRule.getName()).contains(buggy.getName())) {
            Map<Rule, Double> orderedRules = orderRules(method);
            ranking = new TreeMap<>();
            List<Rule> sameRankingRules = new ArrayList<>();
            double maximum = 2;
            int index;
            int counter = 1;
            Iterator<Rule> orderedRulesIterator = orderedRules.keySet().iterator();
            while (orderedRulesIterator.hasNext() || !sameRankingRules.isEmpty()) {
                Rule nextRule = null;
                if (orderedRulesIterator.hasNext()) {
                    nextRule = orderedRulesIterator.next();
                }
                if (nextRule == null || orderedRules.get(nextRule) < maximum) {
                    if (!sameRankingRules.isEmpty()) {
                        index = counter;
                        if (sameRankingRules.contains(buggy) && sameRankingRules.contains(duplicateRule)) {
                            ranking.put(buggy, index);
                            ranking.put(duplicateRule, index);
                            counter++;
                            index++;
                            sameRankingRules.remove(buggy);
                            sameRankingRules.remove(duplicateRule);
                        }
                        for (Rule sameRankingR : sameRankingRules) {
                            ranking.put(sameRankingR, index);
                            counter++;
                        }
                        sameRankingRules = new ArrayList<>();
                    }
                    if (!(nextRule == null)) {
                        maximum = orderedRules.get(nextRule);
                    }
                }
                if (!(nextRule == null)) {
                    sameRankingRules.add(nextRule);
                }
            }
        }

        return ranking;
    }

    public Map<Rule, Integer> getStaticRanking(String staticTechnique, String suspiciousnessFormula) {
        // 0. To get the ranking using static information, we start modifying a default ranking
        Map<Rule, Integer> rankingResult = new HashMap<>(getRanking(suspiciousnessFormula));
        // 1. We perform the analysis of the rules ordered by the corresponding suspiciousness formulae
        Map<Rule, Double> orderedRules = orderRules(suspiciousnessFormula);

        Iterator<Rule> orderedRulesIterator = orderedRules.keySet().iterator();
        List<Rule> sameRankingRules = new ArrayList<>();
        double maximum = 2;
        int counter = 1;

        while (orderedRulesIterator.hasNext() || !sameRankingRules.isEmpty()) {
            Rule nextRule = null;
            if (orderedRulesIterator.hasNext()) {
                nextRule = orderedRulesIterator.next();
            }
            if (nextRule == null || orderedRules.get(nextRule) < maximum) {
                if (!sameRankingRules.isEmpty()) {
                    int index = counter;
                    // 1a. Order the rules using the corresponding static technique
                    Map<Rule, Double> orderedByStatic = orderRules(staticTechnique, sameRankingRules);
                    // 1b. Get the partial ranking using the static values
                    Map<Rule, Integer> partialStaticRanking = getRanking(orderedByStatic, index);
                    // 1c. Update the values in the default ranking
                    rankingResult.putAll(partialStaticRanking);

                    index += sameRankingRules.size();
                    counter = index;
                    sameRankingRules = new ArrayList<>();
                }
                if (!(nextRule == null)) {
                    maximum = orderedRules.get(nextRule);
                }
            }
            if (!(nextRule == null)) {
                sameRankingRules.add(nextRule);
            }
        }
        return rankingResult;
    }

    private Map<Rule, Double> orderRules(String method, List<Rule> rules) {
        // 1. Get the suspiciousness value for the selected formula {"Mountford", "Kulcynski2", "Zoltar", "Ochiai"}
        Map<Rule, Double> suspiciousnessValues = new HashMap<>();
        for (Rule r : rules) {
            suspiciousnessValues.put(r, r.getAnalysisValues().get(method));
        }

        // 2. Sort the map by value
        List<Map.Entry<Rule, Double>> suspiciousList = new ArrayList<>(suspiciousnessValues.entrySet());
        suspiciousList.sort(Map.Entry.comparingByValue((o1, o2) -> (-1) * o1.compareTo(o2)));

        // 3. Add the values to a new LinkedHashMap, now ordered
        Map<Rule, Double> result = new LinkedHashMap<>();
        for (Map.Entry<Rule, Double> entry : suspiciousList) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private Map<Rule, Double> orderRules(String method) {
        return orderRules(method, this.rulesApplied);
    }

    public Map<Rule, Integer> getRanking(Map<Rule, Double> rules, int index) {
        Map<Rule, Integer> ranking = new TreeMap<>();
        double maximum = 2;
        int counter = index;
        for (Rule r : rules.keySet()) {
            double staticValue = rules.get(r);
            if (staticValue < maximum) {
                maximum = staticValue;
                index = counter;
            }
            ranking.put(r, index);
            counter++;
        }
        return ranking;
    }

    public Map<Rule, Integer> getRanking(String suspiciousnessFormula) {
        Map<Rule, Double> orderedRules = orderRules(suspiciousnessFormula);
        return getRanking(orderedRules, 1);
    }

    public double[] calculateExamScore(Map<Rule, Integer> ranking) {
        List<Rule> buggyRules = getBuggyRules();
        int highestRanking = getRuleHighestRanking(ranking, buggyRules);
        int lowestRanking = getLowestRanking(ranking, highestRanking);
        double bestCase = (double) highestRanking / ranking.size();
        double worstCase = (double) lowestRanking / ranking.size();
        double avgCase = (bestCase + worstCase) / 2;
        return new double[]{bestCase, worstCase, avgCase};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
