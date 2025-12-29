package Services;

import java.io.File;
import java.util.ArrayList;

/**
 * Utility class providing general-purpose helper methods for file operations.
 */
public class Utils {
    /**
     * Returns a list of filenames in the specified directory that contain the given string.
     *
     * <p>This method searches only in the top-level of the provided path (non-recursive).
     *
     * @param filename the string to match within filenames
     * @param path     the directory path to search
     * @return an ArrayList of matching filenames
     */
    public static ArrayList<String> getFilesByNameAndPath(String filename, String path) {

        ArrayList<String> result = new ArrayList<>();
        //int pos = filename.indexOf('-');
        //int pos = filename.indexOf('.');
        //String nameE2ETestCase = filename.substring(0,pos);
        File[] files = new File(path).listFiles();
        for(File file: files) {
//            if(file.getName().contains(nameE2ETestCase)) {
            if(file.getName().contains(filename)) {
                result.add(file.getName());
            }
        }
        return  result;
    }

}
