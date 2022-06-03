//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package analysis.procedures;

import behaviour.Model;
import csv.util.CSVUtil;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.tracts.tool.use2text.files.Use2Text;
import transformations.AddPrefixMM;
import transformations.FileOperations;
import transformations.MergeUseMM;

import java.io.*;
import java.util.*;

/***
 * Modified file from the original work:
 * Burgueño, L., Troya, J., Wimmer, M., & Vallecillo, A. (2015).
 * Static Fault Localization in Model Transformations. IEEE
 * Transactions on Software Engineering, 490–506
 *
 * Available at: <a href="https://atenea.lcc.uma.es/projects/MTB/MTB.html">...</a>
 */

public class MatchingTablesBuilder {
    List<List<String>> constraintElements;
    List<List<String>> ruleElements;
    String srcMMPath;
    String trgMMPath;
    double[][] cc;
    double[][] rc;
    double[][] rcr;
    double inhWeight = 0.5D;
    double threshold = 0.1D;

    public MatchingTablesBuilder(String constraintsPath, String rulesPath, String srcMMPath, String trgMMPath) throws Exception {
        this.srcMMPath = srcMMPath;
        this.trgMMPath = trgMMPath;
        String tempFolder = this.getClass().getClassLoader().getResource(".").getPath() + "temp";
        //System.out.println(tempFolder);
        this.constraintElements = getFootprints(constraintsPath);
        //System.out.println("\nConstraints: " + this.constraintElements);
        this.ruleElements = getFootprints(rulesPath);
        //System.out.println("Rules: " + this.ruleElements + "\n");
        this.cc = new double[this.constraintElements.size()][this.ruleElements.size()];
        this.rc = new double[this.constraintElements.size()][this.ruleElements.size()];
        this.rcr = new double[this.constraintElements.size()][this.ruleElements.size()];
        build();
        //print();
    }

    private List<List<String>> getFootprints(String path) {
        List<String[]> fileMatrix = CSVUtil.readAll(path, ',');
        String[] headersRow = fileMatrix.get(0);
        List<List<String>> result = new ArrayList<>();
        for (int i = 1; i < fileMatrix.size(); i++) {
            String[] row = fileMatrix.get(i);
            List<String> partialResult = new ArrayList<>();
            for (int j = 1; j < row.length; j++) {
                if (!row[j].isEmpty()) {
                    partialResult.add(headersRow[j]);
                }
            }
            result.add(partialResult);
        }
        return result;
    }

    public double[][] getCc() {
        return cc;
    }

    public double[][] getRc() {
        return rc;
    }

    public double[][] getRcr() {
        return rcr;
    }

    public int numConstraints() {
        return this.constraintElements.size();
    }

    public int numRules() {
        return this.ruleElements.size();
    }

    public void build() throws Exception {
        this.matchForConstraints();
        this.matchForRules();
        this.matchForConstraintsAndRules();
    }

    public void print() {
        System.out.println("CC:");
        this.print(this.cc);
        System.out.println("RC:");
        this.print(this.rc);
        System.out.println("RCR:");
        this.print(this.rcr);
    }

    private String joinMMs(String pathSrcEcoreMMFile, String pathTrgEcoreMMFile, String pathTempFolder) throws ATLCoreException, IOException {
        String pathSrcUseMMFile = pathTempFolder + "/srcMM.xmi";
        Model.transformEcoreMMToUseMM(pathSrcEcoreMMFile, pathSrcUseMMFile);
        String pathTrgUseMMFile = pathTempFolder + "/trgMM.xmi";
        Model.transformEcoreMMToUseMM(pathTrgEcoreMMFile, pathTrgUseMMFile);
        String pathSrcTrgMMUseFile = pathTempFolder + "/joinedMM.xmi";
        AddPrefixMM.transform("file:/" + pathSrcUseMMFile, "src_");
        AddPrefixMM.transform("file:/" + pathTrgUseMMFile, "trg_");
        (new MergeUseMM()).mergeUSEMM(pathSrcUseMMFile, pathTrgUseMMFile, "file:/" + pathSrcTrgMMUseFile);
        Use2Text runner2 = new Use2Text();
        runner2.loadModels("file:/" + pathSrcTrgMMUseFile);
        runner2.doUse2Text(new NullProgressMonitor());
        String pathSrcTrgMMUseFile_TXT = pathTempFolder + "/joinedMM.use";
        FileOperations.FileCopyAndDeleteSource("temp/MM.use", pathSrcTrgMMUseFile_TXT);
        File f = new File(pathSrcUseMMFile);
        f.delete();
        f = new File(pathTrgUseMMFile);
        f.delete();
        f = new File(pathSrcTrgMMUseFile);
        f.delete();
        return pathSrcTrgMMUseFile_TXT;
    }

