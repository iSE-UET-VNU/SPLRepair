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

        for(int i = 0; i < modificationPoint_sorted_by_weighted.size() -1; i ++){
            for(int j = i + 1; j < modificationPoint_sorted_by_weighted.size(); j++){
                if (modificationPoint_sorted_by_weighted.get(i).getPrevious_product_fixing_score() < modificationPoint_sorted_by_weighted.get(j).getPrevious_product_fixing_score()){
                    Collections.swap(modificationPoint_sorted_by_weighted, i, j);
                }
            }
        }
        for(ModificationPoint p:modificationPoint_sorted_by_weighted){
            System.out.println("Trang::modification point after sorting::" + p);
        }

        return modificationPoint_sorted_by_weighted;
    }

}
