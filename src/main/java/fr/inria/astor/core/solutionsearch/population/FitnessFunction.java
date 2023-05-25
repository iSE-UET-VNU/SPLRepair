package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.astor.core.solutionsearch.extension.AstorExtensionPoint;

import java.util.List;

/**
 * Fitness function
 * 
 * @author Matias Martinez
 *
 */
public interface FitnessFunction extends  AstorExtensionPoint {

	
	public double calculateFitnessValue(VariantValidationResult validationResult);
	public double calculateFitnessValue(List<VariantValidationResult> listofvalidationResults);
	
	public double getWorstMaxFitnessValue();
	
}
