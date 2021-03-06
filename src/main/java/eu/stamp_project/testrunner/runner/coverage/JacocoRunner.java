package eu.stamp_project.testrunner.runner.coverage;

import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.CoveredTestResult;
import eu.stamp_project.testrunner.listener.TestResult;
import eu.stamp_project.testrunner.listener.impl.CoverageCollectorDetailed;
import eu.stamp_project.testrunner.listener.impl.CoverageCollectorMethodDetailed;
import eu.stamp_project.testrunner.listener.impl.CoverageCollectorSummarization;
import eu.stamp_project.testrunner.runner.Failure;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.ResourceBundle.clearCache;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 18/12/17
 */
public abstract class JacocoRunner {

    protected MemoryClassLoader instrumentedClassLoader;

    protected Instrumenter instrumenter;

    protected IRuntime runtime;

    protected List<String> blackList;

    protected CoverageTransformer coverageTransformer;

    /**
     * @param classesDirectory     the path to the directory that contains the .class file of sources
     * @param testClassesDirectory the path to the directory that contains the .class file of test sources
     */
    public JacocoRunner(String classesDirectory, String testClassesDirectory, CoverageTransformer coverageTransformer) {
        this(classesDirectory, testClassesDirectory, Collections.emptyList(), coverageTransformer);
    }

