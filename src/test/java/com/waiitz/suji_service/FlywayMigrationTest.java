package com.waiitz.suji_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest extends PostgresContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreSchemasTablesAndVectorExtension() {
        Integer vectorExtensionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'",
                Integer.class
        );
        Integer usersTableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'app' AND table_name = 'users'",
                Integer.class
        );
        Integer documentsTableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'doc' AND table_name = 'documents'",
                Integer.class
        );
        Integer embeddingsTableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'ai' AND table_name = 'document_chunk_embeddings'",
                Integer.class
        );

        assertThat(vectorExtensionCount).isEqualTo(1);
        assertThat(usersTableCount).isEqualTo(1);
        assertThat(documentsTableCount).isEqualTo(1);
        assertThat(embeddingsTableCount).isEqualTo(1);
    }
}
