package com.llmcouncil.repository;

import com.llmcouncil.model.entity.AppSettingsEntity;
import com.llmcouncil.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for {@link AppSettingsRepository} against a real Postgres (Testcontainers). */
class AppSettingsRepositoryTest extends AbstractPostgresTest {

    @Autowired
    AppSettingsRepository repository;

    @Test
    void findSingleton_noRow_returnsEmpty() {
        assertThat(repository.findSingleton()).isEmpty();
    }

    @Test
    void save_thenFindSingleton_returnsSavedRow() {
        AppSettingsEntity entity = new AppSettingsEntity(AppSettingsEntity.SINGLETON_ID);
        entity.setPresidentModelId("anthropic/claude-sonnet-5");
        repository.save(entity);

        assertThat(repository.findSingleton())
                .get()
                .extracting(AppSettingsEntity::getPresidentModelId)
                .isEqualTo("anthropic/claude-sonnet-5");
    }

    @Test
    void saveTwice_sameFixedId_resultsInExactlyOneRow() {
        AppSettingsEntity first = new AppSettingsEntity(AppSettingsEntity.SINGLETON_ID);
        first.setPresidentModelId("openai/gpt-5");
        repository.save(first);

        AppSettingsEntity second = new AppSettingsEntity(AppSettingsEntity.SINGLETON_ID);
        second.setPresidentModelId("anthropic/claude-sonnet-5");
        repository.save(second);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findSingleton()).get()
                .extracting(AppSettingsEntity::getPresidentModelId)
                .isEqualTo("anthropic/claude-sonnet-5");
    }

    @Test
    void nullableTextColumns_persistAndReadBackNull() {
        AppSettingsEntity entity = new AppSettingsEntity(AppSettingsEntity.SINGLETON_ID);
        // openRouterKeyEncrypted / tavilyKeyEncrypted deliberately left null.
        repository.save(entity);

        AppSettingsEntity reloaded = repository.findSingleton().orElseThrow();
        assertThat(reloaded.getOpenRouterKeyEncrypted()).isNull();
        assertThat(reloaded.getTavilyKeyEncrypted()).isNull();
    }
}
