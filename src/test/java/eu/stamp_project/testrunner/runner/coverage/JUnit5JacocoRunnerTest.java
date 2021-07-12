package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.AbstractTest;
import eu.stamp_project.testrunner.EntryPointTest;
import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.impl.CoverageImpl;
import eu.stamp_project.testrunner.runner.JUnit4Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 19/12/17
 */
public class JUnit5JacocoRunnerTest extends AbstractTest {

    @Test
    public void testWithoutNewJvmOnTestClass() throws Exception {

        /*
            Using the api to compute the coverage on a test class
         */

        JUnit5JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "junit5.TestSuiteExample",
                }
        );
        final Coverage load = CoverageImpl.load();
        assertEquals(30, load.getInstructionsCovered());
        assertEquals(EntryPointTest.NUMBER_OF_INSTRUCTIONS, load.getInstructionsTotal());
        for (String expectedExecutionPath : expectedExecutionPaths) {
            assertTrue(load.getExecutionPath().contains(expectedExecutionPath));
        }
    }

    private static final String[] expectedExecutionPaths = {
            "tobemocked/LoginDao:0,0",
            "tobemocked/LoginController:0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "tobemocked/LoginService:0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "example/Example:2,0,0,4,4,0,7,2,0,2,5,1,0,3",
            "tobemocked/UserForm:0,0"
    };

    @Test
    public void testWithoutNewJvmOnTestCases() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

        JUnit5JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "junit5.TestSuiteExample",
                        ParserOptions.FLAG_testMethodNamesToRun, "test8:test2"
                }
        );
        final Coverage load = CoverageImpl.load();
        assertEquals(23, load.getInstructionsCovered());
        assertEquals(EntryPointTest.NUMBER_OF_INSTRUCTIONS, load.getInstructionsTotal());
    }

    @Ignore
    @Test
    public void testWithoutNewJvmOnTestCasesOnParametrized() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

        JUnit5JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "junit5.ParametrizedTest",
                        ParserOptions.FLAG_testMethodNamesToRun, "test"
                }
        );
        final Coverage load = CoverageImpl.load();
        assertEquals(23, load.getInstructionsCovered());
        assertEquals(EntryPointTest.NUMBER_OF_INSTRUCTIONS, load.getInstructionsTotal());
    }


    private static final String[] expectedMethodDetailedExecutionPaths = {
            "tobemocked/LoginDao:<init>+()V+0|login+(Ltobemocked/UserForm;)I+0",
            "tobemocked/LoginController:<init>+()V+0|login+(Ltobemocked/UserForm;)Ljava/lang/String;+0,0,0,0,0,0,0,0,0,0,0,0,0,0",
            "tobemocked/LoginService:<init>+()V+0|login+(Ltobemocked/UserForm;)Z+0,0,0,0,0,0,0|setCurrentUser+(Ljava/lang/String;)V+0,0,0,0|setLoginDao+(Ltobemocked/LoginDao;)V+0,0",
            "example/Example:charAt+(Ljava/lang/String;I)C+2,0,0,4,4,0,7|<init>+()V+2,0,2,5,1,0,3",
            "tobemocked/UserForm:<init>+()V+0|getUsername+()Ljava/lang/String;+0-"
    };


    @Test
    public void testMethodDetailedCoverageDetail() throws Exception {

        JUnit5JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "junit5.TestSuiteExample",
                        ParserOptions.FLAG_coverage_detail, ParserOptions.CoverageTransformerDetail.METHOD_DETAIL.name(),
                }
        );
        final Coverage load = CoverageImpl.load();
        for (String expectedMethodDetailedExecutionPath : expectedMethodDetailedExecutionPaths) {
            assertTrue(load.getExecutionPath().contains(expectedMethodDetailedExecutionPath));
        }
    }

}
