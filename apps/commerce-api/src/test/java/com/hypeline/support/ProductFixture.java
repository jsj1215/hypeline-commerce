package com.hypeline.support;

import com.hypeline.domain.brand.Brand;
import com.hypeline.domain.product.Category;
import com.hypeline.domain.product.Product;
import com.hypeline.domain.sku.Sku;

import java.util.ArrayList;
import java.util.List;

public final class ProductFixture {

    private static final String[] SIZES = {"S", "M", "L", "XL"};
    private static final String[] COLORS = {"BLACK", "WHITE", "NAVY"};
    private static final Category[] CATEGORIES = Category.values();

    private ProductFixture() {
    }

    public static Brand brand(String name) {
        return Brand.of(name);
    }

    public static Product product(Long brandId, int seq) {
        return Product.of(
            brandId,
            CATEGORIES[seq % CATEGORIES.length],
            "테스트 상품 " + seq,
            10_000L + (seq % 500) * 1_000L,
            "https://cdn.example.com/thumb/" + seq + ".jpg"
        );
    }

    public static List<Product> products(Long brandId, int count) {
        List<Product> products = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            products.add(product(brandId, i));
        }
        return products;
    }

    public static List<Sku> skusOf(Long productId) {
        List<Sku> skus = new ArrayList<>();
        for (String size : SIZES) {
            for (String color : COLORS) {
                skus.add(Sku.of(
                    productId,
                    "SKU-" + productId + "-" + size + "-" + color,
                    size,
                    color,
                    "XL".equals(size) ? 2_000L : 0L
                ));
            }
        }
        return skus;
    }
}
