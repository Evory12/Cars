# Instructions

Here is the information on how to build and run my project.

To build a project you need to use Maven. 
Install it and run following cmd command from the folder with pom.xml for my project.
mvn clean package

To launch project from cmd after Maven build you need to use the command like:
java -jar target/carsselection-1.0-SNAPSHOT-jar-with-dependencies.jar

I'm launching it normally from IntelliJ IDEA + maven.
How to do it:
1. Build a project (look above)
2. Open IntelliJ IDEA.
3. Open src/main/java/Test/Cars.java
4. On Line 19 - public class Cars - press Green arrow and choose Run 'Cars.main()'
