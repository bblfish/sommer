/*
 * Sommer.java
 *
 * Created on December 5, 2006, 7:57 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.rdf.sommer.ant;

import net.java.rdf.sommer.JavassistClassRewriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Tim Boudreau
 */
public class Sommer extends MatchingTask {
    private Path compileClasspath;
    ArrayList<FileSet> filesets = new ArrayList<FileSet>();
    private File todir;

    public Sommer() {
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void setClasspath(Path classpath) {
        if (compileClasspath == null) {
            compileClasspath = classpath;
        } else {
            compileClasspath.append(classpath);
        }
    }
    
    public void setClasspathRef(Reference classpathRef) {
        if (compileClasspath == null) {
            compileClasspath = (Path)classpathRef.getReferencedObject(this.getProject());
        } else {
            compileClasspath.append((Path)classpathRef.getReferencedObject(this.getProject()));
        }
    }

    /**
     * the directory to which the files should be copied
     * @param dir
     */
    public void setToDir(File dir) {
        todir = dir;
    }

    /**
     * Adds a set of files to be deleted.
     * @param set the set of files to be deleted
     */
     public void addFileset(FileSet set) {
         filesets.add(set);
     }


    private boolean failIfEmpty = true;

    public void setFailIfEmpty(boolean val) {
        failIfEmpty = val;
    }

    private String markerName;

    /**
     * the name of static fields to appear in rewritten classes
     * @param name
     */
    public void setMarkerName(String name) {
         markerName = name;
    }

    private String markerValue;
    /**
     * the value of a static field, named by marker name
     * @param value
     */
    public void setMakerValue(String value) {
        markerValue = value;
    }

    public void execute() {

        if (compileClasspath == null || compileClasspath.size() == 0) {
            throw new BuildException("Set a classpath");
        }

        if (markerName == null)
            throw new BuildException("You need the 'markerName' attribute set on your sommer ant task!");

        ArrayList<String> files = new ArrayList<String>();
        for (FileSet fileset: filesets) {
            DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
            for (String file: ds.getIncludedFiles()) {
                files.add(ds.getBasedir()+ File.separator+file);
            }
        }
        if (files.size() == 0) {
            throw new BuildException("No files available in fileset"); //todo: do we really need this?
        }


        try {
            JavassistClassRewriter.transformFiles(Arrays.asList( compileClasspath.list()), files, (todir==null)?null:todir.getAbsolutePath(),
                                                  markerName, (markerName !=null && markerValue ==null)?new Date().toString():markerValue);
        } catch (Exception e) {
           e.printStackTrace();
           throw new BuildException("error recompiling classes",e);
        }
    }

}
