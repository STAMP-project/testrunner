package eu.stamp_project.testrunner.runner;

import eu.stamp_project.testrunner.listener.Coverage;
import eu.stamp_project.testrunner.listener.CoverageTransformer;
import eu.stamp_project.testrunner.listener.impl.CoverageCollectorDetailed;
import eu.stamp_project.testrunner.listener.impl.CoverageCollectorMethodDetailed;
import eu.stamp_project.testrunner.listener.impl.CoverageCollectorSummarization;
import eu.stamp_project.testrunner.utils.ConstantsHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 27/11/18
 * <p>
 * This class aims at parsing options for
 * - {@link JUnit4Runner}, {@link JUnit5Runner}, {@link eu.stamp_project.testrunner.runner.coverage.JacocoRunner}
 */
public class ParserOptions {

    private static final Function<String, List<String>> convertArrayToList =
            value -> Arrays.asList(value.split(ConstantsHelper.PATH_SEPARATOR));

    public static ParserOptions parse(String[] args) {
        System.out.println(String.format("Parsing %s", String.join(" ", args)));
        final ParserOptions parserOptions = new ParserOptions();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case FLAG_pathToCompiledClassesOfTheProject:
                    parserOptions.pathToCompiledClassesOfTheProject = args[++i];
                    break;
                case FLAG_fullQualifiedNameOfTestClassToRun:
                    parserOptions.fullQualifiedNameOfTestClassesToRun = args[++i].split(ConstantsHelper.PATH_SEPARATOR);
                    break;
                case FLAG_testMethodNamesToRun:
                    parserOptions.testMethodNamesToRun = args[++i].split(ConstantsHelper.PATH_SEPARATOR);
                    break;
                case FLAG_blackList:
                    parserOptions.blackList = convertArrayToList.apply(args[++i]);
                    break;
                case FLAG_coverage_detail:
                    parserOptions.coverageTransformerDetail = CoverageTransformerDetail.valueOf(args[++i]);
                    break;
                case FLAG_cover_tests:
                    parserOptions.coverTests = true;
                    break;
                case " ":
                case "":
                    break;
                default:
                    System.err.println(String.format("[ERROR]: %s is not a supported command line options", args[i]));
                    usage();
            }
        }
        return parserOptions;
    }

    private static void usage() {
        final StringBuilder usage = new StringBuilder();
        usage.append("Usage:").append(ConstantsHelper.LINE_SEPARATOR);

        usage.append(FLAG_pathToCompiledClassesOfTheProject).append(ConstantsHelper.WHITE_SPACE)
                .append(FLAG_HELP_pathToCompiledClassesOfTheProject).append(ConstantsHelper.LINE_SEPARATOR);

        usage.append(FLAG_fullQualifiedNameOfTestClassToRun).append(ConstantsHelper.WHITE_SPACE)
                .append(FLAG_HELP_fullQualifiedNameOfTestClassToRun).append(ConstantsHelper.LINE_SEPARATOR);

        usage.append(FLAG_testMethodNamesToRun).append(ConstantsHelper.WHITE_SPACE)
                .append(FLAG_HELP_testMethodNamesToRun).append(ConstantsHelper.LINE_SEPARATOR);

        usage.append(FLAG_blackList).append(ConstantsHelper.WHITE_SPACE)
                .append(FLAG_HELP_blackList).append(ConstantsHelper.LINE_SEPARATOR);

        usage.append(FLAG_coverage_detail).append(ConstantsHelper.WHITE_SPACE)
             .append(FLAG_HELP_coverage_detail).append(ConstantsHelper.LINE_SEPARATOR);

        usage.append(FLAG_cover_tests).append(ConstantsHelper.WHITE_SPACE)
                .append(FLAG_HELP_cover_tests).append(ConstantsHelper.LINE_SEPARATOR);

        System.out.println(usage.toString());
    }

    /**
     * This string represents the path to the compiles classes, sources and tests, of the project.
     * These two paths should be separated by the system path separator, e.g. ':' on Linux.
     */
    private String pathToCompiledClassesOfTheProject;

    public static final String FLAG_pathToCompiledClassesOfTheProject = "--binaries";

    private static final String FLAG_HELP_pathToCompiledClassesOfTheProject = "This flag must be followed by the path of both directories of sources and test binaries. Both paths must be separated by the system path separator, e.g. ':' on Linux";

    /**
     * This list the full qualified names of the test classes to run.
     * For example, eu.stamp_project.my.project.MyClassTest:eu.stamp_project.my.project.MySecondClassTest
     */
    private String[] fullQualifiedNameOfTestClassesToRun;

    public static final String FLAG_fullQualifiedNameOfTestClassToRun = "--class";

    public static final String FLAG_HELP_fullQualifiedNameOfTestClassToRun = "This flag must be followed by the full qualified names of test classes to be run. Names must be separated by the system path separator, e.g. ':' on Linux";

    /**
     * This list is the simple names of test method to run.
     * Simples names must be separated by the system path separator, e.g. ':' on Linux.
     */
    private String[] testMethodNamesToRun;

    public static final String FLAG_testMethodNamesToRun = "--tests";

    public static final String FLAG_HELP_testMethodNamesToRun = "This flag must be followed by the list of simple names of test methods to be run. Names must be separated by the system path separator, e.g. ':' on Linux";

    /**
     * This list is the simple names of test method to NOT run
     * Simples names must be separated by the system path separator, e.g. ':' on Linux.
     */
    private List<String> blackList;

    public static final String FLAG_blackList = "--blacklist";

    public static final String FLAG_HELP_blackList = "This flag must be followed by the list of simple names of test methods to NOT be run. Names must be separated by the system path separator, e.g. ':' on Linux";



    public enum CoverageTransformerDetail {
        SUMMARIZED,
        DETAIL,
        METHOD_DETAIL
    }
    /**
     * This value represents at which level of detail coverage information should be provided
     */
    private CoverageTransformerDetail coverageTransformerDetail;

    public static final String FLAG_coverage_detail = "--coverage-detail";

    public static final String FLAG_HELP_coverage_detail = "The value following this flag defines the level of detail" +
                                                           " provided in the coverage information. Valid values:" +
                                                           "'SUMMARIZED' (default), 'DETAIL' or 'METHOD_DETAIL'.";

    /**
     * This value whether test classes are to be included in the coverage results
     */
    private boolean coverTests;

    public static final String FLAG_cover_tests = "--cover-tests";

    public static final String FLAG_HELP_cover_tests = "The usage of this flag results in test classes being included in the coverage results.";


    private ParserOptions() {
        this.pathToCompiledClassesOfTheProject = "";
        this.fullQualifiedNameOfTestClassesToRun = new String[]{};
        this.testMethodNamesToRun = new String[]{};
        this.blackList = new ArrayList<>();
        this.coverageTransformerDetail = CoverageTransformerDetail.SUMMARIZED;
        this.coverTests = false;
    }

    public String getPathToCompiledClassesOfTheProject() {
        return pathToCompiledClassesOfTheProject;
    }

    public String[] getFullQualifiedNameOfTestClassesToRun() {
        return fullQualifiedNameOfTestClassesToRun;
    }

    public String[] getTestMethodNamesToRun() {
        return testMethodNamesToRun;
    }

    public List<String> getBlackList() {
        return blackList;
    }

    public CoverageTransformer getCoverageTransformer() {
        switch (coverageTransformerDetail) {
            case DETAIL:
                return new CoverageCollectorDetailed();
            case METHOD_DETAIL:
                return new CoverageCollectorMethodDetailed();
            case SUMMARIZED:
            default:
                return new CoverageCollectorSummarization();
        }

    }

    public boolean isCoverTests() {
        return coverTests;
    }
}
