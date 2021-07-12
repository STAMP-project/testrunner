package eu.stamp_project.testrunner.listener.impl;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.TestResult;
import eu.stamp_project.testrunner.runner.Failure;
import eu.stamp_project.testrunner.runner.Loader;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.RuntimeData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of the CoveredTestResultPerTestMethod interface
 *
 * @author andre15silva
 */
public class CoveredTestResultPerTestMethodImpl implements CoveredTestResultPerTestMethod {

	private static final long serialVersionUID = -789740001022671146L;

	protected final Map<String, Coverage> coverageResultsMap;

	protected final List<String> classesDirectory;

	protected transient RuntimeData data;

	protected transient ExecutionDataStore executionData;

	protected transient SessionInfoStore sessionInfos;

	protected transient CoverageTransformer coverageTransformer;

	private List<String> runningTests;
	private List<Failure> failingTests;
	private List<Failure> assumptionFailingTests;
	private List<String> ignoredTests;

	public CoveredTestResultPerTestMethodImpl(RuntimeData data, List<String> classesDirectory, CoverageTransformer coverageTransformer) {
		this.data = data;
		this.classesDirectory = classesDirectory;
		this.coverageResultsMap = new HashMap<>();
		this.coverageTransformer = coverageTransformer;
		this.runningTests = new ArrayList<>();
		this.failingTests = new ArrayList<>();
		this.assumptionFailingTests = new ArrayList<>();
		this.ignoredTests = new ArrayList<>();

	}

	public List<String> getClassesDirectory() {
		return classesDirectory;
	}

	public RuntimeData getData() {
		return data;
	}

	public ExecutionDataStore getExecutionData() {
		return executionData;
	}

	public SessionInfoStore getSessionInfos() {
		return sessionInfos;
	}

	public CoverageTransformer getCoverageTransformer() {
		return coverageTransformer;
	}

	public void setData(RuntimeData data) {
		this.data = data;
	}

	public void setExecutionData(ExecutionDataStore executionData) {
		this.executionData = executionData;
	}

	public void setSessionInfos(SessionInfoStore sessionInfos) {
		this.sessionInfos = sessionInfos;
	}

	@Override
	public Map<String, Coverage> getCoverageResultsMap() {
		return coverageResultsMap;
	}

	@Override
	public Coverage getCoverageOf(String testMethodName) {
		return this.getCoverageResultsMap().get(testMethodName);
	}

	@Override
	public List<String> getRunningTests() {
		return runningTests;
	}

	@Override
	public List<String> getPassingTests() {
		final List<String> failing = this.failingTests.stream()
				.map(failure -> failure.testCaseName)
				.collect(Collectors.toList());
		final List<String> assumptionFailing = this.assumptionFailingTests.stream()
				.map(failure -> failure.testCaseName)
				.collect(Collectors.toList());
		return this.runningTests.stream()
				.filter(description -> !assumptionFailing.contains(description))
				.filter(description -> !failing.contains(description))
				.collect(Collectors.toList());
	}

	@Override
	public TestResult aggregate(TestResult that) {
		if (that instanceof CoveredTestResultPerTestMethodImpl) {
			final CoveredTestResultPerTestMethodImpl thatListener = (CoveredTestResultPerTestMethodImpl) that;
			this.runningTests.addAll(thatListener.runningTests);
			this.failingTests.addAll(thatListener.failingTests);
			this.assumptionFailingTests.addAll(thatListener.assumptionFailingTests);
			this.ignoredTests.addAll(thatListener.ignoredTests);
		}
		return this;
	}

	@Override
	public List<Failure> getFailingTests() {
		return failingTests;
	}

	@Override
	public List<Failure> getAssumptionFailingTests() {
		return assumptionFailingTests;
	}

	@Override
	public List<String> getIgnoredTests() {
		return ignoredTests;
	}

	@Override
	public Failure getFailureOf(String testMethodName) {
		return this.getFailingTests().stream()
				.filter(failure -> failure.testCaseName.equals(testMethodName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(String.format("Could not find %s in failing test", testMethodName)));
	}

	@Override
	public void save() {
		File outputDir = new File(TestResult.OUTPUT_DIR);
		if (!outputDir.exists()) {
			if (!outputDir.mkdirs()) {
				System.err.println("Error while creating output dir");
			}
		}
		File f = new File(outputDir, SERIALIZE_NAME + EXTENSION);
		try (FileOutputStream fout = new FileOutputStream(f)) {
			try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
				oos.writeObject(this);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (Exception e) {
			System.err.println("Error while writing serialized file.");
			throw new RuntimeException(e);
		}
		System.out.println("File saved to the following path: " + f.getAbsolutePath());
	}

	/**
	 * Load from serialized object
	 *
	 * @return an Instance of CoveragePerTestMethod loaded from a serialized file.
	 */
	public static CoveredTestResultPerTestMethodImpl load() {
		return new Loader<CoveredTestResultPerTestMethodImpl>().load(SERIALIZE_NAME);
	}

	@Override
	public String toString() {
		return "CoveredTestResultPerTestMethodImpl{" +
				"coverageResultsMap=" + this.coverageResultsMap.keySet()
				.stream()
				.map(test -> "\t" + test + ": " + coverageResultsMap.get(test).toString())
				.collect(Collectors.joining(ConstantsHelper.LINE_SEPARATOR)) +
				", classesDirectory='" + classesDirectory + '\'' +
				", data=" + data +
				", executionData=" + executionData +
				", sessionInfos=" + sessionInfos +
				", coverageTransformer=" + coverageTransformer +
				", runningTests=" + runningTests +
				", failingTests=" + failingTests +
				", assumptionFailingTests=" + assumptionFailingTests +
				", ignoredTests=" + ignoredTests +
				'}';
	}

}