    public Resource getResourceFromPath(String path) {
        Registry reg = Registry.INSTANCE;
        reg.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        ResourceSet resSet = new ResourceSetImpl();
        resSet.setResourceFactoryRegistry(reg);
        URI uri = URI.createURI("file:/" + path);
        Resource metamodel = resSet.getResource(uri, true);
        return metamodel;
    }

    public void matchForConstraints() throws Exception {
        for (int i = 0; i < this.constraintElements.size(); ++i) {
            List<String> cList = this.constraintElements.get(i);

            for (int j = 0; j < this.ruleElements.size(); ++j) {
                List<String> rList = this.ruleElements.get(j);
                this.cc[i][j] = this.getRelativeRatingForConstraints(cList, rList, this.srcMMPath, this.trgMMPath);
            }
        }

    }

    public void matchForRules() throws Exception {
        for (int i = 0; i < this.constraintElements.size(); ++i) {
            List<String> cList = this.constraintElements.get(i);

            for (int j = 0; j < this.ruleElements.size(); ++j) {
                List<String> rList = this.ruleElements.get(j);
                this.rc[i][j] = this.getRelativeRatingForRules(i, j, cList, rList);
            }
        }

    }

    public void matchForConstraintsAndRules() throws Exception {
        for (int i = 0; i < this.constraintElements.size(); ++i) {
            List<String> cList = this.constraintElements.get(i);

            for (int j = 0; j < this.ruleElements.size(); ++j) {
                List<String> rList = this.ruleElements.get(j);
                this.rcr[i][j] = this.getRelativeRatingForConstraintsAndRules(i, j, cList, rList, this.srcMMPath,
                        this.trgMMPath);
            }
        }

    }

    private void print(double[][] table) {
        for (int i = 0; i < this.constraintElements.size(); ++i) {
            for (int j = 0; j < this.ruleElements.size(); ++j) {
                if (table[i][j] == 0.0D) {
                    System.out.print("\t");
                } else {
                    System.out.print(String.valueOf(table[i][j]).replace('.', ',') + "\t");
                }
            }

            System.out.println();
        }

    }

    private double getRelativeRatingForConstraints(List<String> cTypes, List<String> rTypes, String srcMMPath,
                                                   String trgMMPath) throws Exception {
        double r = this.getAbsoluteRating(cTypes, rTypes);
        r /= cTypes.size();
        return r;
    }

    private double getRelativeRatingForRules(int constr, int rule, List<String> cTypes, List<String> rTypes) throws Exception {
        double r = this.getAbsoluteRating(cTypes, rTypes);
        r /= rTypes.size();
        return r;
    }

    private double getRelativeRatingForConstraintsAndRules(int constr, int rule, List<String> cTypes,
                                                           List<String> rTypes, String srcMMPath, String trgMMPath) throws Exception {
        double r = this.getAbsoluteRating(cTypes, rTypes);
        r /= this.union(rTypes, cTypes).size();
        return r;
    }

    private double getAbsoluteRating(List<String> cElems, List<String> rElems) throws Exception {
        double r = 0.0D;
        List<String> cElemsAux = new LinkedList(cElems);
        List<String> rElemsAux = new LinkedList(rElems);
        Iterator var8 = cElems.iterator();

        while (var8.hasNext()) {
            String e = (String) var8.next();
            if (rElemsAux.contains(e)) {
                ++r;
                cElemsAux.remove(e);
                rElemsAux.remove(e);
            }
        }

        r += this.ratingForIndirectMatches(cElemsAux, rElemsAux);
        return r;
    }

