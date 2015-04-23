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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.kuali.rice.core.api.config.property.ConfigContext;
import org.kuali.rice.core.impl.config.property.JAXBConfigImpl;
import org.kuali.rice.kew.batch.XmlPollerServiceImpl;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WorkflowImporter {
    private static final String WORKFLOW_PROCESSING_FOLDER = "workflow-processing";
    private static ClassPathXmlApplicationContext context;
    private App app;
    private String upgradeRoot;
    
    public void initializeKfs() {
        writeHeader1("Initializing Web Context" );
        writeHeader2( "Calling KualiInitializeListener.contextInitialized" );
        long start = System.currentTimeMillis();

        Properties baseProps = new Properties();
        baseProps.putAll(System.getProperties());
        JAXBConfigImpl config = new JAXBConfigImpl(baseProps);
        ConfigContext.init(config);

        context = new ClassPathXmlApplicationContext("kfs-workflow-importer-startup.xml");
        context.start();

        writeOut("Completed KualiInitializeListener.contextInitialized in " + ((System.currentTimeMillis() - start)/1000) + "sec");
    }

    public WorkflowImporter(App app, String upgradeRoot, List <String> upgradeFolders) {
        this.app = app;
        this.upgradeRoot = upgradeRoot;
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
                        writeHeader2("processing workflow files for folder " + folder);
                        for (File f : workflowFiles) {
                            FileUtils.copyFile(f, getPendingFile(pendingDir, f, folder, indx++));
                            writeOut("file: " + f.getPath());
                        }
                        
                        parser.run();
                    }
                }
            }
        }
        
        catch (Exception ex) {
            writeOut(ex);
        }
        
        finally {
            writeOut("workflow processing completed");
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

    public void writeHeader1(String msg) {
        writeOut("");
        writeOut(App.HEADER1.replace("?", msg));
        writeOut("");
    }

    public void writeHeader2(String msg) {
        writeOut("");
        writeOut(msg);
        writeOut(App.UNDERLINE);
        writeOut("");
    }

    private PrintWriter getOutputLogWriter() throws IOException {
        return new PrintWriter(new FileWriter(upgradeRoot + File.separator + WORKFLOW_PROCESSING_FOLDER + File.separator + "workflow.log", true));
    }

    private void writeOut(Exception ex) {
        System.out.println();
        System.out.println(app.getTimeString() + App.ERROR);
        ex.printStackTrace(System.out);
        writeLog(ex);
    }

    private void writeLog(Exception ex) {
        PrintWriter pw = null;

        try {
            pw = getOutputLogWriter();
            pw.println();
            pw.println(app.getTimeString() + App.ERROR);
            ex.printStackTrace(pw);
        } catch (Exception ex2) {
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex2) {
            };
        }
    }
    
    private void writeOut(String msg) {
        System.out.println(msg);
        writeLog(msg);
    }

    private void writeLog(String msg) {
        PrintWriter pw = null;

        try {
            pw = getOutputLogWriter();
            if (StringUtils.isNotBlank(msg)) {
                pw.println(app.getTimeString() + msg);
            } else {
                pw.println();
            }
        } catch (Exception ex2) {
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            } catch (Exception ex2) {
            };
        }
    }
}
