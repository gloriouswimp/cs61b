package gitlet;

// TODO: any imports you need here

import jh61b.junit.In;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static java.lang.System.exit;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The stored Map of this commit,
     * key: filename value: blobID **/
    private TreeMap<String, String> blobMap;
    /** Date should only be initialized by constructor */
    private Date date;
    /**
     *  The parentID of this commit. By default, secondParent should be null unless
     * this Commit is created by merge command
     */
    private String firstParentID;
    private String secondParentID;

    public Commit() {
        message = null;
        // 存疑，HashMap 的空对象应该怎么做
        blobMap = new TreeMap<>();
        date = new Date(0);
        firstParentID = null;
        secondParentID = null;
    }

    /** Copy constructor casting on the parent
     * TODO: check whether TreeMap should be copy
     * @param parent
     * @param d
     * @param m
     */
    public Commit(Commit parent, Date d, String m) {
        blobMap = new TreeMap<>(parent.blobMap);
        message = m;
        date = d;
        firstParentID = parent.getCommitID();
        secondParentID = null;
    }

    public static Commit initCommit() {
        Commit newCommit = new Commit();
        newCommit.setMessage("initial commit");
        return newCommit;
    }

    /** Only used when everything is done except for storing the commit */
    public String getCommitID() {
        var bytes = serialize(this);
        return sha1(bytes);
    }

    /** save a commit as an object with the given commitID
     * commitID should be created by getCommitID().
     * This method forces user to use and save getCommitID()
     */
    public void saveCommit(String commitID) {
        File saveDir = join(COMMITS_DIR, commitID.substring(0,2));
        File savePath = join(saveDir, commitID);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }
        writeObject(savePath, this);
    }

    /** Check whether the commit is created from merge command
     *
     * @return
     */
    public boolean isFromMerge() {
        return secondParentID != null;
    }
    /** Get Commit object with commitID
     *  If commitID is null, return null
     *  If the given ID doesn't exist, exit program
     * @param commitID
     * @return Commit object
     */
    public static Commit getCommitFromID(String commitID) {
        if(commitID == null){
            return null;
        }
        File commitDir = join(COMMITS_DIR, commitID.substring(0,2));
        if(!commitDir.exists()) {
            message("No commit with that id exists.");
            exit(0);
        }
        else {
            List<String> commitIDList = plainFilenamesIn(commitDir);
            int len = commitID.length();
            for (String id : commitIDList) {
                if (id.substring(0, len).equals(commitID)) {
                    File commitPath = join(commitDir, id);
                    return readObject(commitPath, Commit.class);
                }
            }
            message("No commit with that id exists.");
            exit(0);
        }
        return null;
    }

    public void printLog(String id) {
        message("===");
        message("commit " + id);
        if(isFromMerge()) {
            System.out.println("Merge " + getFirstParentID().substring(0, 7)
                    + " " + getSecondParentID().substring(0, 7));
        }
        message("Date: "+ getFormatDate());
        message(getMessage());
        System.out.println();
    }

    public static HashMap<String, Integer> getInheritedMap(String commitID) {
        class CommitNode {
            public Commit commit;
            public int height;

            public CommitNode(Commit c, int h) {
                this.commit = c;
                this.height = h;
            }
        }

        Deque<CommitNode> deque =  new ArrayDeque<>();
        HashMap<String, Integer> map = new HashMap<>();
        CommitNode first = new CommitNode(getCommitFromID(commitID), 0);
        deque.add(first);
        map.put(commitID, 0);


        while (!deque.isEmpty()) {
            CommitNode n = deque.poll();
            Commit c = n.commit;
            int height = n.height;
            if (c.firstParentID != null) {
                Commit parent = getCommitFromID(c.firstParentID);
                CommitNode parentNode = new CommitNode(parent, height + 1);
                deque.add(parentNode);
                /* check whether the parent visited before */
                if (map.containsKey(c.firstParentID) &&
                        map.get(c.firstParentID) < height + 1) {
                    // nothing to do
                }
                else {
                    map.put(c.firstParentID, height + 1);
                }
            }
            if (c.secondParentID != null) {
                Commit parent = getCommitFromID(c.secondParentID);
                CommitNode parentNode = new CommitNode(parent, height + 1);
                deque.add(parentNode);
                /* check whether the parent visited before */
                if (map.containsKey(c.secondParentID) &&
                        map.get(c.secondParentID) < height + 1) {
                    // nothing to do
                }
                else {
                    map.put(c.secondParentID, height + 1);
                }
            }
        }
        return map;
    }

    /* FILE OPERATIONS */

    /** Add a pair of filename and blobID to the blobMap
     * warning: it should be used after creating the blob
     * @param filename
     * @param blobID
     */
    public void addFile(String filename, String blobID) {
        blobMap.put(filename, blobID);
    }

    public void removeFile(String filename) {
        blobMap.remove(filename);
    }

    /** This method only check if the commit has the filename
     *
     * @param filename
     * @return
     */
    public boolean hasFilename(String filename) {
        return blobMap.containsKey(filename);
    }


    /* GETTERS AND SETTERS */

    public TreeMap<String, String> getBlobMap() { return blobMap; }

    public String getMessage() { return message;}

    public String getFirstParentID() { return firstParentID; }

    public String getSecondParentID() { return secondParentID; }

    public String getFormatDate() {
        Formatter formatter = new Formatter(Locale.US);
        formatter.format("%ta %tb %td %tT %tY %tz", date, date, date, date, date, date);
        return formatter.toString();
    }

    public void setMessage(String message) { this.message = message; }

    public void setSecondParentID(String id) {
        this.secondParentID = id;
    }

    /** Get content of the given file */
    public String getContentFromName(String filename) {
        String blobID = blobMap.get(filename);
        return Blob.readContentFromID(blobID);
    }

    public Commit getFirstParentCommit() {
        if (firstParentID == null) {
            return null;
        }
        return getCommitFromID(firstParentID);
    }

    public Commit getSecondParentCommit() {
        if (secondParentID == null) {
            return null;
        }
        return getCommitFromID(secondParentID);
    }
}
