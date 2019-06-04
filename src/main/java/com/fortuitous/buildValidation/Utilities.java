/* Copyright 2018 Harold Fortuin of
   Fortuitous Consulting Services, Inc.

   You are free to use or modify this software and source code
   as long as you include this Copyright notice.

   No warranty is provided or implied. Use at your own risk.
*/
package com.fortuitous.buildValidation;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

// within-package access should suffice
class Utilities {

	static final char PATH_DELIMITER = '/'; // works for Windows too these days

    static final String CLASS_FILE_EXTENSION = ".class";
    static final String JSP_FILE_EXTENSION = ".jsp";

    // log4j
    private static final Logger logr = Logger.getLogger(Utilities.class);

    // TO DO: update with java.nio?
	boolean existsAndIsFile(File aFile, String aPathFile, boolean bJspTest)
            throws FileException {
    boolean bExistsAndIsFile = false;
    String strException = "";

    String strMissingFile = "FILE: " + aPathFile +
            "\n" + " is either missing or lacks read permissions.";

    if (aPathFile == null || aPathFile.isEmpty() )
    	strException = "Path to file argument is missing.";
    else if (aFile == null)
    	strException = strMissingFile;
    else if (aFile.exists() ) {
        if (aFile.isFile() ) {
        	bExistsAndIsFile = true;
        }
        else
        	strException = "Item: " + aPathFile +
        				    "\n" + " is not a file. Perhaps a directory?";
    }
    else if (bJspTest)
    	// diagnostic print
    	 logr.trace("JSP path: " + aPathFile + " not matching a file.");
    else
    	strException = strMissingFile;

    if (!strException.isEmpty() )
    	throw new FileException(strException);

    return bExistsAndIsFile;
}

    boolean classFileInstanceExists(String path, String classRoot, String fileExtension, Collection<String> ignorePackageCollection) {
        boolean bFileInstanceExists = false;
        String osSourcePath;
        String totalPathToFile;

        osSourcePath = path.replace('.', Utilities.PATH_DELIMITER );
        totalPathToFile = classRoot + Utilities.PATH_DELIMITER + osSourcePath + fileExtension;

        logr.debug("totalPathToFile =" + totalPathToFile);

        bFileInstanceExists = fileInstanceExists(path, totalPathToFile, ignorePackageCollection, false);
        return bFileInstanceExists;
    }

    /**	check for each  file instance;
        but if within an ignorePackage, still returns true
    */
    boolean fileInstanceExists(String path, String totalPathToFile,
    						   Collection<String> ignorePackageCollection,
    						   boolean bJspTest) {
        boolean bFileInstanceExists = false;
        File theFile = null;

        boolean bIsInIgnorePackage = false;

        try
        {
            // check if ignore-able - uses ORIGINAL path for comparison
        	if (ignorePackageCollection != null)
        		bIsInIgnorePackage = isInIgnorePackage(path, ignorePackageCollection);

        	if (!bIsInIgnorePackage) {

        		// confirm the totalPathToFile file exists and is a file
        		theFile = new File(totalPathToFile).getAbsoluteFile();
        		// create info & error strings, and write method

        		// logr.trace(fileExtension + "  FILE PROCESSING ***");
        		// for JSP's, it may be valid if on a given pass, a path and JSP pairing does not exist
        		bFileInstanceExists = existsAndIsFile(theFile, totalPathToFile, bJspTest);
        	} else {
        		logr.info("skip processing : " + path);

        		// since this file is within an ignorePackage, it's OK to bypass and continue processing
        		bFileInstanceExists = true;
        	}

        } catch (FileException msfe) {
           logr.error(msfe);
            msfe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bFileInstanceExists;
    } // end fileInstanceExists()


	/** decides whether to bypass processing for this fully qualified class name,
     * based on containing package
     */
    boolean isInIgnorePackage(String path, Collection<String> ignorePackageCollection) {
        // ignorePackageCollection has the relevant Strings
        boolean bIsInIgnorePackage = false;
        String ignorePkgValue = null;

        Iterator<String> ignorePkgIterator = ignorePackageCollection.iterator();

        try
        {
            ignorePkgIterator = ignorePackageCollection.iterator();

            // the loop quits on match
            while( ignorePkgIterator.hasNext() && !bIsInIgnorePackage) {
                ignorePkgValue = ignorePkgIterator.next();
                logr.trace("ignorePkgValue = " + ignorePkgValue);

                bIsInIgnorePackage = path.startsWith(ignorePkgValue);
            }


        } catch (Exception e) {
        	logr.trace(e.toString() );
        }

        return bIsInIgnorePackage;
    } // end isInIgnorePackage(...)



}
