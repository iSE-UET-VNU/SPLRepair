package fr.inria.main.evolution;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.main.spl.SPLProduct;
import fr.inria.main.spl.SPLSystem;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import fr.inria.astor.approaches.cardumen.CardumenApproach;
import fr.inria.astor.approaches.deeprepair.DeepRepairEngine;
import fr.inria.astor.approaches.jgenprog.JGenProg;
import fr.inria.astor.approaches.jgenprog.extension.TibraApproach;
import fr.inria.astor.approaches.jkali.JKaliEngine;
import fr.inria.astor.approaches.jmutrepair.jMutRepairExhaustive;
import fr.inria.astor.approaches.scaffold.ScaffoldRepairEngine;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.ingredientbased.ExhaustiveIngredientBasedEngine;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.main.AbstractMain;
import fr.inria.main.ExecutionMode;
import fr.inria.main.ExecutionResult;

/**
 * Astor main
 *
 * @author Trang Nguyen, trang.nguyen@vnu.edu.vn
 *
 */
public class SPLRepairMain extends AbstractMain {
    SPLSystem buggy_spl_system = null;
    protected Logger log = Logger.getLogger(SPLRepairMain.class.getName());

    protected AstorCoreEngine core = null;

    /**
     * It creates a repair engine according to an execution mode.
     *
     *
     * @param mode
     * @return
     * @throws Exception
     */

    public AstorCoreEngine createEngine(ExecutionMode mode) throws Exception {
        core = null;
        MutationSupporter mutSupporter = new MutationSupporter();

        if (ExecutionMode.DeepRepair.equals(mode)) {
            core = new DeepRepairEngine(mutSupporter, projectFacade);

        } else if (ExecutionMode.CARDUMEN.equals(mode)) {
            core = new CardumenApproach(mutSupporter, projectFacade);

        } else if (ExecutionMode.jKali.equals(mode)) {
            core = new JKaliEngine(mutSupporter, projectFacade);

        } else if (ExecutionMode.jGenProg.equals(mode)) {
            core = new JGenProg(mutSupporter, projectFacade);

        } else if (ExecutionMode.MutRepair.equals(mode)) {
            core = new jMutRepairExhaustive(mutSupporter, projectFacade);

        } else if (ExecutionMode.EXASTOR.equals(mode)) {
            core = new ExhaustiveIngredientBasedEngine(mutSupporter, projectFacade);

        } else if (ExecutionMode.TIBRA.equals(mode)) {
            core = new TibraApproach(mutSupporter, projectFacade);

        } else if (ExecutionMode.SCAFFOLD.equals(mode)) {
            core = new ScaffoldRepairEngine(mutSupporter, projectFacade);

        } else {
            // If the execution mode is any of the predefined, Astor
            // interpretes as
            // a custom engine, where the value corresponds to the class name of
            // the engine class
            String customengine = ConfigurationProperties.getProperty(ExtensionPoints.NAVIGATION_ENGINE.identifier);
            core = createEngineFromArgument(customengine, mutSupporter, projectFacade);
        }

        // Loading extension Points
        core.loadExtensionPoints();

        core.initModel();

        if (ConfigurationProperties.getPropertyBool("skipfaultlocalization")) {
            // We dont use FL, so at this point the do not have suspicious
            core.initPopulation(new ArrayList<SuspiciousCode>());
        } else if (ConfigurationProperties.getPropertyBool("readfaultlocalizationresultsfromfile")) {
            //We used FL results stored in file
            File FL_file = new File("/Users/thu-trangnguyen/Documents/Projects/SPLRepair/examples/SPL/_MultipleBugs_.NOB_1.ID_4/variants/model_m_ca4_0002/varcop_fl_results.txt");
            Scanner sc = new Scanner(FL_file);
            List<SuspiciousCode> suspicious = new ArrayList<>();
            while (sc.hasNext()){
                String tmp = sc.nextLine();
                if(("\""+tmp.replace("\n", "")+"\"").equals(ConfigurationProperties.getProperty("defaultSBFLmetrics"))){
                    while(sc.hasNext()){
                        tmp = sc.nextLine();
                        if(tmp.equals("---------")) break;

                        String classname = tmp.split(" ")[0];
                        int line_number = Integer.valueOf(tmp.split(" ")[1]);
                        float susp_score = Float.valueOf(tmp.split(" ")[2]);

                        SuspiciousCode e = new SuspiciousCode (classname, null, line_number, susp_score, null);
                        suspicious.add(e);
                    }
                    break;
                }
            }
            // If the FL result is empty, we do not use FL
            if (suspicious == null || suspicious.isEmpty()) {
                suspicious = new ArrayList<SuspiciousCode>();
            }

            core.initPopulation(suspicious);

        } else {
            List<SuspiciousCode> suspicious = core.calculateSuspicious();
            if (suspicious == null || suspicious.isEmpty()) {
                throw new IllegalStateException("No suspicious line detected by the fault localization");
            }

            core.initPopulation(suspicious);
        }

        return core;

    }

