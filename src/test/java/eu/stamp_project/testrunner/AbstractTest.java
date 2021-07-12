package eu.stamp_project.testrunner;

import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import spoon.Launcher;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 19/12/17
 */
public class AbstractTest {
    public static String MAVEN_HOME;

    public static String SOURCE_PROJECT_CLASSES;

    public static String TEST_PROJECT_CLASSES;

    public static String JUNIT_CP;

    public static String EASYMOCK_CP;

    public static String JUNIT5_CP;

    {
        List<String> classPath = Arrays.stream(((URLClassLoader) URLClassLoader.getSystemClassLoader()).getURLs())
                .map(URL::getPath).collect(Collectors.toList());

        System.out.println(classPath);
        if (classPath.size() == 1) {
            // we only have the surefire booter
            // so we take a value by default
            MAVEN_HOME = System.getProperty("user.home") + "/.m2/repository/";
        } else {
            // we infer it from the classpath
            MAVEN_HOME = classPath.stream()
                    .filter(path -> path.contains("/.m2/repository/"))
                    .findFirst()
                    .map(s -> s.substring(0, s.indexOf("/.m2/repository/") + "/.m2/repository/".length()))
                    .get();
        }

        SOURCE_PROJECT_CLASSES = "src/test/resources/test-projects/target/classes/";
        TEST_PROJECT_CLASSES = "src/test/resources/test-projects/target/test-classes/";
        JUNIT_CP = MAVEN_HOME + "junit/junit/4.12/junit-4.12.jar" + ConstantsHelper.PATH_SEPARATOR
                + MAVEN_HOME + "org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar";
        EASYMOCK_CP = MAVEN_HOME + "org/easymock/easymock/3.4/easymock-3.4.jar" + ConstantsHelper.PATH_SEPARATOR
                + MAVEN_HOME + "org/objenesis/objenesis/2.2/objenesis-2.2.jar";
        JUNIT5_CP =
                MAVEN_HOME + "org/junit/jupiter/junit-jupiter-api/5.3.2/junit-jupiter-api-5.3.2.jar" + ConstantsHelper.PATH_SEPARATOR
                        + MAVEN_HOME + "org/apiguardian/apiguardian-api/1.0.0/apiguardian-api-1.0.0.jar" + ConstantsHelper.PATH_SEPARATOR
                        + MAVEN_HOME + "org/opentest4j/opentest4j/1.1.1/opentest4j-1.1.1.jar" + ConstantsHelper.PATH_SEPARATOR
                        + MAVEN_HOME + "org/junit/platform/junit-platform-commons/1.3.2/junit-platform-commons-1.3.2.jar" + ConstantsHelper.PATH_SEPARATOR
                        + MAVEN_HOME + "org/junit/jupiter/junit-jupiter-engine/5.3.2/junit-jupiter-engine-5.3.2.jar" + ConstantsHelper.PATH_SEPARATOR
                        + MAVEN_HOME + "org/junit/jupiter/junit-jupiter-params/5.3.2/junit-jupiter-params-5.3.2.jar" + ConstantsHelper.PATH_SEPARATOR
                        + MAVEN_HOME + "org/junit/platform/junit-platform-engine/1.3.2/junit-platform-engine-1.3.2.jar" + ConstantsHelper.PATH_SEPARATOR
                        + MAVEN_HOME + "org/junit/platform/junit-platform-launcher/1.3.2/junit-platform-launcher-1.3.2.jar";
    }

    public static final String PATH_TO_RUNNER_CLASSES = "src/main/resources/runner-classes/";

    public static final String nl = System.getProperty("line.separator");

    @Before
    public void preparation() throws Exception {

        // make sure you run this from Java 8 otherwise the classloader is not the expected one
        EntryPoint.verbose = true;
        // create folders
        final File target = new File("src/test/resources/test-projects/target");
        if (target.exists()) {
            FileUtils.forceDelete(target);
        }
        target.mkdir();
        final File classes = new File("src/test/resources/test-projects/target/classes");
        classes.mkdir();
        final File testClasses = new File("src/test/resources/test-projects/target/test-classes");
        testClasses.mkdir();
        String command;

        // compiling with spoon
        Launcher l = new Launcher();
        l.setBinaryOutputDirectory("src/test/resources/test-projects/target/classes/");
        l.addInputResource("src/test/resources/test-projects/src/main/java/example/Example.java");
        l.addInputResource("src/test/resources/test-projects/src/main/java/tobemocked/LoginController.java");
        l.addInputResource("src/test/resources/test-projects/src/main/java/tobemocked/LoginDao.java");
        l.addInputResource("src/test/resources/test-projects/src/main/java/tobemocked/LoginService.java");
        l.addInputResource("src/test/resources/test-projects/src/main/java/tobemocked/UserForm.java");
        l.getEnvironment().setShouldCompile(true);
        l.run(); // compile code;

        // now we compile the test code
        l = new Launcher();
        l.setBinaryOutputDirectory("src/test/resources/test-projects/target/test-classes/");
        l.getEnvironment().setShouldCompile(true);
        List<String> classpath = new ArrayList<>();
        classpath.add("src/test/resources/test-projects/target/classes/");
        classpath.addAll(Arrays.asList(JUNIT_CP.split(ConstantsHelper.PATH_SEPARATOR)));
        classpath.addAll(Arrays.asList(EASYMOCK_CP.split(ConstantsHelper.PATH_SEPARATOR)));
        classpath.addAll(Arrays.asList(JUNIT5_CP.split(ConstantsHelper.PATH_SEPARATOR)));
        l.getEnvironment().setSourceClasspath(classpath.toArray(new String[0]));
        l.addInputResource("src/test/resources/test-projects/src/test/java/example/TestSuiteExample.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/example/ParametrizedTestSuiteExample.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/example/ParametrizedTest.java");
        l.addInputResource(                "src/test/resources/test-projects/src/test/java/junit5/ParametrizedTest.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/failing/FailingTestClass.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/example/TestSuiteExample2.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/easymock/LoginControllerIntegrationTest.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/junit5/TestSuiteExample.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/junit5/FailingTestClass.java");
        l.addInputResource("src/test/resources/test-projects/src/test/java/junit5/TestSuiteExample2.java");
        l.run(); // compile tests
    }

    @AfterClass
    public static void afterClass() throws Exception {
        final File target = new File("src/test/resources/test-projects/target");
        if (target.exists()) {
            FileUtils.forceDelete(target);
        }
    }

}
