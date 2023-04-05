package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.validation.TestCaseVariantValidationResult;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.main.spl.SPLProduct;
import fr.inria.main.spl.SPLSystem;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnFieldName;

import java.util.HashMap;

/**
 * Fitness function based on test suite execution.
 *
 * @author Trang Nguyen
 *
 *
 */
public class SPLStrictWeightedProductFitnessFunction extends SPLFitnessFunction {

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
        float total_passing_tests = 0;
        int num_of_completely_fixed_product = 0;
        for(String loc:products.keySet()) {
            SPLProduct apro = products.get(loc);
            AstorCoreEngine product_core = apro.getCoreEngine();
            VariantValidationResult result = product_core.getProgramValidator().validate(product_core.getVariants().get(0), apro.getProjectRepairFacade());
            if(isCompletelyFixed(result)){
                num_of_completely_fixed_product += 1;
            }
            float localfitnessvalue = calculateFitnessValuewithPercentage(result);
            total_passing_tests += localfitnessvalue;
        }
        int weight = ConfigurationProperties.getPropertyInt("WeightedCompletelyFixedProducts");
        return (weight*num_of_completely_fixed_product + total_passing_tests);
    }

    public float calculateFitnessValue(HashMap<String, VariantValidationResult> system_validation_results) {

        float total_passing_tests = 0;
        int num_of_completely_fixed_product = 0;
        for(String loc:system_validation_results.keySet()) {
            VariantValidationResult result = system_validation_results.get(loc);
            float localfitnessvalue = calculateFitnessValuewithPercentage(result);
            if(isCompletelyFixed(result)){
                num_of_completely_fixed_product += 1;
            }
            total_passing_tests += localfitnessvalue;

        }
        int weight = ConfigurationProperties.getPropertyInt("WeightedCompletelyFixedProducts");
        return (weight*num_of_completely_fixed_product + total_passing_tests);
    }

    public double getWorstMaxFitnessValue() {

        return 0;
    }

}
