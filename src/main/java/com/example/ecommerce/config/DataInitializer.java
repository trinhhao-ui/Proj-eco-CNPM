package com.example.ecommerce.config;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.enums.Role;
import com.example.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductSpecificationRepository productSpecificationRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initUsers();
        initCategoriesAndProducts();
        cleanUpLegacyLaptopData();
        initDiscountCodes();
    }

    private void cleanUpLegacyLaptopData() {
        try {
            java.util.List<Category> laptopCategories = categoryRepository.findByNameContainingIgnoreCase("laptop");
            Category fallback = categoryRepository.findByNameIgnoreCase("Flagship Cao Cấp").orElse(null);
            for (Category cat : laptopCategories) {
                java.util.List<Product> products = productRepository.findByCategoryId(cat.getId());
                for (Product p : products) {
                    if (fallback != null && !fallback.getId().equals(cat.getId())) {
                        p.setCategory(fallback);
                        p.setStatus(false);
                        productRepository.save(p);
                    } else {
                        productRepository.delete(p);
                    }
                }
                categoryRepository.delete(cat);
                log.info("Cleaned up legacy category: {}", cat.getName());
            }
        } catch (Exception e) {
            log.warn("Could not clean up legacy laptop category: {}", e.getMessage());
        }
    }

    private void initUsers() {
        if (userRepository.findByEmail("manager@phonestore.vn").isEmpty()) {
            User manager = User.builder()
                    .name("Quản trị viên Hệ thống")
                    .email("manager@phonestore.vn")
                    .password(passwordEncoder.encode("123456"))
                    .phone("0988888888")
                    .address("Tòa nhà Landmark, TP. Hồ Chí Minh")
                    .role(Role.MANAGER)
                    .build();
            userRepository.save(manager);
            log.info("Initialized default MANAGER account: manager@phonestore.vn / 123456");
        }

        if (userRepository.findByEmail("user@phonestore.vn").isEmpty()) {
            User customer = User.builder()
                    .name("Nguyễn Văn Khách")
                    .email("user@phonestore.vn")
                    .password(passwordEncoder.encode("123456"))
                    .phone("0912345678")
                    .address("Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội")
                    .role(Role.USER)
                    .build();
            userRepository.save(customer);
            log.info("Initialized default USER account: user@phonestore.vn / 123456");
        }
    }

    private void initCategoriesAndProducts() {
        Category flagship = categoryRepository.findByNameIgnoreCase("Flagship Cao Cấp").orElse(null);
        if (flagship == null) {
            flagship = categoryRepository.save(Category.builder().name("Flagship Cao Cấp").build());
        }

        Category midrange = categoryRepository.findByNameIgnoreCase("Tầm Trung - Cận Cao Cấp").orElse(null);
        if (midrange == null) {
            midrange = categoryRepository.save(Category.builder().name("Tầm Trung - Cận Cao Cấp").build());
        }

        Category budget = categoryRepository.findByNameIgnoreCase("Phổ Thông - Giá Rẻ").orElse(null);
        if (budget == null) {
            budget = categoryRepository.save(Category.builder().name("Phổ Thông - Giá Rẻ").build());
        }

        if (productRepository.count() == 0) {
            // 1. iPhone 16 Pro Max
            Product p1 = Product.builder()
                    .name("iPhone 16 Pro Max")
                    .brand("Apple")
                    .category(flagship)
                    .price(new BigDecimal("34990000"))
                    .stock(25)
                    .description("Điện thoại iPhone 16 Pro Max với chip A18 Pro mạnh mẽ đỉnh cao, thiết kế khung viền titan siêu nhẹ và nút Camera Control hoàn toàn mới.")
                    .status(true)
                    .build();
            p1 = productRepository.save(p1);

            productSpecificationRepository.save(ProductSpecification.builder()
                    .product(p1)
                    .screenSize("6.9 inch")
                    .screenTech("Super Retina XDR OLED 120Hz")
                    .chipset("Apple A18 Pro")
                    .ram("8GB")
                    .battery("4685 mAh")
                    .chargingWatt("Sạc nhanh 25W")
                    .rearCamera("48MP + 48MP + 12MP")
                    .frontCamera("12MP TrueDepth")
                    .os("iOS 18")
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .product(p1)
                    .storage("256GB")
                    .color("Titan Sa Mạc")
                    .price(new BigDecimal("34990000"))
                    .stock(15)
                    .sku("IP16PM-256-DESERT")
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .product(p1)
                    .storage("512GB")
                    .color("Titan Tự Nhiên")
                    .price(new BigDecimal("39990000"))
                    .stock(10)
                    .sku("IP16PM-512-NATURAL")
                    .build());

            // 2. Samsung Galaxy S24 Ultra
            Product p2 = Product.builder()
                    .name("Samsung Galaxy S24 Ultra")
                    .brand("Samsung")
                    .category(flagship)
                    .price(new BigDecimal("29990000"))
                    .stock(20)
                    .description("Galaxy S24 Ultra trang bị quyền năng Galaxy AI thế hệ mới, khung viền titan cứng cáp, camera 200MP đột phá và bút S Pen tiện lợi.")
                    .status(true)
                    .build();
            p2 = productRepository.save(p2);

            productSpecificationRepository.save(ProductSpecification.builder()
                    .product(p2)
                    .screenSize("6.8 inch")
                    .screenTech("Dynamic AMOLED 2X 120Hz")
                    .chipset("Snapdragon 8 Gen 3 for Galaxy")
                    .ram("12GB")
                    .battery("5000 mAh")
                    .chargingWatt("Sạc nhanh 45W")
                    .rearCamera("200MP + 50MP + 12MP + 10MP")
                    .frontCamera("12MP")
                    .os("One UI 6.1 (Android 14)")
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .product(p2)
                    .storage("256GB")
                    .color("Xám Titan")
                    .price(new BigDecimal("29990000"))
                    .stock(12)
                    .sku("SS-S24U-256-GREY")
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .product(p2)
                    .storage("512GB")
                    .color("Đen Titan")
                    .price(new BigDecimal("33990000"))
                    .stock(8)
                    .sku("SS-S24U-512-BLACK")
                    .build());

            // 3. Xiaomi 14 Ultra
            Product p3 = Product.builder()
                    .name("Xiaomi 14 Ultra")
                    .brand("Xiaomi")
                    .category(flagship)
                    .price(new BigDecimal("24990000"))
                    .stock(15)
                    .description("Xiaomi 14 Ultra hợp tác cùng Leica mang đến trải nghiệm chụp ảnh quang học tuyệt đỉnh, sạc siêu tốc 90W và màn hình 2K siêu nét.")
                    .status(true)
                    .build();
            p3 = productRepository.save(p3);

            productSpecificationRepository.save(ProductSpecification.builder()
                    .product(p3)
                    .screenSize("6.73 inch")
                    .screenTech("AMOLED 120Hz 2K")
                    .chipset("Snapdragon 8 Gen 3")
                    .ram("16GB")
                    .battery("5000 mAh")
                    .chargingWatt("Sạc siêu nhanh 90W")
                    .rearCamera("Leica 50MP + 50MP + 50MP + 50MP")
                    .frontCamera("32MP")
                    .os("Xiaomi HyperOS")
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .product(p3)
                    .storage("512GB")
                    .color("Trắng Gốm")
                    .price(new BigDecimal("24990000"))
                    .stock(15)
                    .sku("MI14U-512-WHITE")
                    .build());

            log.info("Initialized sample smartphone catalog (iPhone 16 Pro Max, S24 Ultra, Xiaomi 14 Ultra) with specifications and variants.");
        }
    }

    private void initDiscountCodes() {
        if (discountCodeRepository.count() == 0) {
            LocalDateTime now = LocalDateTime.now();

            discountCodeRepository.save(DiscountCode.builder()
                    .code("WELCOME10")
                    .discountValue(10)
                    .quantity(100)
                    .startAt(now.minusDays(1))
                    .endAt(now.plusDays(365))
                    .build());

            discountCodeRepository.save(DiscountCode.builder()
                    .code("FREESHIP")
                    .discountValue(5)
                    .quantity(200)
                    .startAt(now.minusDays(1))
                    .endAt(now.plusDays(365))
                    .build());

            discountCodeRepository.save(DiscountCode.builder()
                    .code("VIPPHONE")
                    .discountValue(15)
                    .quantity(50)
                    .startAt(now.minusDays(1))
                    .endAt(now.plusDays(365))
                    .build());

            log.info("Initialized default discount codes: WELCOME10, FREESHIP, VIPPHONE.");
        }
    }
}
