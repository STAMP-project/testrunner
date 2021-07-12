package eu.stamp_project.testrunner.listener.impl;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoveragePerTestMethod;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.TestResult;
import eu.stamp_project.testrunner.runner.Loader;
import eu.stamp_project.testrunner.utils.ConstantsHelper;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.runtime.RuntimeData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 15/11/18
 */
public class CoveragePerTestMethodImpl implements CoveragePerTestMethod {

    private static final long serialVersionUID = 606642107403361456L;

    protected final Map<String, Coverage> coverageResultsMap;

    protected final List<String> classesDirectory;

    protected transient RuntimeData data;

    protected transient ExecutionDataStore executionData;

    protected transient SessionInfoStore sessionInfos;

    protected transient CoverageTransformer coverageTransformer;

    public CoveragePerTestMethodImpl() {
        coverageResultsMap = null;
        classesDirectory = null;
        this.coverageTransformer = new CoverageCollectorSummarization();
    }

    public CoveragePerTestMethodImpl(RuntimeData data, List<String> classesDirectory, CoverageTransformer coverageTransformer) {
        this.data = data;
        this.classesDirectory = classesDirectory;
        this.coverageResultsMap = new HashMap<>();
        this.coverageTransformer = coverageTransformer;
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
    public static CoveragePerTestMethodImpl load() {
        return new Loader<CoveragePerTestMethodImpl>().load(SERIALIZE_NAME);
    }

    @Override
    public String toString() {
        return this.coverageResultsMap.keySet()
                        .stream()
                        .map(test -> "\t" + test + ": " + coverageResultsMap.get(test).toString())
                        .collect(Collectors.joining(ConstantsHelper.LINE_SEPARATOR));
    }
}
