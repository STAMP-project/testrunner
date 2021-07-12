package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.listener.CoveragePerTestMethod;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.TestResult;
import eu.stamp_project.testrunner.runner.Failure;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.apache.commons.io.IOUtils;
import org.jacoco.core.runtime.RuntimeData;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.ResourceBundle.clearCache;

/**
 * Runs {@link JacocoRunnerPerTestMethod} but with a {@link CoveredTestResultPerTestMethod} listener
 *
 * @author andre15silva
 */
public abstract class JacocoRunnerCoveredResultPerTestMethod extends JacocoRunnerPerTestMethod {


	public JacocoRunnerCoveredResultPerTestMethod(List<String> classesDirectory, List<String> testClassesDirectory, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, coverageTransformer);
	}

	public JacocoRunnerCoveredResultPerTestMethod(List<String> classesDirectory, List<String> testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
		super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
	}

	public CoveredTestResultPerTestMethod runCoveredTestResultPerTestMethod(List<String> classesDirectory,
	                                                                        List<String> testClassesDirectory,
	                                                                        String[] testClassNames,
	                                                                        String[] testMethodNames) {
		final RuntimeData data = new RuntimeData();
		URLClassLoader classLoader;
		URL[] dirs = testClassesDirectory.stream()
				.map(x -> {
					try {
						return new File(x).toURI().toURL();
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				})
				.toArray(URL[]::new);
		classLoader = new URLClassLoader(dirs, this.instrumentedClassLoader);

		try {
			// TODO: I'm not sure this for loop is doing anything
			for (String fullyQualifiedClassName : testClassNames) {
				String resource = ConstantsHelper.fullQualifiedNameToPath.apply(fullyQualifiedClassName) + ".class";
				this.instrumentedClassLoader.addDefinition(
						fullyQualifiedClassName,
						IOUtils.toByteArray(classLoader.getResourceAsStream(resource))
				);
			}
			this.runtime.startup(data);
			final CoveredTestResultPerTestMethod listener = this.executeCoveredTestPerTestMethod(data, classesDirectory, testClassNames, testMethodNames);
			if (!((TestResult) listener).getFailingTests().isEmpty()) {
				System.err.println("Some test(s) failed during computation of coverage:\n" +
						((TestResult) listener).getFailingTests()
								.stream()
								.map(Failure::toString)
								.collect(Collectors.joining("\n"))
				);
			}
			this.runtime.shutdown();
			clearCache(this.instrumentedClassLoader);
			return listener;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected CoveragePerTestMethod executeTestPerTestMethod(RuntimeData data, List<String> classesDirectory, String[] testClassNames, String[] testMethodNames) {
		throw new UnsupportedOperationException();
	}

	protected abstract CoveredTestResultPerTestMethod executeCoveredTestPerTestMethod(RuntimeData data,
	                                                                                  List<String> classesDirectory,
	                                                                                  String[] testClassNames,
	                                                                                  String[] testMethodNames);

}