    /**
     * @param classesDirectory     the path to the directory that contains the .class file of sources
     * @param testClassesDirectory the path to the directory that contains the .class file of test sources
     * @param blackList            the names of the test methods to NOT be run.
     */
    public JacocoRunner(String classesDirectory, String testClassesDirectory, List<String> blackList, CoverageTransformer coverageTransformer) {
        try {
            this.instrumentedClassLoader = new MemoryClassLoader(
                    new URL[]{
                            new File(classesDirectory).toURI().toURL(),
                            new File(testClassesDirectory).toURI().toURL()
                    }
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        this.blackList = blackList;
        this.runtime = new LoggerRuntime();
        this.instrumenter = new Instrumenter(this.runtime);
        this.coverageTransformer = coverageTransformer;
        // instrument source code
        instrumentAll(classesDirectory);
    }

    
	protected void recreateInstrumentedClassloaded(String classpath, String classesDirectory, Map<String, byte[]> definitions) {

		URLClassLoader urlloader = getUrlClassloaderFromClassPath(classpath);

		recreateInstrumentedClassloaded(urlloader, classesDirectory, definitions);

	}

	protected void recreateInstrumentedClassloaded(ClassLoader urlloader, String classesDirectory, Map<String, byte[]> definitions) {
		try {

			String[] dirs = classesDirectory.split(File.pathSeparator);

			URL[] urls = new URL[dirs.length];

			for (int i = 0; i < dirs.length; i++) {
				String dir = dirs[i];
				urls[i] = new File(dir).toURI().toURL();
			}

			this.instrumentedClassLoader = new MemoryClassLoader(urls, urlloader);
			this.instrumentedClassLoader.setDefinitions(definitions);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
    
    /**
     * Compute the instruction coverage of the given tests
     * Using directly this method is discouraged, since it won't avoid class loading conflict. Use {@link EntryPoint#runCoverage(String, String, String[], String[])} instead.
     *
     * @param classesDirectory             the path to the directory that contains the .class file of sources
     * @param testClassesDirectory         the path to the directory that contains the .class file of test sources
     * @param fullQualifiedNameOfTestClass the full qualified name of the test class to execute
     * @param testMethodNames              the simple names of the test methods to exeecute
     * @return a {@link Coverage} instance that contains the instruction coverage of the given tests.
     */
    public Coverage run(String classesDirectory,
                        String testClassesDirectory,
                        String fullQualifiedNameOfTestClass,
                        String... testMethodNames) {
        final RuntimeData data = new RuntimeData();
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        URLClassLoader classLoader;
        try {
            classLoader = new URLClassLoader(new URL[]
                    {new File(testClassesDirectory).toURI().toURL()}, this.instrumentedClassLoader);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        final String resource = ConstantsHelper.fullQualifiedNameToPath.apply(fullQualifiedNameOfTestClass) + ".class";
        try {
            this.instrumentedClassLoader.addDefinition(
                    fullQualifiedNameOfTestClass,
                    IOUtils.toByteArray(classLoader.getResourceAsStream(resource))
            );
            runtime.startup(data);
            final TestResult listener = this.executeTest(new String[]{fullQualifiedNameOfTestClass}, testMethodNames, Collections.emptyList());
            if (!((TestResult) listener).getFailingTests().isEmpty()) {
                System.err.println("Some test(s) failed during computation of coverage:\n" +
                        ((TestResult) listener).getFailingTests()
                                .stream()
                                .map(Failure::toString)
                                .collect(Collectors.joining("\n"))
                );
            }
            data.collect(executionData, sessionInfos, false);
            runtime.shutdown();
            clearCache(this.instrumentedClassLoader);

            Coverage computedCoverage =  coverageTransformer.transformJacocoObject(executionData, classesDirectory);
            return computedCoverage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract CoveredTestResult executeTest(String[] testClassNames,
													 String[] testMethodNames,
													 List<String> blackList);
    





    /**
     * Compute the instruction coverage of the given tests
     * Using directly this method is discouraged, since it won't avoid class loading conflict. Use {@link EntryPoint#runCoverage(String, String, String[], String[])} instead.
     *
     * @param classesDirectory               the path to the directory that contains the .class file of sources
     * @param testClassesDirectory           the path to the directory that contains the .class file of test sources
     * @param fullQualifiedNameOfTestClasses the full qualified names of the test classes to execute
     * @return a {@link Coverage} instance that contains the instruction coverage of the given tests.
     */
    public Coverage run(String classesDirectory,
                        String testClassesDirectory,
                        String... fullQualifiedNameOfTestClasses) {
        final RuntimeData data = new RuntimeData();
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        URLClassLoader classLoader;
        try {
            classLoader = new URLClassLoader(new URL[]
                    {new File(testClassesDirectory).toURI().toURL()}, this.instrumentedClassLoader);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Arrays.stream(fullQualifiedNameOfTestClasses).forEach(fullQualifiedNameOfTestClass -> {
            final String resource = ConstantsHelper.fullQualifiedNameToPath.apply(fullQualifiedNameOfTestClass) + ".class";
            try {
                this.instrumentedClassLoader.addDefinition(
                        fullQualifiedNameOfTestClass,
                        IOUtils.toByteArray(classLoader.getResourceAsStream(resource)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            runtime.startup(data);
            CoveredTestResult listener = this.executeTest(fullQualifiedNameOfTestClasses, new String[0], this.blackList);

            if (!listener.getFailingTests().isEmpty()) {
                System.err.println("Some test(s) failed during computation of coverage:\n" +
                        ((TestResult) listener).getFailingTests()
                                .stream()
                                .map(Failure::toString)
                                .collect(Collectors.joining("\n"))
                );
            }
            data.collect(executionData, sessionInfos, false);
            runtime.shutdown();
            clearCache(this.instrumentedClassLoader);
            Coverage coverage = coverageTransformer.transformJacocoObject(executionData, classesDirectory);
            
            listener.setCoverageInformation(coverage);
            
            return listener;
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * We indicate the method to compute the coverage
     * @param parentClassloader 
     * @return
     */
    public CoveredTestResult run(CoverageTransformer coverageCollector, ClassLoader parentClassloader, String classesDirectory, String testClassesDirectory,
								 String fullQualifiedNameOfTestClass, boolean coverTest, String... testMethodNames) {

		final RuntimeData data = new RuntimeData();
		final ExecutionDataStore executionData = new ExecutionDataStore();
		final SessionInfoStore sessionInfos = new SessionInfoStore();
		
		try {
		
			runtime.startup(data);
			
			//TODO: I dont understand why to re-generate the class loader each time we execute the test. If I remove it, it does not compute the coverage  from the second execution
			String classesToInstrument =classesDirectory +File.pathSeparator+ testClassesDirectory ;
			
			this.recreateInstrumentedClassloaded(parentClassloader, classesToInstrument,
					this.instrumentedClassLoader.getDefinitions());

			final CoveredTestResult listener = executeTest(new String[] { fullQualifiedNameOfTestClass }, testMethodNames,
					Collections.emptyList());

			if (!listener.getFailingTests().isEmpty()) {
				System.err.println("Some test(s) failed during computation of coverage:\n" + ((TestResult) listener)
						.getFailingTests().stream().map(Failure::toString).collect(Collectors.joining("\n")));
			}

		
			data.collect(executionData, sessionInfos, false);

			runtime.shutdown();

			clearCache(this.instrumentedClassLoader);

	        String pathToCover = (coverTest)? classesToInstrument: classesDirectory;
			
			Coverage coverageSource = coverageCollector.transformJacocoObject(executionData, pathToCover);
	
			listener.setCoverageInformation(coverageSource);
			return listener;

		} catch (

		Exception e) {
			System.out.println("Error: could not collect test data");
			e.printStackTrace();
			return null;
		}

	}
    
	
    public void instrumentAll(String classesDirectory) {
        final Iterator<File> iterator = FileUtils.iterateFiles(new File(classesDirectory), new String[]{"class"}, true);
        while (iterator.hasNext()) {
            final File next = iterator.next();
            final String fileName = next.getPath().substring(classesDirectory.length() + (classesDirectory.endsWith(ConstantsHelper.FILE_SEPARATOR) ? 0 : 1));
            final String fullQualifiedName = ConstantsHelper.pathToFullQualifiedName.apply(fileName).substring(0, fileName.length() - ".class".length());
            try {
                instrumentedClassLoader.addDefinition(fullQualifiedName,
                        instrumenter.instrument(instrumentedClassLoader.getResourceAsStream(fileName), fullQualifiedName));
            } catch (IOException e) {
                throw new RuntimeException(fileName + "," + new File(fileName).getAbsolutePath() +
                        "," + fullQualifiedName, e);
            }
        }
        clearCache(instrumentedClassLoader);
    }
    
    public MemoryClassLoader getInstrumentedClassLoader() {
		return instrumentedClassLoader;
	}

  	public URLClassLoader getUrlClassloader(String[] classpath, String classesDirectory, String testClassesDirectory) {
  		URLClassLoader classLoader;
  		URL[] urls = new URL[classpath.length + 2];

  		try {

  			for (int i = 0; i < classpath.length; i++) {
  				urls[i] = new File(classpath[i]).toURI().toURL();
  			}

  			urls[classpath.length] = new File(classesDirectory).toURI().toURL();

  			urls[classpath.length + 1] = new File(testClassesDirectory).toURI().toURL();

  			classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader()// this.instrumentedClassLoader
  			);


  		} catch (MalformedURLException e) {
  			throw new RuntimeException(e);
  		}
  		return classLoader;
  	}
  	
  	/**
  	 * Return a classpath with all the dependencies.
  	 * 
  	 * @param classpath
  	 * @param classesDirectory
  	 * @param testClassesDirectory
  	 * @return
  	 */
  	public URLClassLoader getUrlClassloaderFromClassPath(String classpath) {
  		URLClassLoader classLoader;
  		String[] cps = classpath.split(File.pathSeparator);
  		URL[] urls = new URL[cps.length];

  		try {

  			for (int i = 0; i < cps.length; i++) {
  	
  				urls[i] = new File(cps[i]).toURI().toURL();
  			}

  			classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

  		} catch (MalformedURLException e) {
  			throw new RuntimeException(e);
  		}
  		return classLoader;
  	}

}
