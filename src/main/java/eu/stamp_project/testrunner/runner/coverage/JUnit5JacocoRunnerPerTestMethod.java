package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.testrunner.listener.CoveragePerTestMethod;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.junit5.CoveragePerJUnit5TestMethod;
import eu.stamp_project.testrunner.runner.JUnit5Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import org.jacoco.core.runtime.RuntimeData;

import java.util.Collections;
import java.util.List;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 11/04/19
 */
public class JUnit5JacocoRunnerPerTestMethod extends JacocoRunnerPerTestMethod {

    public JUnit5JacocoRunnerPerTestMethod(List<String> classesDirectory, List<String> testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
    }

    @Override
    protected CoveragePerTestMethod executeTestPerTestMethod(RuntimeData data,
                                                             List<String> classesDirectory,
                                                             String[] testClassNames,
                                                             String[] testMethodNames) {
        final CoveragePerJUnit5TestMethod listener = new CoveragePerJUnit5TestMethod(data, classesDirectory, coverageTransformer);
        JUnit5Runner.run(
                testClassNames,
                testMethodNames,
                Collections.emptyList(),
                listener,
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
        new JUnit5JacocoRunnerPerTestMethod(
                options.getPathToCompiledClassesOfTheProject(),
                options.getPathToCompiledTestClassesOfTheProject(),
                options.getBlackList(),
                options.getCoverageTransformer()
        ).runCoveragePerTestMethod(
                options.getPathToCompiledClassesOfTheProject(),
                options.getPathToCompiledTestClassesOfTheProject(),
                options.getFullQualifiedNameOfTestClassesToRun(),
                options.getTestMethodNamesToRun()
        ).save();
    }
}
