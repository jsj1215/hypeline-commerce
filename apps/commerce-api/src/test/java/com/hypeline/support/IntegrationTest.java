package com.hypeline.support;

import com.hypeline.testcontainers.MySqlTestContainersConfig;
import com.hypeline.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실제 MySQL 을 띄워 검증하는 통합 테스트의 기반.
 *
 * <p>인메모리 DB 를 쓰지 않는다. 이 프로젝트가 검증해야 하는 것은 인덱스 사용 여부와
 * 락 동작인데, 둘 다 DB 구현마다 다르게 동작해서 H2 로 통과해도 의미가 없다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(MySqlTestContainersConfig.class)
public abstract class IntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }
}
