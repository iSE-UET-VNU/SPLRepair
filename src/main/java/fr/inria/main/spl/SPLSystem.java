package fr.inria.main.spl;

import fr.inria.astor.core.entities.*;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.astor.core.solutionsearch.population.*;
import org.apache.log4j.Logger;
import spoon.reflect.cu.SourcePosition;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SPLSystem {
    private String location = "";
    private List<String> features = new ArrayList<>();
    private List<String> failing_product_locations = new ArrayList<>();
    private List<String> passing_product_locations = new ArrayList<>();
    public HashMap<String, SPLProduct> products = new HashMap<>();
    private SPLFitnessFunction fitnessFunction = null;
    private PopulationController populationController = null;
    private double originalfitness = 0;

    private int num_of_features = 0;
    private int num_of_failing_products = 0;
    private int num_of_passing_products = 0;

    private List<OperatorInstance> succeed_operators = new ArrayList<>();
    private List<Patch> solutions = new ArrayList<>();

    protected org.apache.log4j.Logger log = Logger.getLogger(SPLSystem.class.getName());
    public SPLSystem(){

    }
    public SPLSystem(String _location) throws FileNotFoundException {
        this.location = _location;
        initialize();
    }
    public void setLocation(String _location){
        this.location = _location;

    }
    public void add_successed_operators(OperatorInstance op){
        succeed_operators.add(op);
    }
    public List<OperatorInstance> getSucceed_operators(){
        return succeed_operators;
    }
    public void addProducts(String location, SPLProduct product){
        products.put(location, product);
    }

    public String getLocation() {
        return location;
    }

    public void setFailing_product_locations(List<String> failing_product_locations) {
        this.failing_product_locations = failing_product_locations;
    }

    public List<String> getFailing_product_locations() {
        return failing_product_locations;
    }

    public void setPassing_product_locations(List<String> passing_product_locations) {
        this.passing_product_locations = passing_product_locations;
    }

    public List<String> getPassing_product_locations() {
        return passing_product_locations;
    }

    public List<SPLProduct> getFailing_products(){
        List<SPLProduct> failing_products = new ArrayList<>();
        for(String loc:failing_product_locations){
            failing_products.add(products.get(loc));
        }
        return failing_products;
    }
    public int getNum_of_failing_products() {
        return num_of_failing_products;
    }
    public void setNum_of_failing_products(int x){
        this.num_of_failing_products = x;
    }

    public int getNum_of_passing_products(){
        return num_of_passing_products;
    }
    public void setNum_of_passing_products(int x){
        this.num_of_passing_products = x;
    }

    public void setFeatures(ArrayList<String> features){
        this.features = features;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setNum_of_features(int num_of_features) {
        this.num_of_features = num_of_features;
    }

    public int getNum_of_features() {
        return num_of_features;
    }

    public SPLProduct getAProduct(String product_location){
        return products.get(product_location);
    }
    public HashMap<String, SPLProduct> getAllProducts(){
        return products;
    }

    public void initialize() throws FileNotFoundException {
        Path variant_dir = Paths.get(location, "variants");
        if(ConfigurationProperties.getProperty("splfitnessfunction").contains("SPLTestCaseFitnessFunction"))
            fitnessFunction = new SPLTestCaseFitnessFunction();
        if(ConfigurationProperties.getProperty("splpopulationcontroller").contains("SPLTestCaseBasedFitnessPopulationController"))
            populationController = new SPLTestCaseBasedFitnessPopulationController();

        try {
            BufferedReader br = new BufferedReader(new FileReader(Paths.get(location, "config.report.csv").toString()));
            String line = "";
            String split_by = ",";
            line = br.readLine();
            String[] features = line.split(split_by);

            for(String f:features){
                if(!f.equals("Product\\Feature") && !f.equals("__TEST_OUTPUT__")) {
                    this.num_of_features += 1;
                    this.features.add(f);
                }
            }
            while ((line = br.readLine()) != null){
                String[] items = line.split(split_by);
                String loc = Paths.get(variant_dir.toString(), items[0]).toString();
                SPLProduct p = new SPLProduct(loc);

                if(items[items.length-1].equals("__FAILED__")) {
                    failing_product_locations.add(loc);
                    p.setTestingStatus(true);
                    products.put(loc, p);
                }else{
                    passing_product_locations.add(loc);
                    p.setTestingStatus(false);
                    products.put(loc, p);
                }
            }

        }catch (FileNotFoundException e){
            log.error("Can not file config file in " + location);
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public FitnessFunction getFitnessFunction(){
        return fitnessFunction;
    }
    public void setOriginalFitness(double v){
        originalfitness = v;
    }
    public double getOriginalFitness(){
        return originalfitness;
    }
    public void check_patches_on_all_products() throws Exception {
        for(String ploc1:products.keySet()){
            SPLProduct product1 = products.get(ploc1);
            List<OperatorInstance> p1_succeed_operators = product1.getSucceed_operators();
            if(!product1.isIsfailingproduct()) continue;
            if(!p1_succeed_operators.isEmpty()){
                for(OperatorInstance pv:p1_succeed_operators){
                    boolean flag = true;
                    for (String ploc2: products.keySet()){
                        if(!ploc1.equals(ploc2)){
                            boolean validation_result = true;
                            SPLProduct product2 = products.get(ploc2);
                            //If the operation instance has been checked by this product,
                            //we don't need to check it again
                            if(product2.is_succeed_operation_instance(pv)){
                                continue;
                            }else if(product2.is_rejected_operation_instance(pv)){
                                flag = false;
                                continue;
                            }
                            //If the operation instance has not been checked,
                            // we apply it to the source of the product
                            //and retest
                            SourcePosition original_element = pv.getOriginal().getPosition();
                            String[] tmp = original_element.getFile().getName().split(File.separator);
                            String product1_stmt = tmp[tmp.length-1].replace(".java", "") + "." + original_element.getLine();
                            String feature_stmt = product1.get_feature_stmt("main." + product1_stmt);
                            String product2_stmt = product2.get_product_stmt(feature_stmt);
                            if(product2_stmt != null){
                                SuspiciousModificationPoint susp_point = (SuspiciousModificationPoint) pv.getModificationPoint();

                                String[] tmp_stmt2 = product2_stmt.split("\\.");

                                SuspiciousCode e = new SuspiciousCode (susp_point.getSuspicious().getClassName(),
                                        null,Integer.parseInt(tmp_stmt2[tmp_stmt2.length-1]), feature_stmt,
                                        susp_point.getSuspicious().getSuspiciousValue(), null);
                                SuspiciousModificationPoint susp_point_in_product2 = new SuspiciousModificationPoint(e, susp_point.getCodeElement(),
                                        susp_point.getCtClass(), susp_point.getContextOfModificationPoint());
                                OperatorInstance op_in_product2 = null;
                                if(pv.getClass().toString().contains("StatementOperatorInstance")){
                                    op_in_product2 = new StatementOperatorInstance(susp_point_in_product2, pv.getOperationApplied(),
                                        pv.getOriginal(), pv.getModified());
                                }else{
                                    op_in_product2 = new OperatorInstance(susp_point_in_product2, pv.getOperationApplied(),
                                            pv.getOriginal(), pv.getModified());
                                }


                                //validation_result = validate_operation_instance_in_a_product(product2, op_in_product2, "");

                                if(!validation_result){
                                    flag = false;
                                }

                            }
                        }
                    }
                    //If the operation instance passed on all the applied products,
                    //add it to the list of succeed operators of the system
                    if(flag) {
                        if (!isexisted_succeed_operator(pv)) succeed_operators.add(pv);
                    }
                }
            }
        }
        evaluate_patches();
    }

    /*
    * Check how many products that a patch succeed and rejected
    * */
    public void evaluate_patches(){
        for(String ploc1:products.keySet()) {
            SPLProduct product1 = products.get(ploc1);
            List<OperatorInstance> p1_succeed_operators = product1.getSucceed_operators();
            if (!p1_succeed_operators.isEmpty()) {
                for (OperatorInstance pv : p1_succeed_operators) {
                    Patch patch = new Patch(pv);
                    a_successful_patch(patch);
                }
            }
            List<OperatorInstance> p1_rejected_operators = product1.getRejected_operators();
            if(!p1_rejected_operators.isEmpty()){
                for(OperatorInstance pv:p1_rejected_operators) {
                    Patch patch = new Patch(pv);
                    a_rejected_patch(patch);
                }
            }
        }
    }

    private void a_successful_patch(Patch p){
        int idx = solutions.indexOf(p);
        if(idx == -1){
            p.increase_num_of_product_successful_fix();
            solutions.add(p);
        }else{
            Patch tmp = solutions.get(idx);
            tmp.increase_num_of_product_successful_fix();
        }
    }

    private void a_rejected_patch(Patch p){
        int idx = solutions.indexOf(p);
        if(idx == -1){
            p.increase_num_of_product_rejected_fix();
            solutions.add(p);
        }else{
            Patch tmp = solutions.get(idx);
            tmp.increase_num_of_product_rejected_fix();
        }
    }


    private boolean isexisted_succeed_operator(OperatorInstance op){
        for(OperatorInstance o: succeed_operators){
            if(o.equals(op)){
                return true;
            }
        }
        return false;
    }

    public boolean validate_operation_instance_in_the_whole_system(OperatorInstance op, String modification_point_feature_level) throws Exception {
        HashMap<String, VariantValidationResult> system_validation_results = new HashMap<>();
        for(String loc:products.keySet()) {
            SPLProduct apro = products.get(loc);
            VariantValidationResult validation_result = validate_operation_instance_in_a_product(apro, op, modification_point_feature_level);
            system_validation_results.put(loc, validation_result);
        }
        double system_fitness_value = fitnessFunction.calculateFitnessValue(system_validation_results);
        return true;
    }

    public VariantValidationResult validate_operation_instance_in_a_product(SPLProduct product, OperatorInstance op, String modification_point_feature_level) throws Exception {
        VariantValidationResult result = null;
        AstorCoreEngine product_core = product.getCoreEngine();
        URL[] originalURL = product_core.getProjectFacade().getClassPathURLforProgramVariant(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);
        SuspiciousModificationPoint susp_point = (SuspiciousModificationPoint) op.getModificationPoint();
        String modification_point_product_level = product.get_product_stmt(modification_point_feature_level);
        String[] tmp_stmt2 = modification_point_product_level.split("\\.");
        SuspiciousCode suspiciousCode = new SuspiciousCode (susp_point.getSuspicious().getClassName(),
                null,Integer.parseInt(tmp_stmt2[tmp_stmt2.length-1]), modification_point_feature_level,
                susp_point.getSuspicious().getSuspiciousValue(), null);

        // We validate the variant after applying the operator
        try {
            ProgramVariant product2_variant = product_core.getVariants().get(0);
            List<ModificationPoint> product_modification_points = product2_variant.getModificationPoints();
            SuspiciousModificationPoint sm_point = null;
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
            if(op.getClass().toString().contains("StatementOperatorInstance")){
                op_in_product2 = new StatementOperatorInstance(sm_point, op.getOperationApplied(),
                        sm_point.getCodeElement(), op.getModified());
            }else{
                op_in_product2 = new OperatorInstance(sm_point, op.getOperationApplied(),
                        sm_point.getCodeElement(), op.getModified());
            }
            product2_variant.createModificationIntanceForAPoint(1, op_in_product2);
            product_core.applyNewMutationOperationToSpoonElement(op_in_product2);

            CompilationResult compilation = product_core.getCompiler().compile(product2_variant, originalURL);
            boolean childCompiles = compilation.compiles();
            product2_variant.setCompilation(compilation);

            product_core.storeModifiedModel(product2_variant);
            if (ConfigurationProperties.getPropertyBool("saveall")) {
                product_core.saveVariant(product2_variant);
            }
            if (childCompiles) {
                result = product_core.getProgramValidator().validate(product2_variant, product.getProjectRepairFacade());
                System.out.println("Trang product::" + product.getProduct_dir());
                double local_fitness_value = fitnessFunction.calculateFitnessValue(result);
                if (result != null && result.isSuccessful()) {
                    product.addSucceed_operators(op_in_product2);
                } else {
                    product.addRejected_operators(op_in_product2);
                }
            }else{
                log.info("Cannot compile the op " + op_in_product2 + "in product: " + product.getProduct_dir());
            }
            // We undo the operator (for try the next one)
            product_core.undoOperationToSpoonElement(op_in_product2);

        }catch (Exception e){
            log.error("SPLSystem: validate operation instance exception in system " + product.getProduct_dir());
            e.printStackTrace();
            return null;
       }
        return result;
    }

    public List<Patch> getSolutions(){
        return solutions;
    }

}
