package fr.inria.astor.core.faultlocalization.gzoltar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.gzoltar.core.test.TestMethod;
import com.gzoltar.core.test.TestRunner;
import com.gzoltar.core.test.TestTask;
import com.gzoltar.core.test.junit.JUnitTestTask;
import com.gzoltar.core.util.ClassType;

import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.faultlocalization.FaultLocalizationResult;
import fr.inria.astor.core.faultlocalization.FaultLocalizationStrategy;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;

/**
 * Facade of Fault Localization techniques like GZoltar or own implementations
 * (package {@link org.inria.sacha.faultlocalization}.).
 * 
 * @author Matias Martinez, matias.martinez@inria.fr
 *
 */
public class NovelGZoltarFaultLocalization implements FaultLocalizationStrategy {

	public static final String PACKAGE_SEPARATOR = "-";
	Logger logger = Logger.getLogger(NovelGZoltarFaultLocalization.class.getName());

	public FaultLocalizationResult searchSuspicious(ProjectRepairFacade project,
			List<String> regressionTestForFaultLocalization) throws Exception {

		return this.calculateSuspicious(
				ConfigurationProperties.getProperty("location") + File.separator
						+ ConfigurationProperties.getProperty("srcjavafolder"),
				project.getOutDirWithPrefix(ProgramVariant.DEFAULT_ORIGINAL_VARIANT),
				ConfigurationProperties.getProperty("packageToInstrument"), ProgramVariant.DEFAULT_ORIGINAL_VARIANT,
				project.getProperties().getFailingTestCases(), regressionTestForFaultLocalization,
				ConfigurationProperties.getPropertyBool("regressionforfaultlocalization"), project);

	}

