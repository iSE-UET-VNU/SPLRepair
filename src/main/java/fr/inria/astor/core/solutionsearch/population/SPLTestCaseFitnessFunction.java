package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.validation.TestCaseVariantValidationResult;
import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.main.spl.SPLProduct;
import fr.inria.main.spl.SPLSystem;

import java.util.HashMap;

/**
 * Fitness function based on test suite execution.
 *
 * @author Trang Nguyen
 *
 *
 */
public class SPLTestCaseFitnessFunction extends SPLFitnessFunction {

    /**
     * In this case the fitness value is associate to the failures: LESS FITNESS
     * is better.
     */
    public double calculateFitnessValue(VariantValidationResult validationResult) {
        if (validationResult == null)
            return this.getWorstMaxFitnessValue();

        TestCaseVariantValidationResult result = (TestCaseVariantValidationResult) validationResult;
        return result.getFailureCount();
    }

    public double calculateFitnessValue(SPLSystem system) {
        HashMap<String, SPLProduct> products = system.getAllProducts();
        double system_fitness_value = 0;
        for(String loc:products.keySet()) {
            SPLProduct apro = products.get(loc);
            AstorCoreEngine product_core = apro.getCoreEngine();
            VariantValidationResult result = product_core.getProgramValidator().validate(product_core.getVariants().get(0), apro.getProjectRepairFacade());
            double localfitnessvalue = calculateFitnessValue(result);
            system_fitness_value += localfitnessvalue;
        }
        return system_fitness_value;
    }

    public double calculateFitnessValue(HashMap<String, VariantValidationResult> system_validation_results) {

        double system_fitness_value = 0;
        for(String loc:system_validation_results.keySet()) {
            VariantValidationResult result = system_validation_results.get(loc);
            double localfitnessvalue = calculateFitnessValue(result);
            if(localfitnessvalue == getWorstMaxFitnessValue())
                system_fitness_value = localfitnessvalue;
            else{
                system_fitness_value += localfitnessvalue;
            }
        }

        return system_fitness_value;
    }

    public double getWorstMaxFitnessValue() {

        return Double.MAX_VALUE;
    }

}