    private double ratingForIndirectMatches(List<String> cElems, List<String> rElems) throws Exception {
        double r = 0.0D;
        List<String> cElemsAux = new LinkedList(cElems);
        List<String> rElemsAux = new LinkedList(rElems);
        Iterator var8 = cElems.iterator();

        while (true) {
            String e;
            List subSuperTypes;
            do {
                if (!var8.hasNext()) {
                    return r;
                }

                e = (String) var8.next();
                EClass clazz;
                if (this.isClass(e)) {
                    if (this.fromSRC(e)) {
                        clazz = this.getEClass4Name(e.substring("Src".length()), this.srcMMPath);
                        subSuperTypes = this.getAllSubAndSuperTypes(clazz, this.srcMMPath);
                    } else {
                        clazz = this.getEClass4Name(e.substring("Trg".length()), this.trgMMPath);
                        subSuperTypes = this.getAllSubAndSuperTypes(clazz, this.trgMMPath);
                    }
                } else if (this.fromSRC(e)) {
                    String className = e.substring("Src".length(), e.indexOf("."));
                    clazz = this.getEClass4Name(className, this.srcMMPath);
                    subSuperTypes = this.getAllSubAndSuperTypes(clazz, this.srcMMPath);
                } else {
                    clazz = this.getEClass4Name(e.substring("Trg".length(), e.indexOf(".")), this.trgMMPath);
                    subSuperTypes = this.getAllSubAndSuperTypes(clazz, this.trgMMPath);
                }
            } while (subSuperTypes == null);

            for (int i = 0; i < subSuperTypes.size(); ++i) {
                if (this.isClass(e) && this.fromSRC(e)) {
                    r += this.aux(cElemsAux, rElemsAux, subSuperTypes, "Src", "");
                } else if (this.isClass(e) && !this.fromSRC(e)) {
                    r += this.aux(cElemsAux, rElemsAux, subSuperTypes, "Trg", "");
                } else {
                    String featureName;
                    if (!this.isClass(e) && this.fromSRC(e)) {
                        featureName = e.substring(e.indexOf(".") + 1);
                        r += this.aux(cElemsAux, rElemsAux, subSuperTypes, "Src", "." + featureName);
                    } else {
                        featureName = e.substring(e.indexOf(".") + 1);
                        r += this.aux(cElemsAux, rElemsAux, subSuperTypes, "Trg", "." + featureName);
                    }
                }
            }
        }
    }

    private double aux(List<String> cElemsAux, List<String> rElemsAux, List<EClass> subSuperTypes, String prefix,
                       String featureName) {
        double w = 0.0D;
        EClass type = null;
        int t = 0;

        boolean found1;
        for (found1 = false; !found1 && t < subSuperTypes.size(); ++t) {
            if (cElemsAux.contains(prefix + subSuperTypes.get(t).getName() + featureName)) {
                found1 = true;
                type = subSuperTypes.get(t);
            }
        }

        if (found1) {
            int k = 0;

            for (boolean found = false; !found && k < subSuperTypes.size(); ++k) {
                String elem = prefix + subSuperTypes.get(k).getName() + featureName;
                if (rElemsAux.contains(elem)) {
                    found = true;
                    w = this.weight(type, subSuperTypes.get(k));
                    cElemsAux.remove(prefix + type.getName() + featureName);
                    rElemsAux.remove(prefix + subSuperTypes.get(k).getName() + featureName);
                }
            }
        }

        return w;
    }

    private double weight(EClass c1, EClass c2) {
        double w = 0.0D;
        int numFeaturesC1 = c1.getEAllAttributes().size();
        int numFeaturesC2 = c2.getEAllAttributes().size();
        if (numFeaturesC1 > numFeaturesC2) {
            w = (double) numFeaturesC2 / (double) numFeaturesC1;
        } else if (numFeaturesC1 < numFeaturesC2) {
            w = (double) numFeaturesC1 / (double) numFeaturesC2;
        } else if (numFeaturesC1 == 0) {
            w = 0.0D;
        } else {
            w = 1.0D;
        }

        return w;
    }

