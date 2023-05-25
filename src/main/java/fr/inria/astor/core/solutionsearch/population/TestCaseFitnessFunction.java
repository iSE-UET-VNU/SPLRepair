package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.validation.TestCaseVariantValidationResult;
import fr.inria.astor.core.entities.validation.VariantValidationResult;

import java.util.List;

/**
 * Fitness function based on test suite execution.
 * 
 * @author Matias Martinez
 *
 */
public class TestCaseFitnessFunction implements FitnessFunction {

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
		double total_fitness = 0.0d;
		for(VariantValidationResult validationResult: listofvalidationResults) {
			total_fitness += calculateFitnessValue(validationResult);
		}
		return total_fitness;
	}

	public double getWorstMaxFitnessValue() {

		return Double.MAX_VALUE;
	}

}
