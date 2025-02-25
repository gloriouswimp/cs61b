package gitlet;

import org.junit.Test;
import static org.junit.Assert.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

import java.util.*;


public class TestMain {

    @Test
    public void testBlobHash() {
        Blob b = new Blob("123");
        b.saveBlob();
    }

    @Test
    public void testReadBlob() {
        System.out.println(Blob.readContentFromID("40bd001563085fc35165329ea1ff5c5ecbdbbeef"));
    }

    @Test
    public void testDate() {
        Date d = new Date(0);
        Formatter formatter = new Formatter(Locale.US);
        formatter.format("%ta %tb %td %tT %tY %tz", d, d, d, d, d, d);
        System.out.println(formatter);
    }

    @Test
    public void testInitCommit() {
        Commit c = Commit.initCommit();
        assertEquals("cddce00c2abb1396801f8a0cc5358b2447f137ce", c.getCommitID());
    }

    @Test
    public void testCommitID() {
        Commit c = Commit.initCommit();
        c.addFile("f1", "addddc");
        c.addFile("f2", "aseees");
        assertEquals("aa5cf4cd6efa466ece870b5e8de22a66883bde5b", c.getCommitID());
    }

    @Test
    public void testInitRepo() {
        Repository.initRepository();
    }
    @Test
    public void testGetBranch() {
        System.out.println(getCurrentBranchName());
        Commit c = getCurrentCommit();
        System.out.println(c.getFormatDate());
    }

    @Test
    public void testSetBranch() {
        setCurrentBranch("new");
        System.out.println(getCurrentBranchName());
        setCurrentBranch("master");
        System.out.println(getCurrentBranchName());
    }

    @Test
    public void testCompareHash() {
        String content = "123";
        String hash = sha1(content);
        assertEquals(hash, "40bd001563085fc35165329ea1ff5c5ecbdbbeef");
    }

    @Test
    public void testClearStage() {
        Stage.clearStage();
    }

    @Test
    public void testStageEmpty() {
        System.out.println(Stage.isEmpty());
    }
    @Test
    public void testMessage(){
        message("123");
        message("234");
    }

    @Test
    public void testFileRemove() {
        List<String> files = plainFilenamesIn(CWD);
        /* asList can't use remove function */
        Iterator<String> it = files.iterator();

        while(it.hasNext()) {
            String s = it.next();
            it.remove();
            System.out.println(files);
        }
        if (files.isEmpty()) {
            message("List is empty");
        }
    }

    @Test
    public void testLinkedList() {
        List<String> files = plainFilenamesIn(CWD);
        for (int i=0; i<files.size(); i++) {
            System.out.println(files);
            String filename = files.get(i);
            files.remove(i);
        }
    }

    @Test
    public void testString() {
        String s = "1234567";
        message(s.substring(0, 5));
    }
    @Test
    public void testLog(){
        Repository.log();
    }
    @Test
    public void testPrintNull() {
        System.out.println("add"+""+"2");
    }
    @Test
    public void testAddUp() {
        int i = 0;
        while (i < 10) {
            System.out.println(i++);
        }
    }
}
