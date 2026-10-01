package com.llmcouncil.repository;

import com.llmcouncil.model.entity.AppSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettingsEntity, Long> {

    default Optional<AppSettingsEntity> findSingleton() {
        return findById(AppSettingsEntity.SINGLETON_ID);
    }
}
