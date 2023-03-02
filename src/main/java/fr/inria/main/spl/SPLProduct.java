package fr.inria.main.spl;

import fr.inria.astor.core.entities.ProgramVariant;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SPLProduct {
    private List<String> failing_test_classes = new ArrayList<>();
    private String product_dir = "";
    private List<ProgramVariant> solutions = new ArrayList<>();


    public SPLProduct(String _product_dir){
        this.product_dir = _product_dir;
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
    }

    public String getCoverage_dir(){
        return Paths.get(product_dir, "coverage").toString();
    }

    public List<String> getFailing_test_classes() {
        String coverage_dir = getCoverage_dir();
        String failed_test_dir = Paths.get(coverage_dir, "failed").toString();
        File failed_test_folder = new File(failed_test_dir);
        File[] list_of_failed_test_files = failed_test_folder.listFiles();
        for(int i = 0; i < list_of_failed_test_files.length; i++){
            if(list_of_failed_test_files[i].isFile()){
                String file_name = list_of_failed_test_files[i].getName();
                String test_class_name = file_name.split("ESTest")[0] + "ESTest";
                failing_test_classes.add(test_class_name);
            }
        }
        return failing_test_classes;
    }

    public void setSolutions(List<ProgramVariant> _soltions){
        for(ProgramVariant v:_soltions){
            this.solutions.add(v);
        }
    }

    public List<ProgramVariant> getSolutions(){
        return this.solutions;
    }
}