    /**
     * We create an instance of the Engine which name is passed as argument.
     *
     * @param customEngineValue
     * @param mutSupporter
     * @param projectFacade
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected AstorCoreEngine createEngineFromArgument(String customEngineValue, MutationSupporter mutSupporter,
                                                       ProjectRepairFacade projectFacade) throws Exception {
        Object object = null;
        try {
            Class classDefinition = Class.forName(customEngineValue);
            object = classDefinition.getConstructor(mutSupporter.getClass(), projectFacade.getClass())
                    .newInstance(mutSupporter, projectFacade);
        } catch (Exception e) {
            log.error("Loading custom engine: " + customEngineValue + " --" + e);
            throw new Exception("Error Loading Engine: " + e);
        }
        if (object instanceof AstorCoreEngine)
            return (AstorCoreEngine) object;
        else
            throw new Exception(
                    "The strategy " + customEngineValue + " does not extend from " + AstorCoreEngine.class.getName());

    }



    @Override
    public ExecutionResult run(String location, String projectName, String dependencies, String packageToInstrument,
                               double thfl, String failing) throws Exception {
        buggy_spl_system = new SPLSystem(location);
        List<String> failing_products = buggy_spl_system.getFailing_products();
        ExecutionResult result = null;
        if(failing_products == null || failing_products.isEmpty()){
            log.error("SPLRepair::There is no failing products in this SPL system");
            return result;
        }

        for(String fv_dir:failing_products) {
            ConfigurationProperties.setProperty("workingDirectory", "./output_astor/" + projectName);
            SPLProduct fp = new SPLProduct(fv_dir);
            long startT = System.currentTimeMillis();
            List<String> failing_test_classes = fp.getFailing_test_classes();
            if (failing_test_classes == null || failing_test_classes.isEmpty()){
                log.error("There is no failing test cases");
                return null;
            }
            if(failing == null){
                failing = "";
            }
            for(int i = 0; i < failing_test_classes.size(); i++){
                if(failing == "" && i == 0) failing = failing_test_classes.get(i);
                else if(!failing.equals("")) failing += ":" + failing_test_classes.get(i);
            }

            initProject(fv_dir, projectName, dependencies, packageToInstrument, thfl, failing);

            String mode = ConfigurationProperties.getProperty("mode").toLowerCase();
            String customEngine = ConfigurationProperties.getProperty(ExtensionPoints.NAVIGATION_ENGINE.identifier);

            if (customEngine != null && !customEngine.isEmpty())
                core = createEngine(ExecutionMode.custom);
            else {
                for (ExecutionMode executionMode : ExecutionMode.values()) {
                    for (String acceptedName : executionMode.getAcceptedNames()) {
                        if (acceptedName.equals(mode)) {
                            core = createEngine(executionMode);
                            break;
                        }
                    }
                }

                if (core == null) {
                    System.err.println("Unknown mode of execution: '" + mode + "',  modes are: "
                            + Arrays.toString(ExecutionMode.values()));
                    return null;
                }

            }

            ConfigurationProperties.print();

            core.startSearch();

            result = core.atEnd();
            List<ProgramVariant> solutions = core.get_solutions();
            fp.setSolutions(solutions);
            buggy_spl_system.addSolutions(solutions);

            long endT = System.currentTimeMillis();
            log.info("Time Total(s): " + (endT - startT) / 1000d);

            //return result;
        }
        return result;
    }

    public SPLSystem getSystem(){
        return buggy_spl_system;
    }

    /**
     * @param args
     * @throws Exception
     * @throws ParseException
     */
    public static void main(String[] args) throws Exception {
        SPLRepairMain m = new SPLRepairMain();
        m.execute(args);
        SPLSystem S = m.getSystem();
        S.remove_duplicated_solutions();
        System.out.println("Trang:all solutions:" + S.getNum_of_solutions());
        m.validate_each_solution();
    }
    public void validate_each_solution(){
        System.out.println("Trang::used for validation");
//        for(int i = 0; i < all_solutions.size(); i++){
//
//        }
    }




