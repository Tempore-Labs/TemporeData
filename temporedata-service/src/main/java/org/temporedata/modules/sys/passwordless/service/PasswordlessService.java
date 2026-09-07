package org.temporedata.modules.sys.passwordless.service;

import org.temporedata.modules.sys.passwordless.entity.PasswordlessEntity;
import org.temporedata.modules.sys.passwordless.repository.PasswordlessRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class PasswordlessService {

    private final PasswordlessRepository passwordlessRepository;

    public List<PasswordlessEntity> getConfig() {
        return passwordlessRepository.findAll();
    }

    @Transactional
    public PasswordlessEntity saveConfig(PasswordlessEntity entity) {
        return passwordlessRepository.save(entity);
    }

    @Transactional
    public String sendCode(Map<String, String> request) {
        String target = request.getOrDefault("target", "");
        String type = request.getOrDefault("type", "email");
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        log.info("Sending {} verification code [{}] to {}", type, code, target);

        PasswordlessEntity entity = new PasswordlessEntity();
        entity.setTarget(target);
        entity.setCode(code);
        entity.setLoginType(type);
        entity.setStatus("PENDING");
        passwordlessRepository.save(entity);

        return "Verification code sent to " + target;
    }

    @Transactional
    public boolean verify(Map<String, String> request) {
        String target = request.getOrDefault("target", "");
        String code = request.getOrDefault("code", "");
        return passwordlessRepository.findAll().stream()
                .anyMatch(e -> target.equals(e.getTarget()) && code.equals(e.getCode()) && "PENDING".equals(e.getStatus()));
    }
}