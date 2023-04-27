package fr.inria.main.evolution;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import com.sun.org.apache.xpath.internal.operations.Mod;
import fr.inria.astor.approaches.jmutrepair.jMutRepairEvolutionary;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.setup.FinderTestCases;
import fr.inria.astor.core.solutionsearch.navigation.FailingProductNavigation;
import fr.inria.main.spl.FixingHistory;
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

/**
 * Astor main
 *
 * @author Trang Nguyen, trang.nguyen@vnu.edu.vn
 *
 */
public class SPLRepairAdaptationMain extends AbstractMain {
    protected Logger log = Logger.getLogger(SPLRepairAdaptationMain.class.getName());


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
//            core = new jMutRepairEvolutionary(mutSupporter, projectFacade);

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
        product.setCoreEngine(core);
        core.setProduct(product);
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



    public SPLSystem run_splrepair_baseline(String location, String projectName, String dependencies, String packageToInstrument,
                                            double thfl, String failing) throws Exception {

        ExecutionResult result = null;
        SPLSystem buggy_spl_system = prepare_engine_for_system(location, projectName, dependencies, packageToInstrument, thfl, failing);
        List<SPLProduct> failingProducts = buggy_spl_system.getFailing_products();
        long startT = System.currentTimeMillis();
        for(SPLProduct selected_failing_product:failingProducts) {
            //SPLProduct selected_failing_product = failingProductNavigation.getSortedFailingProductsList(failingProducts).get(0);
            AstorCoreEngine coreEngine = selected_failing_product.getCoreEngine();
            System.out.println("Trang::selected product:" + selected_failing_product);
            coreEngine.startSearch();
            result = coreEngine.atEnd();
            List<ProgramVariant> succeed_variants = coreEngine.getSolutions();
            List<Patch> system_patches = buggy_spl_system.getSystem_patches();
            for (ProgramVariant v : succeed_variants) {
                Patch p = new Patch(v.getAllOperations());
                if(!system_patches.contains(p)) {
                    p.increase_num_of_product_successful_fix(selected_failing_product.getProduct_dir());
                    system_patches.add(p);
                }else{
                    int idx = system_patches.indexOf(p);
                    Patch p2 = system_patches.get(idx);
                    p2.increase_num_of_product_successful_fix(selected_failing_product.getProduct_dir());
                }
            }
            buggy_spl_system.setSystem_patches(system_patches);
            boolean validate_result = buggy_spl_system.validate_in_the_whole_system(selected_failing_product);
            long endT = System.currentTimeMillis();
            if(((endT - startT) / 1000d) > 1800)
                break;
            if(ConfigurationProperties.getPropertyBool("splearlystop")){
                if(validate_result) {
                    break;
                }
            }

        }
        return buggy_spl_system;
    }
    private SPLSystem prepare_engine_for_system(String location, String projectName, String dependencies, String packageToInstrument,
                                                double thfl, String failing) throws Exception {
        SPLSystem buggy_spl_system = new SPLSystem(location);
        List<String> failing_products = buggy_spl_system.getFailing_product_locations();
        if(failing_products == null || failing_products.isEmpty()){
            log.error("SPLRepair::There is no failing products in this SPL system");
            return buggy_spl_system;
        }
        String mode = ConfigurationProperties.getProperty("mode").toLowerCase();
        String customEngine = ConfigurationProperties.getProperty(ExtensionPoints.NAVIGATION_ENGINE.identifier);
        for(String fv_dir:failing_products) {
            prepare_engine_for_each_product(buggy_spl_system, projectName, fv_dir, dependencies, packageToInstrument, thfl,
                    failing, customEngine, mode);
        }

        List<String> passing_products = buggy_spl_system.getPassing_product_locations();
        for(String pv_dir:passing_products) {
            prepare_engine_for_each_product(buggy_spl_system, projectName, pv_dir, dependencies, packageToInstrument, thfl,
                    failing, customEngine, mode);
        }
        return buggy_spl_system;
    }

