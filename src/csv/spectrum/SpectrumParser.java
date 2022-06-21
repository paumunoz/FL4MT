package csv.spectrum;

import csv.util.CSVUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SpectrumParser {
    public static int getHeadersRow(List<String[]> rows, String match) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[0].startsWith(match)) {
                return i;
            }
        }
        return -1;
    }

    public static int getNumberOfRules(List<String[]> rows, int start) {
        for (int i = start; i < rows.size(); i++) {
            if (rows.get(i)[1].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    protected static int[] getCoordinates(List<String[]> rows, String match) {
        for (int i = 0; i < rows.size(); i++) {
            for (int j = 0; j < rows.get(i).length; j++) {
                if (rows.get(i)[j].equals(match)) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{-1, -1};
    }

    protected static int getColumnCoordinate(List<String[]> rows, String match, int row) {
        for (int i = 0; i < rows.get(row).length; i++) {
            if (rows.get(row)[i].equals(match)) {
                return i;
            }
        }
        return -1;
    }

    protected static int getRowCoordinate(List<String[]> rows, String match, int column) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[column].equals(match)) {
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> getSelectedColumnsByHeader(List<String[]> rows, String[] headers) {
        List<Integer> result = new ArrayList<>();
        int headersRow = getHeadersRow(rows, headers[0]);
        for (String header : headers) {
            boolean found = false;
            result.add(getColumnCoordinate(rows, header, headersRow));
        }
        return result;
    }

    public static int getConstraintId(String fullName, String filename){
        int index = filename.indexOf(fullName);
        return Integer.parseInt(filename.substring(index+fullName.length(), filename.length()-(".csv").length()));
    }

    public static double getMatchingTableValue(int constraintId, int ruleId, String metric, List<String[]> rows){
        int[] coordinates = getCoordinates(rows, metric);
        int column = ruleId + coordinates[1];
        int row = constraintId + coordinates[0];
        return Double.parseDouble(rows.get(row)[column]);
    }

    public static String[] getRow(String[]... strings){
        List<String> result = new ArrayList<>();
        for(String[] element : strings){
            result.addAll(Arrays.asList(element));
        }
        return result.toArray(new String[0]);
    }

    public static List<String> getRules(String footprintsPath){
        List<String> result = new LinkedList<>();
        List<String[]> footprint = CSVUtil.readAll(footprintsPath, ',');
        for(String[] row : footprint.subList(1, footprint.size())){
            result.add(row[0]);
        }
        return result;
    }

}
