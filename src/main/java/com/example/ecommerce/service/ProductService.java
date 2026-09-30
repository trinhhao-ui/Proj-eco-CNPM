package com.example.ecommerce.service;

import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductSpecification;
import com.example.ecommerce.entity.ProductVariant;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ProductSpecificationRepository;
import com.example.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import com.example.ecommerce.dto.PhoneFilterCriteria;
import com.example.ecommerce.specification.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductSpecificationRepository productSpecificationRepository;
    private final CloudinaryService cloudinaryService;

    public Page<Product> getActiveProducts(Pageable pageable) {
        return productRepository.findByStatusTrue(pageable);
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public Page<Product> getProductsByCategory(
            Long categoryId,
            Pageable pageable
    ) {
        return productRepository.findByCategoryIdAndStatusTrue(
                categoryId,
                pageable
        );
    }

    public Page<Product> searchProducts(
            String keyword,
            Pageable pageable
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findByStatusTrue(pageable);
        }

        return productRepository
                .findByNameContainingIgnoreCaseAndStatusTrue(
                        keyword.trim(),
                        pageable
                );
    }

    public Page<Product> searchProductsByCategory(
            String keyword,
            Long categoryId,
            Pageable pageable
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getProductsByCategory(categoryId, pageable);
        }

        return productRepository
                .findByNameContainingIgnoreCaseAndCategoryIdAndStatusTrue(
                        keyword.trim(),
                        categoryId,
                        pageable
                );
    }

    public List<Product> getNewProducts() {
        return productRepository.findTop10ByStatusTrueOrderByCreatedAtDesc();
    }

    public List<Product> getCheapProducts() {
        return productRepository.findTop10ByStatusTrueOrderByPriceAsc();
    }

    public List<Product> getExpensiveProducts() {
        return productRepository.findTop10ByStatusTrueOrderByPriceDesc();
    }

    @Transactional
    public Product createProduct(
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            Long categoryId,
            MultipartFile image
    ) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }

        if (stock == null || stock < 0) {
            return null;
        }

        if (categoryId == null) {
            return null;
        }

        Category category = categoryRepository
                .findById(categoryId)
                .orElse(null);

        if (category == null) {
            return null;
        }

        String imageUrl = null;

        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = cloudinaryService.uploadImage(image);
            } catch (IOException e) {
                return null;
            }
        }

        Product product = Product.builder()
                .name(name.trim())
                .description(description)
                .price(price)
                .stock(stock)
                .category(category)
                .imageUrl(imageUrl)
                .status(true)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public boolean updateProduct(
            Long productId,
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            Long categoryId,
            MultipartFile image
    ) {
        Product product = productRepository
                .findById(productId)
                .orElse(null);

        if (product == null) {
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        if (stock == null || stock < 0) {
            return false;
        }

        Category category = categoryRepository
                .findById(categoryId)
                .orElse(null);

        if (category == null) {
            return false;
        }

        product.setName(name.trim());
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(image);
                product.setImageUrl(imageUrl);
            } catch (IOException e) {
                return false;
            }
        }

        productRepository.save(product);

        return true;
    }

    @Transactional
    public boolean updateStock(
            Long productId,
            Integer stock
    ) {
        if (stock == null || stock < 0) {
            return false;
        }

        Product product = productRepository
                .findById(productId)
                .orElse(null);

        if (product == null) {
            return false;
        }

        product.setStock(stock);

        productRepository.save(product);

        return true;
    }

    @Transactional
    public boolean updatePrice(
            Long productId,
            BigDecimal price
    ) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        Product product = productRepository
                .findById(productId)
                .orElse(null);

        if (product == null) {
            return false;
        }

        product.setPrice(price);

        productRepository.save(product);

        return true;
    }

    @Transactional
    public boolean updateStatus(
            Long productId,
            boolean status
    ) {
        Product product = productRepository
                .findById(productId)
                .orElse(null);

        if (product == null) {
            return false;
        }

        product.setStatus(status);

        productRepository.save(product);

        return true;
    }

    @Transactional
    public boolean deleteProduct(Long productId) {
        Product product = productRepository
                .findById(productId)
                .orElse(null);

        if (product == null) {
            return false;
        }

        /*
         * Product được yêu cầu xóa thật khỏi database.
         *
         * Tuy nhiên Product có thể đang được tham chiếu bởi:
         * - CartItem
         * - OrderItem
         * - Review
         *
         * Nếu database có FK, delete có thể thất bại.
         *
         * Không tự ý cascade delete các dữ liệu lịch sử.
         */
        try {
            productRepository.delete(product);
            productRepository.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Product createProduct(
            String name,
            String brand,
            String description,
            BigDecimal price,
            Integer stock,
            Long categoryId,
            MultipartFile image
    ) {
        Product p = createProduct(name, description, price, stock, categoryId, image);
        if (p != null && brand != null && !brand.isBlank()) {
            p.setBrand(brand.trim());
            return productRepository.save(p);
        }
        return p;
    }

    @Transactional
    public boolean updateProduct(
            Long productId,
            String name,
            String brand,
            String description,
            BigDecimal price,
            Integer stock,
            Long categoryId,
            MultipartFile image
    ) {
        boolean updated = updateProduct(productId, name, description, price, stock, categoryId, image);
        if (updated) {
            Product p = findById(productId);
            if (p != null) {
                p.setBrand(brand != null ? brand.trim() : null);
                productRepository.save(p);
            }
        }
        return updated;
    }

    @Transactional
    public ProductVariant addVariant(
            Long productId,
            String storage,
            String color,
            BigDecimal price,
            Integer stock,
            String sku,
            MultipartFile image
    ) {
        Product product = findById(productId);
        if (product == null) return null;

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = cloudinaryService.uploadImage(image);
            } catch (IOException ignored) {}
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .storage(storage != null ? storage.trim() : "")
                .color(color != null ? color.trim() : "")
                .price(price != null ? price : (product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO))
                .stock(stock != null ? stock : 0)
                .sku(sku != null && !sku.isBlank() ? sku.trim() : null)
                .imageUrl(imageUrl)
                .build();

        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Sync product total stock
        int totalVariantStock = productVariantRepository.findByProductId(productId)
                .stream()
                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                .sum();
        product.setStock(totalVariantStock);
        productRepository.save(product);

        return savedVariant;
    }

    @Transactional
    public ProductVariant updateVariant(
            Long variantId,
            String storage,
            String color,
            BigDecimal price,
            Integer stock,
            String sku
    ) {
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null) return null;

        if (storage != null && !storage.isBlank()) variant.setStorage(storage.trim());
        if (color != null && !color.isBlank()) variant.setColor(color.trim());
        if (price != null && price.compareTo(BigDecimal.ZERO) >= 0) variant.setPrice(price);
        if (stock != null && stock >= 0) variant.setStock(stock);
        if (sku != null) variant.setSku(sku.trim().isEmpty() ? null : sku.trim());

        ProductVariant saved = productVariantRepository.save(variant);

        Product product = variant.getProduct();
        if (product != null) {
            int totalVariantStock = productVariantRepository.findByProductId(product.getId())
                    .stream()
                    .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                    .sum();
            product.setStock(totalVariantStock);
            productRepository.save(product);
        }

        return saved;
    }

    @Transactional
    public boolean deleteVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null) return false;

        Product product = variant.getProduct();
        productVariantRepository.delete(variant);

        if (product != null) {
            int totalVariantStock = productVariantRepository.findByProductId(product.getId())
                    .stream()
                    .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                    .sum();
            product.setStock(totalVariantStock);
            productRepository.save(product);
        }
        return true;
    }

    @Transactional
    public ProductSpecification saveSpecification(
            Long productId,
            String screenSize,
            String screenTech,
            String chipset,
            String ram,
            String battery,
            String chargingWatt,
            String rearCamera,
            String frontCamera,
            String os
    ) {
        Product product = findById(productId);
        if (product == null) return null;

        ProductSpecification spec = productSpecificationRepository
                .findByProductId(productId)
                .orElse(ProductSpecification.builder().product(product).build());

        spec.setScreenSize(screenSize != null && !screenSize.isBlank() ? screenSize.trim() : null);
        spec.setScreenTech(screenTech != null && !screenTech.isBlank() ? screenTech.trim() : null);
        spec.setChipset(chipset != null && !chipset.isBlank() ? chipset.trim() : null);
        spec.setRam(ram != null && !ram.isBlank() ? ram.trim() : null);
        spec.setBattery(battery != null && !battery.isBlank() ? battery.trim() : null);
        spec.setChargingWatt(chargingWatt != null && !chargingWatt.isBlank() ? chargingWatt.trim() : null);
        spec.setRearCamera(rearCamera != null && !rearCamera.isBlank() ? rearCamera.trim() : null);
        spec.setFrontCamera(frontCamera != null && !frontCamera.isBlank() ? frontCamera.trim() : null);
        spec.setOs(os != null && !os.isBlank() ? os.trim() : null);

        return productSpecificationRepository.save(spec);
    }

    public Page<Product> filterProducts(PhoneFilterCriteria criteria, Pageable pageable) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (criteria != null && criteria.getSort() != null) {
            switch (criteria.getSort()) {
                case "price_asc" -> sort = Sort.by(Sort.Direction.ASC, "price");
                case "price_desc" -> sort = Sort.by(Sort.Direction.DESC, "price");
                case "newest" -> sort = Sort.by(Sort.Direction.DESC, "createdAt");
            }
        }
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return productRepository.findAll(ProductSpecifications.withFilter(criteria), sortedPageable);
    }

    public List<String> getAvailableBrands() {
        return productRepository.findDistinctBrands();
    }

    public List<String> getAvailableRams() {
        return productRepository.findDistinctRams();
    }

    public List<String> getAvailableStorages() {
        return productRepository.findDistinctStorages();
    }

    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findAll().stream()
                .filter(p -> p.getStatus() != null && p.getStatus())
                .filter(p -> (p.getStock() != null && p.getStock() <= threshold)
                        || (p.getVariants() != null && p.getVariants().stream().anyMatch(v -> v.getStock() != null && v.getStock() <= threshold)))
                .toList();
    }

    @Transactional
    public Product createProductWithSpecs(
            String name,
            String brand,
            String description,
            BigDecimal price,
            Integer stock,
            Long categoryId,
            MultipartFile image,
            String screenSize,
            String screenTech,
            String chipset,
            String ram,
            String battery,
            String chargingWatt,
            String rearCamera,
            String frontCamera,
            String os
    ) {
        Product p = createProduct(name, brand, description, price, stock, categoryId, image);
        if (p != null) {
            saveSpecification(p.getId(), screenSize, screenTech, chipset, ram, battery, chargingWatt, rearCamera, frontCamera, os);
        }
        return p;
    }
}
