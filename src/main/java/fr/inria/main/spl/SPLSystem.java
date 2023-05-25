package fr.inria.main.spl;

import fr.inria.astor.core.entities.*;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import org.apache.log4j.Logger;
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
    private HashMap<String, List<FixingHistory>> historical_fixing_information = new HashMap<>();
    private List<String> system_stmts = new ArrayList<>();

    private int num_of_features = 0;
    private int num_of_failing_products = 0;
    private int num_of_passing_products = 0;

    private int num_of_attempted_products = 0;
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

    public List<String> getSystem_stmts() {
        return system_stmts;
    }

    public void setSystem_stmts(List<String> system_stmts) {
        this.system_stmts = system_stmts;
    }

    public void setFailing_product_locations(List<String> failing_product_locations) {
        this.failing_product_locations = failing_product_locations;
    }

    public HashMap<String, List<FixingHistory>> getHistorical_fixing_information() {
        return historical_fixing_information;
    }

    public void put_fixing_history(ModificationPoint mp, FixingHistory fh){
        String stmt = ((SuspiciousModificationPoint) mp).getSuspicious().getFeatureInfo() + "_" + mp.getCodeElement();
        if(historical_fixing_information.containsKey(stmt)){
            List<FixingHistory> fixing_info_of_this_point = historical_fixing_information.get(stmt);
            if(!fixing_info_of_this_point.contains(fh)){
                fixing_info_of_this_point.add(fh);
            }
        }else {
            List<FixingHistory> fixings = new ArrayList<>();
            fixings.add(fh);
            historical_fixing_information.put(stmt, fixings);
        }
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

    public void setNum_of_attempted_products(int num_of_attempted_products) {
        this.num_of_attempted_products = num_of_attempted_products;
    }

    public int getNum_of_attempted_products() {
        return num_of_attempted_products;
    }
    public void increase_attempted_products(){
        num_of_attempted_products += 1;
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
            BufferedReader br = null;
            File config_file = new File(Paths.get(location, "config.report.csv").toString());
            if(config_file.exists() && !config_file.isDirectory()) {
                br = new BufferedReader(new FileReader(Paths.get(location, "config.report.csv").toString()));
            }else{
                br = new BufferedReader(new FileReader(Paths.get(location, "config.report.csv.done").toString()));
            }

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
                SPLProduct p = new SPLProduct(loc, this);

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
        List<ProgramVariant> selected_solutions = seeded_product.getCoreEngine().getSolutions();
        if(selected_solutions.isEmpty()) return false;
        boolean result = true;
        for(String ploc:products.keySet()){
            if(!ploc.equals(seeded_product.getProduct_dir())){
                boolean product_result = validate_a_product(getAProduct(ploc), selected_solutions);
                if(!product_result){
                    result = false;
                }
            }
        }
        return result;
    }

    private boolean validate_a_product(SPLProduct product, List<ProgramVariant> variants) throws Exception {

        AstorCoreEngine coreEngine = product.getCoreEngine();
        List<ProgramVariant> product_solutions = coreEngine.get_solutions();
        boolean results = true;
        for(ProgramVariant v:variants){
            List<OperatorInstance> alloperations = v.getAllOperations();
            Patch p = new Patch(alloperations);
            int idx = system_patches.indexOf(p);
            Patch p2 = system_patches.get(idx);

            int generation =  coreEngine.getGenerationsExecuted();
            coreEngine.increase_generation_executed();
            generation += 1;
            ProgramVariant newVariant = coreEngine.getVariantFactory().createProgramVariantFromAnother(coreEngine.getVariants().get(0), generation);
            coreEngine.increase_generation_executed();

            int num_of_found_modification_point = 0;
            boolean flag = true;
            for(OperatorInstance op:alloperations){
                SuspiciousModificationPoint sm_point = product.search_for_modification_point(op);
                if(sm_point != null){
                    //p2.increase_num_of_product_successful_fix(product.getProduct_dir());
                    num_of_found_modification_point += 1;

                    OperatorInstance op2 = product.create_operator_instance_for_product(op, sm_point);
                    if (op2 != null) {
                        newVariant.createModificationIntanceForAPoint(generation, op2);
                    } else {
                        flag = false;
                        break;
                    }
                }
            }
            if(num_of_found_modification_point == 0){
                p2.increase_num_of_product_successful_fix(product.getProduct_dir());
            }else {

                if (flag) {
                    if (product_solutions.contains(newVariant)) continue;
                    apply_variant(product, newVariant, generation);
                    boolean solution = coreEngine.processCreatedVariant(newVariant, generation);
                    if (solution) {
                        product_solutions.add(newVariant);
                        p2.increase_num_of_product_successful_fix(product.getProduct_dir());
                    } else {

                        p2.increase_num_of_product_rejected_fix(product.getProduct_dir());
                        results = false;
                    }
                    revert_variant(product, newVariant, generation);
                    coreEngine.setSolutions(product_solutions);
                    //return solution;
                } else {
                    p2.increase_num_of_product_rejected_fix(product.getProduct_dir());
                    results = false;
                }
            }
        }

        return results;
    }

    public VariantValidationResult validate_a_product(SPLProduct product, ProgramVariant v) throws Exception {
        System.out.println("Trang:: validate product::"  + product.getProduct_dir());
        AstorCoreEngine coreEngine = product.getCoreEngine();

        List<OperatorInstance> alloperations = v.getAllOperations();

        int generation =  coreEngine.getGenerationsExecuted();
        coreEngine.increase_generation_executed();
        generation += 1;
        ProgramVariant newVariant = coreEngine.getVariantFactory().createProgramVariantFromAnother(coreEngine.getVariants().get(0), generation);
        coreEngine.increase_generation_executed();

        int num_of_found_modification_point = 0;
        boolean flag = true;
        for(OperatorInstance op:alloperations){
            SuspiciousModificationPoint sm_point = product.search_for_modification_point(op);
            if(sm_point != null){

                num_of_found_modification_point += 1;

                OperatorInstance op2 = product.create_operator_instance_for_product(op, sm_point);
                if (op2 != null) {
                    newVariant.createModificationIntanceForAPoint(generation, op2);
                } else {
                    flag = false;
                    break;
                }
            }
        }
        VariantValidationResult validationResult = null;
        if(num_of_found_modification_point == 0) {
            validationResult = coreEngine.validateInstance(newVariant);
        } else {
            if (flag) {
                apply_variant(product, newVariant, generation);
                URL[] originalURL = coreEngine.getProjectFacade().getClassPathURLforProgramVariant(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);

                CompilationResult compilation = coreEngine.getCompiler().compile(newVariant, originalURL);

                boolean childCompiles = compilation.compiles();
                newVariant.setCompilation(compilation);

                coreEngine.storeModifiedModel(newVariant);
                if (childCompiles) {
                    validationResult = coreEngine.validateInstance(newVariant);
                }
                revert_variant(product, newVariant, generation);

            }
        }
        System.out.println("Trang:: validationResult" + validationResult);
        return validationResult;
    }

    private void apply_variant(SPLProduct product, ProgramVariant variant, int gen) throws IllegalAccessException {

        AstorCoreEngine coreEngine = product.getCoreEngine();

        List<OperatorInstance> operations = variant.getOperations(gen);
        for(OperatorInstance op:operations){
            if(op != null) {
                coreEngine.applyNewMutationOperationToSpoonElement(op);
            }
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




}
