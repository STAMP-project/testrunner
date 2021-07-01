package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.junit5.CoveredTestResultsPerJUnit5TestMethod;
import eu.stamp_project.testrunner.listener.junit5.JUnit5TestResult;
import eu.stamp_project.testrunner.runner.JUnit5Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.jacoco.core.runtime.RuntimeData;

import java.util.Collections;
import java.util.List;

/**
 * JUnit5 implementation of {@link JacocoRunnerCoveredResultPerTestMethod}.
 *
 * @author andre15silva
 */
public class JUnit5JacocoRunnerCoveredResultPerTestMethod extends JacocoRunnerCoveredResultPerTestMethod {

	public JUnit5JacocoRunnerCoveredResultPerTestMethod(String classesDirectory, String testClassesDirectory, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, coverageTransformer);
	}

	public JUnit5JacocoRunnerCoveredResultPerTestMethod(String classesDirectory, String testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
	}

	@Override
	protected CoveredTestResultPerTestMethod executeCoveredTestPerTestMethod(RuntimeData data, String classesDirectory, String[] testClassNames, String[] testMethodNames) {
		final CoveredTestResultsPerJUnit5TestMethod listener = new CoveredTestResultsPerJUnit5TestMethod(data, classesDirectory, coverageTransformer);
		JUnit5Runner.run(
				testClassNames,
				testMethodNames,
				Collections.emptyList(),
				(JUnit5TestResult) listener,
				this.instrumentedClassLoader
		);
		return listener;
	}

	public static void main(String[] args) {
		final ParserOptions options = ParserOptions.parse(args);
		final String[] splittedArgs0 = options.getPathToCompiledClassesOfTheProject().split(ConstantsHelper.PATH_SEPARATOR);
		final String classesDirectory = options.isCoverTests() ? options.getPathToCompiledClassesOfTheProject() : splittedArgs0[0];
		final String testClassesDirectory = splittedArgs0[1];
		new JUnit5JacocoRunnerCoveredResultPerTestMethod(
				classesDirectory,
				testClassesDirectory,
				options.getBlackList(),
				options.getCoverageTransformer()
		).runCoveredTestResultPerTestMethod(classesDirectory,
				testClassesDirectory,
				options.getFullQualifiedNameOfTestClassesToRun()[0],
				options.getTestMethodNamesToRun()
		).save();
	}

}
