package gitlet;

import org.junit.Test;
import static org.junit.Assert.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;


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
    public void testStatus() {
        Repository.status();
    }
}
