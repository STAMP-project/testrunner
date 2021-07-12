package eu.stamp_project.testrunner;

import eu.stamp_project.mutationtest.descartes.DescartesMutationEngine;
import eu.stamp_project.testrunner.listener.*;
import eu.stamp_project.testrunner.listener.impl.CoverageImpl;
import eu.stamp_project.testrunner.listener.impl.CoveragePerTestMethodImpl;
import eu.stamp_project.testrunner.listener.impl.CoveredTestResultPerTestMethodImpl;
import eu.stamp_project.testrunner.listener.impl.TestResultImpl;
import eu.stamp_project.testrunner.listener.junit4.JUnit4Coverage;
import eu.stamp_project.testrunner.listener.pit.AbstractParser;
import eu.stamp_project.testrunner.listener.pit.AbstractPitResult;
import eu.stamp_project.testrunner.runner.JUnit4Runner;
import eu.stamp_project.testrunner.runner.ParserOptions;
import eu.stamp_project.testrunner.runner.pit.PitRunner;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.apache.commons.io.FileUtils;
import org.jacoco.core.runtime.IRuntime;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.objectweb.asm.ClassReader;
import org.opentest4j.TestAbortedException;
import org.pitest.junit5.JUnit5Configuration;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.engine.gregor.GregorMutationEngine;
import org.pitest.testapi.TestGroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Benjamin DANGLOT benjamin.danglot@inria.fr on 19/12/17
 * <p>
 * This class is the EntryPoint of the project. This is the only class that the
 * user should use.
 * </p>
 * <p>
 * Main methods are:
 * </p>
 * <ul>
 * <li>{@link EntryPoint#runTests(String, String[], String[])} to run all the
 * given test classes.</li>
 * <li>{@link EntryPoint#runCoverage(String, String, String[], String[])} to
 * compute the coverage of the given test classes</li>
 * <li>{@link EntryPoint#runCoveragePerTestMethods(String, String, String[], String[])}
 * to compute the coverage of the test methods of the given test classes</li>
 * </ul>
 * <p>
 * This class relies on {@link JUnit4Runner} and
 * {@link eu.stamp_project.testrunner.runner.coverage.JacocoRunner} to
 * respectively execute tests and compute the coverage. This class creates a new
 * JVM by calling the command "java" with proper arguments.
 * <p>
 * This class has options accessible from the outside:
 * </p>
 * <ul>
 * <li>jUnit5Mode: switches from JUnit 4 to the JUnit 5 test runner</li>
 * <li>verbose: boolean to enable traces to track the progress</li>
 * <li>timeoutInMs: integer timeout time in milliseconds for the whole requested
 * process.</li>
 * <li>workingDirectory: change the directory where the java command will be
 * launch</li>
 * <li>JVMArgs: pass Java Virtual Machine arguments to the java command</li>
 * <li>outPrintStream: to redirect the standard output to a custom print
 * stream</li>
 * <li>errPrintStream: to redirect the standard error output to a custom print
 * stream</li>
 * <li>persistence: if enable, keeps the configuration between runs, else reset
 * it</li>
 * <li>blackList: allows to blacklist specific test methods of a test class/li>
 * <li>coverageDetail: the level of detail in which coverage should be reported</li>
 * </ul>
 */
public class EntryPoint {

    /**
     * enable the JUnit 5 test runner
     */
    public static boolean jUnit5Mode = false;

    /**
     * enable traces to track the progress
     */
    public static boolean verbose = false;

    /**
     * timeout time in milliseconds for the whole requested process
     */
    public static int timeoutInMs = 10000;

    /**
     * working directory of the java sub-process. By default, it is set to null to
     * inherit from this java process.
     */
    public static File workingDirectory = null;

    /**
     * {@link EntryPoint} uses the command "java". This field allows users to
     * specify Java Virtual Machine(JVM) arguments, <i>e.g.</i> -Xms4G. If this
     * value is <code>null</code>, {@link EntryPoint} won't pass any JVMArgs. The
     * value of this field should be properly formatted for command line usage,
     * <i>e.g.</i> -Xms4G -Xmx8G -XX:-UseGCOverheadLimit. The args should be
     * separated with white spaces.
     */
    public static String JVMArgs = null;

    /**
     * Allows to pass a customized PrintStream on which the java process called will
     * printout. If this field is equal to null, {@link EntryPoint} with use the
     * stdout.
     */
    public static PrintStream outPrintStream = null;

    /**
     * Allows to pass a customized PrintStream on which the java process called will
     * printerr. If this field is equal to null, {@link EntryPoint} with use the
     * stderr.
     */
    public static PrintStream errPrintStream = null;

    /**
     * Enable this boolean to keep the values after each run. The list of concerned
     * values are: JVMArgs, outPrintStream, errPrintStream, workingDirectory,
     * timeoutInMs,
     */
    public static boolean persistence = true;

    /**
     * Allows to blacklist specific test methods of a test class. This blackList is
     * used only in {@link EntryPoint#runTests} and
     * {@link EntryPoint#runCoverage(String, String, String[], String[])} since
     * other methods allow to specify which test method to execute.
     */
    public static List<String> blackList = new ArrayList<>();

    /**
     * Allows to set the level of detail at which coverage information should be reported.
     * see {@link ParserOptions.CoverageTransformerDetail}
     */
    public static ParserOptions.CoverageTransformerDetail coverageDetail =
            ParserOptions.CoverageTransformerDetail.SUMMARIZED;

    // PIT OPTIONS

    /**
     * This parameter allows to configure the mutation engine you want to use.
     * By default, it uses descartes (c.f. https://github.com/STAMP-project/pitest-descartes)
     * Possible values: ConstantsHelper.mutationEngine.DESCARTES or ConstantsHelper.mutationEngine.GREGOR
     * see {@link eu.stamp_project.testrunner.utils.ConstantsHelper.MutationEngine}
     */
    private static ConstantsHelper.MutationEngine mutationEngine = ConstantsHelper.MutationEngine.DESCARTES;

    /**
     * This method configure the mutation engine to be used. It also sets the default values for the mutators, see {@link eu.stamp_project.testrunner.EntryPoint#pitMutators}
     *
     * @param mutationEngine the mutation engine to be used. see {@link eu.stamp_project.testrunner.utils.ConstantsHelper.MutationEngine}
     */
    public static void setMutationEngine(ConstantsHelper.MutationEngine mutationEngine) {
        EntryPoint.mutationEngine = mutationEngine;
        EntryPoint.pitMutators = mutationEngine.mutators;
    }

    /**
     * List of mutators to be used.
     * By default, it uses the default mutators of descartes.
     * If you specify the gregor mutation engine, it uses ALL mutators (c.f. http://pitest.org/)
     */
    public static List<String> pitMutators = mutationEngine.mutators;

    /**
     * Configure either using XML format for the output or CSV.
     * See {@link eu.stamp_project.testrunner.listener.pit.AbstractParser.OutputFormat}
     */
    public static AbstractParser.OutputFormat pitOutputFormat = AbstractParser.OutputFormat.XML;

    /* EXECUTION OF TEST API */

    public static TestResult runTests(String classpath, String fullQualifiedNameOfTestClass) throws TimeoutException {
        return EntryPoint.runTests(classpath, new String[]{fullQualifiedNameOfTestClass}, new String[0]);
    }

    public static TestResult runTests(String classpath, String[] fullQualifiedNameOfTestClasses)
            throws TimeoutException {
        return EntryPoint.runTests(classpath, fullQualifiedNameOfTestClasses, new String[0]);
    }

    public static TestResult runTests(String classpath, String fullQualifiedNameOfTestClass, String methodName)
            throws TimeoutException {
        return EntryPoint.runTests(classpath, new String[]{fullQualifiedNameOfTestClass},
                new String[]{methodName});
    }

    public static TestResult runTests(String classpath, String fullQualifiedNameOfTestClass, String[] methodNames)
            throws TimeoutException {
        return EntryPoint.runTests(classpath, new String[]{fullQualifiedNameOfTestClass}, methodNames);
    }

    /**
     * Execution of various test methods inside a given test class.
     * <p>
     * Run all the test methods given inside the given test class. The test class
     * must be given as a full qualified name. For instance, my.package.MyClassTest.
     * This methods will run all the test methods given.
     * </p>
     *
     * @param classpath                      the classpath required to run the given test.
     * @param fullQualifiedNameOfTestClasses test class to be run.
     * @param methodNames                    test methods to be run.
     * @return an instance of TestResult {@link TestResult} containing result of the
     * execution of test methods.
     * @throws TimeoutException when the execution takes longer than timeoutInMs
     */
    public static TestResult runTests(String classpath, String[] fullQualifiedNameOfTestClasses, String[] methodNames)
            throws TimeoutException {
        final String javaCommand = String.join(ConstantsHelper.WHITE_SPACE,
                new String[]{getJavaCommand(),
                        (classpath + ConstantsHelper.PATH_SEPARATOR + ABSOLUTE_PATH_TO_RUNNER_CLASSES).replaceAll(" ", "%20"),
                        EntryPoint.jUnit5Mode ? EntryPoint.JUNIT5_TEST_RUNNER_QUALIFIED_NAME
                                : EntryPoint.JUNIT4_TEST_RUNNER_QUALIFIED_NAME,
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun,
                        String.join(ConstantsHelper.PATH_SEPARATOR, fullQualifiedNameOfTestClasses),
                        methodNames.length == 0 ? ""
                                : (ParserOptions.FLAG_testMethodNamesToRun + ConstantsHelper.WHITE_SPACE
                                + String.join(ConstantsHelper.PATH_SEPARATOR, methodNames)),
                        EntryPoint.blackList.isEmpty() ? ""
                                : (ParserOptions.FLAG_blackList + ConstantsHelper.WHITE_SPACE
                                + String.join(ConstantsHelper.PATH_SEPARATOR, EntryPoint.blackList))
                }
        );
        return EntryPoint.runTests(javaCommand);
    }

    private static TestResult runTests(String commandLine) throws TimeoutException {
        try {
            runGivenCommandLine(commandLine);
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout when running {}", commandLine);
            throw e;
        }
        final TestResult load = TestResultImpl.load();
        if (EntryPoint.verbose) {
            LOGGER.info(
                    "Test has been run: {}", Stream
                            .concat(load.getPassingTests().stream().map(Object::toString),
                                    load.getFailingTests().stream().map(Object::toString))
                            .collect(Collectors.joining(",")));
        }
        return load;
    }

    /* COMPUTE COVERAGE API */

    public static Coverage runCoverage(String classpath, String targetProjectClasses,
                                       String fullQualifiedNameOfTestClass) throws TimeoutException {
        return EntryPoint.runCoverage(classpath, targetProjectClasses, new String[]{fullQualifiedNameOfTestClass},
                new String[0]);
    }

    public static Coverage runCoverage(String classpath, String targetProjectClasses,
                                       String[] fullQualifiedNameOfTestClasses) throws TimeoutException {
        return EntryPoint.runCoverage(classpath, targetProjectClasses, fullQualifiedNameOfTestClasses, new String[0]);
    }

    public static Coverage runCoverage(String classpath, String targetProjectClasses,
                                       String fullQualifiedNameOfTestClass, String[] methodNames) throws TimeoutException {
        return EntryPoint.runCoverage(classpath, targetProjectClasses, new String[]{fullQualifiedNameOfTestClass},
                methodNames);
    }

    public static Coverage runCoverage(String classpath, String targetProjectClasses,
                                       String[] fullQualifiedNameOfTestClasses, String[] methodNames) throws TimeoutException {
        return EntryPoint.runCoverage(
                classpath,
                Collections.singletonList(targetProjectClasses.split(ConstantsHelper.PATH_SEPARATOR)[0]),
                Collections.singletonList(targetProjectClasses.split(ConstantsHelper.PATH_SEPARATOR)[1]),
                fullQualifiedNameOfTestClasses,
                methodNames
        );
    }

        /**
		 * Compute of the instruction coverage using <a
		 * href=http://www.eclemma.org/jacoco/>JaCoCo</a> for various test classes.
		 * <p>
		 * This method compute the instruction coverage, using <a
		 * href=http://www.eclemma.org/jacoco/>JaCoCo</a> obtained by executing the
		 * given test classes. This method require the path to the binaries, i.e.
		 * .class, of the source code on which the instruction must be computed. This
		 * method computes the "global" coverage, <i>i.e.</i> the coverage obtained if
		 * all the test are run. For a test method per test method result, see
		 * {@link EntryPoint#runCoveragePerTestMethods(String, String, String[], String[])}
		 * </p>
		 *
		 * @param classpath                      the classpath required to run the given tests classes.
		 * @param targetSourceClasses            path to the folders that contain source binaries, i.e. .class, on which
		 *                                       Jacoco computes the coverage.
         * @param targetTestClasses              path to the folders that contain test binaries, i.e. .class, on which
         *                                       Jacoco computes the coverage.
		 * @param fullQualifiedNameOfTestClasses test classes to be run.
		 * @param methodNames                    test methods to be run. Can be empty
		 * @return an instance of Coverage {@link Coverage} containing result of the
		 * execution of test classes.
		 * @throws TimeoutException when the execution takes longer than timeoutInMs
		 */
    public static Coverage runCoverage(String classpath,
                                       List<String> targetSourceClasses,
                                       List<String> targetTestClasses,
                                       String[] fullQualifiedNameOfTestClasses,
                                       String[] methodNames) throws TimeoutException {
        final String javaCommand = String.join(ConstantsHelper.WHITE_SPACE,
                new String[]{getJavaCommand(),
                        (classpath + ConstantsHelper.PATH_SEPARATOR + ABSOLUTE_PATH_TO_RUNNER_CLASSES
                                + ConstantsHelper.PATH_SEPARATOR + ABSOLUTE_PATH_TO_JACOCO_DEPENDENCIES).replaceAll(" ", "%20"),
                        EntryPoint.jUnit5Mode ? EntryPoint.JUNIT5_JACOCO_RUNNER_QUALIFIED_NAME : EntryPoint.JUNIT4_JACOCO_RUNNER_QUALIFIED_NAME,
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject,
                        targetSourceClasses.stream().reduce((x, y) -> x + ConstantsHelper.PATH_SEPARATOR + y).get().replaceAll(" ", "%20"),
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject,
                        targetTestClasses.stream().reduce((x, y) -> x + ConstantsHelper.PATH_SEPARATOR + y).get().replaceAll(" ", "%20"),
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun,
                        String.join(ConstantsHelper.PATH_SEPARATOR, fullQualifiedNameOfTestClasses),
                        methodNames.length == 0 ? "" :
                                (ParserOptions.FLAG_testMethodNamesToRun + ConstantsHelper.WHITE_SPACE +
                                        String.join(ConstantsHelper.PATH_SEPARATOR, methodNames)),
                        EntryPoint.blackList.isEmpty() ? "" :
                                (ParserOptions.FLAG_blackList + ConstantsHelper.WHITE_SPACE
                                        + String.join(ConstantsHelper.PATH_SEPARATOR, EntryPoint.blackList)),
                        EntryPoint.coverageDetail == ParserOptions.CoverageTransformerDetail.SUMMARIZED ? "" :
                                (ParserOptions.FLAG_coverage_detail + ConstantsHelper.WHITE_SPACE
                                        + EntryPoint.coverageDetail.name()),
                });
        return EntryPoint.runCoverage(javaCommand);
    }

    /* COMPUTE COVERAGE PER TEST METHOD API */

    public static CoveragePerTestMethod runCoveragePerTestMethods(String classpath, String targetProjectClasses,
                                                                  String fullQualifiedNameOfTestClass) throws TimeoutException {
        return EntryPoint.runCoveragePerTestMethods(classpath, targetProjectClasses,
                new String[]{fullQualifiedNameOfTestClass}, new String[0]);
    }

    public static CoveragePerTestMethod runCoveragePerTestMethods(String classpath, String targetProjectClasses,
                                                                  String fullQualifiedNameOfTestClass, String testMethodName) throws TimeoutException {
        return EntryPoint.runCoveragePerTestMethods(classpath, targetProjectClasses,
                new String[]{fullQualifiedNameOfTestClass}, new String[]{testMethodName});
    }

    public static CoveragePerTestMethod runCoveragePerTestMethods(String classpath, String targetProjectClasses,
                                                                  String fullQualifiedNameOfTestClass, String[] testMethodNames) throws TimeoutException {
        return EntryPoint.runCoveragePerTestMethods(classpath, targetProjectClasses,
                new String[]{fullQualifiedNameOfTestClass}, testMethodNames);
    }

    public static CoveragePerTestMethod runCoveragePerTestMethods(String classpath, String targetProjectClasses,
                                                                  String[] fullQualifiedNameOfTestClasses) throws TimeoutException {
        return EntryPoint.runCoveragePerTestMethods(classpath, targetProjectClasses, fullQualifiedNameOfTestClasses,
                new String[0]);
    }

    private static Coverage runCoverage(String commandLine) throws TimeoutException {
        try {
            runGivenCommandLine(commandLine);
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout when running {}", commandLine);
            throw e;
        }
        final Coverage load = CoverageImpl.load();
        if (EntryPoint.verbose) {
            LOGGER.info("Global coverage has been computed {}", load.toString());
        }
        return load;
    }

    public static CoveragePerTestMethod runCoveragePerTestMethods(String classpath, String targetProjectClasses,
                                                                  String[] fullQualifiedNameOfTestClasses, String[] methodNames) throws TimeoutException {
        return EntryPoint.runCoveragePerTestMethods(
                classpath,
                Collections.singletonList(targetProjectClasses.split(ConstantsHelper.PATH_SEPARATOR)[0]),
                Collections.singletonList(targetProjectClasses.split(ConstantsHelper.PATH_SEPARATOR)[1]),
                fullQualifiedNameOfTestClasses,
                methodNames
        );
    }

        /**
		 * Compute of the instruction coverage using <a
		 * href=http://www.eclemma.org/jacoco/>JaCoCo</a> for various test methods
		 * inside the given test class.
		 * <p>
		 * This method computes the instruction coverage, using <a
		 * href=http://www.eclemma.org/jacoco/>JaCoCo</a> obtained by executing the
		 * given test methods inside the given test classes. This method requires the
		 * path to the binaries, i.e. .class, of the source code on which the
		 * instruction must be computed. This method computes the per test method
		 * coverage, <i>i.e.</i> the coverage obtained by each test methods, separately.
		 * It does not run one by one test methods, but rather use a specific
		 * implementation of {@link org.junit.runner.notification.RunListener}.
		 * </p>
		 *
		 * @param classpath                      the classpath required to run the given tests classes.
		 * @param targetSourceClasses           paths to the folders that contain source binaries, i.e. .class, on which
		 *                                       Jacoco computes the coverage.
         * @param targetTestClasses           paths to the folders that contain test binaries, i.e. .class, on which
         *                                       Jacoco computes the coverage.
		 * @param fullQualifiedNameOfTestClasses test classes to be run.
		 * @param methodNames                    test methods to be run.
		 * @return a Map that associate each test method name to its instruction
		 * coverage, as an instance of JUnit4Coverage {@link JUnit4Coverage} of
		 * test classes.
		 * @throws TimeoutException when the execution takes longer than timeoutInMs
		 */
    public static CoveragePerTestMethod runCoveragePerTestMethods(String classpath,
                                                                  List<String> targetSourceClasses,
                                                                  List<String> targetTestClasses,
                                                                  String[] fullQualifiedNameOfTestClasses,
                                                                  String[] methodNames) throws TimeoutException {
        final String javaCommand = String.join(ConstantsHelper.WHITE_SPACE,
                new String[]{
                        getJavaCommand(),
                        (classpath + ConstantsHelper.PATH_SEPARATOR + ABSOLUTE_PATH_TO_RUNNER_CLASSES
                                + ConstantsHelper.PATH_SEPARATOR + ABSOLUTE_PATH_TO_JACOCO_DEPENDENCIES).replaceAll(" ", "%20"),
                        EntryPoint.jUnit5Mode ? EntryPoint.JUNIT5_JACOCO_RUNNER_PER_TEST_QUALIFIED_NAME : EntryPoint.JUNIT4_JACOCO_RUNNER_PER_TEST_QUALIFIED_NAME,
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject,
                        targetSourceClasses.stream().reduce((x, y) -> x + ConstantsHelper.PATH_SEPARATOR + y).get().replaceAll(" ", "%20"),
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject,
                        targetTestClasses.stream().reduce((x, y) -> x + ConstantsHelper.PATH_SEPARATOR + y).get().replaceAll(" ", "%20"),
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun,
                        String.join(ConstantsHelper.PATH_SEPARATOR, fullQualifiedNameOfTestClasses),
                        methodNames.length == 0 ? "" : ParserOptions.FLAG_testMethodNamesToRun + ConstantsHelper.WHITE_SPACE +
                                String.join(ConstantsHelper.PATH_SEPARATOR, methodNames),
                        EntryPoint.blackList.isEmpty() ? ""
                                : (ParserOptions.FLAG_blackList + ConstantsHelper.WHITE_SPACE
                                + String.join(ConstantsHelper.PATH_SEPARATOR, EntryPoint.blackList)),
                        EntryPoint.coverageDetail == ParserOptions.CoverageTransformerDetail.SUMMARIZED ? "" :
                                (ParserOptions.FLAG_coverage_detail + ConstantsHelper.WHITE_SPACE
                                 + EntryPoint.coverageDetail.name()),
                });
        try {
            EntryPoint.runGivenCommandLine(javaCommand);
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout when running {}", javaCommand);
            throw e;
        }
        final CoveragePerTestMethod load = CoveragePerTestMethodImpl.load();
        if (EntryPoint.verbose) {
            LOGGER.info("Coverage per test methods has been computed {}{}", ConstantsHelper.LINE_SEPARATOR,
                    load.toString());
        }
        return load;
    }

    /* COMPUTE COVERED TEST RESULT PER TEST METHOD API */

    public static CoveredTestResultPerTestMethod runCoveredTestResultPerTestMethods(String classpath, String targetProjectClasses,
                                                                  String fullQualifiedNameOfTestClass) throws TimeoutException {
        return EntryPoint.runCoveredTestResultPerTestMethods(classpath, targetProjectClasses,
                new String[]{fullQualifiedNameOfTestClass}, new String[0]);
    }

    public static CoveredTestResultPerTestMethod runCoveredTestResultPerTestMethods(String classpath, String targetProjectClasses,
                                                                  String fullQualifiedNameOfTestClass, String testMethodName) throws TimeoutException {
        return EntryPoint.runCoveredTestResultPerTestMethods(classpath, targetProjectClasses,
                new String[]{fullQualifiedNameOfTestClass}, new String[]{testMethodName});
    }

    public static CoveredTestResultPerTestMethod runCoveredTestResultPerTestMethods(String classpath, String targetProjectClasses,
                                                                  String fullQualifiedNameOfTestClass, String[] testMethodNames) throws TimeoutException {
        return EntryPoint.runCoveredTestResultPerTestMethods(classpath, targetProjectClasses,
                new String[]{fullQualifiedNameOfTestClass}, testMethodNames);
    }

    public static CoveredTestResultPerTestMethod runCoveredTestResultPerTestMethods(String classpath, String targetProjectClasses,
                                                                  String[] fullQualifiedNameOfTestClasses) throws TimeoutException {
        return EntryPoint.runCoveredTestResultPerTestMethods(classpath, targetProjectClasses, fullQualifiedNameOfTestClasses,
                new String[0]);
    }

    public static CoveredTestResultPerTestMethod runCoveredTestResultPerTestMethods(String classpath, String targetProjectClasses,
                                                                  String[] fullQualifiedNameOfTestClasses, String[] methodNames) throws TimeoutException {
        return EntryPoint.runCoveredTestResultPerTestMethods(
                classpath,
                Collections.singletonList(targetProjectClasses.split(ConstantsHelper.PATH_SEPARATOR)[0]),
                Collections.singletonList(targetProjectClasses.split(ConstantsHelper.PATH_SEPARATOR)[1]),
                fullQualifiedNameOfTestClasses,
                methodNames
        );
    }

    /**
     * Compute the test result and instruction coverage  using <a
     * href=http://www.eclemma.org/jacoco/>JaCoCo</a> for various test methods
     * inside the given test classes.
     * <p>
     * This method computes the instruction coverage, using <a
     * href=http://www.eclemma.org/jacoco/>JaCoCo</a> obtained by executing the
     * given test methods inside the given test classes. This method requires the
     * path to the binaries, i.e. .class, of the source code on which the
     * instruction must be computed. This method computes the per test method
     * coverage, <i>i.e.</i> the coverage obtained by each test methods, separately.
     * It does not run one by one test methods, but rather use a specific
     * implementation of {@link org.junit.runner.notification.RunListener}.
     * </p>
     *
     * @param classpath                      the classpath required to run the given tests classes.
     * @param targetSourceClasses            path to the folders that contain source binaries, i.e. .class, on which
     *                                       Jacoco computes the coverage.
     * @param targetTestClasses              path to the folders that contain test source binaries, i.e. .class, on which
     *                                       Jacoco computes the coverage.
     * @param fullQualifiedNameOfTestClasses test classes to be run.
     * @param methodNames                    test methods to be run.
     * @return a Map that associate each test method name to its instruction
     * coverage, as an instance of CoveredTestResultPerTestMethod {@link CoveredTestResultPerTestMethod} of
     * test classes.
     * @throws TimeoutException when the execution takes longer than timeoutInMs
     */
    public static CoveredTestResultPerTestMethod runCoveredTestResultPerTestMethods(String classpath,
                                                                                    List<String> targetSourceClasses,
                                                                                    List<String> targetTestClasses,
                                                                                    String[] fullQualifiedNameOfTestClasses,
                                                                                    String[] methodNames) throws TimeoutException {
        final String javaCommand = String.join(ConstantsHelper.WHITE_SPACE,
                new String[]{
                        getJavaCommand(),
                        (classpath + ConstantsHelper.PATH_SEPARATOR + ABSOLUTE_PATH_TO_RUNNER_CLASSES
                                + ConstantsHelper.PATH_SEPARATOR + ABSOLUTE_PATH_TO_JACOCO_DEPENDENCIES).replaceAll(" ", "%20"),
                        EntryPoint.jUnit5Mode ? EntryPoint.JUNIT5_JACOCO_RUNNER_COVERED_RESULT_PER_TEST_QUALIFIED_NAME : EntryPoint.JUNIT4_JACOCO_RUNNER_COVERED_RESULT_PER_TEST_QUALIFIED_NAME,
                        ParserOptions.FLAG_pathToCompiledClassesOfTheProject,
                        targetSourceClasses.stream().reduce((x, y) -> x + ConstantsHelper.PATH_SEPARATOR + y).get().replaceAll(" ", "%20"),
                        ParserOptions.FLAG_pathToCompiledTestClassesOfTheProject,
                        targetTestClasses.stream().reduce((x, y) -> x + ConstantsHelper.PATH_SEPARATOR + y).get().replaceAll(" ", "%20"),
                        ParserOptions.FLAG_fullQualifiedNameOfTestClassToRun,
                        String.join(ConstantsHelper.PATH_SEPARATOR, fullQualifiedNameOfTestClasses),
                        methodNames.length == 0 ? "" : ParserOptions.FLAG_testMethodNamesToRun + ConstantsHelper.WHITE_SPACE +
                                String.join(ConstantsHelper.PATH_SEPARATOR, methodNames),
                        EntryPoint.blackList.isEmpty() ? ""
                                : (ParserOptions.FLAG_blackList + ConstantsHelper.WHITE_SPACE
                                + String.join(ConstantsHelper.PATH_SEPARATOR, EntryPoint.blackList)),
                        EntryPoint.coverageDetail == ParserOptions.CoverageTransformerDetail.SUMMARIZED ? "" :
                                (ParserOptions.FLAG_coverage_detail + ConstantsHelper.WHITE_SPACE
                                        + EntryPoint.coverageDetail.name()),
                });
        try {
            EntryPoint.runGivenCommandLine(javaCommand);
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout when running {}", javaCommand);
            throw e;
        }
        final CoveredTestResultPerTestMethod load = CoveredTestResultPerTestMethodImpl.load();
        if (EntryPoint.verbose) {
            LOGGER.info("Coverage per test methods has been computed {}{}", ConstantsHelper.LINE_SEPARATOR,
                    load.toString());
        }
        return load;
    }

        /* COMPUTE MUTATION SCORE WITH PIT API */

    /**
     * @param classpath           the classpath of the project for which we need to compute the mutation score
     * @param pathToRootProject   the path to the root folder of the project
     * @param filterTargetClasses the regex to match source classes (application) to mutate
     * @param targetTests         the full qualified name of the test class for which we want to compute the mutation score.
     * @return a list of {@link eu.stamp_project.testrunner.listener.pit.AbstractPitResult} that contains the mutation analysis. The type depends on the format used.
     */
    public static List<? extends AbstractPitResult> runPit(final String classpath,
                                                           final String pathToRootProject,
                                                           final String filterTargetClasses,
                                                           final String targetTests) {
        final String classpathToExecute;
        if (jUnit5Mode) {
            classpathToExecute = String.join(ConstantsHelper.PATH_SEPARATOR,
                    new String[]{
                            classpath,
                            ABSOLUTE_PATH_TO_PIT_DEPENDENCIES,
                            ABSOLUTE_PATH_TO_PIT_DEPENDENCIES_FOR_JUNIT5
                    }
            );
        } else {
            classpathToExecute = String.join(ConstantsHelper.PATH_SEPARATOR,
                    new String[]{
                            classpath,
                            ABSOLUTE_PATH_TO_PIT_DEPENDENCIES
                    }
            );
        }
        PitRunner.main(
                new String[]{
                        classpathToExecute,
                        pathToRootProject,
                        filterTargetClasses,
                        targetTests,
                        EntryPoint.pitOutputFormat.name().toLowerCase(),
                        EntryPoint.mutationEngine.name().toLowerCase(),
                        String.join(ConstantsHelper.PATH_SEPARATOR, EntryPoint.pitMutators)
                }
        );
        return AbstractParser.build(EntryPoint.pitOutputFormat).parseAndDelete(
                pathToRootProject + PitRunner.REPORT_PITS
        );
    }

    private static void runGivenCommandLine(String commandLine) throws TimeoutException {
        List<String> command = Arrays.asList(commandLine.split(" "));
        command = command.stream().map(s -> s.replaceAll("%20", " ")).collect(Collectors.toList());

        if (EntryPoint.verbose) {
            LOGGER.info("Run: {}", command);
        }
        if (workingDirectory != null && !workingDirectory.exists()) {
            LOGGER.warn(
                    "The specified working directory does not exist: {}." + "{} Inherit from this process: {}."
                            + " Reset workingDirectory variable.",
                    workingDirectory.getAbsolutePath(), new File(".").getAbsolutePath());
            workingDirectory = null;
        }
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder().command(command);
            if (workingDirectory != null) {
                pb.directory(workingDirectory);
            }

            if (EntryPoint.verbose) {
                // Redirecting to given output stream
                if (EntryPoint.outPrintStream != null) {
                    pb.redirectOutput(Redirect.PIPE);
                    pb.redirectError(Redirect.PIPE);
                } else {
                    // Redirecting to main process IO (System out/err)
                    pb.inheritIO();
                }
            } else {
                // Redirecting to null file is required to avoid thread deadlocks (when verbose
                // is disabled)
                pb.redirectOutput(File. createTempFile("test-runner-error", ".tmp"))
                        .redirectErrorStream(true);
            }
            process = pb.start();
            if (EntryPoint.verbose && EntryPoint.outPrintStream != null) {
                inheritIO(process.getInputStream(), EntryPoint.outPrintStream);
                inheritIO(process.getErrorStream(), EntryPoint.outPrintStream);
            }

            boolean finished = process.waitFor(timeoutInMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                throw new RuntimeException("Forked process did not finished correctly");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
            if (!persistence) {
                reset();
            }
        }
    }

    /**
     * This method redirect Process IO to given destination using pipes
     *
     * @param src  process input stream
     * @param dest destination output stream
     */
    private static void inheritIO(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(src);
                while (sc.hasNextLine()) {
                    dest.println(sc.nextLine());
                }
                sc.close();
            }
        }).start();
    }

    private static void reset() {
        EntryPoint.JVMArgs = null;
        EntryPoint.workingDirectory = null;
        EntryPoint.timeoutInMs = EntryPoint.DEFAULT_TIMEOUT;
        EntryPoint.outPrintStream = null;
        EntryPoint.errPrintStream = null;
        EntryPoint.blackList.clear();
    }

    /*
     * FIELDS
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryPoint.class);

    private static final String JAVA_COMMAND = "java";

    private static final String CLASSPATH_OPT = "-classpath";

    private static final String JUNIT4_TEST_RUNNER_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.JUnit4Runner";

    private static final String JUNIT5_TEST_RUNNER_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.JUnit5Runner";

    private static final String JUNIT4_JACOCO_RUNNER_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.coverage.JUnit4JacocoRunner";

    private static final String JUNIT5_JACOCO_RUNNER_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.coverage.JUnit5JacocoRunner";

    private static final String JUNIT4_JACOCO_RUNNER_PER_TEST_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.coverage.JUnit4JacocoRunnerPerTestMethod";

    private static final String JUNIT5_JACOCO_RUNNER_PER_TEST_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.coverage.JUnit5JacocoRunnerPerTestMethod";

    private static final String JUNIT4_JACOCO_RUNNER_COVERED_RESULT_PER_TEST_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.coverage.JUnit4JacocoRunnerCoveredResultPerTestMethod";

    private static final String JUNIT5_JACOCO_RUNNER_COVERED_RESULT_PER_TEST_QUALIFIED_NAME = "eu.stamp_project.testrunner.runner.coverage.JUnit5JacocoRunnerCoveredResultPerTestMethod";

    private static final String ABSOLUTE_PATH_TO_RUNNER_CLASSES = initAbsolutePathToRunnerClasses();

    private static final int DEFAULT_TIMEOUT = 10000;

    static String getJavaCommand() {
        if (EntryPoint.JVMArgs != null && !EntryPoint.JVMArgs.isEmpty()) {
            return JAVA_COMMAND + ConstantsHelper.WHITE_SPACE + EntryPoint.JVMArgs + ConstantsHelper.WHITE_SPACE
                    + CLASSPATH_OPT;
        } else {
            return JAVA_COMMAND + ConstantsHelper.WHITE_SPACE + CLASSPATH_OPT;
        }
    }

    private static String RemoveWinFileSeparator(String string) {
        if (!"/".equals(ConstantsHelper.FILE_SEPARATOR) && string.startsWith(ConstantsHelper.FILE_SEPARATOR)) {
            return string.substring(1);
        } else {
            return string;
        }
    }

    private static final Function<List<Class<?>>, String> CLASSES_TO_PATH_OF_DEPENDENCIES = classes -> classes.stream()
            .map(clazz -> clazz.getProtectionDomain().getCodeSource().getLocation()).map(URL::getPath)
            .map(path -> path.startsWith("file:") ? path.substring("file:".length()) : path)
            .map(path -> path.split("!")[0]).map(path -> path.replace("/", ConstantsHelper.FILE_SEPARATOR))
            .map(EntryPoint::RemoveWinFileSeparator)
            .peek(path -> LOGGER.info("{}", path))
            .collect(Collectors.joining(ConstantsHelper.PATH_SEPARATOR));

    private static final List<Class<?>> JACOCO_DEPENDENCIES = Arrays.asList(IRuntime.class, FileUtils.class, ClassReader.class);

    private static final String ABSOLUTE_PATH_TO_JACOCO_DEPENDENCIES = CLASSES_TO_PATH_OF_DEPENDENCIES
            .apply(JACOCO_DEPENDENCIES);

    private static final List<Class<?>> PIT_DEPENDENCIES = Arrays.asList(
            GregorMutationEngine.class,
            TestGroupConfig.class,
            PluginServices.class,
            DescartesMutationEngine.class
    );

    private static final List<Class<?>> PIT_DEPENDENCIES_FOR_JUNIT5 = Arrays.asList(
            JUnit5Configuration.class,
            ExtensionContext.class,
            TestAbortedException.class
    );

    private static final String ABSOLUTE_PATH_TO_PIT_DEPENDENCIES = CLASSES_TO_PATH_OF_DEPENDENCIES
            .apply(PIT_DEPENDENCIES);

    private static final String ABSOLUTE_PATH_TO_PIT_DEPENDENCIES_FOR_JUNIT5 = CLASSES_TO_PATH_OF_DEPENDENCIES
            .apply(PIT_DEPENDENCIES_FOR_JUNIT5);

    private static String initAbsolutePathToRunnerClasses() {
        URL resource = ClassLoader.getSystemClassLoader().getResource("runner-classes/");

        // this is the way to make it work from a jenkins plugin
        if (resource == null) {
            resource = EntryPoint.class.getClassLoader().getResource("runner-classes/");
        }
        // if the resource is null, this is because of the usage of a custom class
        // loader.
        // For example, if we use the test-runner within a maven plugin, the resource
        // must be find using
        // ClassRealm#findResource(String)
        // to not add every dependencies to each case, we use here reflection
        if (resource == null) {
            // for now, we use specifically ClassRealm.
            // If we encounter new problems of the same type, i.e. class loading problem
            // we will extends this support other cases, but for now, we implement the way
            // for maven plugin.
            try {
                final Class<? extends ClassLoader> aClass = EntryPoint.class.getClassLoader().getClass();
                final Method findResources = aClass.getMethod("findResource", String.class);
                resource = (URL) findResources.invoke(EntryPoint.class.getClassLoader(), "runner-classes/");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        String path = resource.getPath();
        if (path.contains("!") && path.startsWith("file:")) {
            path = path.substring("file:".length()).split("!")[0];
        }
        path = RemoveWinFileSeparator(path.replace("/", ConstantsHelper.FILE_SEPARATOR));
        LOGGER.info("Path to runner Classes: {}", path);
        return path;
    }

}
