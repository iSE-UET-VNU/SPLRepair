package fr.inria.main.spl;

import fr.inria.astor.approaches.cardumen.ExpressionReplaceOperator;
import fr.inria.astor.core.entities.*;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.astor.core.solutionsearch.spaces.operators.AstorOperator;
import fr.inria.astor.core.validation.results.TestCasesProgramValidationResult;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SPLProduct {
    protected static Logger log = Logger.getLogger(SPLProduct.class.getSimpleName());
    private List<String> failing_test_classes = new ArrayList<>();
    private Set<String> original_failing_test_cases = new HashSet<>();
    private Set<String> original_passing_test_cases = new HashSet<>();
    //Store by stmt id in feature level
    private Set<String> failing_test_coverage = new HashSet<>();
    private HashMap<String, String> source_feature_to_product = new HashMap<>();
    private HashMap<String, String> source_product_to_feature = new HashMap<>();
    private String product_dir = "";
    private boolean searched_patches = false;
    private float product_complexity = 0.0f;

    private ProjectRepairFacade projectRepairFacade = new ProjectRepairFacade();
    private AstorCoreEngine coreEngine = null;
    private boolean isfailingproduct = true;



    public SPLProduct(String _product_dir, SPLSystem system){
        this.product_dir = _product_dir;
        init_source_code(system);
    }

    private void init_source_code(SPLSystem system){

        String mapping_file_dir= Paths.get(product_dir, ConfigurationProperties.getProperty("featureVariantMappingFile")).toString();
        List<String> system_stmts = system.getSystem_stmts();
        if(!Files.exists(Paths.get(mapping_file_dir))) return;
        try {
            File mapping_file = new File(mapping_file_dir);
            Scanner sc = new Scanner(mapping_file);

            while (sc.hasNext()){
                String tmp = sc.nextLine();
                String stmts[] = tmp.split(" ");
                String feature_stmt = stmts[0].split(":")[1];
                source_feature_to_product.put(feature_stmt, stmts[1].split(":")[1]);
                source_product_to_feature.put(stmts[1].split(":")[1], feature_stmt);
                if(!system_stmts.contains(feature_stmt)){
                    system_stmts.add(feature_stmt);
                }
            }
            system.setSystem_stmts(system_stmts);
        }catch (FileNotFoundException e){
            log.error("Mapping file not found" + e);
        }
        //init failing coverage
        String coverage_dir = Paths.get(product_dir, "coverage").toString();
        String failed_test_coverage_file_dir= Paths.get(coverage_dir, ConfigurationProperties.getProperty("failedTestCoverageFile")).toString();

        if(Files.exists(Paths.get(failed_test_coverage_file_dir))) {
            try {
                File failed_coverage = new File(failed_test_coverage_file_dir);
                Scanner sc = new Scanner(failed_coverage);

                while (sc.hasNext()) {
                    String tmp = sc.nextLine();
                    failing_test_coverage.add(tmp);
                }
            } catch (FileNotFoundException e) {
                log.error("Mapping file not found" + e);
            }
        }

        init_original_failing_tests();
        init_original_passing_tests();
    }

    public Set<String> getFailing_test_coverage() {
        return failing_test_coverage;
    }


    public HashMap<String, String> getSource_feature_to_product() {
        return source_feature_to_product;
    }

    public HashMap<String, String> getSource_product_to_feature() {
        return source_product_to_feature;
    }

    public void setSearched_patches(boolean searched_patches) {
        this.searched_patches = searched_patches;
    }

    public boolean getSearched_patches(){
        return searched_patches;
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

    public float getProduct_complexity() {
        return product_complexity;
    }

    public void setProduct_complexity(float product_complexity) {
        this.product_complexity = product_complexity;
    }
    public void init_original_failing_tests(){
        String coverage_dir = getCoverage_dir();
        String failed_test_dir = Paths.get(coverage_dir, "failed").toString();
        File failed_test_folder = new File(failed_test_dir);
        File[] list_of_failed_test_files = failed_test_folder.listFiles();
        if( list_of_failed_test_files != null) {
            for (File list_of_failed_test_file : list_of_failed_test_files) {
                if (list_of_failed_test_file.isFile()) {
                    String file_name = list_of_failed_test_file.getName();
                    String test_id = file_name.split("\\.coverage")[0];
                    if(!original_failing_test_cases.contains(test_id))
                        original_failing_test_cases.add(test_id);
                }
            }
        }
    }

    public void init_original_passing_tests(){
        String coverage_dir = getCoverage_dir();
        String failed_test_dir = Paths.get(coverage_dir, "passed").toString();
        File failed_test_folder = new File(failed_test_dir);
        File[] list_of_failed_test_files = failed_test_folder.listFiles();
        if( list_of_failed_test_files != null) {
            for (File list_of_failed_test_file : list_of_failed_test_files) {
                if (list_of_failed_test_file.isFile()) {
                    String file_name = list_of_failed_test_file.getName();
                    String test_id = file_name.split("\\.coverage")[0];
                    if(!original_passing_test_cases.contains(test_id))
                        original_passing_test_cases.add(test_id);
                }
            }
        }
    }

    public Set<String> getOriginal_failing_test_cases() {
        return original_failing_test_cases;
    }

    public Set<String> getOriginal_passing_test_cases() {
        return original_passing_test_cases;
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
                    if(!failing_test_classes.contains(test_class_name))
                        failing_test_classes.add(test_class_name);
                }
            }
        }
        return failing_test_classes;
    }


    public int get_num_of_failing_tests(){

        int count = 0;
        String coverage_dir = getCoverage_dir();
        String failed_test_dir = Paths.get(coverage_dir, "failed").toString();
        File failed_test_folder = new File(failed_test_dir);
        File[] list_of_failed_test_files = failed_test_folder.listFiles();
        if( list_of_failed_test_files != null) {
            for (File list_of_failed_test_file : list_of_failed_test_files) {
                if (list_of_failed_test_file.isFile()) {
                    count += 1;
                }
            }
        }
        return count;
    }

    public int get_num_of_passing_tests(){

        int count = 0;
        String coverage_dir = getCoverage_dir();
        String failed_test_dir = Paths.get(coverage_dir, "passed").toString();
        File failed_test_folder = new File(failed_test_dir);
        File[] list_of_failed_test_files = failed_test_folder.listFiles();
        if( list_of_failed_test_files != null) {
            for (File list_of_failed_test_file : list_of_failed_test_files) {
                if (list_of_failed_test_file.isFile()) {
                    count += 1;
                }
            }
        }
        return count;
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


    public String get_product_stmt(String feature_stmt){
        return source_feature_to_product.get(feature_stmt);
    }

    public String get_feature_stmt(String product_stmt){
        return source_product_to_feature.get(product_stmt);
    }


    public SuspiciousModificationPoint search_for_modification_point(OperatorInstance op){
        ProgramVariant product_variant = coreEngine.getVariants().get(0);
        List<ModificationPoint> product_modification_points = product_variant.getModificationPoints();
        SuspiciousModificationPoint sm_point = null;
        SuspiciousModificationPoint susp_point = (SuspiciousModificationPoint) op.getModificationPoint();
        String modification_point_feature_level = susp_point.getSuspicious().getFeatureInfo();
        String modification_point_product_level = get_product_stmt(modification_point_feature_level);
        if(modification_point_product_level == null){
            log.info("modification point does not exist in the product");
            return null;
        }
        String[] tmp_stmt2 = modification_point_product_level.split("\\.");
        SuspiciousCode suspiciousCode = new SuspiciousCode (susp_point.getSuspicious().getClassName(),
                null,Integer.parseInt(tmp_stmt2[tmp_stmt2.length-1]), modification_point_feature_level,
                susp_point.getSuspicious().getSuspiciousValue(), null);
        for(ModificationPoint mp:product_modification_points){
            if(((SuspiciousModificationPoint) mp).getSuspicious().equals(suspiciousCode) && mp.getCodeElement().equals(op.getModificationPoint().getCodeElement())){
                sm_point = (SuspiciousModificationPoint) mp;
                break;
            }
        }
        return sm_point;
    }

    public OperatorInstance create_operator_instance_for_product(OperatorInstance op, SuspiciousModificationPoint sm_point) throws IllegalAccessException {

        if(sm_point == null){
            log.info("The modification point does not exist\n");
            return null;
        }
        OperatorInstance op_in_product2 = null;
        AstorOperator astorOperator = op.getOperationApplied();
        if(!astorOperator.canBeAppliedToPoint(sm_point))
            return null;
        if(op instanceof StatementOperatorInstance){
            op_in_product2 = new StatementOperatorInstance(sm_point, op.getOperationApplied(),
                    sm_point.getCodeElement(), op.getModified());
        }
        else{
            op_in_product2 = new OperatorInstance(sm_point, op.getOperationApplied(),
                    sm_point.getCodeElement(), op.getModified());
        }
        return op_in_product2;
    }

    @Override
    public String toString(){
        if(isfailingproduct)
            return product_dir + " is a failing product";
        else
            return product_dir + " is a passing product";
    }

    public float calculate_similarity(SPLProduct otherProduct){
        Set<String> this_product_stmts = source_feature_to_product.keySet();
        Set<String> other_product_stmts = otherProduct.getSource_feature_to_product().keySet();
        Set<String> intersection = new HashSet<>(this_product_stmts);
        intersection.retainAll(other_product_stmts);
        float product_similarity = (float) intersection.size() / this_product_stmts.size();
        intersection = new HashSet<>(failing_test_coverage);
        intersection.retainAll(otherProduct.getFailing_test_coverage());
        float coverage_similarity = (float) intersection.size() / failing_test_coverage.size();
        return (product_similarity + coverage_similarity) / 2;
    }

    public int measure_fixing_score_for_modification_point(VariantValidationResult validationResult){
        if(validationResult instanceof TestCasesProgramValidationResult) {

            Set<String> new_failing_tests = ((TestCasesProgramValidationResult) validationResult).get_failing_tests();
            System.out.println("Trang::original failing tests:: " + original_failing_test_cases);
            System.out.println("Trang:: new failing tests:" + new_failing_tests);
            int negatively_impacted_passed_tests = num_of_negatively_impacted_passing_tests(new_failing_tests);
            int positively_impacted_failed_tests = num_of_possitively_impacted_failing_tests(new_failing_tests);
            //CleanFix
            if(positively_impacted_failed_tests > 0 && negatively_impacted_passed_tests == 0) return 2;
            //NoisyFix
            else if(positively_impacted_failed_tests > 0 && negatively_impacted_passed_tests > 0) return 1;
            //NoneFix
            else if(positively_impacted_failed_tests == 0 && negatively_impacted_passed_tests == 0) return 0;
            //NegativeFix
            else if(positively_impacted_failed_tests == 0 && negatively_impacted_passed_tests > 0) return  -1;
        }
        return -1;
    }
    private int num_of_negatively_impacted_passing_tests(Set<String> new_failing_tests){
        int count = 0;
        for(String s:new_failing_tests){
            if(original_passing_test_cases.contains(s)){
                count += 1;
            }
        }
        return count;
    }

    private int num_of_possitively_impacted_failing_tests(Set<String> new_failing_tests){
        int count = 0;
        for(String s:original_failing_test_cases){
            if(!new_failing_tests.contains(s)){
                count += 1;
            }
        }
        return count;
    }
}
