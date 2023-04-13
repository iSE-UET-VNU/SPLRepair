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
            if(((SuspiciousModificationPoint) mp).getSuspicious().equals(suspiciousCode)){
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
        if(op.getClass().toString().contains("StatementOperatorInstance")){
            op_in_product2 = new StatementOperatorInstance(sm_point, op.getOperationApplied(),
                    sm_point.getCodeElement(), op.getModified());
        }else{
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
}
