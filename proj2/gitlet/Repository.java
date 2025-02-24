package gitlet;


import java.awt.image.AreaAveragingScaleFilter;
import java.io.File;
import java.sql.Array;
import java.util.*;

import static gitlet.Utils.*;
import static java.lang.System.exit;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  Since there is only one repository, all method and attr are static
 *
 *  Repository has to do the following:
 *  1. manage the dir of .gitlet, hold all dir path needed to be used
 *      The dir can be designed as:
 *      .gitlet
 *      |-- objects
 *      |-- refs
 *          |-- heads
 *  @author TODO
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The objects dir */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /** The refs dir */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    /** The Head dir */
    public static final File HEAD_DIR = join(REFS_DIR, "heads");
    /** The head path which represent the current branch */
    public static final File BRANCH_FILE = join(REFS_DIR, "branch");
    public static final File ADD_DIR = join(REFS_DIR, "addfile");
    public static final File REMOVE_DIR = join(REFS_DIR, "rmfile");

    public static boolean isInit() {
        return GITLET_DIR.exists()
                && GITLET_DIR.isDirectory();
    }

    public static void checkInit() {
        if(!isInit()) {
            message("Not in an initialized Gitlet directory.");
            exit(0);
        }
    }
    /** To Do:
     *  1. check whether the gitlet is initialized   | ok
     *  2. initialize the dir   | ok
     *  3. initialize the master commit |ok
     */
    public static void initRepository() {
        if(isInit()) {
            message("A Gitlet version-control system already exists in the current directory.");
            exit(0);
        }
        setupPersistence();
        Commit c = Commit.initCommit();
        String commitID = c.getCommitID();
        c.saveCommit(commitID);
        saveHead("master", commitID);
    }

    public static void setupPersistence() {
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        REFS_DIR.mkdir();
        HEAD_DIR.mkdir();
        writeContents(BRANCH_FILE, "master");
        ADD_DIR.mkdir();
        REMOVE_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
    }

    /* METHODS ABOUT BRANCHES AND HEADS */

    /** Record head and the commit it refers to
     * The head filename is the same as the branch name
     * The content is the commitID
     * */
    public static void saveHead(String headName, String commitID) {
        File headPath = join(HEAD_DIR, headName);
        writeContents(headPath, commitID);
    }

    /** get the current branch name
     * test: pass
     * @return
     */
    public static String getCurrentBranchName() {
        return readContentsAsString(BRANCH_FILE);
    }

    /** set current branch name
     * test: pass
     * @param branchName
     */
    public static void setCurrentBranch(String branchName) {
        writeContents(BRANCH_FILE, branchName);
    }

    /** Get commit of given branch name
     * test:  pass
     * @param branchName
     * @return Commit
     */
    public static Commit getBranchCommit(String branchName) {
        File branchPath = join(HEAD_DIR, branchName);
        String commitID = readContentsAsString(branchPath);
        return Commit.getCommitFromID(commitID);
    }

    /** Get current commit
     * test: pass
     * @return
     */
    public static Commit getCurrentCommit() {
        return getBranchCommit(getCurrentBranchName());
    }

    /** Implementation of the add command
     *
     * @param filename
     */
    public static void addStage(String filename) {
        checkInit();
        File file = join(CWD, filename);

        /* file doesn't exist */
        if(!(file.exists() && file.isFile())) {
            message("File does not exist.");
            exit(0);
        }

        String fileContent = readContentsAsString(file);
        TreeMap<String, String> map = getCurrentCommit().getBlobMap();
        List<String> rmFiles = plainFilenamesIn(REMOVE_DIR);

        /* file is tracked */
        if(map.containsKey(filename)) {
            // is it possible to just compare the hash value rather than
            // check the content?
            String oldHashID = map.get(filename);
            String newHashID = sha1(fileContent);
            if(oldHashID.equals(newHashID)) {
                Stage.removeFile(filename, ADD_DIR);
                Stage.removeFile(filename, REMOVE_DIR);
                return;
            }
        }

        /* file is not tracked */
        Stage.stageFile(filename, fileContent);
    }

    /** Implementation of rm command
     *
     * @param filename
     */
    public static void removeStage(String filename) {
        checkInit();

        /* Check whether the file is staged or in the commit */
        Commit c = getCurrentCommit();
        if(!c.hasFilename(filename) && !Stage.hasFile(filename, ADD_DIR)) {
            System.out.println("No reason to remove the file.");
            exit(0);
        }

        /* Remove the file from working dir and remove it from add stage */
        Stage.removeFile(filename, ADD_DIR);
        join(CWD, filename).delete();
        /* File is recorded in commit */
        if(c.hasFilename(filename)) {
            Stage.addEmptyFile(filename, REMOVE_DIR);
        }
    }

    /** Implementation of commit command
     *
     * @param message
     */
    public static void commit(String message) {
        /* Aborted case */
        if(Stage.isEmpty()) {
            message("No changes added to the commit.");
            exit(0);
        }
        /* Add changes to new commit */
        Commit parent = getCurrentCommit();
        Commit newCommit = new Commit(parent, new Date(), message);
        List<String> addFiles = plainFilenamesIn(ADD_DIR);
        List<String> removeFiles = plainFilenamesIn(REMOVE_DIR);
        for(String i : removeFiles) {
            newCommit.removeFile(i);
        }
        for(String filename : addFiles) {
            /* Create blob object */
            File file = join(ADD_DIR, filename);
            String content = readContentsAsString(file);
            Blob b = new Blob(content);
            String bID = b.getId();
            b.saveBlob();
            /* Add file to commit */
            newCommit.addFile(filename, bID);
        }
        String commitID = newCommit.getCommitID();
        newCommit.saveCommit(commitID);

        /* Change branch and head */
        String branch = getCurrentBranchName();
        writeContents(join(HEAD_DIR, branch), commitID);
        Stage.clearStage();
    }

    /** Implementation of log command
     *
     */
    public static void log() {
        Commit c = getCurrentCommit();
        String branchName = getCurrentBranchName();
        File headFile = join(HEAD_DIR, branchName);
        String id = readContentsAsString(headFile);
        while(c != null) {
            c.printLog(id);
            id = c.getFirstParentID();
            c = Commit.getCommitFromID(id);
        }
    }

    /** Implementation of global-log command
     *  TODO: need to check this function after branch/merge command is completed
     */
    public static void global_log() {
        List<String> files  = plainFilenamesIn(COMMITS_DIR);
        for(String id : files) {
            Commit.getCommitFromID(id).printLog(id);
        }
    }

    /** Implementation of find command
     *
     * @param message
     */
    public static void find(String message) {
        List<String> files = plainFilenamesIn(COMMITS_DIR);
        boolean finded = false;
        for(String id : files) {
            var c = Commit.getCommitFromID(id);
            if(c.getMessage().equals(message)) {
                finded = true;
                message(id);
            }
        }
        if(!finded) {
            message("Found no commit with that message.");
        }
    }

    /** Implementation of status command
     *  TODO: not completed yet
     */
    public static void status() {
        /* Print branches */
        message("=== Branches ===");
        List<String> heads = plainFilenamesIn(HEAD_DIR);
        String currentBranch = getCurrentBranchName();
        for(String branch : heads) {
            if(branch.equals(currentBranch)) {
                System.out.print("*");
            }
            message(branch);
        }
        System.out.println();
        /* Check file in working dir */
        List<String> files = plainFilenamesIn(CWD);
        List<String> addFiles = plainFilenamesIn(ADD_DIR);
        List<String> removeFiles = plainFilenamesIn(REMOVE_DIR);
        /* warning:
        In this case, we don't save the commit anymore, so it is valid not to
        create a new BlobTree. */
        TreeMap<String, String> commitTree = getCurrentCommit().getBlobMap();
        Set<String> commitFiles = commitTree.keySet();

        List<String> stageFiles = new ArrayList<>();
        List<String> rmFiles = new ArrayList<>();
        List<String> modifiedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        List<String> utFiles = new ArrayList<>();
        /* Check the file in CWD */
        for(String f : files) {
            String content = readContentsAsString(join(CWD, f));
            if(addFiles.contains(f)) {
                String addContent = Stage.getContentFromAdd(f);
                if(addContent.equals(content)) {stageFiles.add(f);}
                else {modifiedFiles.add(f);}
                addFiles.remove(f);
                commitFiles.remove(f);
            } else if(removeFiles.contains(f)) {
                utFiles.add(f);
                removeFiles.remove(f);
                commitFiles.remove(f);
            } else if(commitTree.containsKey(f)) {
                String blobID = commitTree.get(f);
                String blobContent = Blob.readContentFromID(blobID);
                if(!blobContent.equals(content)) {
                    modifiedFiles.add(f);
                }
                commitFiles.remove(f);
            }
            files.remove(f);
        }

        for(String f : addFiles) {
            modifiedFiles.add(f);
            addFiles.remove(f);
        }

        for(String f : removeFiles) {
            if(!files.contains(f)) {
                rmFiles.add(f);
            }
            removeFiles.remove(f);
        }

        for(String f : commitFiles) {
            if(files.contains(f)) {
                String content = readContentsAsString(join(CWD, f));
                String blobID = commitTree.get(f);
                String blobContent = Blob.readContentFromID(blobID);
                if(!blobContent.equals(content)) {
                    utFiles.add(f);
                }
            }
            else {
                deletedFiles.add(f);
            }
        }
        /* Print status */
        message("=== Staged Files ===");
        printStatus(stageFiles);
        message("=== Removed Files ===");
        printStatus(rmFiles);
        message("=== Modifications Not Staged For Commit ===");
        printStatus(modifiedFiles);
        message("=== Untracked Files ===");
        printStatus(utFiles);
    }

    public static void checkout() {

    }
}
