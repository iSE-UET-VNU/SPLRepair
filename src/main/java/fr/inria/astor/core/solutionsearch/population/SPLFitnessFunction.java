package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.validation.VariantValidationResult;
import fr.inria.main.spl.SPLSystem;

import java.util.HashMap;

public abstract class SPLFitnessFunction implements FitnessFunction{
    public abstract float calculateFitnessValue(SPLSystem system);
    public abstract float calculateFitnessValue(HashMap<String, VariantValidationResult> system_validation_results);
}
