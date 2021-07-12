package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.AbstractTest;
import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.impl.CoverageImpl;
import eu.stamp_project.testrunner.listener.impl.CoveragePerTestMethodImpl;
import eu.stamp_project.testrunner.runner.ParserOptions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 19/12/17
 */
public class JacocoRunnerPerTestMethodTest extends AbstractTest {

    @Test
    public void testWithoutNewJvmOnTestClassParametrized() throws Exception {

        /*
            Using the api to compute the coverage on a test class
         */

        JUnit4JacocoRunnerPerTestMethod.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.ParametrizedTest",
                        ParserOptions.FLAG_testMethodNamesToRun, "test"
                }
        );
        final CoveragePerTestMethodImpl load = CoveragePerTestMethodImpl.load();
        System.out.println(load);
        assertEquals(34, load.getCoverageResultsMap().get("example.ParametrizedTest#test").getInstructionsCovered());
        System.out.println(load.getCoverageResultsMap().get("example.ParametrizedTest#test").getExecutionPath());
    }

    @Test
    public void testWithoutNewJvmOnTestCases() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

        JUnit4JacocoRunnerPerTestMethod.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample",
                        ParserOptions.FLAG_testMethodNamesToRun, "test3:test2:copyOftest2"
                }
        );
        final CoveragePerTestMethodImpl load = CoveragePerTestMethodImpl.load();
        System.out.println(load);
        assertEquals(23, load.getCoverageResultsMap().get("example.TestSuiteExample#test2").getInstructionsCovered());
        assertEquals(23, load.getCoverageResultsMap().get("example.TestSuiteExample#test3").getInstructionsCovered());
        assertEquals(23, load.getCoverageResultsMap().get("example.TestSuiteExample#copyOftest2").getInstructionsCovered());
        System.out.println(load.getCoverageResultsMap().get("example.TestSuiteExample#test2").getExecutionPath());
        System.out.println(load.getCoverageResultsMap().get("example.TestSuiteExample#copyOftest2").getExecutionPath());
        System.out.println(load.getCoverageResultsMap().get("example.TestSuiteExample#test3").getExecutionPath());
    }

    @Test
    public void testWithoutNewJvmOnTestClassAll() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

        JUnit4JacocoRunnerPerTestMethod.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample",
                }
        );
        final CoveragePerTestMethodImpl load = CoveragePerTestMethodImpl.load();
        System.out.println(load);
        load.getCoverageResultsMap()
                .keySet()
                .stream()
                .map(s -> load.getCoverageResultsMap().get(s).getExecutionPath())
                .forEach(System.out::println);
    }
}