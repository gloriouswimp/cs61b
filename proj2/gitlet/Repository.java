package gitlet;


import java.awt.image.AreaAveragingScaleFilter;
import java.io.File;
import java.nio.file.Files;
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

    private static List<String> getBranchList() {
        return plainFilenamesIn(HEAD_DIR);
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

    public static String getBranchCommitID(String branchName) {
        File path = join(HEAD_DIR, branchName);
        return readContentsAsString(path);
    }

    public static void setCurrentBranchCommitID(String commitID) {
        File path = join(HEAD_DIR, getCurrentBranchName());
        writeContents(path, commitID);
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

    /* METHODS ABOUT FILES STATUS */
    private static List<String> getUntrackedList() {
        List<String> list = new LinkedList<>();
        List<String> filesInCWD = plainFilenamesIn(CWD);
        Set<String> filesInCommit = getCurrentCommit().getBlobMap().keySet();

        for (String filename : filesInCWD) {
            if (!Stage.hasFile(filename, ADD_DIR)) {
                if (Stage.hasFile(filename, REMOVE_DIR) || !filesInCommit.contains(filename)) {
                    list.add(filename);
                }
            }
        }
        return list;
    }

    private static List<String> getTrackedList() {
        List<String> list = new LinkedList<>();
        List<String> filesInCWD = plainFilenamesIn(CWD);
        Set<String> filesInCommit = getCurrentCommit().getBlobMap().keySet();

        for (String filename : filesInCWD) {
            if (Stage.hasFile(filename, ADD_DIR) || filesInCommit.contains(filename)) {
                list.add(filename);
            }
        }
        return list;
    }

    /* IMPLEMENTATION OF COMMAND */

    /** Implementation of the add command
     *
     * @param filename
     */
    public static void addStage(String filename) {
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
        File[] dirs = COMMITS_DIR.listFiles(File::isDirectory);

        for(int i=0; i<dirs.length; i++) {
            List<String> files = plainFilenamesIn(dirs[i]);
            for (String commitID : files) {
                Commit.getCommitFromID(commitID).printLog(commitID);
            }
        }
    }

    /** Implementation of find command
     *
     * @param message
     */
    public static void find(String message) {
        File[] dirs = COMMITS_DIR.listFiles(File::isDirectory);
        boolean finded = false;
        for (int i=0; i<dirs.length; i++) {
            List<String> files = plainFilenamesIn(dirs[i]);
            for (String commitID : files) {
                Commit c = Commit.getCommitFromID(commitID);
                if (c.getMessage().equals(message)) {
                    finded = true;
                    message(commitID);
                }
            }
        }
        if(!finded) {
            message("Found no commit with that message.");
        }
    }

    /** helper function for status
     *
     * @param heading
     * @param list
     */
    private static void printStatus(String heading, List<String> list, String append) {
        message(heading);
        for (String s : list) {
            message(s );
        }
        System.out.println();
    }
    /** Implementation of status command
     *  TODO: not test yet
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
        List<String> filesInCWD = plainFilenamesIn(CWD);
        List<String> filesInADD = plainFilenamesIn(ADD_DIR);
        List<String> filesInRemove = plainFilenamesIn(REMOVE_DIR);
        /* warning:
        In this case, we don't save the commit anymore, so it is valid not to
        create a new BlobTree. */
        Commit currentCommit = getCurrentCommit();
        TreeMap<String, String> commitTree = currentCommit.getBlobMap();
        Set<String> commitFiles = commitTree.keySet();

        List<String> stageFiles = new ArrayList<>();
        List<String> removeFiles = new ArrayList<>();
        List<String> modifiedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        List<String> untrackedFiles = new ArrayList<>();
        /* Check the file in CWD */
        for (String filename : filesInCWD) {
            if (filesInADD.contains(filename)) {
                File filePath = join(CWD, filename);
                String content = Stage.getContentFromAdd(filename);
                if (content.equals(readContentsAsString(filePath))) {
                    stageFiles.add(filename);
                }
                else {
                    modifiedFiles.add(filename);
                }
            } else if (filesInRemove.contains(filename)) {
                untrackedFiles.add(filename);
            } else if (commitFiles.contains(filename)) {
                String content = readContentsAsString(join(CWD, filename));
                String commitContent = currentCommit.getContentFromName(filename);
                if (!commitContent.equals(content)) {
                    modifiedFiles.add(filename);
                }
            } else {
                untrackedFiles.add(filename);
            }
        }

        for (String filename : filesInADD) {
            if (!filesInCWD.contains(filename)) {
                deletedFiles.add(filename);
            }
        }

        for (String filename : filesInRemove) {
            if (!filesInCWD.contains(filename)) {
                removeFiles.add(filename);
            }
        }

        for (String filename : commitFiles) {
            if (filesInADD.contains(filename) || filesInRemove.contains(filename)) {
                continue;
            }
            if (!filesInCWD.contains(filename)) {
                deletedFiles.add(filename);
            }
        }
        deletedFiles.sort(Comparator.naturalOrder());
        printStatus("=== Staged Files ===", stageFiles, null);
        printStatus("=== Removed Files ===", removeFiles, null);
        message("=== Modifications Not Staged For Commit ===");
        System.out.println();
        printStatus("=== Untracked Files ===", untrackedFiles, null);
    }


    public static void checkoutBranch(String branchName) {
        List<String> branchList = getBranchList();
        List<String> untrackedList = getUntrackedList();
        List<String> trackedList = getTrackedList();
        if (!branchList.contains(branchName)) {
            message("No such branch exists.");
            exit(0);
        }
        if (getCurrentBranchName().equals(branchName)) {
            message("No need to checkout the current branch.");
            exit(0);
        }
        if (!untrackedList.isEmpty()) {
            message("There is an untracked file in the way; delete it, or add and commit it first.");
            exit(0);
        }

        for (String filename : trackedList) {
            join(CWD, filename).delete();
        }
        Commit c = getBranchCommit(branchName);
        TreeMap<String, String> map = c.getBlobMap();
        for (String filename : map.keySet()) {
            writeContents(join(CWD, filename), Blob.readContentFromID(map.get(filename)));
        }
        setCurrentBranch(branchName);
        Stage.clearStage();
    }

    public static void checkoutDefaultFile(String filename) {
        Commit c = getCurrentCommit();
        if (!c.hasFilename(filename)) {
            message("File does not exist in that commit.");
            exit(0);
        }
        String content = c.getContentFromName(filename);
        writeContents(join(CWD, filename), content);
    }

    public static void checkoutDesignatedFile(String commitID, String filename) {
        Commit c = Commit.getCommitFromID(commitID);
        if (!c.hasFilename(filename)) {
            message("File does not exist in that commit.");
            exit(0);
        }
        String content = c.getContentFromName(filename);
        writeContents(join(CWD, filename), content);
    }

    public static void branch(String branchName){
        if (getBranchList().contains(branchName)) {
            message("A branch with that name already exists.");
            exit(0);
        }
        String currentCommitID = getBranchCommitID(getCurrentBranchName());
        saveHead(branchName, currentCommitID);
    }

    public static void removeBranch(String branchName) {
        List<String> branchList = getBranchList();
        if (!branchList.contains(branchName)) {
            message("A branch with that name does not exist.");
            exit(0);
        }
        if (branchName.equals(getCurrentBranchName())) {
            message("Cannot remove the current branch.");
            exit(0);
        }
        File branchPath = join(HEAD_DIR, branchName);
        branchPath.delete();
    }

    public static void reset(String commitID) {
        List<String> untrackedList = getUntrackedList();
        if (!untrackedList.isEmpty()) {
            message("There is an untracked file in the way; delete it, or add and commit it first.");
            exit(0);
        }
        TreeMap<String, String> map = Commit.getCommitFromID(commitID).getBlobMap();
        Set<String> filesInCommit = map.keySet();

        List<String> trackedList = getTrackedList();
        for (String filename : trackedList) {
            join(CWD, filename).delete();
        }
        for (String filename : filesInCommit) {
            String content = Blob.readContentFromID(map.get(filename));
            writeContents(join(CWD, filename), content);
        }
        setCurrentBranchCommitID(commitID);
    }

    /** Helper method of merge
     *
     * @param currentID
     * @param branchID
     * @return
     */
    private static String getSplitPoint(String currentID, String branchID) {
        HashMap<String, Integer> mapOfA = Commit.getInheritedMap(currentID);
        HashMap<String, Integer> mapOfB = Commit.getInheritedMap(branchID);
        int minVal = -1;
        String resultID = null;

        for (String id : mapOfA.keySet()) {
            if (mapOfB.containsKey(id)) {
                if (minVal == -1) {
                    minVal = mapOfA.get(id);
                    resultID = id;
                    continue;
                }
                if (minVal > mapOfA.get(id)) {
                    minVal = mapOfA.get(id);
                    resultID = id;
                }
            }
        }
        return resultID;
    }

    public static void mergeChecker(String branchName) {
        if (!Stage.isEmpty()) {
            message("You have uncommitted changes.");
            exit(0);
        }
        if (!getBranchList().contains(branchName)) {
            message("A branch with that name does not exist.");
            exit(0);
        }
        if (getCurrentBranchName().equals(branchName)) {
            message("Cannot merge a branch with itself.");
            exit(0);
        }
        if (!getUntrackedList().isEmpty()) {
            message("There is an untracked file in the way; delete it, or add and commit it first.");
            exit(0);
        }

    }
    private static Blob writeDiff(String filename, String idInCurrent, String idInBranch) {
        String curContent = Blob.readContentFromID(idInCurrent);
        String braContent = Blob.readContentFromID(idInBranch);
        String resContent = null;
        if (curContent == null) {
            resContent = "<<<<<<< HEAD\n"+"=======\n"+braContent+">>>>>>>";
        }
        else if (braContent == null) {
            resContent = "<<<<<<< HEAD\n"+curContent+"=======\n"+">>>>>>>";
        }
        else {
            resContent = "<<<<<<< HEAD\n"+curContent+"=======\n"
                    +braContent+">>>>>>>";
        }
        writeContents(join(CWD, filename), resContent);
        Blob blob = new Blob(resContent);
        blob.saveBlob();
        return blob;
    }

    public static void merge(String branchName) {
        mergeChecker(branchName);
        String currentID = getBranchCommitID(getCurrentBranchName());
        String branchID = getBranchCommitID(branchName);
        String splitID = getSplitPoint(currentID, branchID);

        /* Check trivial cases */
        if (splitID.equals(branchID)) {
            message("Given branch is an ancestor of the current branch.");
            exit(0);
        }
        if (splitID.equals(currentID)) {
            checkoutBranch(branchName);
            message("Current branch fast-forwarded.");
            exit(0);
        }

        Commit currentCommit = Commit.getCommitFromID(currentID);
        Commit branchCommit = Commit.getCommitFromID(branchID);
        Commit splitCommit = Commit.getCommitFromID(splitID);
        boolean isConflicted = false;

        Commit mergeCommit = new Commit(currentCommit, new Date(),
                "Merged " + getCurrentBranchName() + " into " + branchName + ".");
        TreeMap<String, String> mergeMap = mergeCommit.getBlobMap();

        Set<String> FileSet = new HashSet<String>();
        TreeMap<String, String> currentMap = currentCommit.getBlobMap();
        TreeMap<String, String> branchMap = branchCommit.getBlobMap();
        TreeMap<String, String> splitMap = splitCommit.getBlobMap();
        for (String filename : currentMap.keySet()) {
            FileSet.add(filename);
        }
        for (String filename : branchMap.keySet()) {
            FileSet.add(filename);
        }
        for (String filename : splitMap.keySet()) {
            FileSet.add(filename);
        }

        for (String filename : FileSet) {
            if (currentMap.containsKey(filename) && branchMap.containsKey(filename) &&
                    splitMap.containsKey(filename)) {
                String idInCurrent = currentMap.get(filename);
                String idInSplit = splitMap.get(filename);
                String idInBranch = branchMap.get(filename);

                /* case 1 */
                if (idInCurrent.equals(idInSplit) && !idInCurrent.equals(idInBranch)) {
                    mergeMap.put(filename, idInBranch);
                    checkoutDesignatedFile(branchID, filename);
                    continue;
                }
                /* case 2 */
                if (!idInCurrent.equals(idInSplit) && idInBranch.equals(idInSplit)) {
                    checkoutDesignatedFile(currentID, filename);
                    continue;
                }
                /* subcase of case 3 */
                if (idInCurrent.equals(idInBranch)) {
                    checkoutDesignatedFile(currentID, filename);
                    continue;
                }
                /* subcase of case 8 */
                /* comment: logic can be simplified */
                if (!idInCurrent.equals(idInBranch)) {
                    Blob b = writeDiff(filename, idInCurrent, idInBranch);
                    isConflicted = true;
                    mergeMap.put(filename, b.getId());
                    continue;
                }
            }
            /* subcase of case 3 */
            if (!currentMap.containsKey(filename) && splitMap.containsKey(filename) &&
                    !branchMap.containsKey(filename)) {
                join(CWD, filename).delete();
            }
            /* case 4 */
            if (currentMap.containsKey(filename) && !splitMap.containsKey(filename) &&
                    !branchMap.containsKey(filename)) {
                checkoutDesignatedFile(currentID, filename);
            }
            /* case 5 */
            if (!currentMap.containsKey(filename) && !splitMap.containsKey(filename) &&
                    branchMap.containsKey(filename)) {
                checkoutDesignatedFile(branchID, filename);
                mergeMap.put(filename, branchMap.get(filename));
            }
            /* case 6 */
            if (currentMap.containsKey(filename) && splitMap.containsKey(filename) &&
                    !branchMap.containsKey(filename)) {
                String idInCurrent = currentMap.get(filename);
                String idInSplit = splitMap.get(filename);
                if (idInCurrent.equals(idInSplit)) {
                    join(CWD, filename).delete();
                    mergeMap.remove(filename);
                }
            }
            /* case 7 */
            if (!currentMap.containsKey(filename) && splitMap.containsKey(filename) &&
                    branchMap.containsKey(filename)) {
                if (splitMap.get(filename).equals(branchMap.get(filename))) {
                    join(CWD, filename).delete();
                }
            }
            /* subcase of case 8 */
            if (!currentMap.containsKey(filename) && splitMap.containsKey(filename) &&
                    branchMap.containsKey(filename)) {
                String idInSplit = splitMap.get(filename);
                String idInBranch = branchMap.get(filename);
                if (!idInSplit.equals(idInBranch)) {
                    Blob b = writeDiff(filename, null, idInBranch);
                    isConflicted = true;
                    mergeMap.put(filename, b.getId());
                    continue;
                }
            }
            /* subcase of case 8 */
            if (currentMap.containsKey(filename) && splitMap.containsKey(filename) &&
                    !branchMap.containsKey(filename)) {
                String idInSplit = splitMap.get(filename);
                String idInCurrent = currentMap.get(filename);
                if (!idInCurrent.equals(idInSplit)) {
                    Blob b = writeDiff(filename, idInCurrent, null);
                    isConflicted = true;
                    mergeMap.put(filename, b.getId());
                    continue;
                }
            }
            /* subcase of case 8 */
            if (currentMap.containsKey(filename) && branchMap.containsKey(filename)) {
                String idInCurrent = currentMap.get(filename);
                String idInBranch = branchMap.get(filename);
                if (!idInCurrent.equals(idInBranch)) {
                    Blob b = writeDiff(filename, idInCurrent, idInBranch);
                    isConflicted = true;
                    mergeMap.put(filename, b.getId());
                    continue;
                }
            }
        }
        if (isConflicted) {
            message("Encountered a merge conflict.");
        }
        mergeCommit.setSecondParentID(branchID);
        mergeCommit.saveCommit(mergeCommit.getCommitID());
        setCurrentBranchCommitID(mergeCommit.getCommitID());
    }
}