    public SPLSystem run_splrepair_fivar(String location, String projectName, String dependencies, String packageToInstrument,
                                            double thfl, String failing) throws Exception {
        System.out.println("Trang:run_splrepair_fivar");
        SPLSystem buggy_spl_system = prepare_engine_for_system(location, projectName, dependencies, packageToInstrument, thfl, failing);
        ExecutionResult result = null;

        FailingProductNavigation failingProductNavigation = new FailingProductNavigation();
        List<SPLProduct> sorted_failingProducts =  failingProductNavigation.sorted_failing_products_by_complexity(buggy_spl_system);
        SPLProduct selected_failing_product = sorted_failingProducts.get(0);
        long startT = System.currentTimeMillis();
        while (selected_failing_product != null){
            if(selected_failing_product.getSearched_patches()) continue;
            selected_failing_product.setSearched_patches(true);
            AstorCoreEngine coreEngine = selected_failing_product.getCoreEngine();
            System.out.println("Trang::selected product:" + selected_failing_product + "  product_complexity::" + selected_failing_product.getProduct_complexity());
            coreEngine.startSearch();
            result = coreEngine.atEnd();
            List<ProgramVariant> succeed_variants = coreEngine.getSolutions();
            List<Patch> system_patches = buggy_spl_system.getSystem_patches();
            for (ProgramVariant v : succeed_variants) {
                Patch p = new Patch(v.getAllOperations());
                if(!system_patches.contains(p)) {
                    p.increase_num_of_product_successful_fix(selected_failing_product.getProduct_dir());
                    system_patches.add(p);
                }else{
                    int idx = system_patches.indexOf(p);
                    Patch p2 = system_patches.get(idx);
                    p2.increase_num_of_product_successful_fix(selected_failing_product.getProduct_dir());
                }
            }
            buggy_spl_system.setSystem_patches(system_patches);
            boolean validate_result = buggy_spl_system.validate_in_the_whole_system(selected_failing_product);
            SPLProduct next_selected_failing_product = failingProductNavigation.select_next_failing_product(selected_failing_product, sorted_failingProducts);
            if(next_selected_failing_product == null) {
                break;
            }
            init_previous_fixing_score_for_modificationpoints(selected_failing_product, next_selected_failing_product);
            selected_failing_product = next_selected_failing_product;
            long endT = System.currentTimeMillis();
            if(((endT - startT) / 1000d) > 1800d)
                break;
            if(ConfigurationProperties.getPropertyBool("splearlystop")){
                if(validate_result) {
                    break;
                }
            }
        }
        return buggy_spl_system;
    }

    public void init_previous_fixing_score_for_modificationpoints(SPLProduct seededProduct, SPLProduct nextProduct){
        HashMap<String, Integer> modificationPoints = new HashMap<>();
        List<ProgramVariant> currentProduct_variant = seededProduct.getCoreEngine().getVariants();
        for(ProgramVariant v:currentProduct_variant){
            List<ModificationPoint> variant_mps = v.getModificationPoints();
            for(ModificationPoint mp:variant_mps) {
                String stmt = ((SuspiciousModificationPoint) mp).getSuspicious().getFeatureInfo() + "_" + mp.getCodeElement();
                if(!modificationPoints.containsKey(stmt) || mp.getPrevious_fix_type() > modificationPoints.get(stmt)){
                    modificationPoints.put(stmt, mp.getPrevious_fix_type());
                }
            }
        }
        List<ProgramVariant> nextProduct_variant = nextProduct.getCoreEngine().getVariants();
        for(ProgramVariant v:nextProduct_variant){
            List<ModificationPoint> variant_mps = v.getModificationPoints();
            for(ModificationPoint mp:variant_mps) {
                String stmt = ((SuspiciousModificationPoint) mp).getSuspicious().getFeatureInfo() + "_" + mp.getCodeElement();
                if(modificationPoints.containsKey(stmt)){
                    Integer tmp = modificationPoints.get(stmt);
                    mp.setPrevious_fix_type(tmp);
                }
            }
        }
    }

