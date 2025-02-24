package gitlet;

import java.io.File;
import java.util.List;

import static gitlet.Utils.*;
import static gitlet.Repository.ADD_DIR;
import static gitlet.Repository.REMOVE_DIR;

/** This class manage all the files in Stage area.
 *
 */
public class Stage {

    /** This method add the given file to the staged area
     * warning: this method doesn't check whether the file should be added
     *
     * @param filename
     * @param content
     */
    public static void stageFile(String filename, String content) {
        File addPath = join(ADD_DIR, filename);
        writeContents(addPath, content);
        File removePath = join(REMOVE_DIR, filename);
        removePath.delete();
    }

    /** Add an empty file into dir
     *
     * @param filename
     * @param dir
     */
    public static void addEmptyFile(String filename, File dir) {
        createEmptyFile(join(dir,filename));
    }

    /** Remove the given file from the given dir
     *
     * @param filename
     * @param dir
     */
    public static void removeFile(String filename, File dir) {
        join(dir, filename).delete();
    }

    /** Check whether dir contains the given file
     *
     * @param filename
     * @param dir
     * @return
     */
    public static boolean hasFile(String filename, File dir) {
        var fileList = plainFilenamesIn(dir);
        return fileList.contains(filename);
    }

    public static boolean isEmpty() {
        return plainFilenamesIn(ADD_DIR).isEmpty() &&
                plainFilenamesIn(REMOVE_DIR).isEmpty();
    }

    public static void clearStage() {
        List<String> addList = plainFilenamesIn(ADD_DIR);
        List<String> rmList = plainFilenamesIn(REMOVE_DIR);
        for(String i: addList) {
            join(ADD_DIR, i).delete();
        }
        for(String i: rmList) {
            join(REMOVE_DIR, i).delete();
        }
    }

    /** get file content from name in the ADD_DIR
     *
     * @param filename
     * @return
     */
    public static String getContentFromAdd(String filename) {
        if(!hasFile(filename, ADD_DIR)) {
            message("file doesn't exist in the addfile dir");
            System.exit(0);
        }
        File f = join(ADD_DIR, filename);
        return readContentsAsString(f);
    }
}
