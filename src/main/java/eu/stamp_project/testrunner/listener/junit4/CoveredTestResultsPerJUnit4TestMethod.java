package eu.stamp_project.testrunner.listener.junit4;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.TestResult;
import eu.stamp_project.testrunner.listener.impl.CoverageImpl;
import eu.stamp_project.testrunner.listener.impl.CoveredTestResultPerTestMethodImpl;
import eu.stamp_project.testrunner.runner.Failure;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JUnit4 implementation of {@link CoveredTestResultPerTestMethod}
 *
 * @author andre15silva
 */
public class CoveredTestResultsPerJUnit4TestMethod extends JUnit4TestResult implements CoveredTestResultPerTestMethod {

	private static final long serialVersionUID = -6934847896187177463L;

	private CoveredTestResultPerTestMethodImpl internalCoveredTestResult;

	/**
	 * This field is used to support parametrized test
	 * In fact, parametrized are reported as follow:
	 * - the test method is named "test"
	 * - it reports, for each parameter as follow: test[0], test[1].
	 * This field stores every coverage, i.e. for each input.
	 * Then, we are able to aggregate them to obtain the coverage of the test, for EVERY input.
	 */
	private Map<String, List<IClassCoverage>> coveragesPerMethodName;

	public CoveredTestResultsPerJUnit4TestMethod(RuntimeData data, String classesDirectory, CoverageTransformer coverageTransformer) {
		this.internalCoveredTestResult = new CoveredTestResultPerTestMethodImpl(data, classesDirectory, coverageTransformer);
		this.coveragesPerMethodName = new HashMap<>();
	}

	private static final Predicate<String> isParametrized = testMethodName ->
			Pattern.compile(".+\\[\\d+\\]").matcher(testMethodName).matches();

	private static final Function<String, String> fromParametrizedToSimpleName = parametrizedName ->
			parametrizedName.contains("[") ? parametrizedName.split("\\[")[0] : parametrizedName;

	@Override
	public synchronized void testStarted(Description description) throws Exception {
		this.internalCoveredTestResult.setExecutionData(new ExecutionDataStore());
		this.internalCoveredTestResult.setSessionInfos(new SessionInfoStore());
		this.internalCoveredTestResult.getData().setSessionId(description.getMethodName());
		this.internalCoveredTestResult.getData().collect(
				this.internalCoveredTestResult.getExecutionData(),
				this.internalCoveredTestResult.getSessionInfos(),
				true
		);
	}

	@Override
	public synchronized void testFinished(Description description) throws Exception {
		this.internalCoveredTestResult.getRunningTests().add(description.getMethodName());

		this.internalCoveredTestResult.getData().collect(
				this.internalCoveredTestResult.getExecutionData(),
				this.internalCoveredTestResult.getSessionInfos(),
				false
		);

		Coverage jUnit4Coverage =
				internalCoveredTestResult.getCoverageTransformer().transformJacocoObject(this.internalCoveredTestResult.getExecutionData(),
						this.internalCoveredTestResult.getClassesDirectory());
		this.internalCoveredTestResult.getCoverageResultsMap().put(description.getMethodName(), jUnit4Coverage);
		if (isParametrized.test(description.getMethodName())) {
			this.collectForParametrizedTest(fromParametrizedToSimpleName.apply(description.getMethodName()));
		}
	}

	@Override
	public void testFailure(org.junit.runner.notification.Failure failure) throws Exception {
		this.internalCoveredTestResult.getFailingTests().add(
				new Failure(
						failure.getDescription().getMethodName(),
						failure.getDescription().getClassName(),
						failure.getException()
				)
		);
	}

	@Override
	public void testAssumptionFailure(org.junit.runner.notification.Failure failure) {
		this.internalCoveredTestResult.getAssumptionFailingTests().add(
				new Failure(
						failure.getDescription().getMethodName(),
						failure.getDescription().getClassName(),
						failure.getException()
				)
		);
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		this.internalCoveredTestResult.getIgnoredTests().add(description.getMethodName());
	}

