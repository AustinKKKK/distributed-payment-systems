package com.example.tier4.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class ReplicationJob {

    private final JdbcTemplate primaryJdbc;
    private final JdbcTemplate replicaJdbc;

    public ReplicationJob(@Qualifier("primaryDataSource") DataSource primaryDataSource,
                          @Qualifier("replicaDataSource") DataSource replicaDataSource) {
        this.primaryJdbc = new JdbcTemplate(primaryDataSource);
        this.replicaJdbc = new JdbcTemplate(replicaDataSource);
    }

    @Scheduled(fixedRate = 5000)
    public void replicatePayments() {
        System.out.println("=== REPLICATION JOB RUNNING ===");
        //1. Check if there are any columns that has been refreshed within 15secs
        List<PaymentRow> recentRows = primaryJdbc.query(
                "SELECT payment_id, idempotency_key, status, created_at, updated_at " +
                "FROM payments WHERE updated_at > now() - interval '15 seconds'",
                (ResultSet rs, int rowNum) -> new PaymentRow(
                        rs.getLong("payment_id"),
                        rs.getString("idempotency_key"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                )
        );

        //2. Upated replica cols
        for (PaymentRow row : recentRows) {
            replicaJdbc.update(
                    "INSERT INTO payments (payment_id, idempotency_key, status, created_at, updated_at) " +
                    "OVERRIDING SYSTEM VALUE " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (payment_id) DO UPDATE SET " +
                    "status = EXCLUDED.status, updated_at = EXCLUDED.updated_at",
                    row.paymentId(), row.idempotencyKey(), row.status(), row.createdAt(), row.updatedAt()
            );
        }

        if (!recentRows.isEmpty()) {
            System.out.println("REPLICATED " + recentRows.size() + " payment(s) to replica");
        }
    }

    private record PaymentRow(
            Long paymentId,
            String idempotencyKey,
            String status,
            java.sql.Timestamp createdAt,
            java.sql.Timestamp updatedAt
    ) {}

}
