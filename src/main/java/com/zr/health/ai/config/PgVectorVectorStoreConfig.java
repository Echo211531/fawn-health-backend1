package com.zr.health.ai.config;

import com.zr.health.ai.rag.HealthAppDocumentLoader;
import com.zr.health.ai.rag.RagKnowledgeManifestService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * PgVector 向量库：数据已持久化在 PostgreSQL 中。
 * <p>
 * 启动时仅在「开启加载」且「源文档摘要相对库中记录发生变化」时重新向量化；
 * 若摘要一致则跳过，避免每次启动重复 embedding。
 */
@Slf4j
@Configuration
public class PgVectorVectorStoreConfig {

    private static final String VECTOR_TABLE_NAME = "vector_store";

    @Value("${load.documents.on.startup:false}")
    private boolean loadDocumentsOnStartup;

    /**
     * 为 true（默认）时：若 {@code classpath:document} 下 Markdown 内容摘要与库中一致，则跳过加载。
     * 为 false 时：每次启动在开启 {@link #loadDocumentsOnStartup} 时都会重新向量化（会先清空向量表再写入）。
     */
    @Value("${load.documents.skip-if-unchanged:true}")
    private boolean skipLoadIfUnchanged;

    @Resource
    private HealthAppDocumentLoader healthAppDocumentLoader;

    @Resource
    private RagKnowledgeManifestService ragKnowledgeManifestService;

    @Bean
    @Primary
    public VectorStore pgVectorVectorStore(@Qualifier("postgresJdbcTemplate") JdbcTemplate jdbcTemplate,
                                          EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1536)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName(VECTOR_TABLE_NAME)
                .maxDocumentBatchSize(10000)
                .build();

        if (!loadDocumentsOnStartup) {
            log.info("load.documents.on.startup=false，跳过知识库向量化，仅使用已有 PostgreSQL 中的向量数据。");
            return vectorStore;
        }

        ragKnowledgeManifestService.ensureManifestTable(jdbcTemplate);
        HealthAppDocumentLoader.ManifestDigest digest = healthAppDocumentLoader.computeManifestDigest();
        Optional<RagKnowledgeManifestService.ManifestRecord> stored =
                ragKnowledgeManifestService.findManifest(jdbcTemplate, RagKnowledgeManifestService.SCOPE_CLASSPATH_DOCUMENT_MD);
        long rowCount = ragKnowledgeManifestService.countVectorRows(jdbcTemplate, VECTOR_TABLE_NAME);

        if (skipLoadIfUnchanged && digest.sha256Hex() != null && !digest.sha256Hex().isEmpty()) {
            if (stored.isPresent() && digest.sha256Hex().equals(stored.get().contentSha256())) {
                log.info("知识库源文档未变更（SHA-256 一致），跳过向量化加载。向量表 {} 当前约 {} 条。", VECTOR_TABLE_NAME, rowCount);
                return vectorStore;
            }
            // 升级兼容：库中已有向量但没有 manifest 记录时，写入当前摘要并跳过重复加载
            if (stored.isEmpty() && rowCount > 0) {
                ragKnowledgeManifestService.upsertManifest(jdbcTemplate,
                        RagKnowledgeManifestService.SCOPE_CLASSPATH_DOCUMENT_MD,
                        digest.sha256Hex(),
                        digest.markdownFileCount());
                log.info("检测到向量表已有数据但无清单记录，已写入 rag_knowledge_manifest 并跳过重复加载（升级兼容）。");
                return vectorStore;
            }
        }

        if (rowCount > 0) {
            ragKnowledgeManifestService.truncateVectorTable(jdbcTemplate, VECTOR_TABLE_NAME);
        }

        List<Document> documents = healthAppDocumentLoader.loadMarkdowns();
        int batchSize = 25;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
        }
        ragKnowledgeManifestService.upsertManifest(jdbcTemplate,
                RagKnowledgeManifestService.SCOPE_CLASSPATH_DOCUMENT_MD,
                digest.sha256Hex(),
                digest.markdownFileCount());
        log.info("✅ 知识库已加载到 PgVector：{} 个文档片段，源 Markdown 文件数 {}，摘要已持久化。",
                documents.size(), digest.markdownFileCount());

        return vectorStore;
    }
}