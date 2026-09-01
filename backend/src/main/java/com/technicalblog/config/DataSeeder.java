package com.technicalblog.config;

import com.technicalblog.entity.Category;
import com.technicalblog.entity.Role;
import com.technicalblog.entity.User;
import com.technicalblog.repository.CategoryRepository;
import com.technicalblog.repository.UserRepository;
import com.technicalblog.util.SlugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Creates the first ADMIN account and the starter roadmap sections.
 * Runs only when the corresponding table is still empty, so restarts never duplicate data.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<Map.Entry<String, String>> DEFAULT_CATEGORIES = List.of(
            Map.entry("Introduction to LLD", "Why low level design matters and how to approach it"),
            Map.entry("Object Oriented Programming", "Classes, objects and the four pillars of OOP"),
            Map.entry("Design Principles", "SOLID, DRY, KISS and YAGNI"),
            Map.entry("Creational Design Patterns", "Singleton, Factory, Builder and friends"),
            Map.entry("Structural Design Patterns", "Adapter, Decorator, Facade, Proxy and more"),
            Map.entry("Behavioral Design Patterns", "Strategy, Observer, Command, State and more"),
            Map.entry("LLD Problems", "End to end low level design problems"));

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminProperties;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      PasswordEncoder passwordEncoder,
                      AdminSeedProperties adminProperties) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!adminProperties.seedEnabled()) {
            return;
        }
        seedAdmin();
        seedCategories();
    }

    private void seedAdmin() {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = User.builder()
                .username(adminProperties.username())
                .email(adminProperties.email())
                .password(passwordEncoder.encode(adminProperties.password()))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Seeded ADMIN account {}. Change the password after the first login.", admin.getEmail());
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        int order = 1;
        for (Map.Entry<String, String> entry : DEFAULT_CATEGORIES) {
            categoryRepository.save(Category.builder()
                    .name(entry.getKey())
                    .slug(SlugUtils.toSlug(entry.getKey()))
                    .description(entry.getValue())
                    .displayOrder(order)
                    .build());
            order++;
        }
        log.info("Seeded {} starter categories", DEFAULT_CATEGORIES.size());
    }
}
