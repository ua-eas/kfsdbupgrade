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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WorkflowImporter {
    private static ClassPathXmlApplicationContext context;

    public void initializeKfs(App app, String upgradeRoot) {
        app.writeHeader1("Initializing Web Context" );
        app.writeHeader2( "Calling KualiInitializeListener.contextInitialized" );
        long start = System.currentTimeMillis();

     /*
        Properties baseProps = new Properties();
        baseProps.putAll(System.getProperties());
        JAXBConfigImpl config = new JAXBConfigImpl(baseProps);
        ConfigContext.init(config);

        new ClassPathXmlApplicationContext(upgradeRoot + File.separator + "kfs-workflow-importer-startup.xml").start();
         */
        app.writeOut("Completed KualiInitializeListener.contextInitialized in " + ((System.currentTimeMillis() - start)/1000) + "sec");
    }

    public WorkflowImporter(App app, String upgradeRoot, List <String> upgradeFolders) {
        try {
            initializeKfs(app, upgradeRoot);

            File workflowWorkDir = new File(upgradeRoot + File.separator + "workflow-work");
            FileUtils.deleteDirectory(workflowWorkDir);
            
            File pendingDir = new File(workflowWorkDir.getPath() + File.separator + "pending" );
            File completedDir = new File(workflowWorkDir.getPath() + File.separator + "completed" );
            File failedDir = new File(workflowWorkDir.getPath() + File.separator + "problem");

            pendingDir.mkdirs();
            completedDir.mkdirs();
            failedDir.mkdirs();
            
            int indx = 1;
            for (String folder : upgradeFolders) {
                File fdir = new File(upgradeRoot + File.separator + folder);
                
                if (fdir.exists() && fdir.isDirectory()) {
                    List <File> workflowFiles = new ArrayList<File>();
                    loadWorkflowFiles(fdir, workflowFiles);
                    
                    if (!workflowFiles.isEmpty()) {
                        for (File f : workflowFiles) {
                            FileUtils.copyFile(f, getPendingFile(pendingDir, f, folder, indx++));
                        }
                    }
                }
            }
            
            /*
            XmlPollerServiceImpl parser = new XmlPollerServiceImpl();
            parser.setXmlPendingLocation(pendingDir.getAbsolutePath() );
            parser.setXmlCompletedLocation(completedDir.getAbsolutePath() );
            parser.setXmlProblemLocation(failedDir.getAbsolutePath() );

            parser.run();
                */
        }
        
        catch (Exception ex) {
            app.writeOut(ex);
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

}
