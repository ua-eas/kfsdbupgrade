package ua.utility.kfsdbupgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by S.G. Skinner on 2017-05-04
 */
public class GecBootstrapDataTest extends AppTestBase {
    private static final String PROBLEM_QUERY_FILE_1 = "gec_test_queries/uaf-3849_problem_queries.sql";

    /*
     * Test query that failed for syntax reasons, isolated for testing. If you see this
     * in the master branch, this test should pass.
     *
     * Test prep: Fully Upgraded DB, with truncated FP_GEC_ENTRY_REL_T; this will have the no-op
     *            effect, since the inner query of the file counts against a now empty table. But,
     *            we will still get validation of it being sent off to the server
     */
    @Test
    public void testProblemQuerySubset () {
        boolean success = executeSqlFile(PROBLEM_QUERY_FILE_1);
        Assert.assertTrue("Query file did NOT process successfully!", success);
    }


    private boolean executeSqlFile(String filename) {
        boolean success = true;

        App app = getApp();
        File file = getTestResource(filename);
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = app.getUpgradeConnection();
            stmt = conn.createStatement();

            // This is what we're really interested in for this junit class
            success = app.runSqlFile(conn, stmt, file, ";");

            if (!success) {
                success = false;
                app.writeProcessedFileInfo("[failure]: " + file.getPath());
                app.doRollback(conn);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            app.closeDbObjects(conn, stmt, null);
        }

        return success;
    }

}
