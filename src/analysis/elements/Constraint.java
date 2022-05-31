package analysis.elements;

import java.util.*;

public class Constraint implements Comparable<Constraint> {
    private final String name;

    private final int order;
    private final String oclConstraint;
    private final List<Rule> rulesApplied;

    public Constraint(String name, String oclConstraint) {
        this.name = name;
        this.order = Integer.parseInt(name.substring(("OCL").length()));
        this.oclConstraint = oclConstraint;
        this.rulesApplied = new ArrayList<>();
    }

    public double[] getStaticImprovedExamScore(String method, String technique) {
        Map<String, Integer> solvedTieRanking = getSolvedTieByStaticRanking(technique, method);
        return calculateExamScore(solvedTieRanking);
    }

    public double[] getInheritanceImprovedExamScore(String method, Map<String, List<String>> rulesInheritance) {
        Map<String, Integer> solvedTieRanking = getSolvedTieByInheritanceRanking(method, rulesInheritance);
        return calculateExamScore(solvedTieRanking);
    }

    public double[] getExamScore(String method) {
        return calculateExamScore(getRanking(method));
    }

    public double[] calculateExamScore(Map<String, Integer> ranking) {
        List<Rule> buggyRules = getBuggy();
        int highestRanking = getHighestRanking(ranking, buggyRules);
        int lowestRanking = getLowestRanking(ranking, highestRanking);
        double bestCase = (double) highestRanking / ranking.size();
        double worstCase = (double) lowestRanking / ranking.size();
        double avgCase = (bestCase + worstCase) / 2;
        return new double[]{bestCase, worstCase, avgCase};
    }

    public Map<String, Integer> getSolvedTieByInheritanceRanking(String method, Map<String, List<String>> rulesInheritance) {
        Map<String, Integer> ranking = getRanking(method);
        Rule buggy = getHighestBuggyRule(ranking);
        Rule duplicateRule = getDuplicate(buggy, ranking);

        if (duplicateRule != null && rulesInheritance.get(duplicateRule.getName()) != null && rulesInheritance.get(duplicateRule.getName()).contains(buggy.getName())) {
            Map<String, Double> orderedRules = orderRules(method);
            ranking = new TreeMap<>();
            List<String> sameRankingRules = new ArrayList<>();
            double maximum = 2;
            int index;
            int counter = 1;
            Iterator<String> orderedRulesIterator = orderedRules.keySet().iterator();
            while (orderedRulesIterator.hasNext() || !sameRankingRules.isEmpty()) {
                String nextRule = "";
                if (orderedRulesIterator.hasNext()) {
                    nextRule = orderedRulesIterator.next();
                }
                if (nextRule.isEmpty() || orderedRules.get(nextRule) < maximum) {
                    if (!sameRankingRules.isEmpty()) {
                        index = counter;
                        if (sameRankingRules.contains(buggy.getName()) && sameRankingRules.contains(duplicateRule)) {
                            ranking.put(buggy.getName(), index);
                            ranking.put(duplicateRule.getName(), index);
                            counter++;
                            index++;
                            sameRankingRules.remove(buggy.getName());
                            sameRankingRules.remove(duplicateRule.getName());
                        }
                        for (String sameRankingR : sameRankingRules) {
                            ranking.put(sameRankingR, index);
                            counter++;
                        }
                        sameRankingRules = new ArrayList<>();
                    }
                    if (!nextRule.isEmpty()) {
                        maximum = orderedRules.get(nextRule);
                    }
                }
                if (!nextRule.isEmpty()) {
                    sameRankingRules.add(nextRule);
                }
            }
        }

        return ranking;
    }

    public Map<String, Integer> getSolvedTieByStaticRanking(String technique, String method) {
        Map<String, Integer> ranking = getRanking(method);
        Rule buggy = getHighestBuggyRule(ranking);
        Rule duplicateRule = getDuplicateMethod(buggy, ranking, method);
        if (duplicateRule != null && buggy.getAnalysisValues().get(technique) > duplicateRule.getAnalysisValues().get(technique)) {
            Map<String, Double> orderedRules = orderRules(method);
            ranking = new TreeMap<>();
            List<String> sameRankingRules = new ArrayList<>();
            double maximum = 2;
            int index;
            int counter = 1;
            Iterator<String> orderedRulesIterator = orderedRules.keySet().iterator();
            while (orderedRulesIterator.hasNext() || !sameRankingRules.isEmpty()) {
                String nextRule = "";
                if (orderedRulesIterator.hasNext()) {
                    nextRule = orderedRulesIterator.next();
                }
                if (nextRule.isEmpty() || orderedRules.get(nextRule) < maximum) {
                    if (!sameRankingRules.isEmpty()) {
                        index = counter;
                        if (sameRankingRules.contains(buggy.getName())) {
                            ranking.put(buggy.getName(), index);
                            counter++;
                            index++;
                            sameRankingRules.remove(buggy.getName());
                        }
                        for (String sameRankingR : sameRankingRules) {
                            ranking.put(sameRankingR, index);
                            counter++;
                        }
                        sameRankingRules = new ArrayList<>();
                    }
                    if (!nextRule.isEmpty()) {
                        maximum = orderedRules.get(nextRule);
                    }
                }
                if (!nextRule.isEmpty()) {
                    sameRankingRules.add(nextRule);
                }
            }
        }
        return ranking;
    }

