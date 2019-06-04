/* Copyright 2018 Harold Fortuin of
   Fortuitous Consulting Services, Inc.

   You are free to use or modify this software and source code
   as long as you include this Copyright notice.

   No warranty is provided or implied. Use at your own risk.
*/
package com.fortuitous.buildValidation;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class ValidateConfigAgainstJsps {

	// to please Eclipse
	static final long serialVersionUID = 927394629374927L;

    static final String XPATH = "xPath";
    static final String JSP_XPATH = "jspxPath";
    static final String IGNORE_PACKAGE = "ignorePackage";

    // log4j
    private static final Logger logr = Logger.getLogger(ValidateConfigAgainstJsps.class);

    // holds each unique xPath in the classPaths.xml
    private static Collection<String> xPathPropCollection = null;

    // leftover from ValidateConfigVsClass - holds each unique ignorePackage in the classPaths.xml
    private static Collection<String> ignorePackageCollection = null;

    private static HashMap<String,String> mapXPathToFileLocation = new HashMap<String,String>();

    // within this package
   private static Utilities utils = new Utilities();    
    
    /** Usage: java validateConfigAgainstJsps [settings]

     where settings should include each of these:
     [0] = jspXPaths.xml - filetypes (filetype-xml) and associated xPath entries with
           jsp filenames, such as the J2EE-standard web.xml

     [1] = jspConfigurationFiles.xml - each config file has a unique configurationFile type attribute;
             each configurationFile element has subelements: the location for the XML file,
                and n jspDirectory subelements each containing a directory path possibly containing .jsp files
                 
     */
    // public static void main(String[] args) throws NullPointerException {
    public static void main(String[] args) {

        // if any argument missing, of course the ArrayOutOfBoundsException is thrown
        String axPathConfigFile = args[0];
        String aConfigurationFilesXmlFile = args[1];

       logr.trace("args[0], the jspXPaths.xml path/file = " + args[0]);
       logr.trace("args[1], the jspConfigurationFiles.xml path/file = " + args[1]);

        String xmlFileType = null;

        XMLConfiguration xmlXPathsConfig = null;
        XMLConfiguration xmlConfig = null;

        XPathExpressionEngine xPathEngine = null;

        File xPathsFile = null;
        File xmlConfigurationsFile = null;

        Object xPathProp = null;

        Iterator<HierarchicalConfiguration> configsIterator = null;

        String xmlJspDirectory;
        String xmlFiletypeLocation = null;

        try {
        	// open and parse the XML that lists each file type and relevant directories
        	xmlConfigurationsFile = new File(aConfigurationFilesXmlFile).getAbsoluteFile();
        	// throws FileException if file not found or lacking read permissions
        	boolean bSkipExceptionIfNoFile = false;
        	boolean bIsXmlFileValid = utils.existsAndIsFile(xmlConfigurationsFile, 
        													aConfigurationFilesXmlFile,
        													bSkipExceptionIfNoFile);

    		// throws FileException if XPath file not found or lacking read permissions
    		// to set up fileLocationsConfig entries
    		xPathsFile = new File(axPathConfigFile).getAbsoluteFile();

    		bIsXmlFileValid = utils.existsAndIsFile(xPathsFile, 
    												axPathConfigFile, 
    												bSkipExceptionIfNoFile);
        	
            xmlConfig = new XMLConfiguration(xmlConfigurationsFile);
            xPathEngine = new XPathExpressionEngine();

            xmlConfig.setExpressionEngine( xPathEngine );

            // reset for reuse later
            bIsXmlFileValid = false;

            // logr.debug("xPathConfigurationFile=" + xPathConfigurationFiles);

            List<HierarchicalConfiguration> configurationFileElements =
            		xmlConfig.configurationsAt("configurationFile");
            HierarchicalConfiguration hconfConfigFile = null;
           // HierarchicalConfiguration hconfJspDir = null;

            configsIterator = configurationFileElements.iterator();

            // per configurationFile element
            while (configsIterator.hasNext()) {
            	hconfConfigFile = configsIterator.next();

        		// parse filetypes and iterate over each below
        		xmlFileType = hconfConfigFile.getString("@type");
        		logr.trace("xmlFileType = " + xmlFileType);

        		// filetype's location
        		xmlFiletypeLocation = hconfConfigFile.getString("location");
        		logr.trace("xmlFiletypeLocation = " + xmlFiletypeLocation);

        		xmlXPathsConfig = new XMLConfiguration(xPathsFile);

        		/* I standardized the conversion of "web.xml" to "web-xml"
        		 * and similar for any other filetype, so we don't encounter XPath issues downstream as
        		 * in populateIgnorePackageCollection();
        		 *
        		 * Otherwise, replace the . with - in configurationFiles.xml, to match classXPaths.xml element names
        		   xmlFileType = xmlFileType.replace('.',  '-');
        		*/

         		xPathProp = xmlXPathsConfig.getProperty(xmlFileType + "." + JSP_XPATH);
        		
    		 /* Unlike ValidateConfigVsClass, we need to evaluate, 
  		   	    per jsp entry in say the web.xml, whether the particular .jsp is within a given directory OR ANOTHER;
  		   	    INVALID CONDITION is only once 1 .jsp is not in ANY of the directories
 			 */
         		List<Object> listJspDirs = hconfConfigFile.getList("jspDirectory");
        		
        		for (Object objJspDir: listJspDirs ) {        			
        			xmlJspDirectory = (String)objJspDir;
        			
        			processPerJspSubelement(xPathProp, 
        									xmlFileType, 
        									xmlFiletypeLocation, 
        									xmlJspDirectory, 
        									Utilities.JSP_FILE_EXTENSION);
        		}

            } // end while for jspConfigurationFiles.xml
        } catch (Exception e) {
            e.printStackTrace();
        } // end catch
        
    	// CHECK NOW if all the JSPs on the map object have a path
        boolean bAllJspsExist = eachJspInDirectory(mapXPathToFileLocation);
        logr.info("Every JSP matched to a directory location? " + bAllJspsExist);
        logr.info("*** PROCESSING COMPLETE ***");
    } // end main()


    private static void processPerJspSubelement(Object xPathProp,
    											String xmlFileType,
    											String xmlFiletypeLocation,
    											String rootDir,
    											String fileExtension) throws Exception {
        // nice to have when debugging
        int countXPath = -1;
        Iterator<String> xPathsIterator = null;

        String xPathValue;
        // nice to have when debugging
        boolean bFilesPerXPathExist = false;


    	// returns an ArrayList if > 1 xPath node
		logr.trace("xPathProp class = " + xPathProp.getClass().getName() );

		if (xPathProp instanceof Collection) {

			xPathPropCollection = (Collection<String>) xPathProp;
			countXPath = xPathPropCollection.size();
			logr.trace("count of xPath nodes=" + countXPath);

			xPathsIterator = xPathPropCollection.iterator();

			while (xPathsIterator.hasNext()) {
				xPathValue = xPathsIterator.next();

				// process each xPath in file
				bFilesPerXPathExist = filesPerXPathExist(xmlFileType,
																   xmlFiletypeLocation,
																   xPathValue,
																   rootDir,
																   fileExtension);
			} // end while
		} // end if Collection
		// if only 1 xPath specified
		else if (xPathProp instanceof String) {
			countXPath = 1;
			logr.trace("count of xPath nodes=" + countXPath);
			xPathValue = (String)xPathProp;
			// process the 1 xPath
			bFilesPerXPathExist = filesPerXPathExist(xmlFileType,
															   xmlFiletypeLocation,
															   xPathValue,
															   rootDir,
															   fileExtension);
		}
    }

    /** for each unique xPath,
        check WHICH of the n matching entries in the (web.xml) has a corresponding .jsp
     */
    private static boolean filesPerXPathExist(String xmlFileType,
    										  String xmlFileLocation,
    										  String xPath, String classRoot, String fileExtension)
        throws FileException, Exception {
        boolean exists = false;

       logr.info("filesPerXPathExist() arg: xmlFileType = " + xmlFileType);
        // xpath printed later

        File xmlConfigFile = null;
        XMLConfiguration xmlConfig = null;
        // String xPathValue = null;
        // int xPathValueLength = -1;

        Object xPathProp = null;

        // useful for debugging
        int countXPath = -1;

        // @SuppressWarnings("unchecked")
        Collection<String> xPathInstanceCollection = null;

        Iterator<String> xPathsIterator = null;

        try
        {

            // get the n matching xPath's in the environment xmlFileLocation
            xmlConfigFile = new File(xmlFileLocation).getAbsoluteFile();

            // TEST if exists AT ALL -
            logr.trace("*** xmlConfigFile = " + xmlConfigFile.toString() );
            boolean bExceptionIfNoFile = true;
            utils.existsAndIsFile(xmlConfigFile, xmlFileLocation, bExceptionIfNoFile);

            xmlConfig = new XMLConfiguration(xmlConfigFile);
            XPathExpressionEngine xPathEngine = new XPathExpressionEngine();

            xmlConfig.setExpressionEngine( xPathEngine );

            xPathProp = xmlConfig.getList(xPath);

            logr.debug("xPathProp=" + xPathProp);

            // returns an ArrayList if > 1 xPath node
            logr.debug("xPathProp class = " + xPathProp.getClass().getName() );


            if (xPathProp instanceof Collection) {

                xPathInstanceCollection = (Collection<String>)xPathProp;
                countXPath = xPathInstanceCollection.size();
               logr.trace("** ENTERING PER xPath-instance processing loop ***");
                xPathsIterator = xPathInstanceCollection.iterator();

                if (fileExtension.equalsIgnoreCase(Utilities.JSP_FILE_EXTENSION) )
                	exists = jspFilesProcessing(xPathsIterator, classRoot);

            } // end if Collection

      } catch (Exception e) {
            e.printStackTrace();
        }

        return exists;
    } // end filesPerXPathExist()

    /** 
     * track cases where tested-for-existence true, but file not found, in a Map object -
     * such files SHOULD BE found in some other JSP directory
     */
   private static boolean jspFilesProcessing(Iterator<String> itJspXPath, String jspRootDir) {

	   boolean bAllJspsExist = false;

	   String xPathValue = null;
	   int xPathValueLength = -1;
	   boolean bCurrentFileAndPathValid = false;
	   String mapCurrentValue = null;

   		while( itJspXPath.hasNext() ) {
   			xPathValue = itJspXPath.next();

   			// remove any leading PATH_DELIMITER, as typically in the jsp-file entries
   			if (xPathValue.charAt(0) == Utilities.PATH_DELIMITER) {
   				xPathValueLength = xPathValue.length();
   				xPathValue = xPathValue.substring(1, xPathValueLength);
   			} // end leading-delimiter if

   			if (jspFileInstanceExists(xPathValue, jspRootDir) ) {
   				// test if mapped to ""; if so, OVERWRITE that value
   				mapCurrentValue = mapXPathToFileLocation.get(xPathValue);
   				if (mapCurrentValue == null)
   					bCurrentFileAndPathValid = true;
   				else if (mapCurrentValue.isEmpty() )
   					bCurrentFileAndPathValid = true;

   				// be sure we OVERWRITE if currently the xPathValue on map set to ""
   				if (bCurrentFileAndPathValid)
   					mapXPathToFileLocation.put(xPathValue, jspRootDir);
   			}
   			else {
   				// be sure NOT ALREADY on the map with a different jspRootDir
   				mapCurrentValue = mapXPathToFileLocation.get(xPathValue);
   				
   				 /* helpful for debugging to know whether the JSP file
   				  * from the XPath operation was previously encountered
   				  */
   				if (mapCurrentValue == null)
   					mapXPathToFileLocation.put(xPathValue, "");	
   			} // end else

       } // end itJspXPath while

	   return bAllJspsExist;
   }

   /** test IF every JSP web.xml entry has a corresponding directory path; if so, return TRUE
    *
    * @param aMapXPathToFileLocation
    * @return
    */
   private static boolean eachJspInDirectory(HashMap<String, String> aMapXPathToFileLocation) {
	   boolean bEachJspInDirectory = true;

	   Set<String> theJspKeys = aMapXPathToFileLocation.keySet();
	   String valueDirectoryPath = null;

	   if (theJspKeys.size() == 0)
		   bEachJspInDirectory = false;
	   else {
		   for (String jspKey: theJspKeys) {
			   valueDirectoryPath = aMapXPathToFileLocation.get(jspKey);
			   if (valueDirectoryPath == null || valueDirectoryPath.isEmpty() ) {
				   bEachJspInDirectory = false;
				   break;
			   }
			   else {
				   logr.info("MATCHED PATH-TO-FILE: " + valueDirectoryPath + "/" + jspKey);
			   }
		   } // end for
	   } // end else

	   
	   return bEachJspInDirectory;
   }

  private static boolean jspFileInstanceExists(String path, String candidateJspRoot) {
	  boolean bFileInstanceExists = false;

      String totalPathToFile = candidateJspRoot + Utilities.PATH_DELIMITER + path;

      boolean bIsJspTest = true;
      bFileInstanceExists = utils.fileInstanceExists(path, totalPathToFile, ignorePackageCollection, bIsJspTest);
	  return bFileInstanceExists;
  }

 } // end class