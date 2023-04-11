package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.validation.TestCaseVariantValidationResult;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.main.spl.SPLProduct;
import fr.inria.main.spl.SPLSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Fitness function based on test suite execution.
 *
 * @author Trang Nguyen
 *
 *
 */
public class SPLProductAndTestCaseFitnessFunction extends SPLFitnessFunction {
    List<String> original_passing_products = new ArrayList<>();

    /**
     * In this case the fitness value is associate to the failures: LESS FITNESS
     * is better.
     */
    public double calculateFitnessValue(VariantValidationResult validationResult) {
        if (validationResult == null)
            return this.getWorstMaxFitnessValue();

        TestCaseVariantValidationResult result = (TestCaseVariantValidationResult) validationResult;
        return result.getPassingTestCases();
    }
    public float calculateFitnessValuewithPercentage(VariantValidationResult validationResult) {
        if (validationResult == null)
            return (float) this.getWorstMaxFitnessValue();

        TestCaseVariantValidationResult result = (TestCaseVariantValidationResult) validationResult;
        if(result.getCasesExecuted() == 0)
            return 0;
        return (float) result.getPassingTestCases()/result.getCasesExecuted();
    }
    public boolean isCompletelyFixed(VariantValidationResult validationResult){
        if(validationResult != null && validationResult.isSuccessful()){
            return true;
        }else{
            return false;
        }
    }
    public float calculateFitnessValue(SPLSystem system) {
        HashMap<String, SPLProduct> products = system.getAllProducts();
        HashMap<String, VariantValidationResult> original_validation = new HashMap<>();
        int num_of_completely_fixed_product = 0;
        for(String loc:products.keySet()) {
            SPLProduct apro = products.get(loc);
            AstorCoreEngine product_core = apro.getCoreEngine();
            VariantValidationResult result = product_core.getProgramValidator().validate(product_core.getVariants().get(0), apro.getProjectRepairFacade());
            original_validation.put(loc, result);
            if(isCompletelyFixed(result)){
                num_of_completely_fixed_product += 1;
                original_passing_products.add(loc);
            }
        }
        float test_case_level_fitness = calculateFitnessAtTestCaseLevel(original_validation);
        System.out.println("Trang::test case level fitness:" + test_case_level_fitness);
        return (num_of_completely_fixed_product/products.size() + test_case_level_fitness)/2;
    }

    public float calculateFitnessAtTestCaseLevel(HashMap<String, VariantValidationResult> system_validation_results){
        float totallocalfitness = 0.0f;
        for(String loc:system_validation_results.keySet()) {
            VariantValidationResult result = system_validation_results.get(loc);
            float localfitnessvalue = calculateFitnessValuewithPercentage(result);
            totallocalfitness += localfitnessvalue;
        }
        if(system_validation_results.isEmpty()){
            return 0.0f;
        }
        return totallocalfitness/system_validation_results.size();
    }

    public float calculateFitnessValue(HashMap<String, VariantValidationResult> system_validation_results) {

        float total_passing_tests = 0;
        int num_of_completely_fixed_product = 0;
        int penalty = 0;
        for(String loc:system_validation_results.keySet()) {
            VariantValidationResult result = system_validation_results.get(loc);
            if(isCompletelyFixed(result)){
                num_of_completely_fixed_product += 1;
            }else {
                if (original_passing_products.contains(loc)) {
                    penalty += 1;
                }
            }
        }
        float test_case_level_fitness = calculateFitnessAtTestCaseLevel(system_validation_results);
        System.out.println("Trang::test_case_level_fitness:" + test_case_level_fitness);
        System.out.println("Trang::penalty::" + penalty);
        return ((num_of_completely_fixed_product - penalty)/system_validation_results.size() + test_case_level_fitness)/2;
    }

    public double getWorstMaxFitnessValue() {

        return 0;
    }

}