    public Map<String, Integer> getRanking(String suspiciousFormula) {
        Map<String, Double> orderedRules = orderRules(suspiciousFormula);
        Map<String, Integer> ranking = new TreeMap<>();
        double maximum = 2;
        int index = -1;
        int counter = 1;
        for (String r : orderedRules.keySet()) {
            if (orderedRules.get(r) < maximum) {
                maximum = orderedRules.get(r);
                index = counter;
            }
            ranking.put(r, index);
            counter++;
        }
        return ranking;
    }

    public Rule getHighestBuggyRule(Map<String, Integer> ranking) {
        List<Rule> buggys = getBuggy();
        Rule highestBug = buggys.get(0);
        for (Rule r : buggys.subList(1, buggys.size())) {
            if (ranking.get(highestBug.getName()) > ranking.get(r.getName())) {
                highestBug = r;
            }
        }
        return highestBug;
    }

    public Rule getDuplicate(Rule buggy, Map<String, Integer> ranking) {
        Rule duplicate = null;
        for (Rule r : getNonBuggy()) {
            if (buggy.duplicateRule(r) && ranking.get(r.getName()).equals(ranking.get(buggy.getName()))) {
                duplicate = r;
                break;
            }
        }
        return duplicate;
    }

    public Rule getDuplicateMethod(Rule buggy, Map<String, Integer> ranking, String method) {
        Rule duplicate = null;
        for (Rule r : getNonBuggy()) {
            if (buggy.getAnalysisValues().get(method).equals(r.getAnalysisValues().get(method))) {
                duplicate = r;
                break;
            }
        }
        return duplicate;
    }

    private int getLowestRanking(Map<String, Integer> ranking, int highestRanking) {
        int lowestRanking = highestRanking - 1;
        for (String r : ranking.keySet()) {
            if (ranking.get(r) == highestRanking) {
                lowestRanking++;
            }
        }
        return lowestRanking;
    }

    private int getHighestRanking(Map<String, Integer> ranking, List<Rule> buggyRules) {
        int highestRanking = -1;
        for (Rule buggy : buggyRules) {
            for (String r : ranking.keySet()) {
                if (r.equals(buggy.getName()) && (highestRanking < 0 || highestRanking > ranking.get(r))) {
                    highestRanking = ranking.get(r);
                    break;
                }
            }
        }
        return highestRanking;
    }

    private Map<String, Double> orderRules(String method) {
        // 1a. Get the suspiciousness value for the selected formula {"Mountford", "Kulcynski2", "Zoltar", "Ochiai"}
        Map<String, Double> suspiciousnessValues = new HashMap<>();
        for (Rule r : rulesApplied) {
            suspiciousnessValues.put(r.getName(), r.getAnalysisValues().get(method));
        }

        // 1b. Sort the map by value
        List<Map.Entry<String, Double>> suspiciousList = new ArrayList<>(suspiciousnessValues.entrySet());
        suspiciousList.sort(Map.Entry.comparingByValue(new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return (-1) * o1.compareTo(o2);
            }
        }));

        // 1c. Add the values to a new LinkedHashMap, now ordered
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : suspiciousList) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
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

    public int getOrder() {
        return order;
    }

    public List<Rule> getBuggy() {
        List<Rule> buggyRules = new ArrayList<>();
        for (Rule r : this.rulesApplied) {
            if (r.isBuggy()) {
                buggyRules.add(r);
            }
        }
        return buggyRules;
    }

    public List<Rule> getNonBuggy() {
        List<Rule> nonBuggyRules = new ArrayList<>();
        for (Rule r : this.rulesApplied) {
            if (!r.isBuggy()) {
                nonBuggyRules.add(r);
            }
        }
        return nonBuggyRules;
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

    @Override
    public int compareTo(Constraint o) {
        return Integer.compare(this.order, o.order);
    }
}
