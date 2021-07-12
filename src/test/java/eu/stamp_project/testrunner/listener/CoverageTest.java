package eu.stamp_project.testrunner.listener;

import eu.stamp_project.testrunner.AbstractTest;
import eu.stamp_project.testrunner.listener.impl.CoverageImpl;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.runner.coverage.JUnit4JacocoRunner;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CoverageTest extends AbstractTest {

    @Test
    public void compareTwoCoverages() throws Exception {

        /*
            Using the api to compute the coverage on a test class
         */

        JUnit4JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample",
                        ParserOptions.FLAG_testMethodNamesToRun, "test4"
                }
        );
        final Coverage test4Coverage = CoverageImpl.load();

        JUnit4JacocoRunner.main(new String[]{
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample",
                        ParserOptions.FLAG_testMethodNamesToRun, "test8"
                }
        );
        final Coverage test8Coverage = CoverageImpl.load();

        assertTrue(test4Coverage.getInstructionsCovered() > test8Coverage.getInstructionsCovered());
        assertEquals(test4Coverage.getInstructionsTotal(),test8Coverage.getInstructionsTotal());
        assertTrue(test4Coverage.isBetterThan(test8Coverage));

        System.out.println("------- TEST 4 COVERAGE -------");
        System.out.println(String.join(ConstantsHelper.LINE_SEPARATOR, test4Coverage.getExecutionPath().split(";")));

        System.out.println("------- TEST 8 COVERAGE -------");
        System.out.println(String.join(ConstantsHelper.LINE_SEPARATOR, test8Coverage.getExecutionPath().split(";")));
    }
}
