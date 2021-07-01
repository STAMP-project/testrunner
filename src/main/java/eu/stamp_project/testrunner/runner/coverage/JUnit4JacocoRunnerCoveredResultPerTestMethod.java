package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.junit4.CoveredTestResultsPerJUnit4TestMethod;
import eu.stamp_project.testrunner.listener.junit4.JUnit4TestResult;
import eu.stamp_project.testrunner.runner.JUnit4Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.jacoco.core.runtime.RuntimeData;

import java.util.Collections;
import java.util.List;

/**
 * JUnit4 implementation of {@link JacocoRunnerCoveredResultPerTestMethod}.
 *
 * @author andre15silva
 */
public class JUnit4JacocoRunnerCoveredResultPerTestMethod extends JacocoRunnerCoveredResultPerTestMethod {

	public JUnit4JacocoRunnerCoveredResultPerTestMethod(String classesDirectory, String testClassesDirectory, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, coverageTransformer);
	}

	public JUnit4JacocoRunnerCoveredResultPerTestMethod(String classesDirectory, String testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
	}

	@Override
	protected CoveredTestResultPerTestMethod executeCoveredTestPerTestMethod(RuntimeData data, String classesDirectory, String[] testClassNames, String[] testMethodNames) {
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
		final String[] splittedArgs0 = options.getPathToCompiledClassesOfTheProject().split(ConstantsHelper.PATH_SEPARATOR);
		final String classesDirectory = options.isCoverTests() ? options.getPathToCompiledClassesOfTheProject() : splittedArgs0[0];
		final String testClassesDirectory = splittedArgs0[1];
		new JUnit4JacocoRunnerCoveredResultPerTestMethod(
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
