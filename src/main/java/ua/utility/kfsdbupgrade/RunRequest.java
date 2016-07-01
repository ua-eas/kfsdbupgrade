package ua.utility.kfsdbupgrade;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Models a request to execute a database upgrade run. Contains a list of
 * directories to process, and for each directory, a list of child files to be
 * processed.
 */
public final class RunRequest {

	private final List<String> directories;
	private final Map<String, List<String>> directoriesToFiles;
	private final Date timestamp;

	/**
	 * Constructor.
	 * 
	 * @param directories
	 *            Value for {@link #getDirectories()}
	 * @param directoriesToFiles
	 *            Value for {@link #getDirectoriesToFiles()}
	 */
	public RunRequest(List<String> directories, Map<String, List<String>> directoriesToFiles) {
		this.directories = Collections.unmodifiableList(directories);
		this.directoriesToFiles = Collections.unmodifiableMap(directoriesToFiles);
		this.timestamp = new Date();
	}

	/**
	 * @return {@link List} of {@link String} directory names that are requested
	 *         to be processed.
	 */
	public List<String> getDirectories() {
		return directories;
	}

	/**
	 * 
	 * @param directory
	 *            {@link String} name of a directory
	 * @return {@link List} of {@link String} child filenames requested to be
	 *         processed
	 */
	public List<String> getFilesForDirectory(String directory) {
		return directoriesToFiles.get(directory);
	}

	/**
	 * 
	 * @return {@link Map} of {@link String} directory name keys mapping to
	 *         {@link List} of {@link String} filename values
	 */
	public Map<String, List<String>> getDirectoriesToFiles() {
		return directoriesToFiles;
	}

	/**
	 * @return {@link String} representation of this {@link RunRequest} in a
	 *         human-readable format.
	 */
	public String toStringHumanReadable(){
		StringBuilder sb = new StringBuilder();
		sb.append("Run Request created at " + App.DF.format(timestamp) + System.lineSeparator());
		sb.append("Directories and files to be processed: " + System.lineSeparator());
		for (String dir : directories) {
			sb.append("--" + dir + System.lineSeparator());
			for (String file : directoriesToFiles.get(dir)) {
				sb.append("  |-" + file + System.lineSeparator());
			}
		}
		return sb.toString();
	}

}
