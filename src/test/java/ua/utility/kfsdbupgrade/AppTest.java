package ua.utility.kfsdbupgrade;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class AppTest {

	@Test
	public void buildRunRequestFromLastProcessedFile() {
		String propertyFileName = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "kfsdbupgrade.properties";
		App app = new App(propertyFileName);
		/*
		 * note that above created using system independent properties;
		 * unfortunately the rest of the code base still specifically expects
		 * the unix-style '/' as the path separator
		 */
		RunRequest runRequest = app.buildRunRequest(null);
		// verify that all directories were loaded
		List<String> allDirectories = createFullDirectoryList();
		Assert.assertEquals(allDirectories, runRequest.getDirectories());

		runRequest = app
				.buildRunRequest("src/main/resources/upgrade-files/5.0_5.0.1/rice_server/rice-server-script.xml");
		// verify that only desired directories were loaded
		List<String> necessaryDirectories = createPartialDirectoryList();
		Assert.assertEquals(necessaryDirectories, runRequest.getDirectories());
		/*
		 * since run is starting assuming
		 * 5.0_5.0.1/rice_server/rice-server-script.xml succeeded, it should NOT
		 * be in the file list for the 5.0_5.0.1 directory
		 */
		List<String> necessaryFiles = createPartialFileList();
		Assert.assertEquals(necessaryFiles, runRequest.getFilesForDirectory("5.0_5.0.1"));
	}

	/**
	 * @return {@link List} of the {@link String} directory names for a full
	 *         upgrade run
	 */
	private static List<String> createFullDirectoryList() {
		List<String> directories = new ArrayList<String>();
		directories.add("pre-upgrade");
		directories.add("3.0_3.0.1");
		directories.add("3.0.1_4.0");
		directories.add("4.0_4.1");
		directories.add("4.1_4.1.1");
		directories.add("4.1.1_5.0");
		directories.add("5.0_5.0.1");
		directories.add("5.0.1_5.0.2");
		directories.add("5.0.2_5.0.3");
		directories.add("5.0.3_5.0.4");
		directories.add("5.0.4_5.0.5");
		directories.add("5.0.5_5.1");
		directories.add("5.1_5.1.1");
		directories.add("5.1.1_5.1.2");
		directories.add("5.1.2_5.2");
		directories.add("5.2_5.2.1");
		directories.add("5.2.1_5.2.2");
		directories.add("5.2.2_5.3");
		directories.add("5.3.1_5.3.2");
		directories.add("5.3.2_5.4");
		directories.add("5.4_5-4.1");
		directories.add("5.4.1_6.0");
		directories.add("post-upgrade");
		directories.add("ua-workflow");
		return directories;
	}

	/**
	 * 
	 * @return {@link List} of the {@link String} directory names for an upgrade
	 *         run starting from the "<code>5.0_5.0.1</code>" directory
	 */
	private static List<String> createPartialDirectoryList() {
		List<String> directories = new ArrayList<String>();
		directories.add("5.0_5.0.1");
		directories.add("5.0.1_5.0.2");
		directories.add("5.0.2_5.0.3");
		directories.add("5.0.3_5.0.4");
		directories.add("5.0.4_5.0.5");
		directories.add("5.0.5_5.1");
		directories.add("5.1_5.1.1");
		directories.add("5.1.1_5.1.2");
		directories.add("5.1.2_5.2");
		directories.add("5.2_5.2.1");
		directories.add("5.2.1_5.2.2");
		directories.add("5.2.2_5.3");
		directories.add("5.3.1_5.3.2");
		directories.add("5.3.2_5.4");
		directories.add("5.4_5-4.1");
		directories.add("5.4.1_6.0");
		directories.add("post-upgrade");
		directories.add("ua-workflow");
		return directories;
	}

	/**
	 * @return {@link List} of the {@link String} file names for the
	 *         "<code>5.0_5.0.1</code>" directory assuming the file
	 *         <code>rice_server/rice-server-script.xml</code> succeeded and
	 *         should not be included
	 */
	private static List<String> createPartialFileList() {
		List<String> files = new ArrayList<String>();
		files.add("rice_server/kew_upgrade.xml");
		files.add("rice_server/kim_upgrade.xml");
		files.add("rice_server/parameter_updates.xml");
		files.add("db/master-structure-script.xml");
		files.add("db/master-constraint-script.xml");
		files.add("db/master-data-script.xml");
		return files;
	}

	@Test
	public void unprocessedFilesLogic() {
		Set<File> processed = new HashSet<File>();
		// Note: Assuming that the testing directory has at least 3 child files
		File directory = new File(System.getProperty("user.dir") + File.separator + "src" + File.separator + "main"
				+ File.separator + "resources" + File.separator + "upgrade-files" + File.separator + "post-upgrade"
				+ File.separator + "sql");
		File[] children = directory.listFiles();

		for (File child : children) {
			processed.add(child);
		}

		Set<File> unprocessedFiles = App.getUnprocessedFiles(directory, processed);

		// since all files are processed, unprocessedFiles should be empty
		Assert.assertTrue(unprocessedFiles.isEmpty());

		// remove a single file from processing configuration, verify logic
		File toRemove = processed.iterator().next();
		processed.remove(toRemove);

		unprocessedFiles = App.getUnprocessedFiles(directory, processed);
		Assert.assertFalse(unprocessedFiles.isEmpty());
		Assert.assertEquals(1, unprocessedFiles.size());
		Assert.assertTrue(unprocessedFiles.contains(toRemove));

		// remove some additional files
		Set<File> removed = new HashSet<File>();
		removed.add(toRemove);
		for (int i = 0; i < 2; i++) {
			toRemove = processed.iterator().next();
			processed.remove(toRemove);
			removed.add(toRemove);
		}

		unprocessedFiles = App.getUnprocessedFiles(directory, processed);
		Assert.assertFalse(unprocessedFiles.isEmpty());
		Assert.assertEquals(3, unprocessedFiles.size());
		Assert.assertTrue(unprocessedFiles.containsAll(removed));
	}

}
