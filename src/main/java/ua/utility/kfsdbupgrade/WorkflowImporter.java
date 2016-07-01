/*
 * The Kuali Financial System, a comprehensive financial management system for higher education.
 * 
 * Copyright 2005-2014 The Kuali Foundation
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ua.utility.kfsdbupgrade;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.kuali.rice.core.api.config.property.ConfigContext;
import org.kuali.rice.core.impl.config.property.JAXBConfigImpl;
import org.kuali.rice.kew.batch.XmlPollerServiceImpl;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WorkflowImporter {
    private static final String WORKFLOW_PROCESSING_FOLDER = "workflow-processing";
    private static ClassPathXmlApplicationContext context;
    private String upgradeRoot;
	private static final Logger LOGGER = Logger.getLogger(WorkflowImporter.class);
    
    public void initializeKfs() {
		LOGGER.info("Initializing Web Context");
		LOGGER.info("Calling KualiInitializeListener.contextInitialized");
        long start = System.currentTimeMillis();

        Properties baseProps = new Properties();
        baseProps.putAll(System.getProperties());
        JAXBConfigImpl config = new JAXBConfigImpl(baseProps);
        ConfigContext.init(config);

        context = new ClassPathXmlApplicationContext("kfs-workflow-importer-startup.xml");
        context.start();

		LOGGER.info("Completed KualiInitializeListener.contextInitialized in "
				+ ((System.currentTimeMillis() - start) / 1000) + "sec");
    }

	public WorkflowImporter(String upgradeRoot, List<String> upgradeFolders) {
        this.upgradeRoot = upgradeRoot;
		Appender logFileAppender;
		try {
			logFileAppender = new FileAppender(new SimpleLayout(), getLogFileName());
			LOGGER.addAppender(logFileAppender);
		} catch (IOException e) {
			/*
			 * Unable to recover, but still logging to console, so reasonable to
			 * continue
			 */
			LOGGER.error("Unable to log to file " + getLogFileName() + " . IOException encountered: ", e);
		}
        try {
            initializeKfs();

            File workflowWorkDir = new File(WORKFLOW_PROCESSING_FOLDER);
            
            if (!workflowWorkDir.exists()) {
                workflowWorkDir.mkdirs();
            }
            
            File pendingDir = new File(workflowWorkDir.getPath() + File.separator + "pending" );
            File completedDir = new File(workflowWorkDir.getPath() + File.separator + "completed" );
            File failedDir = new File(workflowWorkDir.getPath() + File.separator + "problem");

            if (pendingDir.exists()) {
                FileUtils.deleteDirectory(pendingDir);
                FileUtils.deleteDirectory(completedDir);
                FileUtils.deleteDirectory(failedDir);
            }
            
            pendingDir.mkdirs();
            completedDir.mkdirs();
            failedDir.mkdirs();
            
            XmlPollerServiceImpl parser = new XmlPollerServiceImpl();
            parser.setXmlPendingLocation(pendingDir.getAbsolutePath() );
            parser.setXmlCompletedLocation(completedDir.getAbsolutePath() );
            parser.setXmlProblemLocation(failedDir.getAbsolutePath() );

            for (String folder : upgradeFolders) {
                File fdir = new File("upgrade-files" + File.separator + folder);
                
                if (fdir.exists() && fdir.isDirectory()) {
                    List <File> workflowFiles = new ArrayList<File>();
                    loadWorkflowFiles(fdir, workflowFiles);
                    removeModifiedBaseWorkflowFiles(workflowFiles);
                    
                    if (!workflowFiles.isEmpty()) {
                        int indx = 1;
						LOGGER.info("processing workflow files for folder " + folder);
                        for (File f : workflowFiles) {
                            FileUtils.copyFile(f, getPendingFile(pendingDir, f, folder, indx++));
							LOGGER.info("file: " + f.getPath());
                        }
                        
                        parser.run();
                    }
                }
            }
        }
        
        catch (Exception ex) {
			LOGGER.fatal("Exception encountered: ", ex);
        }
        
        finally {
			LOGGER.info("workflow processing completed");
        }
    }
    
    private void removeModifiedBaseWorkflowFiles(List <File> files) {
        Set <String> hs = new HashSet<String>();
        
        // add all the modified files to the set
        for (File f : files) {
            if (f.getPath().endsWith("_mod.xml")) {
                hs.add(f.getPath());
            }
        }
        
        Iterator <File> it = files.iterator();
        
        while (it.hasNext()) {
            File f = it.next();
            
            // if this is a base file and it has a modified counterpart - remove it
            if (!f.getPath().endsWith("_mod.xml")) {
                String s = f.getPath().substring(0, f.getPath().lastIndexOf("."));
                
                if (hs.contains(s + "_mod.xml")) {
                    it.remove();
                }
            }
        }
    }
    
    private File getPendingFile(File pendingDir, File f, String parentFolderName, int indx) {
        StringBuilder retval = new StringBuilder(256);
        retval.append(pendingDir.getPath());
        retval.append(File.separator);
        retval.append(getFilePrefix(indx));
        retval.append("-");
        retval.append(parentFolderName);
        retval.append("-");
        retval.append(f.getName());
        
        return new File(retval.toString());
    }
    
    private String getFilePrefix(int indx) {
        if (indx > 99) {
            return ("" + indx);
        } else if (indx > 9) {
            return ("0" + indx);
        } else {
            return ("00" + indx);
        }
    }
    
    private void loadWorkflowFiles(File curfile, List <File> workflowFiles) {
        if (curfile.isDirectory()) {
            File[] files = curfile.listFiles();
            
            if (files != null) {
                for (File f : files) {
                   loadWorkflowFiles(f, workflowFiles);
                }
            }
        } else if (curfile.isFile()) {
            if (curfile.getPath().contains(File.separator + "workflow" + File.separator)
                && curfile.getName().endsWith(".xml")) {
                workflowFiles.add(curfile);
            }
        }
    }

	private String getLogFileName() {
		return upgradeRoot + File.separator + WORKFLOW_PROCESSING_FOLDER + File.separator + "workflow.log";
    }
}
