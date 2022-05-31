package analysis.elements;

import java.util.*;

public class Case {

    private final Map<String, List<String>> constraintInheritance;
    private final Map<String, List<String>> ruleInheritance;

    private final String CURRENT_DIR = System.getProperty("user.dir");
    private final String INPUT_DIR = "/datafiles/input/";
    private final String OUTPUT_DIR = "/datafiles/output/";

    private final boolean embedding;
    private final boolean hierarchy;

    private final String caseStudy;

    public Case(boolean embedding, boolean hierarchy, String caseStudy){
        this.embedding = embedding;
        this.hierarchy = hierarchy;
        this.caseStudy = caseStudy;

        this.constraintInheritance = getConstraintsInheritance();
        if(hierarchy){
            this.ruleInheritance = getRulesInheritanceOriginal();
        } else {
            this.ruleInheritance = getRulesInheritanceNoHierarchy();
        }
    }

    public String getConstraintsFootprintPath(){
        return CURRENT_DIR + INPUT_DIR + "constraints/" + (embedding? "original" : "embedding") + "/";
    }

    public String getFolder(){
        String folder = "original";
        if(embedding && hierarchy){
            folder = "embedding";
        } else if(!hierarchy){
            folder = "nohierarchy";
        }
        return folder;
    }

    public String getRulesFootprintPath(){
        return CURRENT_DIR + INPUT_DIR + "rules/" + getFolder() + "/";
    }

    public String getMutantsPath(){
        return CURRENT_DIR + INPUT_DIR + "mutants/" + (hierarchy? "original" : "nohierarchy") + "/";
    }

    public String getModelsPath(){
        return CURRENT_DIR + INPUT_DIR + "models/";
    }

    public String getOutputMatchingTablesPath(){
        return CURRENT_DIR + OUTPUT_DIR + "matchingtables/" + getFolder() + "/";
    }

    public String getOutputTiesPath(){
        return CURRENT_DIR + OUTPUT_DIR + "ties/";
    }

    public String getOutputTiesExamScore(){ return CURRENT_DIR + OUTPUT_DIR + "examscoreanalysis/";}

    public String getOutputConstraintsPath(){
        return CURRENT_DIR + OUTPUT_DIR + "constraintanalysis/";
    }

    public String getOutputSpectrumPath(){
        return CURRENT_DIR + OUTPUT_DIR + "spectrumanalysis/" + getFolder() + "/";
    }

    public Map<String, List<String>> getConstraintInheritance() {
        return constraintInheritance;
    }

    public Map<String, List<String>> getRuleInheritance() {
        return ruleInheritance;
    }

    public String getCaseStudy(){
        return caseStudy;
    }

    public String fileName(){
        return caseStudy + (embedding? "_embedding" : "") + (hierarchy? "_hierarchy" : "_nohierarchy") + ".csv";
    }

    public String toString() {
        return "Case: {Embedding: " + (embedding? "Yes" : "No") + "," +
                "Hierarchy: " + (hierarchy? "Yes" : "No") + "}";
    }

    /***
     * They contain the element and the list of elements that inherit from that one
     * Example:
     *  OCL3 : {OCL8, OCL9, OCL10, OCL11, OCL12}
     */
    private Map<String, List<String>> getConstraintsInheritance(){
        Map<String, List<String>> constraintsInheritance = new HashMap<>();
        constraintsInheritance.put("OCL1", new ArrayList<>(Arrays.asList("OCL2")));
        constraintsInheritance.put("OCL2", new ArrayList<>(Arrays.asList("OCL3")));
        constraintsInheritance.put("OCL3", new ArrayList<>(Arrays.asList("OCL8", "OCL9", "OCL10", "OCL11", "OCL12")));

        return constraintsInheritance;
    }

    private Map<String, List<String>> getRulesInheritanceOriginal(){
        Map<String, List<String>> rulesInheritance = new HashMap<>();
        rulesInheritance.put("NamedElement", new ArrayList<>(Arrays.asList("Package", "Class", "Property")));
        rulesInheritance.put("Property", new ArrayList<>(Arrays.asList("Attributes", "References")));
        rulesInheritance.put("References", new ArrayList<>(Arrays.asList("WeakReferences", "StrongReferences")));

        return  rulesInheritance;
    }

    private Map<String, List<String>> getRulesInheritanceNoHierarchy(){
        return new HashMap<>();
    }
}