    private List<EClass> getAllSubAndSuperTypes(EClass clazz, String mmPath) {
        List<EClass> classes = new LinkedList();
        if (clazz == null) {
            return null;
        } else {
            List<EClass> subclClasses = this.putInOrder(this.getAllSubClasses(clazz, mmPath));
            classes.addAll(subclClasses);
            classes.add(clazz);
            List<EClass> superClasses = clazz.getEAllSuperTypes();
            classes.addAll(this.reverse(superClasses));
            return classes;
        }
    }

    private List<EClass> reverse(List<EClass> superClasses) {
        List<EClass> newList = new LinkedList();

        for (int i = superClasses.size() - 1; i >= 0; --i) {
            newList.add(superClasses.get(i));
        }

        return newList;
    }

    private boolean isClass(String e) {
        return !e.contains(".");
    }

    private boolean fromSRC(String e) {
        return e.startsWith("Src");
    }

    private Set<String> union(List<String> types, List<String> types2) {
        Set<String> set = new HashSet();
        Iterator var5 = types.iterator();

        String s;
        while (var5.hasNext()) {
            s = (String) var5.next();
            set.add(s);
        }

        var5 = types2.iterator();

        while (var5.hasNext()) {
            s = (String) var5.next();
            set.add(s);
        }

        return set;
    }

    public EClass getEClass4Name(String className, String metamodelPath) throws Exception {
        Resource res = this.getResourceFromPath(metamodelPath);
        TreeIterator it = res.getAllContents();

        while (it.hasNext()) {
            EObject object = (EObject) it.next();
            if (object instanceof EClass) {
                EClass clazz = (EClass) object;
                if (clazz.getName().equals(className)) {
                    return clazz;
                }
            } else if (object instanceof EEnum) {
                return null;
            }
        }

        throw new Exception(className + " not found in the metamodel " + metamodelPath);
    }

    public List<EClass> getAllSubClasses(EClass clazz, String metamodelPath) {
        Resource res = this.getResourceFromPath(metamodelPath);
        List<EClass> subclasses = new LinkedList();
        TreeIterator it = res.getAllContents();

        while (it.hasNext()) {
            EObject object = (EObject) it.next();
            if (object instanceof EClass) {
                EClass cl = (EClass) object;
                if (this.names(cl.getEAllSuperTypes()).contains(clazz.getName())) {
                    subclasses.add(cl);
                }
            }
        }

        return subclasses;
    }

    private List<EClass> putInOrder(List<EClass> subclasses) {
        List<EClass> orderedSubClasses = new LinkedList();
        if (subclasses.size() != 0) {
            EClass c = subclasses.get(0);
            int numSuperClasses = subclasses.get(0).getEAllSuperTypes().size();
            int numIterationes = subclasses.size();

            for (int k = 0; k < numIterationes; ++k) {
                int numIt2 = subclasses.size();

                for (int it2 = 0; it2 < numIt2; ++it2) {
                    EClass e = subclasses.get(it2);
                    if (e.getEAllSuperTypes().size() > numSuperClasses) {
                        c = e;
                        numSuperClasses = e.getEAllSuperTypes().size();
                    }
                }

                orderedSubClasses.add(c);
                subclasses.remove(c);
                if (subclasses.size() > 0) {
                    numSuperClasses = 0;
                    c = subclasses.get(0);
                }
            }
        }

        return orderedSubClasses;
    }

    private List<String> names(EList<EClass> superTypes) {
        List<String> l = new LinkedList();
        if (superTypes != null && !superTypes.isEmpty()) {
            Iterator var4 = superTypes.iterator();

            while (var4.hasNext()) {
                EClass c = (EClass) var4.next();
                l.add(c.getName());
            }
        }

        return l;
    }

    public void generateCSV(String path) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        writer.print("CC\n");

        int i;
        int j;
        for (i = 0; i < this.constraintElements.size(); ++i) {
            for (j = 0; j < this.ruleElements.size(); ++j) {
                if (this.cc[i][j] >= this.threshold && this.rcr[i][j] >= this.threshold) {
                    writer.print(String.valueOf(this.cc[i][j]).replace(".", ",") + ";");
                } else {
                    writer.print("0;");
                }
            }

            writer.print("\n");
        }

