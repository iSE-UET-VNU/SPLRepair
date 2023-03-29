package fr.inria.astor.core.solutionsearch.navigation;

import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.main.spl.SPLProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UniformRandomFailingProductNavigation implements FailingProductNavigationStrategy{
    @Override
    public List<SPLProduct> getSortedModificationPointsList(List<SPLProduct> failingProducts) {
        List<SPLProduct> shuffList = new ArrayList<SPLProduct>(failingProducts);
        Collections.shuffle(shuffList);
        return shuffList;
    }
}