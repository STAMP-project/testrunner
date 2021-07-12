package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.junit4.CoveredTestResultsPerJUnit4TestMethod;
import eu.stamp_project.testrunner.listener.junit4.JUnit4TestResult;
import eu.stamp_project.testrunner.runner.JUnit4Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import org.jacoco.core.runtime.RuntimeData;

import java.util.Collections;
import java.util.List;

/**
 * JUnit4 implementation of {@link JacocoRunnerCoveredResultPerTestMethod}.
 *
 * @author andre15silva
 */
public class JUnit4JacocoRunnerCoveredResultPerTestMethod extends JacocoRunnerCoveredResultPerTestMethod {

	public JUnit4JacocoRunnerCoveredResultPerTestMethod(List<String> classesDirectory, List<String> testClassesDirectory, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, coverageTransformer);
	}

	public JUnit4JacocoRunnerCoveredResultPerTestMethod(List<String> classesDirectory, List<String> testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
	}

	@Override
	protected CoveredTestResultPerTestMethod executeCoveredTestPerTestMethod(RuntimeData data, List<String> classesDirectory, String[] testClassNames, String[] testMethodNames) {
		final CoveredTestResultsPerJUnit4TestMethod listener = new CoveredTestResultsPerJUnit4TestMethod(data, classesDirectory, coverageTransformer);
		JUnit4Runner.run(
				testClassNames,
				testMethodNames,
				Collections.emptyList(),
				(JUnit4TestResult) listener,
				this.instrumentedClassLoader
		);
		return listener;
	}

	public static void main(String[] args) {
		final ParserOptions options = ParserOptions.parse(args);
		new JUnit4JacocoRunnerCoveredResultPerTestMethod(
				options.getPathToCompiledClassesOfTheProject(),
				options.getPathToCompiledTestClassesOfTheProject(),
				options.getBlackList(),
				options.getCoverageTransformer()
		).runCoveredTestResultPerTestMethod(
				options.getPathToCompiledClassesOfTheProject(),
				options.getPathToCompiledTestClassesOfTheProject(),
				options.getFullQualifiedNameOfTestClassesToRun(),
				options.getTestMethodNamesToRun()
		).save();
	}

}
