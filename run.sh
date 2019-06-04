# builds successfully on Ubuntu using OpenJDK 8
# appends to log.html per the log4j.xml
# command-line output
#  
# DEPENDENCIES COPIED via maven build and its pom.xml to target/
#  ~/.m2/repository/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar
#  ~/.m2/repository/commons-configuration/commons-configuration/commons-configuration-1.10.jar
#  ~/.m2/repository/commons-jxpath/commons-jxpath/commons-jxpath-1.3.jar  
#  ~/.m2/repository/commons-lang/commons-lang/commons-lang-2.6.jar
#  ~/.m2/repository/commons-logging/commons-logging/commons-logging-1.2.jar  
#  ~/.m2/repository/log4j/log4j/log4j-1.2.12.jar
#  ~/.m2/repository/org/jdom/jdom2/2.0.6/jdom2-2.0.6.jar
#
java -jar target/ValidateConfigAgainstJsps.jar jspXPaths.xml jspConfigurationFiles.xml