package analysis.elements;

import java.util.*;

public class Rule {
    private String name;
    private Map<String, Double> analysisValues;
    private boolean buggy;

    public Rule(String name, List<String> analysisValues, String[] headers){
        this.analysisValues = new HashMap<>();
        for(int i = 2; i<analysisValues.size(); i++){
            this.analysisValues.put(headers[i], Double.parseDouble(analysisValues.get(i)));
        }
        this.buggy = analysisValues.get(0).equals("1");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public boolean duplicateRule(Rule r){
        boolean duplicate = true;
        for(String key : this.getAnalysisValues().keySet()){
            // 1. We do not take into account the static values to determine if a row is duplicated
            if(!key.equals("CC") && !key.equals("RCR") && !key.equals("RC")){
                if(!r.getAnalysisValues().get(key).equals(this.getAnalysisValues().get(key))){
                    duplicate = false;
                    break;
                }
            }
        }
        return duplicate;
    }

    public Map<String, Double> getAnalysisValues() {
        return analysisValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return buggy == rule.buggy && Objects.equals(name, rule.name) && Objects.equals(analysisValues, rule.analysisValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, analysisValues, buggy);
    }
}
