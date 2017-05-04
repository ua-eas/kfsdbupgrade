package ua.utility.kfsdbupgrade;

import java.io.File;

/**
 * Created by S.G. Skinner on 2017-05-03
 */
public abstract class AppTestBase {
    protected static final String DEFAULT_PROP_FILE =
            System.getProperty("user.dir")
                    + File.separator + "src"
                    + File.separator + "main"
                    + File.separator + "resources"
                    + File.separator + "kfsdbupgrade.properties";
    protected static final String INDEX_SQL_FILE =
            System.getProperty("user.dir")
                    + File.separator + "src"
                    + File.separator + "main"
                    + File.separator + "resources"
                    + File.separator + "kfsdbupgrade.properties";
    protected static final String TEST_RESOURCES_DIR =
            System.getProperty("user.dir")
                    + File.separator + "src"
                    + File.separator + "test"
                    + File.separator + "resources";


    protected App getApp() {
        return getApp(DEFAULT_PROP_FILE);
    }


    protected App getApp(String propFile) {
        return new App(propFile);
    }


    protected File getTestResource(String filename) {
        return new File(TEST_RESOURCES_DIR + File.separator + filename);
    }

}
