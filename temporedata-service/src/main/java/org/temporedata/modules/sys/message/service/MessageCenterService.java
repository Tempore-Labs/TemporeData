package org.temporedata.modules.sys.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.sys.message.entity.MessageEntity;
import org.temporedata.modules.sys.message.repository.MessageRepository;
import org.temporedata.modules.sys.notify.entity.NotifyEntity;
import org.temporedata.modules.sys.notify.repository.NotifyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unified message center (P4-SYS 4.2).
 *
 * Single dispatch point for platform notifications. It honors the notify
 * channel switches (zy_notify: name = channel, status = ENABLED/DISABLED),
 * persists an in-app message for consumption by {@code /api/message}, and
 * keeps outbound channels (EMAIL / WEBHOOK ...) as log-only placeholders so
 * business consumers (baseline alarm, audit policy alert, workflow failure)
 * do not each re-implement sending.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCenterService {

    private final MessageRepository messageRepository;
    private final NotifyRepository notifyRepository;

    /**
     * Send a message through the named channel.
     *
     * @param channel  channel name, e.g. ALARM / AUDIT / WORKFLOW (also a notify switch key)
     * @param title    short title
     * @param content  full text
     * @param userId   recipient; falls back to "system" when null
     * @param tenantId tenant scope
     * @return the persisted in-app message, or null when the channel is switched off
     */
    @Transactional
    public MessageEntity send(String channel, String title, String content, String userId, String tenantId) {
        if (channelDisabled(channel)) {
            log.info("[MessageCenter] channel '{}' disabled, skip: {}", channel, title);
            return null;
        }
        MessageEntity msg = MessageEntity.builder()
                .name(title == null ? channel : title)
                .description(content)
                .status("UNREAD")
                .createBy(userId == null ? "system" : userId)
                .tenantId(tenantId == null ? "DEFAULT" : tenantId)
                .build();
        MessageEntity saved = messageRepository.save(msg);
        log.info("[MessageCenter] sent {} to {} (tenant={})", channel, saved.getCreateBy(), saved.getTenantId());
        return saved;
    }

    private boolean channelDisabled(String channel) {
        if (channel == null || channel.isBlank()) return false;
        NotifyEntity n = notifyRepository.findAll().stream()
                .filter(e -> channel.equals(e.getName()))
                .findFirst().orElse(null);
        // No switch configured -> default enabled.
        return n != null && "DISABLED".equalsIgnoreCase(n.getStatus());
    }
}