	private FaultLocalizationResult calculateSuspicious(String locationSrc, String locationBytecode,
			String packageToInst, String mutatorIdentifier, List<String> failingTest, List<String> allTest,
			boolean mustRunAllTest, ProjectRepairFacade project) throws Exception {

		System.out.println("Calculating suspicious");

		String output = project.getProperties().getWorkingDirRoot();

		String noout = (ConfigurationProperties.hasProperty("outfl") ? ConfigurationProperties.getProperty("outfl")
				: output);

		// Outs
		String outputdirGzoltar = noout + File.separator + "outputgzoltar";
		File fileOutGzoltar = (new File(outputdirGzoltar));
		if (!fileOutGzoltar.exists()) {
			fileOutGzoltar.mkdirs();
		}
		String serfile = outputdirGzoltar + File.separator + "gzoltar_mm.ser";
		String pathTestsFiles = noout + File.separator + "outTest.txt";

		// Info related to project
		String src_classes_dir = String.join(File.pathSeparator, project.getProperties().getOriginalAppBinDir());
		String testClassPath = String.join(File.pathSeparator, project.getProperties().getOriginalTestBinDir());

		// Junit path
		String junitpath = retrieveJUnitLibPath();

		String gzoltarversion = "1.7.4-SNAPSHOT";
		// GZoltar path
		String gzoltar_cli_jar = getGZoltarCLIPath(gzoltarversion);
		String gzoltar_agent_jar = getAgentRt(gzoltarversion);

		String commandGetTest = "java -cp " + src_classes_dir + ":" + testClassPath + ":" + junitpath + ":"
				+ gzoltar_cli_jar + "  com.gzoltar.cli.Main listTestMethods " + testClassPath + "    --outputFile "
				+ pathTestsFiles;

		System.out.println(commandGetTest);
		final Process p = Runtime.getRuntime().exec(commandGetTest);

		new Thread(new Runnable() {
			public void run() {
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = null;

				try {
					while ((line = input.readLine()) != null)
						System.out.println(line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

		p.waitFor();

		System.out.println("End obtaining test");

		String commandRunTestMethods = "java -XX:MaxPermSize=4096M -javaagent:" + gzoltar_agent_jar + "=destfile="
				+ serfile + ",buildlocation=" + src_classes_dir + ",inclnolocationclasses=false,output=FILE"
				+ "        -cp " + src_classes_dir + ":" + junitpath + ":" + testClassPath + ":" + gzoltar_cli_jar
				+ "  com.gzoltar.cli.Main runTestMethods " + "   --testMethods " + pathTestsFiles
				+ "  --collectCoverage";

		System.out.println(commandRunTestMethods);
		final Process p2 = Runtime.getRuntime().exec(commandRunTestMethods);

		new Thread(new Runnable() {
			public void run() {
				BufferedReader input = new BufferedReader(new InputStreamReader(p2.getInputStream()));
				String line = null;

				try {
					while ((line = input.readLine()) != null)
						System.out.println(line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

		p2.waitFor();

		System.out.println("Report: ");

		String runRapportCommand = "java -XX:MaxPermSize=4096M " + "-cp "

				+ src_classes_dir + ":"

				+ junitpath + ":"

				+ testClassPath + ":"

				+ gzoltar_cli_jar + "      com.gzoltar.cli.Main faultLocalizationReport " + "        --buildLocation "

				+ src_classes_dir + "        --granularity line " + "        --inclPublicMethods "
				+ "        --inclStaticConstructors " + "        --inclDeprecatedMethods " + "        --dataFile "
				+ serfile + "        --outputDirectory " + outputdirGzoltar + "        --family sfl "
				+ "        --formula ochiai " + "        --metric entropy " + "        --formatter txt";

		System.out.println(runRapportCommand);
		final Process processRunReport = Runtime.getRuntime().exec(runRapportCommand);

		new Thread(new Runnable() {
			public void run() {
				BufferedReader input = new BufferedReader(new InputStreamReader(processRunReport.getInputStream()));
				String line = null;

				try {
					while ((line = input.readLine()) != null)
						System.out.println(line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

		processRunReport.waitFor();

		File gzoltarOutFile = new File(outputdirGzoltar);
		FaultLocalizationResult result = parseOutputFile(gzoltarOutFile, 0.1);
		System.out.println(result);

		// Files.deleteIfExists(gzoltarOutFile.toPath());
		FileUtils.deleteDirectory(gzoltarOutFile);
		return result;

	}

	private String getAgentRt(String gzoltarversion) throws IllegalAccessException {

		String jarToFind = "com.gzoltar.agent.rt-" + gzoltarversion + "-all.jar";
		return getFromFolder(jarToFind);
	}

	private String getGZoltarCLIPath(String gzoltarversion) throws IllegalAccessException {

		String jarToFind = "com.gzoltar.cli-" + gzoltarversion + "-jar-with-dependencies.jar";

		return getFromFolder(jarToFind);

		// return getFromClassPath(jarToFind);

	}

	private String getFromFolder(String jarToFind) throws IllegalAccessException {
		String locationGzoltarJar = ConfigurationProperties.getProperty("locationGzoltarJar");

		if (locationGzoltarJar == null || locationGzoltarJar.isEmpty()) {
			locationGzoltarJar = "./lib/";

		}
		File f = new File(locationGzoltarJar + File.separator + jarToFind);

		if (!f.exists())
			throw new IllegalAccessException("We cannot localize the jar at " + f.getAbsolutePath());

		return f.getAbsolutePath();
	}

	private String getFromClassPath(String jarToFind) throws IllegalAccessException {
		String cp = System.getProperty("java.class.path");
		System.out.println("CP " + cp);
		String[] cps = cp.split(File.pathSeparator);
		String np = "";
		for (String aJar : cps) {

			if (aJar.contains(jarToFind)) {

				np += ((np.isEmpty()) ? "" : File.pathSeparator) + aJar;
			}

		}
		if (np.isEmpty()) {
			throw new IllegalAccessException("Could not find file with " + jarToFind);
		}

		System.out.println("gzoltar path " + np);
		return np;
	}

	private String retrieveJUnitLibPath() {

		String cp = System.getProperty("java.class.path");
		String[] cps = cp.split(File.pathSeparator);
		String np = "";
		for (String aJar : cps) {

			if (aJar.contains("junit-4.12.jar") || aJar.contains("hamcrest-core-1.3.jar")) {

				np += ((np.isEmpty()) ? "" : File.pathSeparator) + aJar;
			}

		}
		System.out.println("junit path " + np);
		// return
		// "/Users/matias/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/matias/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar";

		return np;
	}

	private void runLocally(List<String> allTest, boolean offline, boolean collectCoverage, boolean initTestClass,
			URL[] searchPathURLs) {
		for (String test : allTest) {

			String[] split = test.split(",");
			TestMethod testMethod = new TestMethod(ClassType.valueOf(split[0]), split[1]);

			TestTask testRunnerGzoltar = new JUnitTestTask(searchPathURLs, offline, collectCoverage, initTestClass,
					testMethod);
			TestRunner.run(testRunnerGzoltar);

		}
	}

	@Override
	public List<String> findTestCasesToExecute(ProjectRepairFacade projectFacade) {

		List<String> testall = null;
		try {
			testall = GzoltarTestClassesFinder.findIn(projectFacade);
			System.out.println("Test all " + testall);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return testall;

	}

	public SuspiciousCode parseLine(String line) {
		try {
			if (line.equals("Component,OCHIAI"))
				return null;
			SuspiciousCode sc = new SuspiciousCode();
			//
			String[] infoLine = line.split(";");

			sc.setSusp(Double.parseDouble(infoLine[1]));

			infoLine = infoLine[0].split(":");
			sc.setLineNumber(Integer.valueOf(infoLine[1]));

			infoLine = infoLine[0].split("#");

			String className = infoLine[0].replace("$", ".");
			sc.setClassName(className);

			String method = "";
			if (infoLine.length > 1)
				method = infoLine[1];//
			sc.setMethodName(method);

			return sc;
		} catch (Exception e) {
			logger.error("-->" + line);
			logger.error(e);
			e.printStackTrace();
			return null;
		}
	}

	public FaultLocalizationResult parseOutputFile(File path, Double thr) {

		List<SuspiciousCode> codes = analyzeSuspiciousValues(thr, path);

		FaultLocalizationResult result = new FaultLocalizationResult(codes);

		analyzeTestCaseExecution(path, result);

		return result;

	}

	private List<SuspiciousCode> analyzeSuspiciousValues(Double thr, File path) {
		List<SuspiciousCode> codes = new ArrayList<>();
		File spectrapath = new File(path.getAbsolutePath() + File.separator + "sfl" + File.separator + "txt"
				+ File.separator + "ochiai.ranking.csv");

		try (BufferedReader br = new BufferedReader(new FileReader(spectrapath))) {

			String line;
			while ((line = br.readLine()) != null) {
				SuspiciousCode sc = parseLine(line);
				if (sc != null && sc.getSuspiciousValue() > 0 && sc.getSuspiciousValue() >= thr)
					codes.add(sc);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return codes;
	}

	private void analyzeTestCaseExecution(File path, FaultLocalizationResult results) {
		File testpath = new File(path.getAbsolutePath() + File.separator + "sfl" + File.separator + "txt"
				+ File.separator + "tests.csv");
		List<String> failingTestCases = new ArrayList<>();
		List<String> allTestCases = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(testpath))) {

			String line;
			while ((line = br.readLine()) != null) {
				String[] lineS = line.split(",");
				String name = lineS[0].split("#")[0];
				if (lineS[1].equals("FAIL")) {
					if (!failingTestCases.contains(name))
						failingTestCases.add(name);
				}

				if (!allTestCases.contains(name))
					allTestCases.add(name);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		results.setFailingTestCases(failingTestCases);
		results.setExecutedTestCases(allTestCases);
	}

	private List<String> analyzeTestCaseExecution(File path) {
		File testpath = new File(path.getAbsolutePath() + File.separator + "sfl" + File.separator + "txt"
				+ File.separator + "tests.csv");
		List<String> failingTestCases = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(testpath))) {

			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				String[] lineS = line.split(",");
				if (lineS[1].equals("FAIL")) {
					String name = lineS[0].split("#")[0];
					if (!failingTestCases.contains(name))
						failingTestCases.add(name);
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return failingTestCases;
	}

}
