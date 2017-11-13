# How to run tests

Requirements:
Java JDK 9
Maven

1) Build jar file and move it to the top level folder.

    > mvn package
    > mv ./target/javassist*-GA.jar ./javaassist.jar

2) Check that ./src/test/javassist/JvstTestRoot.PATH and .JAR_PATH point to the compiled jar file.
   The default is "../../".

3) Run Tests

    > mvn test
    > mvn surefire:test
