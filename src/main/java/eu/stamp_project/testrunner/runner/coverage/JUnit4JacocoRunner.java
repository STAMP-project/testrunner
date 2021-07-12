package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResult;
import eu.stamp_project.testrunner.listener.junit4.JUnit4Coverage;
import eu.stamp_project.testrunner.listener.junit4.JUnit4TestResult;
import eu.stamp_project.testrunner.runner.JUnit4Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;

import java.util.List;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 11/04/19
 */
public class JUnit4JacocoRunner extends JacocoRunner {

    public JUnit4JacocoRunner(List<String> classesDirectory, List<String> testClassesDirectory, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, coverageTransformer);
    }
    
    public JUnit4JacocoRunner(List<String> classesDirectory, List<String> testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
    }

    /**
     * The entry method to execute junit tests.
     * This method is not meant to be used directly, but rather using {@link EntryPoint}
     * For the expected arguments, see {@link ParserOptions}
     */
    public static void main(String[] args) {
        final ParserOptions options = ParserOptions.parse(args);
        final JacocoRunner jacocoRunner =
                new JUnit4JacocoRunner(
                        options.getPathToCompiledClassesOfTheProject(),
                        options.getPathToCompiledTestClassesOfTheProject(),
                        options.getBlackList(),
                        options.getCoverageTransformer()
                );
        final String[] testClassesToRun = options.getFullQualifiedNameOfTestClassesToRun();
        if (testClassesToRun.length > 1) {
            jacocoRunner.run(
                    options.getPathToCompiledClassesOfTheProject(),
                    options.getPathToCompiledTestClassesOfTheProject(),
                    testClassesToRun
            ).save();
        } else {
            if (options.getTestMethodNamesToRun().length == 0) {
                jacocoRunner.run(
                        options.getPathToCompiledClassesOfTheProject(),
                        options.getPathToCompiledTestClassesOfTheProject(),
                        testClassesToRun
                ).save();
            } else {
                jacocoRunner.run(
                        options.getPathToCompiledClassesOfTheProject(),
                        options.getPathToCompiledTestClassesOfTheProject(),
                        testClassesToRun[0],
                        options.getTestMethodNamesToRun()
                ).save();
            }
        }
    }

    @Override
    protected CoveredTestResult executeTest(String[] testClassNames,
											String[] testMethodNames,
											List<String> blackList) {
        final CoveredTestResult listener = new JUnit4Coverage();
        JUnit4Runner.run(
                testClassNames,
                testMethodNames,
                blackList,
                (JUnit4TestResult) listener,
                this.instrumentedClassLoader
        );
        return listener;
    }
 
}
