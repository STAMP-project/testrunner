package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.AbstractTest;
import eu.stamp_project.testrunner.EntryPointTest;
import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoveredTestResult;
import eu.stamp_project.testrunner.listener.impl.CoverageCollectorDetailed;
import eu.stamp_project.testrunner.listener.impl.CoverageDetailed;
import eu.stamp_project.testrunner.listener.impl.CoverageImpl;
import eu.stamp_project.testrunner.runner.JUnit4Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.junit.Test;

import java.net.URLClassLoader;

import static org.junit.Assert.*;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 19/12/17
 */
public class JacocoRunnerTest extends AbstractTest {

    @Test
    public void testWithoutNewJvmOnTestClass() throws Exception {

        /*
            Using the api to compute the coverage on a test class
         */

        JUnit4JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample"
                }
        );
        final Coverage load = CoverageImpl.load();
        assertEquals(30, load.getInstructionsCovered());
        assertEquals(EntryPointTest.NUMBER_OF_INSTRUCTIONS, load.getInstructionsTotal());
        System.out.println(String.join(ConstantsHelper.LINE_SEPARATOR, load.getExecutionPath().split(";")));
        System.out.println(load.getInstructionsCovered());
    }

    @Test
    public void testWithoutNewJvmOnTestCases() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

        JUnit4JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample",
                        ParserOptions.FLAG_testMethodNamesToRun, "test8:test2"
                }
        );
        final Coverage load = CoverageImpl.load();
        assertEquals(23, load.getInstructionsCovered());
        assertEquals(EntryPointTest.NUMBER_OF_INSTRUCTIONS, load.getInstructionsTotal());
        System.out.println(load.getExecutionPath());
    }

    @Test
    public void testRunCoveredTestResults() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

        JacocoRunner runner = new JUnit4JacocoRunner(
                "src/test/resources/test-projects/target/classes/",
                "src/test/resources/test-projects/target/test-classes/",
                new CoverageCollectorDetailed()
        );
        URLClassLoader urlloader = runner.getUrlClassloaderFromClassPath(JUNIT_CP);
        runner.instrumentAll("src/test/resources/test-projects/target/test-classes/");

        CoveredTestResult coveredTestResult = runner.run(
                new CoverageCollectorDetailed(),
                urlloader,
                "src/test/resources/test-projects/target/classes/",
                "src/test/resources/test-projects/target/test-classes/",
                "example.TestSuiteExample",
                true,
                "test8"
        );

        // Assert test results
        assertEquals(1, coveredTestResult.getRunningTests().size());
        assertEquals(1, coveredTestResult.getPassingTests().size());
        assertEquals(0, coveredTestResult.getFailingTests().size());
        assertEquals(0, coveredTestResult.getIgnoredTests().size());

        // Assert coverage of test class
        CoverageDetailed covLine = (CoverageDetailed) coveredTestResult.getCoverageInformation();
        System.out.println(covLine);
        assertEquals(3, covLine.getDetailedCoverage().get("example/TestSuiteExample").getCov().get(2).intValue());
    }

}
