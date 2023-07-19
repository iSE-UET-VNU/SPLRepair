package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.validation.TestCaseVariantValidationResult;
import fr.inria.astor.core.entities.validation.VariantValidationResult;

import java.util.List;

/**
 * Fitness function based on test suite execution.
 *
 * @author Trang Nguyen
 *
 */
public class FiVarFitnessFunction implements FitnessFunction {

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

    @Override
    public double calculateFitnessValue(List<VariantValidationResult> listofvalidationResults) {
        double product_level = 0.0d;
        double testcase_level = 0.0d;
        int num_of_failed_products = 0;
        for(VariantValidationResult validationResult: listofvalidationResults) {
            if(validationResult == null)
                testcase_level += 1.0d;
            else{
                TestCaseVariantValidationResult result = (TestCaseVariantValidationResult) validationResult;
                if(result.getCasesExecuted() != 0)
                    testcase_level += (result.getFailureCount()/ result.getCasesExecuted());
                if(result.getFailureCount() != 0)
                    num_of_failed_products += 1;
            }
        }
        if(listofvalidationResults.size() > 0){
            product_level = (double) num_of_failed_products/ listofvalidationResults.size();
            testcase_level = testcase_level/ listofvalidationResults.size();
        }
        return (product_level + testcase_level)/2;
    }

    public double getWorstMaxFitnessValue() {

        return Double.MAX_VALUE;
    }

}