    private SPLProduct prepare_engine_for_each_product(SPLSystem buggy_spl_system, String projectName, String product_dir, String dependencies, String packageToInstrument,
                                                       double thfl, String failing, String customEngine, String mode) throws Exception {
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
            createEngine(product, ExecutionMode.custom);
        else {
            for (ExecutionMode executionMode : ExecutionMode.values()) {
                for (String acceptedName : executionMode.getAcceptedNames()) {
                    if (acceptedName.equals(mode)) {
                        createEngine(product, executionMode);
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
        float total_percentage_fixed = 0.0f;
        String location = cmd.getOptionValue("location");
        ConfigurationProperties.properties.setProperty("location", location);

        String fl_result_file = cmd.getOptionValue("flresult");
        ConfigurationProperties.properties.setProperty("faultLocalizationResultFileName", fl_result_file);
        String[] system_name_tmp = location.split(System.getProperty("file.separator"));
        String repair_mode = cmd.getOptionValue("mode");
        if(cmd.hasOption("repairmode")){
            repair_mode += "_" + cmd.getOptionValue("repairmode");
        }
        if(cmd.hasOption("editoperationvalidation")){
            repair_mode += "_" + "edit_validation";
        }
        if(cmd.hasOption("splearlystop")){
            repair_mode += "_" + "earlystop";
        }

        String system_name = system_name_tmp[system_name_tmp.length - 1];
        String output_file = Paths.get(ConfigurationProperties.getProperty("workingDirectory"), system_name + "_" + fl_result_file +  "_" + repair_mode + ".txt").toString();
        BufferedWriter writer = new BufferedWriter(new FileWriter(output_file));
        String[] system_locations = new File(location).list();


        for (String sloc: system_locations) {
            if(sloc.startsWith(".")) continue;

            long startT = System.currentTimeMillis();
            num_of_system += 1;
            SPLRepairAdaptationMain m = new SPLRepairAdaptationMain();

            SPLSystem S = m.execute_spl_repair(args, Paths.get(location, sloc).toString());
            List<Patch> system_patches = S.getSystem_patches();
            System.out.println();

            long endT = System.currentTimeMillis();
            writer.write(S.getLocation() + "\n");
            int partially_fix_patches = 0;
            int adequate_patches = 0;
            float percentage_fixed_products = 0.0f;
            for(Patch p: system_patches){
                if(p.getNum_of_product_successful_fix() == S.getNum_of_products()){
                    adequate_patches += 1;
                    writer.write(p.toString());
                    writer.write("\n---------\n");
                    percentage_fixed_products = 1.0f;
                }
            }
            if(adequate_patches == 0) {
                for (Patch p : system_patches) {
                    if (p.getNum_of_product_successful_fix() > 0 && p.getNum_of_product_successful_fix() != S.getNum_of_products()) {
                        partially_fix_patches += 1;
                        writer.write(p.toString());
                        writer.write("\n---------\n");
                        if ((float) p.getNum_of_product_successful_fix() / S.getNum_of_products() > percentage_fixed_products) {
                            percentage_fixed_products = (float) p.getNum_of_product_successful_fix() / S.getNum_of_products();
                        }
                    }
                }
            }


            writer.write("Number of test adequate patches:: " + adequate_patches + "\n");
            writer.write("Number of patches partially fixed the product:: " + partially_fix_patches + "\n");

            writer.write("Repairing time (s): " + (endT - startT) / 1000d + "\n");
            total_time += (endT - startT) / 1000d;
            writer.write("*******************************************\n");
            if(adequate_patches > 0){
                num_systems_containing_test_adequate_patch += 1;
            }
            if(partially_fix_patches > 0 && adequate_patches == 0){
                num_systems_partially_fixed += 1;
            }

            if(system_patches == null || system_patches.isEmpty()){
                percentage_fixed_products = (float) S.getNum_of_passing_products()/S.getNum_of_products();
            }
            total_percentage_fixed += percentage_fixed_products;
        }
        writer.write("------------------------summary-------------------\n");
        writer.write("Total number of systems:" + num_of_system + "\n");
        writer.write("Total number of systems containing test adequate patches:" + num_systems_containing_test_adequate_patch + "\n");
        writer.write("Total number of systems partially fixed:" + num_systems_partially_fixed + "\n");
        if(num_of_system != 0)
            writer.write("Average percentage of passing products/system:" + total_percentage_fixed/num_of_system + "\n");
        writer.write("Total repairing time (s): " + total_time + "\n");
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
        String repairmode = ConfigurationProperties.getProperty("repairmode");
        if(repairmode != null && repairmode.toLowerCase().equals("fivar"))
            return run_splrepair_fivar(location, projectName, dependencies, packageToInstrument, thfl, failing);
        else
            return run_splrepair_baseline(location, projectName, dependencies, packageToInstrument, thfl, failing);
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
