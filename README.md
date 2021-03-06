# validateConfigurationAgainstJsps
Check that JSPs (JavaServer Pages) files referenced in XML configuration files such as the web.xml exist in specified JSP directories. The motivations behind this tool are explained best at https://bit.ly/2zYGL1M
However, for this sibling tool to the validateConfigurationAgainstBuild, the validation is applied to JSP files,
and it can be called as part of an Ant, maven, or Gradle-administered build process.

Note that each directory which should contain JSPs should be specified in the jspConfigurationFiles.xml, since
by design, no recursion of the directory tree is done. Then specify the XPaths to return JSP file names from your config file (such
as the JavaEE web.xml) within the jspXPaths.xml.

The code was built and tested against OpenJDK 8 on Ubuntu and JDK 10 on Windows.

Also note that using maven, one can run the build from the included build.bat (tested on Windows) or build.sh (tested on Ubuntu).
Maven creates a target subdirectory including the resulting jar, which includes .class files and log4j.xml, and dependent libraries are copied to the target/lib directory. Then use the platform-specific run script to execute the provided example validation. 
(The output below was generated by placing the tekniqul/WeatherDataApp project in a parallel directory to the root of this project   locally, using the given tool configuration files jspXPaths.xml and jspConfigurationFiles.xml)

Each validated file will be noted in the console output (and appended to the log.html) similar to:
MATCHED PATH-TO-FILE: ../WeatherDataApp_WAR/InputValues.jsp
MATCHED PATH-TO-FILE: ../WeatherDataApp_WAR/WEB-INF/Results.jsp

If EVERY JSP file was found within the configured directories, 
the resulting line with appear in the console output 2nd line from its end, ending with "true":
Every JSP matched to a directory location? true

Remember that the failure to find one or more JSP files could be from: 
--incorrect configuration of this tool 
--incorrect spelling of the JSP filename(s) 
--incorrect spelling of one or more JSP directories
--missing JSP file(s) 
--in some operating systems, read-only status of build file(s)

and possibly other causes. The tool's console output should make it simple to identify the resulting error, if any.

By identifying improper or missing configuration or JSP files in JavaEE projects, build personnel can prevent related runtime errors, thereby saving valuable time and resources at deployment.
