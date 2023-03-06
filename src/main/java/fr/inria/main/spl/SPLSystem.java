package fr.inria.main.spl;

import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.stats.PatchStat;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
    private HashMap<String, SPLProduct> products = new HashMap<>();


    private int num_of_features = 0;
    private int num_of_failing_products = 0;
    private int num_of_passing_products = 0;


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
                    p.setTestingStatus(false);
                    products.put(loc, p);
                }else{
                    passing_product_locations.add(loc);
                    p.setTestingStatus(true);
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


    public void validate_solutions(){
        //Todo:
        //For each solution, check it each product
        //If the repaired statement is not included in the product --> do not need to re-test
        //otherwise
        //For failing product, if the solution is already check -> do not need to check again
        //For passing product and the failing product in which the solution hasn't been check --> apply and retest
        for(String ploc1:products.keySet()){
            System.out.println("Trang:: product 1:" + ploc1);
            List<OperatorInstance> p1_successed_operators = products.get(ploc1).getSuccessed_operators();
            if(p1_successed_operators.isEmpty()) continue;
            for (String ploc2: products.keySet()){
                if(!ploc1.equals(ploc2)){
                    System.out.println("Trang:: product 2:" + ploc2);
                    SPLProduct product2 = products.get(ploc2);
                    List<OperatorInstance> p2_successed_operators = product2.getSuccessed_operators();
                    List<OperatorInstance> p2_rejected_operators = product2.getRejected_operators();
                    for(OperatorInstance pv:p1_successed_operators){
                         //If the operation instance has been checked by this product,
                        //we don't need to check it again
//                        if(product2.is_successed_operation_instance(pv)){
//                            System.out.println("Trang:: This operation has been tested and successed");
//                        }else if(product2.is_rejected_operation_instance(pv)){
//                            System.out.println("Trang:: This operation has been tested and rejected");
//                        }
                        System.out.println("Trang::operation:" + pv);
                    }

                }
            }

        }
    }



}
