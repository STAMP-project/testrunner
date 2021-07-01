package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.testrunner.listener.CoveragePerTestMethod;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.junit4.CoveragePerJUnit4TestMethod;
import eu.stamp_project.testrunner.listener.junit4.JUnit4TestResult;
import eu.stamp_project.testrunner.runner.JUnit4Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.jacoco.core.runtime.RuntimeData;

import java.util.Collections;
import java.util.List;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 11/04/19
 */
public class JUnit4JacocoRunnerPerTestMethod extends JacocoRunnerPerTestMethod {

    public JUnit4JacocoRunnerPerTestMethod(String classesDirectory, String testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
    }

    @Override
    protected CoveragePerTestMethod executeTestPerTestMethod(RuntimeData data,
                                                             String classesDirectory,
                                                             String[] testClassNames,
                                                             String[] testMethodNames) {
        final CoveragePerTestMethod listener = new CoveragePerJUnit4TestMethod(data, classesDirectory, coverageTransformer);
        JUnit4Runner.run(
                testClassNames,
                testMethodNames,
                Collections.emptyList(),
                (JUnit4TestResult) listener,
                this.instrumentedClassLoader
        );
        return listener;
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
        new JUnit4JacocoRunnerPerTestMethod(
                classesDirectory,
                testClassesDirectory,
                options.getBlackList(),
                options.getCoverageTransformer()
        ).runCoveragePerTestMethod(classesDirectory,
                testClassesDirectory,
                options.getFullQualifiedNameOfTestClassesToRun()[0],
                options.getTestMethodNamesToRun()
        ).save();
    }
}
