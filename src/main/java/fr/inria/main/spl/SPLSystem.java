package fr.inria.main.spl;

import fr.inria.astor.core.entities.ProgramVariant;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SPLSystem {
    private String location = "";
    private List<String> features = new ArrayList<>();
    private List<String> failing_products = new ArrayList<>();
    private List<String> passing_products = new ArrayList<>();

    private int num_of_features = 0;
    private int num_of_failing_products = 0;
    private int num_of_passing_products = 0;

    private List<ProgramVariant> solutions = new ArrayList<>();

    protected org.apache.log4j.Logger log = Logger.getLogger(SPLSystem.class.getName());
    public SPLSystem(){

    }
    public SPLSystem(String _location) throws FileNotFoundException {
        this.location = _location;
        intialize();
    }
    public void setLocation(String _location){
        this.location = _location;

    }

    public String getLocation() {
        return location;
    }

    public void setFailing_products(List<String> failing_products) {
        this.failing_products = failing_products;
    }

    public List<String> getFailing_products() {
        return failing_products;
    }

    public void setPassing_products(List<String> passing_products) {
        this.passing_products = passing_products;
    }

    public List<String> getPassing_products() {
        return passing_products;
    }

    public void setSolutions(List<ProgramVariant> solutions) {
        this.solutions = solutions;
    }

    public void addSolutions(List<ProgramVariant> _solutions){
        for(ProgramVariant v:_solutions){
            this.solutions.add(v);
        }
    }

    public List<ProgramVariant> getSolutions() {
        return solutions;
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

    public void intialize() throws FileNotFoundException {
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
                if(items[items.length-1].equals("__FAILED__")) {
                    failing_products.add(Paths.get(variant_dir.toString(), items[0]).toString());
                }else{
                    passing_products.add(Paths.get(variant_dir.toString(), items[0]).toString());
                }
            }

        }catch (FileNotFoundException e){
            log.error("Can not file config file in " + location);
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int getNum_of_solutions(){
        return solutions.size();
    }

    public void remove_duplicated_solutions(){
        List<ProgramVariant> removed_variants = new ArrayList<>();
        for(int i = 0; i < this.solutions.size()- 1; i++){
            ProgramVariant v1 = this.solutions.get(i);
            String patchdiff_v1 = v1.getPatchDiff().getFormattedDiff();
            String patch_items_v1[] = patchdiff_v1.split("@\n");
            for(int j = i + 1; j < this.solutions.size(); j += 1){
                ProgramVariant v2 = this.solutions.get(j);
                String patchdiff_v2 = v2.getPatchDiff().getFormattedDiff();
                String patch_items_v2[] = patchdiff_v2.split("@\n");
                if(patch_items_v2[patch_items_v2.length - 1].equals(patch_items_v1[patch_items_v1.length - 1])){
                    removed_variants.add(v2);
                    break;
                }
            }
        }
        if(removed_variants.size() > 0)
            this.solutions.removeAll(removed_variants);
    }
}
