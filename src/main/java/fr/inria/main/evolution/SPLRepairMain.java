package fr.inria.main.evolution;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.*;
import java.util.List;

import fr.inria.astor.core.entities.OperatorInstance;
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

//    protected AstorCoreEngine core = null;


    /**
     * It creates a repair engine according to an execution mode.
     *
     *
     * @param mode
     * @return
     * @throws Exception
     */

    public void createEngine(SPLProduct product, ExecutionMode mode) throws Exception {
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

                        SuspiciousCode e = new SuspiciousCode (classname, null, line_number, susp_score, null);
                        suspicious.add(e);
                    }
                    break;
                }
            }
            // If the FL result is empty, we do not use FL
            if (suspicious.isEmpty()) {
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
        product.setCoreEngine(core);

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
        List<String> failing_products = buggy_spl_system.getFailing_product_locations();
        ExecutionResult result = null;
        if(failing_products == null || failing_products.isEmpty()){
            log.error("SPLRepair::There is no failing products in this SPL system");
            return buggy_spl_system;
        }

        String mode = ConfigurationProperties.getProperty("mode").toLowerCase();
        String customEngine = ConfigurationProperties.getProperty(ExtensionPoints.NAVIGATION_ENGINE.identifier);


        for(String fv_dir:failing_products) {
            ConfigurationProperties.setProperty("workingDirectory", "./output_astor/" + projectName);
            SPLProduct fp = buggy_spl_system.getAProduct(fv_dir);
            //projectFacade = fp.getProjectRepairFacade();
            long startT = System.currentTimeMillis();
            List<String> failing_test_classes = fp.getFailing_test_classes();
            if (failing_test_classes == null || failing_test_classes.isEmpty()){
                log.error("There is no failing test cases");
                return buggy_spl_system;
            }
            if(failing == null){
                failing = "";
            }
            for(int i = 0; i < failing_test_classes.size(); i++){
                if(failing.equals("") && i == 0) failing = failing_test_classes.get(i);
                else if(!failing.equals("")) failing += ":" + failing_test_classes.get(i);
            }

            initProject(fv_dir, projectName, dependencies, packageToInstrument, thfl, failing);

            if (customEngine != null && !customEngine.isEmpty())
                createEngine(fp, ExecutionMode.custom);
            else {
                for (ExecutionMode executionMode : ExecutionMode.values()) {
                    for (String acceptedName : executionMode.getAcceptedNames()) {
                        if (acceptedName.equals(mode)) {
                            createEngine(fp, executionMode);
                            break;
                        }
                    }
                }

                if (fp.getCoreEngine() == null) {
                    System.err.println("Unknown mode of execution: '" + mode + "',  modes are: "
                            + Arrays.toString(ExecutionMode.values()));
                    return buggy_spl_system;
                }

            }

            ConfigurationProperties.print();
            fp.getCoreEngine().startSearch();

            result = fp.getCoreEngine().atEnd();

            fp.setSucceed_operators(fp.getCoreEngine().getSuccessed_operators());
            fp.setRejected_operators(fp.getCoreEngine().getRejected_operators());
            fp.setProjectRepairFacade(projectFacade);


            long endT = System.currentTimeMillis();
            log.info("Time Total(s): " + (endT - startT) / 1000d);

            //return result;
        }
        return buggy_spl_system;
    }

    //public SPLSystem getSystem(){
//        return buggy_spl_system;
//    }

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
        int num_correctly_patch = 0;
        String location = cmd.getOptionValue("location");

        ConfigurationProperties.properties.setProperty("location", location);
        String output_file = Paths.get(ConfigurationProperties.getProperty("workingDirectory"), "results-FL.txt").toString();
        BufferedWriter writer = new BufferedWriter(new FileWriter(output_file));
        String[] system_locations = new File(location).list();

        for (String sloc: system_locations) {
            String system_name = sloc;
            if(sloc.startsWith(".")) continue;

            long startT = System.currentTimeMillis();
            num_of_system += 1;
            SPLRepairMain m = new SPLRepairMain();

            SPLSystem S = m.execute_spl_repair(args, Paths.get(location, sloc).toString());
            S.validate_solutions();
            System.out.println();

            long endT = System.currentTimeMillis();
            writer.write(S.getLocation() + "\n");
            writer.write("Number of successed operator::" + S.getSucceed_operators().size() + "\n");
            for (OperatorInstance o : S.getSucceed_operators()) {
                SourcePosition original_element = o.getOriginal().getPosition();
                String[] tmp = original_element.getFile().getName().split(File.separator);
                String[] loc_tmp = original_element.toString().split(File.separator);
                String product_loc = "";
                for (String t : loc_tmp) {
                    if (t.contains("model_m_")) {
                        product_loc = Paths.get(Paths.get(S.getLocation(), "variants").toString(), t).toString();
                    }
                }
                String product_stmt = tmp[tmp.length - 1].replace(".java", "") + "." + original_element.getLine();
                String feature_stmt = S.getAProduct(product_loc).get_feature_stmt("main." + product_stmt);
                writer.write("Repairing location: " + feature_stmt + "\n");
                writer.write(o + "\n");
            }
            if(S.correctly_patch()){
                writer.write("This system is correctly repaired\n");
                num_correctly_patch += 1;
            }
            writer.write("Repairing time (s): " + (endT - startT) / 1000d + "\n");
            total_time += (endT - startT) / 1000d;

        }
        writer.write("------------------------summary-------------------\n");
        writer.write("Total number of systems:" + num_of_system + "\n");
        writer.write("Total number of correctly repaired systems:" + num_correctly_patch + "\n");
        writer.write("Total reparing time:" + total_time + "\n");
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

        SPLSystem buggy_system = run_splrepair(location, projectName, dependencies, packageToInstrument, thfl, failing);

        return buggy_system;
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
