package eu.stamp_project.testrunner.listener.junit5;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.impl.CoveredTestResultPerTestMethodImpl;
import eu.stamp_project.testrunner.runner.Failure;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JUnit5 implementation of {@link CoveredTestResultPerTestMethod}
 *
 * @author andre15silva
 */
public class CoveredTestResultsPerJUnit5TestMethod extends JUnit5TestResult implements CoveredTestResultPerTestMethod {

	private static final long serialVersionUID = 5003707306546430948L;

	private CoveredTestResultPerTestMethodImpl internalCoveredTestResult;

	public CoveredTestResultsPerJUnit5TestMethod(RuntimeData data, List<String> classesDirectory, CoverageTransformer coverageTransformer) {
		this.internalCoveredTestResult = new CoveredTestResultPerTestMethodImpl(data, classesDirectory, coverageTransformer);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isTest()) {
			this.internalCoveredTestResult.getIgnoredTests().add(this.toString.apply(testIdentifier));
		}
	}

	@Override
	public synchronized void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.isTest()) {
			this.internalCoveredTestResult.setExecutionData(new ExecutionDataStore());
			this.internalCoveredTestResult.setSessionInfos(new SessionInfoStore());
			this.internalCoveredTestResult.getData().setSessionId(this.toString.apply(testIdentifier));
			this.internalCoveredTestResult.getData().collect(
					this.internalCoveredTestResult.getExecutionData(),
					this.internalCoveredTestResult.getSessionInfos(),
					true
			);
			this.internalCoveredTestResult.getRunningTests().add(this.toString.apply(testIdentifier));
		}
	}

	@Override
	public synchronized void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testIdentifier.isTest()) {
			this.internalCoveredTestResult.getData().collect(
					this.internalCoveredTestResult.getExecutionData(),
					this.internalCoveredTestResult.getSessionInfos(),
					false
			);

			Coverage jUnit5Coverage =
					internalCoveredTestResult.getCoverageTransformer().transformJacocoObject(
							this.internalCoveredTestResult.getExecutionData(),
							this.internalCoveredTestResult.getClassesDirectory()
					);
			this.internalCoveredTestResult.getCoverageResultsMap().put(this.toString.apply(testIdentifier), jUnit5Coverage);
			switch (testExecutionResult.getStatus()) {
				case FAILED:
					this.internalCoveredTestResult.getFailingTests().add(
							new Failure(
									this.toString.apply(testIdentifier),
									this.toClassName.apply(testIdentifier),
									testExecutionResult.getThrowable().get()
							)
					);
					break;
				default:
					break;
			}
		}
	}

	@Override
	public Map<String, Coverage> getCoverageResultsMap() {
		return this.internalCoveredTestResult.getCoverageResultsMap();
	}

	@Override
	public Coverage getCoverageOf(String testMethodName) {
		return this.internalCoveredTestResult.getCoverageOf(testMethodName);
	}

	@Override
	public void save() {
		this.internalCoveredTestResult.save();
	}
}
