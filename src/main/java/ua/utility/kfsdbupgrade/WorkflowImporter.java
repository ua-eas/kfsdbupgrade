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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.kuali.rice.core.api.config.property.ConfigContext;
import org.kuali.rice.core.impl.config.property.JAXBConfigImpl;
import org.kuali.rice.kew.batch.XmlPollerServiceImpl;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

public class WorkflowImporter {
    private GenericApplicationContext context;

    public void initializeKfs(App app, String upgradeRoot) {
        app.writeHeader1("Initializing Web Context" );
        app.writeHeader2( "Calling KualiInitializeListener.contextInitialized" );
        long start = System.currentTimeMillis();

        Properties baseProps = new Properties();
        baseProps.putAll(System.getProperties());
        JAXBConfigImpl config = new JAXBConfigImpl(baseProps);
        ConfigContext.init(config);

        context = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(context);
        xmlReader.loadBeanDefinitions(new ClassPathResource("kfs-workflow-importer-startup.xml"));
        PropertyPlaceholderConfigurer ppc = (PropertyPlaceholderConfigurer)context.getBean("propertyPlaceholderConfigurer");
        ppc.setLocation(new FileSystemResource(upgradeRoot + File.separator + "kfsdbupgrade.properties"));
        context.refresh();

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
            boolean foundone = false;
            for (String folder : upgradeFolders) {
                File fdir = new File(upgradeRoot + File.separator + "upgrade-files" + File.separator + folder);
                
                if (fdir.exists() && fdir.isDirectory()) {
                    List <File> workflowFiles = new ArrayList<File>();
                    loadWorkflowFiles(fdir, workflowFiles);
                    removeModifiedBaseWorkflowFiles(workflowFiles);
                    
                    if (!workflowFiles.isEmpty()) {
                        for (File f : workflowFiles) {
                            FileUtils.copyFile(f, getPendingFile(pendingDir, f, folder, indx++));
                            foundone = true;
                            break;
                        }
                    }
                }
                
                if (foundone) {
                    break;
                }
            }
            
            XmlPollerServiceImpl parser = new XmlPollerServiceImpl();
            parser.setXmlPendingLocation(pendingDir.getAbsolutePath() );
            parser.setXmlCompletedLocation(completedDir.getAbsolutePath() );
            parser.setXmlProblemLocation(failedDir.getAbsolutePath() );

            parser.run();
        }
        
        catch (Exception ex) {
            app.writeOut(ex);
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

}
