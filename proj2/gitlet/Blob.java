package gitlet;

import java.io.File;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

public class Blob {
    private String id;
    private String content;
    private File filePath;

    /** Constructor of Blob,
     * initialize the id, filePath from the content */
    public Blob(String content) {
        this.content = content;
        this.id = sha1(content);
        this.filePath = join(BLOBS_DIR, id);
    }

    /** Save blob in the .gitlet/object dir
     * To Do:
     * 1. check whether the blob exist
     * 2. save the blob
     * */
    public void saveBlob() {
        if(!filePath.exists()) {
            writeContents(filePath, content);
        }
    }

    /** Read the content of a blob from id
     * To Do:
     * 1. check whether the file exist
     * 2. return the content
     * */
    public static String readContentFromID(String id) {
        File path = join(BLOBS_DIR, id);
        String content = null;
        if(path.exists()) {
            content = readContentsAsString(path);
        }
        return content;
    }

    public String getId() {
        return id;
    }
}
