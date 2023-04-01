package fr.inria.astor.core.solutionsearch.population;

import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.main.spl.SPLSystem;

import java.util.List;

public abstract class SPLPopulationController implements PopulationController {
    public abstract  boolean selectOperatorInstanceForNextGeneration(SPLSystem system, OperatorInstance newop, double newfitness);
}
