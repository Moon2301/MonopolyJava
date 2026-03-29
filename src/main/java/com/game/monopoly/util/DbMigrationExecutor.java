package com.game.monopoly.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbMigrationExecutor implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        String[] columnsToDrop = {
            "balanceAfter", "currencyType", "reasonType", 
            "userProfileId", "referenceId", "createdAt"
        };
        
        System.out.println("--- DB MIGRATION: Starting comprehensive schema cleanup for CurrencyLedger ---");
        for (String col : columnsToDrop) {
            try {
                // Xóa các cột CamelCase dư thừa đang gây lỗi "Field doesn't have a default value"
                jdbcTemplate.execute("ALTER TABLE CurrencyLedger DROP COLUMN " + col);
                System.out.println("--- DB MIGRATION: Successfully dropped redundant column: " + col + " ---");
            } catch (Exception e) {
                // Bỏ qua nếu cột không tồn tại
                System.out.println("--- DB MIGRATION: Column " + col + " not found or already dropped. Skipping. ---");
            }
        }
        System.out.println("--- DB MIGRATION: Cleanup finished. ---");
    }
}
