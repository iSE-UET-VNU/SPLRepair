package fr.inria.main.spl;

import fr.inria.astor.approaches.cardumen.CardumenApproach;
import fr.inria.astor.approaches.deeprepair.DeepRepairEngine;
import fr.inria.astor.approaches.jgenprog.JGenProg;
import fr.inria.astor.approaches.jgenprog.extension.TibraApproach;
import fr.inria.astor.approaches.jkali.JKaliEngine;
import fr.inria.astor.approaches.jmutrepair.jMutRepairExhaustive;
import fr.inria.astor.approaches.scaffold.ScaffoldRepairEngine;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.ingredientbased.ExhaustiveIngredientBasedEngine;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.main.ExecutionMode;
import fr.inria.main.evolution.ExtensionPoints;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SPLProduct {
    protected static Logger log = Logger.getLogger(SPLProduct.class.getSimpleName());
    private List<String> failing_test_classes = new ArrayList<>();
    private HashMap<String, String> source_feature_to_product = new HashMap<>();
    private HashMap<String, String> source_product_to_feature = new HashMap<>();
    private String product_dir = "";
    private List<ProgramVariant> solutions = new ArrayList<>();
    private List<ProgramVariant> rejected_patches = new ArrayList<>();

    private List<OperatorInstance> succeed_operators = new ArrayList<>();
    private List<OperatorInstance> rejected_operators = new ArrayList<>();


    private ProjectRepairFacade projectRepairFacade = new ProjectRepairFacade();
    private AstorCoreEngine coreEngine = null;
    private boolean isfailingproduct = true;


    public SPLProduct(String _product_dir){
        this.product_dir = _product_dir;
        init_source_code();
    }
    private void init_source_code(){

        String mapping_file_dir= Paths.get(product_dir, ConfigurationProperties.getProperty("featureVariantMappingFile")).toString();
        if(!Files.exists(Paths.get(mapping_file_dir))) return;
        try {
            File mapping_file = new File(mapping_file_dir);
            Scanner sc = new Scanner(mapping_file);

            while (sc.hasNext()){
                String tmp = sc.nextLine();
                String stmts[] = tmp.split(" ");
                source_feature_to_product.put(stmts[0].split(":")[1], stmts[1].split(":")[1]);
                source_product_to_feature.put(stmts[1].split(":")[1], stmts[0].split(":")[1]);
            }
        }catch (FileNotFoundException e){
            log.error("Mapping file not found" + e);
        }

    }

    public HashMap<String, String> getSource_feature_to_product() {
        return source_feature_to_product;
    }

    public HashMap<String, String> getSource_product_to_feature() {
        return source_product_to_feature;
    }

    public void setProduct_dir(String _product_dir){
        this.product_dir = _product_dir;
    }
    public String getProduct_dir(){
        return this.product_dir;
    }

    public void setFailing_test_classes(List<String> _failing_test_classes){
        for(String s:_failing_test_classes){
            failing_test_classes.add(s);
        }
        if(failing_test_classes.size() > 0){
            isfailingproduct = true;
        }
    }

    public boolean isIsfailingproduct() {
        return isfailingproduct;
    }
    public void setTestingStatus(boolean testingStatus){
        isfailingproduct = testingStatus;
    }
    public String getCoverage_dir(){
        return Paths.get(product_dir, "coverage").toString();
    }

    public List<String> getFailing_test_classes() {
        String coverage_dir = getCoverage_dir();
        String failed_test_dir = Paths.get(coverage_dir, "failed").toString();
        File failed_test_folder = new File(failed_test_dir);
        File[] list_of_failed_test_files = failed_test_folder.listFiles();
        if( list_of_failed_test_files != null) {
            for (File list_of_failed_test_file : list_of_failed_test_files) {
                if (list_of_failed_test_file.isFile()) {
                    String file_name = list_of_failed_test_file.getName();
                    String test_class_name = file_name.split("ESTest")[0] + "ESTest";
                    failing_test_classes.add(test_class_name);
                }
            }
        }
        return failing_test_classes;
    }

    public void setSolutions(List<ProgramVariant> _solutions){
        for(ProgramVariant v:_solutions){
            this.solutions.add(v);
        }
    }

    public List<ProgramVariant> getSolutions(){
        return this.solutions;
    }

    public void setRejected_patches(List<ProgramVariant> _rejected_patches){
        for(ProgramVariant v:_rejected_patches){
            this.rejected_patches.add(v);
        }
    }

    public void setSucceed_operators(List<OperatorInstance> _op){
        succeed_operators = _op;
    }
    public List<OperatorInstance> getSucceed_operators(){
        return succeed_operators;
    }
    public void addSucceed_operators(OperatorInstance _op){
        succeed_operators.add(_op);
    }
    public void setRejected_operators(List<OperatorInstance> _op){
        rejected_operators = _op;
    }

    public List<OperatorInstance> getRejected_operators(){
        return rejected_operators;
    }

    public void addRejected_operators(OperatorInstance _op){
        rejected_operators.add(_op);
    }


    public List<ProgramVariant> getRejected_patches(){
        return rejected_patches;
    }
    public void setCoreEngine(AstorCoreEngine _core){
        coreEngine = _core;
    }
    public AstorCoreEngine getCoreEngine(){
        return coreEngine;
    }
    public void setProjectRepairFacade(ProjectRepairFacade _projectRepairFacade){
        projectRepairFacade = _projectRepairFacade;
    }
    public ProjectRepairFacade getProjectRepairFacade(){
        return projectRepairFacade;
    }

    public boolean  is_rejected_operation_instance(OperatorInstance _op){
        if(rejected_operators.size() > 0 && rejected_operators.contains(_op)){
            return true;
        }
        return false;
    }

    public boolean is_succeed_operation_instance(OperatorInstance _op){
        if(succeed_operators.size() > 0 && succeed_operators.contains(_op)){
            return true;
        }
        return false;
    }

    public String get_product_stmt(String feature_stmt){
        return source_feature_to_product.get(feature_stmt);
    }

    public String get_feature_stmt(String product_stmt){
        return source_product_to_feature.get(product_stmt);
    }

}
