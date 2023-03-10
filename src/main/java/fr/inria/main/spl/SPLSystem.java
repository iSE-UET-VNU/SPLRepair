package fr.inria.main.spl;

import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import org.apache.log4j.Logger;
import spoon.reflect.cu.SourcePosition;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SPLSystem {
    private String location = "";
    private List<String> features = new ArrayList<>();
    private List<String> failing_product_locations = new ArrayList<>();
    private List<String> passing_product_locations = new ArrayList<>();
    public HashMap<String, SPLProduct> products = new HashMap<>();


    private int num_of_features = 0;
    private int num_of_failing_products = 0;
    private int num_of_passing_products = 0;

    private List<OperatorInstance> succeed_operators = new ArrayList<>();
    //private HashMap<String, List<ProgramVariant>> solutions = new HashMap<String, List<ProgramVariant>>();

    //this field is used to store the rejected patches of each variants
    //private HashMap<String, List<ProgramVariant>> rejected_patches = new HashMap<String, List<ProgramVariant>>();

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
//
//    public void setSolutions(String product, List<ProgramVariant> psolutions) {
//        this.solutions.put(product, psolutions);
//    }



//    public HashMap<String, List<ProgramVariant>> getSolutions() {
//        return solutions;
//    }

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


    public void validate_solutions() throws IllegalAccessException {

        for(String ploc1:products.keySet()){
            SPLProduct product1 = products.get(ploc1);
            List<OperatorInstance> p1_successed_operators = product1.getSucceed_operators();
            if(p1_successed_operators.isEmpty()) continue;
            for(OperatorInstance pv:p1_successed_operators){
                boolean flag = true;
                for (String ploc2: products.keySet()){
                    if(!ploc1.equals(ploc2)){
                        SPLProduct product2 = products.get(ploc2);
                             //If the operation instance has been checked by this product,
                            //we don't need to check it again
                            if(product2.is_successed_operation_instance(pv)){
                                break;
                            }else if(product2.is_rejected_operation_instance(pv)){
                                flag = false;
                                break;
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

                                String tmp_stmt2[] = product2_stmt.split("\\.");

                                SuspiciousCode e = new SuspiciousCode (susp_point.getSuspicious().getClassName(),
                                        null,Integer.parseInt(tmp_stmt2[tmp_stmt2.length-1]),
                                        susp_point.getSuspicious().getSuspiciousValue(), null);
                                SuspiciousModificationPoint susp_point_in_product2 = new SuspiciousModificationPoint(e, susp_point.getCodeElement(),
                                        susp_point.getCtClass(), susp_point.getContextOfModificationPoint());
                                OperatorInstance op_in_product2 = new OperatorInstance(susp_point_in_product2, pv.getOperationApplied(),
                                        pv.getOriginal(), pv.getModified());
                                flag = validate_operation_instance(product2, op_in_product2);

                            }
                        }
                    if(flag == false) break;
                }
                if(flag == true) if(!isexisted_successed_operator(pv)) succeed_operators.add(pv);

            }
        }
    }

    private boolean isexisted_successed_operator(OperatorInstance op){
        for(OperatorInstance o: succeed_operators){
            if(o.toString().equals(op.toString())){
                return true;
            }
        }
        return false;
    }

    private boolean validate_operation_instance(SPLProduct product, OperatorInstance op) throws IllegalAccessException {
        boolean flag = true;
        AstorCoreEngine product_core = null;
        if(product.isIsfailingproduct()){

            product_core = product.getCoreEngine();
        }else {
            product.createEngine();
            product_core = product.getCoreEngine();
        }
        if(product_core != null){
            // We validate the variant after applying the operator
            product_core.applyNewMutationOperationToSpoonElement(op);
            ProgramVariant product2_variant = new ProgramVariant();
            product2_variant.putModificationInstance(1, op);
            VariantValidationResult result = product_core.getProgramValidator().validate(product2_variant, product.getProjectRepairFacade());
            if(result != null && result.isSuccessful()){
                product.addSuccessed_operators(op);
            }else{
                product.addRejected_operators(op);
                flag = false;
            }
            // We undo the operator (for try the next one)
            product_core.undoOperationToSpoonElement(op);
        }
        return flag;
    }

}
