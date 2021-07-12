package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.AbstractTest;
import eu.stamp_project.testrunner.listener.CoveredTestResultPerTestMethod;
import eu.stamp_project.testrunner.listener.impl.CoveredTestResultPerTestMethodImpl;
import eu.stamp_project.testrunner.runner.ParserOptions;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUnit4JacocoRunnerCoveredResultPerTestMethodTest extends AbstractTest {

	// FIXME: The behaviour in this case is not consistent
	// There should be an equal number of keys in both running tests and the coverage map
	// There should also always be a key for the method given as input
	@Test
	@Ignore
	public void testWithoutNewJvmOnTestClassParametrized() throws Exception {

        /*
            Using the api to compute the coverage on a test class
         */

		JUnit4JacocoRunnerCoveredResultPerTestMethod.main(new String[]{
						ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
						ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
						ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.ParametrizedTest",
						ParserOptions.FLAG_testMethodNamesToRun, "test"
				}
		);
		final CoveredTestResultPerTestMethod load = CoveredTestResultPerTestMethodImpl.load();
		System.out.println(load);

		assertEquals(5, load.getRunningTests().size());
		assertEquals(5, load.getPassingTests().size());
		assertEquals(0, load.getFailingTests().size());
		assertEquals(0, load.getIgnoredTests().size());

		assertEquals(5, load.getCoverageResultsMap().size());
		assertEquals(34, load.getCoverageResultsMap().get("test").getInstructionsCovered());
		assertEquals(23, load.getCoverageResultsMap().get("test[0]").getInstructionsCovered());
		assertEquals(26, load.getCoverageResultsMap().get("test[1]").getInstructionsCovered());
		assertEquals(19, load.getCoverageResultsMap().get("test[2]").getInstructionsCovered());
		assertEquals(19, load.getCoverageResultsMap().get("test[3]").getInstructionsCovered());

		System.out.println(load.getCoverageResultsMap().get("test").getExecutionPath());
	}

	@Test
	public void testWithoutNewJvmOnTestCases() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

		JUnit4JacocoRunnerCoveredResultPerTestMethod.main(new String[]{
						ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
						ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
						ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample",
						ParserOptions.FLAG_testMethodNamesToRun, "test3:test2:copyOftest2"
				}
		);
		final CoveredTestResultPerTestMethod load = CoveredTestResultPerTestMethodImpl.load();
		System.out.println(load);

		assertEquals(3, load.getRunningTests().size());
		assertEquals(3, load.getPassingTests().size());
		assertEquals(0, load.getFailingTests().size());
		assertEquals(0, load.getIgnoredTests().size());

		assertEquals(3, load.getCoverageResultsMap().size());
		assertEquals(23, load.getCoverageResultsMap().get("example.TestSuiteExample#test2").getInstructionsCovered());
		assertEquals(23, load.getCoverageResultsMap().get("example.TestSuiteExample#test3").getInstructionsCovered());
		assertEquals(23, load.getCoverageResultsMap().get("example.TestSuiteExample#copyOftest2").getInstructionsCovered());
		System.out.println(load.getCoverageResultsMap().get("example.TestSuiteExample#test2").getExecutionPath());
		System.out.println(load.getCoverageResultsMap().get("example.TestSuiteExample#copyOftest2").getExecutionPath());
		System.out.println(load.getCoverageResultsMap().get("example.TestSuiteExample#test3").getExecutionPath());
	}

	@Test
	public void testWithoutNewJvmOnTestClassAll() throws Exception {

        /*
            Using the api to compute the coverage on test cases
         */

		JUnit4JacocoRunnerCoveredResultPerTestMethod.main(new String[]{
						ParserOptions.FLAG_pathToCompiledClassesOfTheProject, SOURCE_PROJECT_CLASSES,
						ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject, TEST_PROJECT_CLASSES,
						ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun, "example.TestSuiteExample",
				}
		);
		final CoveredTestResultPerTestMethod load = CoveredTestResultPerTestMethodImpl.load();

		System.out.println(load);
		load.getCoverageResultsMap()
				.keySet()
				.stream()
				.map(s -> s + " -> " + load.getCoverageResultsMap().get(s).getExecutionPath())
				.forEach(System.out::println);
		for (String expectedExecutionPath : expectedExecutionPathsTest3) {
			assertEquals(23, load.getCoverageResultsMap().get("example.TestSuiteExample#test3").getInstructionsCovered());
			assertTrue(load.getCoverageResultsMap().get("example.TestSuiteExample#test3").getExecutionPath().contains(expectedExecutionPath));
		}
		for (String expectedExecutionPath : expectedExecutionPathsTest4) {
			assertEquals(26, load.getCoverageResultsMap().get("example.TestSuiteExample#test4").getInstructionsCovered());
			assertTrue(load.getCoverageResultsMap().get("example.TestSuiteExample#test4").getExecutionPath().contains(expectedExecutionPath));
		}
	}

	private static final String[] expectedExecutionPathsTest3 = {
			"tobemocked/LoginDao:0,0",
			"tobemocked/LoginController:0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
			"tobemocked/LoginService:0,0,0,0,0,0,0,0,0,0,0,0,0,0",
			"example/Example:2,0,0,4,4,0,0,2,0,2,5,1,0,3",
			"tobemocked/UserForm:0,0"
	};

	private static final String[] expectedExecutionPathsTest4 = {
			"tobemocked/LoginDao:0,0",
			"tobemocked/LoginController:0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
			"tobemocked/LoginService:0,0,0,0,0,0,0,0,0,0,0,0,0,0",
			"example/Example:2,0,0,4,0,0,7,2,0,2,5,1,0,3",
			"tobemocked/UserForm:0,0"
	};


}