package eu.stamp_project.testrunner.listener.impl;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * It computes the coverage and summarizes the results i.e., number of total lines, and number of covered lines.
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 14/11/18
 */
public class CoverageCollectorSummarization implements CoverageTransformer {

    private static final long serialVersionUID = 109548359596802378L;


    public CoverageCollectorSummarization() {
        // empty
    }


    public static List<Integer> getListOfCountForCounterFunction(IClassCoverage coverage,
                                                                 Function<ICounter, Integer> counterGetter) {
        return coverage.getMethods()
                .stream()
                .filter(iMethodCoverage -> !"<clinit>".equals(iMethodCoverage.getName()))
                .flatMap(iMethodCoverage ->
                        IntStream.range(iMethodCoverage.getFirstLine(), iMethodCoverage.getLastLine() + 1)
                                .mapToObj(iMethodCoverage::getLine)
                                .map(ILine::getInstructionCounter)
                                .map(counterGetter)
                ).collect(Collectors.toList());
    }

  

    @Override
    public Coverage transformJacocoObject(ExecutionDataStore executionData, String classesDirectory) {
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        try {
            //TODO: change the interface to an array of URL
            String[] paths = classesDirectory.split(File.pathSeparator);
            for (String path : paths) {
                analyzer.analyzeAll(new File(path));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final int[] counter = new int[2];
        final StringBuilder builderExecutionPath = new StringBuilder();
        coverageBuilder.getClasses().forEach(coverage -> {
            final List<Integer> listOfCountForCounterFunction =
                    CoverageCollectorSummarization.getListOfCountForCounterFunction(coverage, ICounter::getCoveredCount);
            builderExecutionPath.append(coverage.getName())
                    .append(":")
                    .append(listOfCountForCounterFunction
                            .stream()
                            .map(Objects::toString)
                            .collect(Collectors.joining(","))
                    ).append(";");
            counter[0] += listOfCountForCounterFunction.stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            counter[1] += CoverageCollectorSummarization.getListOfCountForCounterFunction(coverage, ICounter::getTotalCount)
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        });
        
        Coverage coverage = new CoverageImpl( counter[0], counter[1],builderExecutionPath.toString());
        return coverage;
    }

}
