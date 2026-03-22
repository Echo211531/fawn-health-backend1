package com.zr.health.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 知识库加载状态：将 classpath 文档清单摘要持久化到 PostgreSQL，
 * 避免每次启动都对相同文档重复向量化写入 PgVector。
 */
@Slf4j
@Service
public class RagKnowledgeManifestService {

    /** 当前项目仅一类知识：resources/document 下 Markdown */
    public static final String SCOPE_CLASSPATH_DOCUMENT_MD = "classpath:document/**/*.md";

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS rag_knowledge_manifest (
                scope VARCHAR(128) PRIMARY KEY,
                content_sha256 VARCHAR(64) NOT NULL,
                markdown_file_count INT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    /**
     * 确保元数据表存在。
     */
    public void ensureManifestTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(DDL);
    }

    /**
     * 查询已持久化的 manifest。
     */
    public Optional<ManifestRecord> findManifest(JdbcTemplate jdbcTemplate, String scope) {
        String sql = "SELECT content_sha256, markdown_file_count FROM rag_knowledge_manifest WHERE scope = ?";
        List<ManifestRecord> rows = jdbcTemplate.query(sql, (rs, rowNum) ->
                new ManifestRecord(rs.getString("content_sha256"), rs.getInt("markdown_file_count")), scope);
        return rows.stream().findFirst();
    }

    /**
     * 保存或更新 manifest。
     */
    public void upsertManifest(JdbcTemplate jdbcTemplate, String scope, String sha256Hex, int markdownFileCount) {
        String sql = """
                INSERT INTO rag_knowledge_manifest (scope, content_sha256, markdown_file_count, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (scope) DO UPDATE SET
                    content_sha256 = EXCLUDED.content_sha256,
                    markdown_file_count = EXCLUDED.markdown_file_count,
                    updated_at = CURRENT_TIMESTAMP
                """;
        jdbcTemplate.update(sql, scope, sha256Hex, markdownFileCount);
    }

    /**
     * 清空向量表（文档变更或需要全量重载时调用）。
     */
    public void truncateVectorTable(JdbcTemplate jdbcTemplate, String vectorTableName) {
        String sql = "TRUNCATE TABLE " + quoteIdentifier(vectorTableName) + " RESTART IDENTITY";
        jdbcTemplate.execute(sql);
        log.warn("已清空向量表 {}，将重新写入知识库", vectorTableName);
    }

    /**
     * 查询向量表当前行数。
     */
    public long countVectorRows(JdbcTemplate jdbcTemplate, String vectorTableName) {
        String sql = "SELECT COUNT(*) FROM " + quoteIdentifier(vectorTableName);
        Long n = jdbcTemplate.queryForObject(sql, Long.class);
        return n != null ? n : 0L;
    }

    /**
     * PostgreSQL 简单标识符引用（表名仅允许字母数字下划线）。
     */
    private static String quoteIdentifier(String name) {
        if (name == null || !name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("非法表名: " + name);
        }
        return "\"" + name + "\"";
    }

    /**
     * 已持久化的清单记录。
     */
    public record ManifestRecord(String contentSha256, int markdownFileCount) {}
}
