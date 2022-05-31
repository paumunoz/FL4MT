package csv.util;

import com.opencsv.*;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CSVUtil {

    // LISTING FILES METHODS
    protected static FilenameFilter getFilterByExtension(String extension){
        return (dir, name) -> name.endsWith(extension);
    }

    public static File[] listFilesInDirectory(String path, String extension){
        File f = new File(path);
        return f.listFiles(getFilterByExtension(extension));
    }

    public static String[] listFileNamesInDirectory(String path, String extension){
        File f = new File(path);
        return f.list(getFilterByExtension(extension));
    }

    // READER METHODS
    public static List<String[]> readAll(Reader reader, char separator) {
        CSVParser parser =
                new CSVParserBuilder()
                        .withSeparator(separator)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreQuotations(true)
                        .build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();
        List<String[]> list = new ArrayList<>();
        try {
            list = csvReader.readAll();
            reader.close();
            csvReader.close();
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String[]> readAll(String path, char separator) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return CSVUtil.readAll(reader, separator);
    }

    // WRITER METHODS
    public static void writeAll(List<String[]> stringArray, String path) throws Exception {
        FileWriter fw = new FileWriter(path);
        ICSVWriter writer = new CSVWriterBuilder(fw)
                .withSeparator(',')
                .build();
        writer.writeAll(stringArray);
        writer.close();
    }
}
