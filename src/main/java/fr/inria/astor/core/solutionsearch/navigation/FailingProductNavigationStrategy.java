package fr.inria.astor.core.solutionsearch.navigation;

import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.solutionsearch.extension.AstorExtensionPoint;
import fr.inria.main.spl.SPLProduct;

import java.util.List;

public interface FailingProductNavigationStrategy extends AstorExtensionPoint {
    /**
     * Returns a list with sorted failing products, with the order to be
     * navigated.
     *
     * @param list
     *            of failing products to be navigated
     * @return list of failing products sorted by a given strategy
     */
    List<SPLProduct> getSortedModificationPointsList(List<SPLProduct> failingProducts);
}