	private void collectForParametrizedTest(String testMethodName) {
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(this.internalCoveredTestResult.getExecutionData(), coverageBuilder);
		try {
			String[] paths = this.internalCoveredTestResult.getClassesDirectory().split(File.pathSeparator);
			for (String path : paths) {
				analyzer.analyzeAll(new File(path));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (!this.coveragesPerMethodName.containsKey(testMethodName)) {
			this.coveragesPerMethodName.put(testMethodName, new ArrayList<>());
		}
		coverageBuilder.getClasses()
				.forEach(classCoverage ->
						this.coveragesPerMethodName.get(testMethodName)
								.add(classCoverage)
				);
	}

	public Map<String, List<IClassCoverage>> getCoveragesPerMethodName() {
		return coveragesPerMethodName;
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
	public List<String> getRunningTests() {
		return this.internalCoveredTestResult.getRunningTests();
	}

	@Override
	public List<String> getPassingTests() {
		return this.internalCoveredTestResult.getPassingTests();
	}

	@Override
	public TestResult aggregate(TestResult that) {
		if (that instanceof CoveredTestResultsPerJUnit4TestMethod) {
			return this.internalCoveredTestResult.aggregate(((CoveredTestResultsPerJUnit4TestMethod) that).internalCoveredTestResult);
		} else {
			return this;
		}
	}

	@Override
	public List<Failure> getFailingTests() {
		return this.internalCoveredTestResult.getFailingTests();
	}

	@Override
	public List<Failure> getAssumptionFailingTests() {
		return this.internalCoveredTestResult.getAssumptionFailingTests();
	}

	@Override
	public List<String> getIgnoredTests() {
		return this.internalCoveredTestResult.getIgnoredTests();
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
		if (!this.coveragesPerMethodName.isEmpty()) {
			this.aggregateParametrizedTestCoverage();
		}
		this.internalCoveredTestResult.save();
	}

	/*
	This method will use the coveragesPerMethodName map to create a coverage for the test method.
	This coverage is the aggregation of the coverages of each input for the same test method.
 */
	private void aggregateParametrizedTestCoverage() {
		this.coveragesPerMethodName.keySet().forEach(testMethodName -> {
					int covered = 0;
					int total = 0;
					final ArrayList<IClassCoverage> classCoverages =
							new ArrayList<>(this.coveragesPerMethodName.get(testMethodName));
					while (!classCoverages.isEmpty()) {
						final IClassCoverage current = classCoverages.get(0);
						final List<IClassCoverage> subListOnSameClass =
								getSameClassCoverage(current.getName(), classCoverages);
						final List<List<Integer>> coveragePerMethods = subListOnSameClass.stream()
								.map(coverage ->
										CoverageImpl.getListOfCountForCounterFunction(coverage, ICounter::getCoveredCount)
								).collect(Collectors.toList());
						final List<Integer> bestCoverage = IntStream.range(0, coveragePerMethods.get(0).size())
								.boxed()
								.map(index ->
										coveragePerMethods.stream()
												.map(integers -> integers.get(index))
												.max(Integer::compareTo)
												.get()
								).collect(Collectors.toList());
						covered += bestCoverage.stream().mapToInt(Integer::intValue).sum();
						total += CoverageImpl.getListOfCountForCounterFunction(subListOnSameClass.get(0), ICounter::getTotalCount)
								.stream()
								.mapToInt(Integer::intValue)
								.sum();
						classCoverages.removeAll(subListOnSameClass);
					}
					final JUnit4Coverage value = new JUnit4Coverage(covered, total);
					value.setExecutionPath(this.internalCoveredTestResult.getCoverageResultsMap().keySet()
							.stream()
							.map(this.internalCoveredTestResult.getCoverageResultsMap()::get)
							.map(Coverage::getExecutionPath)
							.collect(Collectors.joining("#"))
					);
					this.internalCoveredTestResult.getCoverageResultsMap().put(testMethodName, value);
				}
		);
	}

	/*
	 * return the sublist of the given list which the coverage of the same class, pointed by the className
	 */
	private List<IClassCoverage> getSameClassCoverage(String className, List<IClassCoverage> classCoverages) {
		return classCoverages.stream()
				.filter(iClassCoverage -> className.equals(iClassCoverage.getName()))
				.collect(Collectors.toList());
	}

}