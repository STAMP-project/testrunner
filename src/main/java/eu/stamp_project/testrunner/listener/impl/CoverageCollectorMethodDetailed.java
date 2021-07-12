package eu.stamp_project.testrunner.listener.impl;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CoverageCollectorMethodDetailed implements CoverageTransformer {
    @Override
    public Coverage transformJacocoObject(ExecutionDataStore executionData, List<String> classesDirectory) {
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        try {
            for (String directory : classesDirectory) {
                analyzer.analyzeAll(new File(directory));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final int[] counter = new int[2];
        final StringBuilder builderExecutionPath = new StringBuilder();
        coverageBuilder.getClasses().forEach(coverage -> {
            final List<Integer> listOfCountForCounterFunction =
                    CoverageImpl.getListOfCountForCounterFunction(coverage, ICounter::getCoveredCount);
            builderExecutionPath.append(coverage.getName())
                    .append(":")
                    .append(getCoverageInformationPerMethod(coverage,  ICounter::getCoveredCount))
                   .append("-");
            counter[0] += listOfCountForCounterFunction.stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            counter[1] += CoverageImpl.getListOfCountForCounterFunction(coverage, ICounter::getTotalCount)
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        });

        Coverage coverage = new CoverageImpl( counter[0], counter[1],builderExecutionPath.toString());
        return coverage;
    }

    public static String getCoverageInformationPerMethod(IClassCoverage coverage,
                                                         Function<ICounter, Integer> counterGetter) {
        StringBuilder builder = new StringBuilder();
        coverage.getMethods()
                .stream()
                .filter(iMethodCoverage -> !"<clinit>".equals(iMethodCoverage.getName()))
                .forEach(iMethodCoverage -> {
                    builder.append(iMethodCoverage.getName()).append("+");
                    builder.append(iMethodCoverage.getDesc()).append("+");
                    builder.append(IntStream.range(iMethodCoverage.getFirstLine(), iMethodCoverage.getLastLine() + 1)
                                            .mapToObj(iMethodCoverage::getLine)
                                            .map(ILine::getInstructionCounter)
                                            .map(counterGetter)
                                            .map(Object::toString)
                                            .collect(Collectors.joining(",")));
                    builder.append("|");
                });
        if (builder.length() > 0) {
            builder.replace(builder.length()-1, builder.length(),"");
        }
        return builder.toString();
    }
}
