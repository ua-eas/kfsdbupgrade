package ua.utility.kfsdbupgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Important: This test class can be mutative!!! If this is run against a DB that does
 *            not have any of these indexes, we'll be in for a long wait. If this is
 *            applied with indexes in place, then the tests run in about 2 minutes.
 *            The run on the latter is so quick due to the existence-detection logic
 *            being smart enough to skip actual execution of the DML when already
 *            present.
 *
 * A class to test index detection/creation logic. The App should not attempt to
 * double-apply an existing index, even when the index names might not match
 * between the SQL and pre-existing DB index. This is done through a series of
 * queries that will line up column names from the DML against a table's actual
 * indexed columns.
 *
 * If the column count or column names do not line up between the two source indexes,
 * it is not a match, and the next table's index is then tested untill the index list
 * on the table is exhausted.
 *
 * If the columns do line up when going through a tables indexes, then it is a match,
 * even regardless of the index name, and the index building is skipped since it
 * already exists.
 *
 * If ALL of a table's indexes fail to match the DML, only then is the DML index
 * applied. It was this case that sparked this junit, as the detection was failing
 * at some point, but no longer appears to be.
 *
 */
public class IndexScriptExecutionTest extends AppTestBase {
    private static final String SQL_INDEX_TEST_FILE = "past_problem_indexes.sql";
    private static final String PARALLEL_DML_FORMAT = "ALTER SESSION %s PARALLEL DML";


    /*
     * The logic behind the App#createExistingIndexes() method is now smart enough not
     * to care about index names, as it does a comparison on the actual columns that
     * are involved, and also considers the 'UNIQUE' keyword of the DML. Thus, the
     * test file from here processes without issue. Under the hood, if the index is
     * detected as existing, then it will be skipped for execution, and moves on to the
     * next line in the file.
     */
    @Ignore // Integration test, shouldn't hold up a build
    @Test
    public void testProblemIndexDml() {
        App app = getApp();
        File file = getTestResource(SQL_INDEX_TEST_FILE);
        boolean success = executIndexDml(app, file);
        Assert.assertTrue("Former issue-cases did NOT process without error.", success);
    }


    /*
     * Sanity check after having modified some small things in App.java, plus
     * can be used as a general smoke test for new index DML.
     */
    @Ignore // Integration test, shouldn't hold up a build
    @Test
    public void testFullIndexDml() {
        App app = getApp();
        File file = new File(app.getPostUpgradeDirectory() + File.separator + App.KFS_INDEXES_SQL_PATH);
        boolean success = executIndexDml(app, file);
        Assert.assertTrue("Full index SQL file did NOT process without error.", success);
    }


    private boolean executIndexDml(App app, File file) {
        boolean success = true;
        enableParallelDml(app);

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = app.getUpgradeConnection();
            stmt = conn.createStatement();

            // This is what we're really interested in for this junit class
            success = app.createExistingIndexes(conn, stmt, file);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            disableParallelDml(app);
            app.closeDbObjects(conn, stmt, null);
        }

        return success;
    }


    private void enableParallelDml(App app) {
        executeParallelDmlQuery(DmlParallelProcState.ENABLE, app);
    }


    private void disableParallelDml(App app) {
        executeParallelDmlQuery(DmlParallelProcState.DISABLE, app);
    }


    private void executeParallelDmlQuery(DmlParallelProcState state, App app) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = app.getUpgradeConnection();
            conn.setAutoCommit(true);
            stmt = conn.createStatement();
            String query = String.format(PARALLEL_DML_FORMAT, state);
            stmt.execute(query);
            stmt.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            app.closeDbObjects(conn, stmt, null);
        }
    }


    enum DmlParallelProcState {
        ENABLE("ENABLE"),
        DISABLE("DISABLE");

        private String name;

        private DmlParallelProcState(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return getName();
        }
    }

}
