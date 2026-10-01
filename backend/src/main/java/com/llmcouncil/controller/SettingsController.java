package com.llmcouncil.controller;

import com.llmcouncil.model.dto.SettingsRequest;
import com.llmcouncil.model.dto.SettingsResponse;
import com.llmcouncil.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kullanıcının OpenRouter key'ini ve konsey üyesi/başkan seçimini okur/yazar. Gerçek key
 * hiçbir response'ta dönmez — sadece var/yok ve son 4 hane (bkz. {@link SettingsResponse}).
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse get() {
        return settingsService.getResponse();
    }

    @PutMapping
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return settingsService.save(request);
    }
}
