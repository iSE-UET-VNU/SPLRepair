package fr.inria.astor.core.solutionsearch.navigation;

import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.main.spl.SPLProduct;
import fr.inria.main.spl.SPLSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FailingProductNavigation {

    public void computeProductComplexity(SPLSystem system){
        int system_size = system.getSystem_stmts().size();
        List<SPLProduct> failingproducts = system.getFailing_products();
        for(SPLProduct product:failingproducts){
            float product_size_complexity = (float) product.getSource_feature_to_product().size()/system_size;
            int failing_tests = product.get_num_of_failing_tests();
            int passing_tests = product.get_num_of_passing_tests();
            float product_test_complexity = (float) failing_tests/(failing_tests + passing_tests);
            float complexity = (product_size_complexity + product_test_complexity)/2;
            product.setProduct_complexity(complexity);
        }
    }

    //Start to fix from the simplest product to the most complex product
    public List<SPLProduct> sorted_failing_products_by_complexity(SPLSystem system){
        computeProductComplexity(system);
        List<SPLProduct> failing_products = system.getFailing_products();
        for(int i = 0; i < failing_products.size() - 1; i++){
            for(int j = i + 1; j < failing_products.size(); j++){
                if(failing_products.get(i).getProduct_complexity() > failing_products.get(j).getProduct_complexity()){
                    Collections.swap(failing_products, i, j);
                }
            }
        }
        return failing_products;
    }

    public SPLProduct select_next_failing_product(SPLProduct current_product, List<SPLProduct> sorted_product_list){
        int index = sorted_product_list.indexOf(current_product);
        float max_similarity = 0.0f;
        int index_of_most_similar_product = -1;
        for(int i = 0; i < sorted_product_list.size(); i++){
            if(i != index && !sorted_product_list.get(i).getSearched_patches()){
                float similar_value = current_product.calculate_similarity(sorted_product_list.get(i));
                if(similar_value > max_similarity){
                    max_similarity = similar_value;
                    index_of_most_similar_product = i;
                }
            }
        }
        if( index_of_most_similar_product != -1)
            return sorted_product_list.get(index_of_most_similar_product);
        else return null;
    }
}