        writer.print("\n");
        writer.print("\n");
        writer.print("RC\n");

        for (i = 0; i < this.constraintElements.size(); ++i) {
            for (j = 0; j < this.ruleElements.size(); ++j) {
                if (this.rc[i][j] >= this.threshold && this.rcr[i][j] >= this.threshold) {
                    writer.print(String.valueOf(this.rc[i][j]).replace(".", ",") + ";");
                } else {
                    writer.print("0;");
                }
            }

            writer.print("\n");
        }

        writer.print("\n");
        writer.print("\n");
        writer.print("RCR\n");

        for (i = 0; i < this.constraintElements.size(); ++i) {
            for (j = 0; j < this.ruleElements.size(); ++j) {
                if (this.rcr[i][j] >= this.threshold) {
                    writer.print(String.valueOf(this.rcr[i][j]).replace(".", ",") + ";");
                } else {
                    writer.print("0;");
                }
            }

            writer.print("\n");
        }

        writer.close();
    }

    public int[][] generateReport() {
        int[][] report = new int[this.constraintElements.size()][this.ruleElements.size()];

        for (int i = 0; i < this.constraintElements.size(); ++i) {
            int[] array = this.generateReportForConstraint(i);
            report[i] = array;
        }

        return report;
    }

    public void printReport(int[][] report) {
        for (int i = 0; i < this.constraintElements.size(); ++i) {
            System.out.print("C" + (i + 1) + ": ");

            for (int j = 0; j < this.ruleElements.size(); ++j) {
                if (report[i][j] != -1) {
                    if (j != report[0].length - 1 && report[i][j + 1] != -1) {
                        System.out.print("R" + (report[i][j] + 1) + ", ");
                    } else {
                        System.out.print("R" + (report[i][j] + 1));
                    }
                }
            }

            System.out.print("\n");
        }

    }

    private int[] generateReportForConstraint(int i) {
        double[] rowCopy = this.copy(this.cc[i]);
        int[] reportConstraintI = new int[rowCopy.length];

        for (int j = 0; j < rowCopy.length; ++j) {
            int index = this.getIndexOfHigherValue(rowCopy, i);
            reportConstraintI[j] = index;
            if (index != -1) {
                rowCopy[index] = 0.0D;
            }
        }

        return reportConstraintI;
    }

    private double[] copy(double[] ds) {
        double[] copy = new double[ds.length];

        for (int i = 0; i < ds.length; ++i) {
            copy[i] = ds[i];
        }

        return copy;
    }

    private int getIndexOfHigherValue(double[] rowCopy, int rowIndex) {
        double higherValue = 0.0D;
        int higherIndex = 0;
        if (this.allZero(rowCopy)) {
            higherIndex = -1;
        } else {
            for (int i = 0; i < this.ruleElements.size(); ++i) {
                if (rowCopy[i] > higherValue) {
                    higherValue = rowCopy[i];
                    higherIndex = i;
                } else if (rowCopy[i] != 0.0D && rowCopy[i] == higherValue && this.rcr[rowIndex][i] > this.rcr[rowIndex][higherIndex]) {
                    higherValue = this.cc[rowIndex][i];
                    higherIndex = i;
                }
            }
        }

        return higherIndex;
    }

    private boolean allZero(double[] rowCopy) {
        boolean allZero = true;

        for (int i = 0; i < rowCopy.length && allZero; ++i) {
            if (rowCopy[i] != 0.0D) {
                allZero = false;
            }
        }

        return allZero;
    }

    public void applyThreashold(double threshold) {
        if (!(threshold < 0.0D) && !(threshold > 1.0D)) {
            for (int i = 0; i < this.constraintElements.size(); ++i) {
                for (int j = 0; j < this.ruleElements.size(); ++j) {
                    if (this.rcr[i][j] < threshold) {
                        this.rcr[i][j] = 0.0D;
                        this.cc[i][j] = 0.0D;
                        this.rc[i][j] = 0.0D;
                    }
                }
            }
        } else {
            System.out.println("Invalid threshold. It must be a value between 0 and 1 both inclusive.");
        }

    }
}
