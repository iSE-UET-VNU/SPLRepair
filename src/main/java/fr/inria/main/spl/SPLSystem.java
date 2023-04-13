package fr.inria.main.spl;

import fr.inria.astor.core.entities.*;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import org.apache.log4j.Logger;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SPLSystem {
    private String location = "";
    private List<String> features = new ArrayList<>();
    private List<String> failing_product_locations = new ArrayList<>();
    private List<String> passing_product_locations = new ArrayList<>();
    public HashMap<String, SPLProduct> products = new HashMap<>();

    private int num_of_features = 0;
    private int num_of_failing_products = 0;
    private int num_of_passing_products = 0;
    List<Patch> system_patches = new ArrayList<>();


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
    public int getNum_of_products(){
        return num_of_failing_products + num_of_passing_products;
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
                    num_of_failing_products += 1;
                }else{
                    passing_product_locations.add(loc);
                    p.setTestingStatus(false);
                    products.put(loc, p);
                    num_of_passing_products += 1;
                }
            }

        }catch (FileNotFoundException e){
            log.error("Can not file config file in " + location);
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean validate_in_the_whole_system(SPLProduct seeded_product) throws Exception {
        System.out.println("Trang::seeded product is:" + seeded_product.getProduct_dir());
        List<ProgramVariant> selected_solutions = seeded_product.getCoreEngine().getSolutions();
        if(selected_solutions.isEmpty()) return false;
        boolean result = true;
        for(String ploc:products.keySet()){
            if(!ploc.equals(seeded_product.getProduct_dir())){
                boolean product_result = validate_a_product(getAProduct(ploc), selected_solutions);
                if(!product_result){
                    System.out.println("Trang::Failing product::" + ploc);
                    result = false;
                }
            }
        }
        return result;
    }

    private boolean validate_a_product(SPLProduct product, List<ProgramVariant> variants) throws Exception {
        AstorCoreEngine coreEngine = product.getCoreEngine();
        List<ProgramVariant> product_solutions = coreEngine.get_solutions();


        for(ProgramVariant v:variants){
            Patch p = new Patch(v.getAllOperations());
            int idx = system_patches.indexOf(p);
            Patch p2 = system_patches.get(idx);

            int generation =  coreEngine.getGenerationsExecuted();
            coreEngine.increase_generation_executed();
            generation += 1;
            ProgramVariant newVariant = coreEngine.getVariantFactory().createProgramVariantFromAnother(coreEngine.getVariants().get(0), generation);
            coreEngine.increase_generation_executed();
            Map<Integer, List<OperatorInstance>> op = v.getOperations();
            boolean flag = true;
            for(Integer i:op.keySet()){
                List<OperatorInstance> each_ops = op.get(i);

                for(OperatorInstance nop:each_ops){
                    SuspiciousModificationPoint sm_point = product.search_for_modification_point(nop);
                    if(sm_point == null){
                        p2.increase_num_of_product_successful_fix(product.getProduct_dir());
                        return true;
                    }else {
                        OperatorInstance op2 = product.create_operator_instance_for_product(nop, sm_point);
                        if (op2 != null) {
                            newVariant.createModificationIntanceForAPoint(generation, op2);
                        } else {
                            flag = false;
                            break;
                        }
                    }
                }
                if(!flag)
                    break;
            }
            if(flag) {
                if (product_solutions.contains(newVariant)) continue;
                apply_variant(product, newVariant, generation);
                boolean solution = coreEngine.processCreatedVariant(newVariant, generation);
                if (solution) {
                    product_solutions.add(newVariant);
                    p2.increase_num_of_product_successful_fix(product.getProduct_dir());
                }else{
                    p2.increase_num_of_product_rejected_fix(product.getProduct_dir());
                }
                revert_variant(product, newVariant, generation);
                coreEngine.setSolutions(product_solutions);
                //return solution;
            }
        }

        return true;
    }

    private void apply_variant(SPLProduct product, ProgramVariant variant, int gen) throws IllegalAccessException {
        AstorCoreEngine coreEngine = product.getCoreEngine();

        List<OperatorInstance> operations = variant.getOperations(gen);
        for(OperatorInstance op:operations){
            if(op != null)
                coreEngine.applyNewMutationOperationToSpoonElement(op);
        }
    }

    private void revert_variant(SPLProduct product, ProgramVariant variant, int gen) throws IllegalAccessException {
        AstorCoreEngine coreEngine = product.getCoreEngine();

        List<OperatorInstance> operations = variant.getOperations(gen);
        for(OperatorInstance op:operations){
            coreEngine.undoOperationToSpoonElement(op);
        }

    }

    public void setSystem_patches(List<Patch> system_patches) {
        this.system_patches = system_patches;
    }

    public List<Patch> getSystem_patches() {
        return system_patches;
    }
    //    public List<Patch> getSystemSolution(){
//        List<Patch> system_patches = new ArrayList<>();
//        for(String ploc:products.keySet()) {
//            List<ProgramVariant> variants = products.get(ploc).getCoreEngine().getSolutions();
//            for(ProgramVariant v:variants){
//                Patch p = new Patch(v.getAllOperations());
//                if(!system_patches.contains(p)) {
//                    p.increase_num_of_product_successful_fix();
//                    system_patches.add(p);
//                }else{
//                    int idx = system_patches.indexOf(p);
//                    Patch p2 = system_patches.get(idx);
//                    p2.increase_num_of_product_successful_fix();
//                }
//            }
//        }
//        return system_patches;
//    }



}
