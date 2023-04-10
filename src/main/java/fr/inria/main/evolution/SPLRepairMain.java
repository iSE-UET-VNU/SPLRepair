package fr.inria.main.evolution;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import fr.inria.astor.approaches.jgenprog.SPLGenProg;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.setup.FinderTestCases;
import fr.inria.astor.core.solutionsearch.navigation.FailingProductNavigation;
import fr.inria.astor.core.solutionsearch.population.SPLFitnessFunction;
import fr.inria.main.spl.Patch;
import fr.inria.main.spl.SPLProduct;
import fr.inria.main.spl.SPLSystem;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
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
import spoon.reflect.cu.SourcePosition;

/**
 * Astor main
 *
 * @author Trang Nguyen, trang.nguyen@vnu.edu.vn
 *
 */
public class SPLRepairMain extends AbstractMain {
    protected Logger log = Logger.getLogger(SPLRepairMain.class.getName());


    /**
     * It creates a repair engine according to an execution mode.
     *
     *
     * @param mode
     * @return
     * @throws Exception
     */

    public AstorCoreEngine createEngine(SPLProduct product, ExecutionMode mode) throws Exception {
        AstorCoreEngine core = null;

        MutationSupporter mutSupporter = new MutationSupporter();

        if (ExecutionMode.DeepRepair.equals(mode)) {
            core = new DeepRepairEngine(mutSupporter, projectFacade);

        } else if (ExecutionMode.CARDUMEN.equals(mode)) {
            core = new CardumenApproach(mutSupporter, projectFacade);

        } else if (ExecutionMode.jKali.equals(mode)) {
            core = new JKaliEngine(mutSupporter, projectFacade);

        } else if (ExecutionMode.jGenProg.equals(mode)) {
            core = new JGenProg(mutSupporter, projectFacade);
        } else if (ExecutionMode.SPLGenProg.equals(mode)) {
            core = new SPLGenProg(mutSupporter, projectFacade);
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
            List<String> regressionTestForFaultLocalization = null;
            regressionTestForFaultLocalization = FinderTestCases.findJUnit4XTestCasesForRegression(projectFacade);
            projectFacade.getProperties().setRegressionCases(regressionTestForFaultLocalization);
            core.initPopulation(new ArrayList<SuspiciousCode>());
        } else if (ConfigurationProperties.getPropertyBool("readfaultlocalizationresultsfromfile")) {
            //We used FL results stored in file
            String loc = product.getProduct_dir();

            String fl_result_file = Paths.get(loc, ConfigurationProperties.getProperty("faultLocalizationResultFileName")).toString();
            File FL_file = new File(fl_result_file);
            Scanner sc = new Scanner(FL_file);
            List<SuspiciousCode> suspicious = new ArrayList<>();
            while (sc.hasNext()){
                String tmp = sc.nextLine();

                if((tmp.replace("\n", "")).equals(ConfigurationProperties.getProperty("defaultSBFLmetrics"))){
                    while(sc.hasNext()){
                        tmp = sc.nextLine();
                        if(tmp.equals("---------")) break;
                        String[] aboutline = tmp.split(" ")[0].split("\\.");
                        String[] temp_class_name = new String[aboutline.length-1];
                        for(int i = 0; i < temp_class_name.length; i++){
                            temp_class_name[i] = aboutline[i];
                        }
                        String classname = String.join(".", temp_class_name);
                        int line_number = Integer.parseInt(aboutline[aboutline.length-1]);
                        float susp_score = Float.parseFloat(tmp.split(" ")[1]);
                        String product_stmt = tmp.split(" ")[0];
                        String feature_stmt_info = product.get_feature_stmt(product_stmt);
                        SuspiciousCode e = new SuspiciousCode (classname, null, line_number,  feature_stmt_info, susp_score, null);
                        suspicious.add(e);
                    }
                    break;
                }
            }
            // If the FL result is empty, we do not use FL
            if (suspicious.isEmpty()) {
                suspicious = new ArrayList<SuspiciousCode>();
            }
            List<String> regressionTestForFaultLocalization = null;
            regressionTestForFaultLocalization = FinderTestCases.findJUnit4XTestCasesForRegression(projectFacade);
            projectFacade.getProperties().setRegressionCases(regressionTestForFaultLocalization);

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



    public SPLSystem run_splrepair(String location, String projectName, String dependencies, String packageToInstrument,
                                   double thfl, String failing) throws Exception {
        SPLSystem buggy_spl_system = new SPLSystem(location);
        List<String> failing_product_locations = buggy_spl_system.getFailing_product_locations();
        if(failing_product_locations == null || failing_product_locations.isEmpty()){
            log.error("SPLRepair::There is no failing products in this SPL system");
            return buggy_spl_system;
        }

        String mode = ConfigurationProperties.getProperty("mode").toLowerCase();
        String customEngine = ConfigurationProperties.getProperty(ExtensionPoints.NAVIGATION_ENGINE.identifier);

        for(String fv_dir:failing_product_locations) {
            prepare_engine(buggy_spl_system, projectName, fv_dir, dependencies, packageToInstrument, thfl,
                    failing, customEngine, mode);
        }

        List<String> passing_product_locations = buggy_spl_system.getPassing_product_locations();
        for(String pv_dir:passing_product_locations) {
            prepare_engine(buggy_spl_system, projectName, pv_dir, dependencies, packageToInstrument, thfl,
                    failing, customEngine, mode);
        }
        if(buggy_spl_system.getFitnessFunction() != null){
            SPLFitnessFunction fitnessFunction = (SPLFitnessFunction) buggy_spl_system.getFitnessFunction();
            double originalfitness = fitnessFunction.calculateFitnessValue(buggy_spl_system);
            buggy_spl_system.setOriginalFitness(originalfitness);
            buggy_spl_system.setLastfitness(originalfitness);
        }
        int generation = 0;
        int maxgeneration = Integer.parseInt(ConfigurationProperties.getProperty("maxGenerationSPL"));
        FailingProductNavigation failingProductNavigation = new FailingProductNavigation();
        List<SPLProduct> failingProducts = buggy_spl_system.getFailing_products();
        while (generation < maxgeneration){
            SPLProduct selected_failing_product = failingProductNavigation.getSortedFailingProductsList(failingProducts).get(0);
            AstorCoreEngine coreEngine = selected_failing_product.getCoreEngine();
            if(coreEngine instanceof SPLGenProg) {
                OperatorInstance op = ((SPLGenProg) coreEngine).gen_an_operation_instance();

                if(op != null){
                    SourcePosition original_element = op.getOriginal().getPosition();
                    String[] tmp = original_element.getFile().getName().split(File.separator);
                    String product1_stmt = tmp[tmp.length-1].replace(".java", "") + "." + original_element.getLine();
                    String feature_stmt = selected_failing_product.get_feature_stmt("main." + product1_stmt);
                    try{
                        buggy_spl_system.validate_operation_instance_in_the_whole_system(op, feature_stmt);

                    }catch (Exception e){
                        throw e;
                    }
                }
            }
            generation += 1;
        }
        //buggy_spl_system.evaluate_patches();
        return buggy_spl_system;
    }

    private SPLProduct prepare_engine(SPLSystem buggy_spl_system, String projectName, String product_dir,  String dependencies, String packageToInstrument,
                                      double thfl,  String failing, String customEngine, String mode) throws Exception {

        SPLProduct product = buggy_spl_system.getAProduct(product_dir);

        List<String> failing_test_classes = product.getFailing_test_classes();

        if(failing == null){
            failing = "";
        }
        for(int i = 0; i < failing_test_classes.size(); i++){
            if(failing.equals("") && i == 0) failing = failing_test_classes.get(i);
            else if(!failing.equals("")) failing += ":" + failing_test_classes.get(i);
        }
        initProject(product_dir, projectName, dependencies, packageToInstrument, thfl, failing);
        if (customEngine != null && !customEngine.isEmpty())
            product.setCoreEngine(createEngine(product, ExecutionMode.custom));
        else {
            for (ExecutionMode executionMode : ExecutionMode.values()) {
                for (String acceptedName : executionMode.getAcceptedNames()) {
                    if (acceptedName.equals(mode)) {
                        product.setCoreEngine(createEngine(product, executionMode));
                        break;
                    }
                }
            }

            if (product.getCoreEngine() == null) {
                System.err.println("Unknown mode of execution: '" + mode + "',  modes are: "
                        + Arrays.toString(ExecutionMode.values()));
                return product;
            }

        }
        ConfigurationProperties.print();
        product.setProjectRepairFacade(projectFacade);
        return product;
    }


    /**
     * @param args
     * @throws Exception
     * @throws ParseException
     */
    public static void main(String[] args) throws Exception {
        CommandLine cmd = null;
        try {
            cmd =  new BasicParser().parse(options, args);
        } catch (UnrecognizedOptionException e) {
            throw e;
        }
        int num_of_system = 0;
        double total_time = 0d;
        int num_systems_containing_test_adequate_patch = 0;
        int num_systems_partially_fixed = 0;
        int total_fixed_products = 0;
        String location = cmd.getOptionValue("location");
        ConfigurationProperties.properties.setProperty("location", location);
        String fitness_func = cmd.getOptionValue("splfitnessfunction");
        ConfigurationProperties.properties.setProperty("splfitnessfunction", fitness_func);



        String[] system_name_tmp = location.split(System.getProperty("file.separator"));
        String system_name = system_name_tmp[system_name_tmp.length - 1];
        String output_file = Paths.get(ConfigurationProperties.getProperty("workingDirectory"), system_name + "_" + fitness_func + "_localization.txt").toString();
        BufferedWriter writer = new BufferedWriter(new FileWriter(output_file));
        String[] system_locations = new File(location).list();

        for (String sloc: system_locations) {
            if(sloc.startsWith(".")) continue;

            long startT = System.currentTimeMillis();
            num_of_system += 1;
            SPLRepairMain m = new SPLRepairMain();
            try {
                SPLSystem S = m.execute_spl_repair(args, Paths.get(location, sloc).toString());
                long endT = System.currentTimeMillis();
                writer.write(S.getLocation() + "\n");

                Patch patch = S.getSystem_patch();
                writer.write(patch.toString());
                if(patch.getNum_of_product_successful_fix() > 0 && patch.getNum_of_product_rejected_fix() > 0){
                    num_systems_partially_fixed += 1;
                    total_fixed_products += patch.getNum_of_product_successful_fix();
                }
                if (patch.getNum_of_product_successful_fix() > 0 && patch.getNum_of_product_rejected_fix() == 0) {
                    writer.write("This is an adequate patch.\n");
                    num_systems_containing_test_adequate_patch += 1;
                }
                writer.write("Repairing time (s): " + (endT - startT) / 1000d + "\n");
                total_time += (endT - startT) / 1000d;
                writer.write("*******************************************\n");
            }catch (Exception e){
                writer.write("Exception in the system:: " + sloc + "\n");
            }
        }
        writer.write("------------------------summary-------------------\n");
        writer.write("Total number of systems:" + num_of_system + "\n");
        writer.write("Total number of systems containing test adequate patches:" + num_systems_containing_test_adequate_patch + "\n");
        writer.write("Total number of systems partially fixed:" + num_systems_partially_fixed + "\n");
        if(num_systems_partially_fixed != 0)
            writer.write("On partially fixed systems, num of products are fixed:" + total_fixed_products/num_systems_partially_fixed + "\n");
        writer.write("Total repairing time:" + total_time + "\n");
        writer.close();
    }



    public SPLSystem execute_spl_repair(String[] args, String location) throws Exception {
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

        String packageToInstrument = ConfigurationProperties.getProperty("packageToInstrument");
        double thfl = ConfigurationProperties.getPropertyDouble("flthreshold");
        String projectName = ConfigurationProperties.getProperty("projectIdentifier");

        setupLogging();

        return run_splrepair(location, projectName, dependencies, packageToInstrument, thfl, failing);
    }


    @Override
    public ExecutionResult run(String location, String projectName, String dependencies, String packageToInstrument, double thfl, String failing) throws Exception {
        return null;
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
