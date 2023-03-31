package fr.inria.astor.core.solutionsearch.navigation;

import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.main.spl.SPLProduct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FailingProductNavigation {
    public List<SPLProduct> getSortedFailingProductsList(List<SPLProduct> failingProducts) {
        List<SPLProduct> shuffList = new ArrayList<SPLProduct>(failingProducts);
        Collections.shuffle(shuffList);
        return shuffList;
    }
}