    public ExecutionResult execute(String[] args) throws Exception {
        boolean correct = processArguments(args);

        log.info("Running Astor on a JDK at " + System.getProperty("java.home"));

        if (!correct) {
            System.err.println("Problems with commands arguments");
            return null;
        }
        if (isExample(args)) {
            executeExample(args);
            return null;
        }

        String dependencies = ConfigurationProperties.getProperty("dependenciespath");
        dependencies += (ConfigurationProperties.hasProperty("extendeddependencies"))
                ? (File.pathSeparator + ConfigurationProperties.hasProperty("extendeddependencies"))
                : "";
        String failing = ConfigurationProperties.getProperty("failing");
        String location = ConfigurationProperties.getProperty("location");
        String packageToInstrument = ConfigurationProperties.getProperty("packageToInstrument");
        double thfl = ConfigurationProperties.getPropertyDouble("flthreshold");
        String projectName = ConfigurationProperties.getProperty("projectIdentifier");

        setupLogging();

        ExecutionResult result = run(location, projectName, dependencies, packageToInstrument, thfl, failing);

        return result;
    }

    public AstorCoreEngine getEngine() {
        return core;
    }

    public void setupLogging() throws IOException {

        if (ConfigurationProperties.getPropertyBool("nolog")) {

            LogManager.getRootLogger().setLevel(Level.FATAL);
        } else {
            String patternLayout = "";
            if (ConfigurationProperties.getPropertyBool("disablelog")) {
                patternLayout = "%m%n";
            } else {
                patternLayout = ConfigurationProperties.getProperty("logpatternlayout");
            }

            Logger.getRootLogger().getLoggerRepository().resetConfiguration();
            ConsoleAppender console = new ConsoleAppender();
            console.setLayout(new PatternLayout(patternLayout));
            console.activateOptions();
            Logger.getRootLogger().addAppender(console);

            String loglevelSelected = ConfigurationProperties.properties.getProperty("loglevel");
            if (loglevelSelected != null)
                LogManager.getRootLogger().setLevel(Level.toLevel(loglevelSelected));

            if (ConfigurationProperties.hasProperty("logfilepath")) {
                FileAppender fa = new FileAppender();
                String filePath = ConfigurationProperties.getProperty("logfilepath");
                File fileLog = new File(filePath);
                if (!fileLog.exists()) {
                    fileLog.getParentFile().mkdirs();
                    fileLog.createNewFile();
                }

                fa.setName("FileLogger");
                fa.setFile(fileLog.getAbsolutePath());
                fa.setLayout(new PatternLayout(patternLayout));
                fa.setThreshold(LogManager.getRootLogger().getLevel());
                fa.setAppend(true);
                fa.activateOptions();
                Logger.getRootLogger().addAppender(fa);
                this.log.info("Log file at: " + filePath);

            }
        }
    }
}
