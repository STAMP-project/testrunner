package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResult;
import eu.stamp_project.testrunner.listener.junit5.JUnit5Coverage;
import eu.stamp_project.testrunner.runner.JUnit5Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.utils.ConstantsHelper;

import java.util.List;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 11/04/19
 */
public class JUnit5JacocoRunner extends JacocoRunner {

	
    public JUnit5JacocoRunner(String classesDirectory, String testClassesDirectory, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, coverageTransformer);
    }
    
    public JUnit5JacocoRunner(String classesDirectory, String testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);;
    }

    
    
    /**
     * The entry method to execute junit tests.
     * This method is not meant to be used directly, but rather using {@link EntryPoint}
     * For the expected arguments, see {@link ParserOptions}
     */
    public static void main(String[] args) {
        final ParserOptions options = ParserOptions.parse(args);
        final String[] splittedArgs0 = options.getPathToCompiledClassesOfTheProject().split(ConstantsHelper.PATH_SEPARATOR);
        final String classesDirectory = options.isCoverTests() ? options.getPathToCompiledClassesOfTheProject() : splittedArgs0[0];
        final String testClassesDirectory = splittedArgs0[1];
        final JacocoRunner jacocoRunner =
                new JUnit5JacocoRunner(classesDirectory,
                        testClassesDirectory,
                        options.getBlackList(),
                        options.getCoverageTransformer()
                );
        final String[] testClassesToRun = options.getFullQualifiedNameOfTestClassesToRun();
        if (testClassesToRun.length > 1) {
            jacocoRunner.run(classesDirectory,
                    testClassesDirectory,
                    testClassesToRun
            ).save();
        } else {
            if (options.getTestMethodNamesToRun().length == 0) {
                jacocoRunner.run(classesDirectory,
                        testClassesDirectory,
                        testClassesToRun
                ).save();
            } else {
                jacocoRunner.run(classesDirectory,
                        testClassesDirectory,
                        testClassesToRun[0],
                        options.getTestMethodNamesToRun()
                ).save();
            }
        }
    }

    @Override
    protected CoveredTestResult executeTest(String[] testClassNames, String[] testMethodNames, List<String> blackList) {
        final JUnit5Coverage listener = new JUnit5Coverage();
        JUnit5Runner.run(
                testClassNames,
                testMethodNames,
                blackList,
                listener,
                this.instrumentedClassLoader
        );
        return listener;
    }
    
}
