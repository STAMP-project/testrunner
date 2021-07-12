package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.testrunner.listener.CoveragePerTestMethod;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResult;
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
import java.util.stream.Stream;

import static java.util.ResourceBundle.clearCache;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 27/11/18
 */
public abstract class JacocoRunnerPerTestMethod extends JacocoRunner {

    /**
     * Compute the instruction coverage of the given tests per test methods
     * Using directly this method is discouraged, since it won't avoid class loading conflict. Use {@link EntryPoint#runCoverage(String, String, String[], String[])} instead.
     *
     * @param classesDirectory             the path to the directory that contains the .class file of sources
     * @param testClassesDirectory         the path to the directory that contains the .class file of test sources
     * @param testClassNames               the fully qualified name of the test classes to execute
     * @param testMethodNames              the simple or fully qualified names of the test methods to execute
     * @return a {@link CoveragePerTestMethod} instance that contains the instruction coverage of the given tests.
     */
    public CoveragePerTestMethod runCoveragePerTestMethod(List<String> classesDirectory,
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
            for (String fullyQualifiedClassName : testClassNames) {
                String resource = ConstantsHelper.fullQualifiedNameToPath.apply(fullyQualifiedClassName) + ".class";
                this.instrumentedClassLoader.addDefinition(
                        fullyQualifiedClassName,
                        IOUtils.toByteArray(classLoader.getResourceAsStream(resource))
                );
            }
            this.runtime.startup(data);
            final CoveragePerTestMethod listener = this.executeTestPerTestMethod(data, classesDirectory, testClassNames, testMethodNames);
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
    protected CoveredTestResult executeTest(String[] testClassNames, String[] testMethodNames, List<String> blackList) {
        throw new UnsupportedOperationException();
    }

    protected abstract CoveragePerTestMethod executeTestPerTestMethod(RuntimeData data,
                                                                      List<String> classesDirectory,
                                                                      String[] testClassNames,
                                                                      String[] testMethodNames);

    /**
     * @param classesDirectory     the path to the directory that contains the .class file of sources
     * @param testClassesDirectory the path to the directory that contains the .class file of test sources
     */
    public JacocoRunnerPerTestMethod(List<String> classesDirectory, List<String> testClassesDirectory, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, coverageTransformer);
    }

    /**
     * @param classesDirectory     the path to the directory that contains the .class file of sources
     * @param testClassesDirectory the path to the directory that contains the .class file of test sources
     * @param blackList            the names of the test methods to NOT be run.
     */
    public JacocoRunnerPerTestMethod(List<String> classesDirectory, List<String> testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
        super(classesDirectory, testClassesDirectory, blackList, coverageTransformer);
    }
}
