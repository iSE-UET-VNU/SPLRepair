package fr.inria.astor.core.solutionsearch.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.entities.WeightElement;

/**
 * Strategy of navigation based on weight random.
 *
 * @author Matias Martinez
 *
 */
public class SPLWeightRandomSuspiciousNavigation implements SuspiciousNavigationStrategy {

    @Override
    public List<ModificationPoint> getSortedModificationPointsList(List<ModificationPoint> modificationPoints) {
        WeightRandomSuspiciousNavigation weightRandomSuspiciousNavigation = new WeightRandomSuspiciousNavigation();
        List<ModificationPoint> modificationPoint_sorted_by_weighted = weightRandomSuspiciousNavigation.getSortedModificationPointsList(modificationPoints);

        return modificationPoint_sorted_by_weighted;
    }

}
