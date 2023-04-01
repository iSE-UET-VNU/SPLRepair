package fr.inria.main.spl;

import fr.inria.astor.core.entities.*;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.astor.core.solutionsearch.spaces.operators.AstorOperator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
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
    private int generation = 0;


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
        if(!succeed_operators.contains(_op))
            succeed_operators.add(_op);
    }
    public void setRejected_operators(List<OperatorInstance> _op){
        rejected_operators = _op;
    }

    public List<OperatorInstance> getRejected_operators(){
        return rejected_operators;
    }

    public void addRejected_operators(OperatorInstance _op){
        if(!rejected_operators.contains(_op))
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

    public List<OperatorInstance> apply_previous_operator_instances_to_product_variant(List<OperatorInstance> operators) throws IllegalAccessException {
        List<OperatorInstance> applied_operators = new ArrayList<>();
        for(OperatorInstance op:operators) {
            OperatorInstance op2 = create_operator_instance_for_product(op);
            if(op2 != null){
                applied_operators.add(op2);
            }else {
                log.info("Cannot applied these all operators");
                return null;
            }
        }
        for(OperatorInstance op:applied_operators){
            coreEngine.applyNewMutationOperationToSpoonElement(op);
        }
        return applied_operators;
    }

    public void revert_applied_operators(List<OperatorInstance> applied_operators) throws IllegalAccessException {
        if(applied_operators != null && applied_operators.size() > 0) {
            for (int i = applied_operators.size() - 1; i >= 0; i--) {
                coreEngine.undoOperationToSpoonElement(applied_operators.get(i));
            }
        }
    }
    public OperatorInstance create_operator_instance_for_product(OperatorInstance op) throws IllegalAccessException {
        ProgramVariant product_variant = coreEngine.getVariants().get(0);
        List<ModificationPoint> product_modification_points = product_variant.getModificationPoints();

        SuspiciousModificationPoint sm_point = null;
        SuspiciousModificationPoint susp_point = (SuspiciousModificationPoint) op.getModificationPoint();
        String modification_point_feature_level = susp_point.getSuspicious().getFeatureInfo();
        String modification_point_product_level = get_product_stmt(modification_point_feature_level);
        String[] tmp_stmt2 = modification_point_product_level.split("\\.");
        SuspiciousCode suspiciousCode = new SuspiciousCode (susp_point.getSuspicious().getClassName(),
                null,Integer.parseInt(tmp_stmt2[tmp_stmt2.length-1]), modification_point_feature_level,
                susp_point.getSuspicious().getSuspiciousValue(), null);
        for(ModificationPoint mp:product_modification_points){
            if(((SuspiciousModificationPoint) mp).getSuspicious().equals(suspiciousCode)){
                sm_point = (SuspiciousModificationPoint) mp;
                break;
            }
        }
        if(sm_point == null){
            log.info("The modification point does not exist\n");
            return null;
        }
        OperatorInstance op_in_product2 = null;
        AstorOperator astorOperator = op.getOperationApplied();
        if(!astorOperator.canBeAppliedToPoint(sm_point))
            return null;
        if(op.getClass().toString().contains("StatementOperatorInstance")){
            op_in_product2 = new StatementOperatorInstance(sm_point, op.getOperationApplied(),
                    sm_point.getCodeElement(), op.getModified());
        }else{
            op_in_product2 = new OperatorInstance(sm_point, op.getOperationApplied(),
                    sm_point.getCodeElement(), op.getModified());
        }

        product_variant.createModificationIntanceForAPoint(generation, op_in_product2);
        generation++;
        return op_in_product2;
    }
    public VariantValidationResult apply_operator_instance_to_product_variant(OperatorInstance op) throws Exception {
        VariantValidationResult result = null;
        ProgramVariant product_variant = coreEngine.getVariants().get(0);
        URL[] originalURL = coreEngine.getProjectFacade().getClassPathURLforProgramVariant(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);
        OperatorInstance op_in_product2 = create_operator_instance_for_product(op);
        if(op_in_product2 == null) {
            log.info("Operation in product " + product_dir + " is not created.");
            return null;
        }
        boolean applied = coreEngine.applyNewMutationOperationToSpoonElement(op_in_product2);
        if(applied) {
            CompilationResult compilation = coreEngine.getCompiler().compile(product_variant, originalURL);
            boolean childCompiles = compilation.compiles();
            product_variant.setCompilation(compilation);

            coreEngine.storeModifiedModel(product_variant);
            if (ConfigurationProperties.getPropertyBool("saveall")) {
                coreEngine.saveVariant(product_variant);
            }
            if (childCompiles) {
                result = coreEngine.getProgramValidator().validate(product_variant, projectRepairFacade);


                if (result != null && result.isSuccessful()) {
                    addSucceed_operators(op_in_product2);
                } else {
                    addRejected_operators(op_in_product2);
                }
            } else {
                log.info("Cannot compile the op " + op_in_product2 + "in product: " + product_dir);
            }
            // We undo the operator (for try the next one)
            coreEngine.undoOperationToSpoonElement(op_in_product2);
        }
        return result;
    }
}
