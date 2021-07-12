package eu.stamp_project.testrunner.listener.impl;

import eu.stamp_project.testrunner.listener.CoverageTransformer;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One implementation of {@link CoverageTransformer} that returns a {@link CoverageDetailed}.
 */
public class CoverageCollectorDetailed implements CoverageTransformer {

	private static final long serialVersionUID = 109548359596802378L;

	@Override
	public CoverageDetailed transformJacocoObject(ExecutionDataStore executionData, List<String> classesDirectory) {

	    CoverageInformation covered = new CoverageInformation();
		
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
		try {
			for (String directory : classesDirectory) {
				analyzer.analyzeAll(new File(directory));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (IClassCoverage classCoverage : coverageBuilder.getClasses()) {

			Map<Integer, Integer> covClass = new HashMap<>();

			for (IMethodCoverage methodCoverage : classCoverage.getMethods()) {

				if (!"<clinit>".equals(methodCoverage.getName())) {

					for (int i = methodCoverage.getFirstLine(); i <= methodCoverage.getLastLine() + 1; i++) {
						int coveredI = methodCoverage.getLine(i).getInstructionCounter().getCoveredCount();
						covClass.put(i, coveredI);
					}

				}
			}
			CoverageFromClass l = new CoverageFromClass(classCoverage.getName(), classCoverage.getPackageName(),
					classCoverage.getFirstLine(), classCoverage.getLastLine(), covClass);

			covered.put(classCoverage.getName(), l);

		}
		return new CoverageDetailed(covered);
	}



}
