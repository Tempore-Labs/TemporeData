package org.temporedata.modules.sys.message.service;

import org.temporedata.modules.sys.message.entity.MessageEntity;
import org.temporedata.modules.sys.message.repository.MessageRepository;
import org.temporedata.modules.sys.user.entity.UserEntity;
import org.temporedata.modules.sys.user.repository.UserRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /** Current authenticated user's messages (personal center / message center). */
    public List<MessageEntity> myList() {
        return list(currentUserId(), null);
    }

    /** Current authenticated user's unread count (topbar badge). */
    public long myUnreadCount() {
        return getUnreadCount(currentUserId());
    }

    /** Resolve the current user id from the JWT subject (name). */
    private String currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(UserEntity::getId).orElse(null);
    }

    public List<MessageEntity> list(String userId, String tenantId) {
        return messageRepository.findAll().stream()
                .filter(m -> (userId == null || userId.isEmpty() || userId.equals(m.getCreateBy())))
                .filter(m -> (tenantId == null || tenantId.isEmpty() || tenantId.equals(m.getTenantId())))
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String userId) {
        return messageRepository.findAll().stream()
                .filter(m -> (userId == null || userId.isEmpty() || userId.equals(m.getCreateBy())))
                .filter(m -> !"READ".equals(m.getStatus()))
                .count();
    }

    @Transactional
    public MessageEntity send(MessageEntity entity) {
        entity.setStatus("UNREAD");
        return messageRepository.save(entity);
    }

    @Transactional
    public MessageEntity markAsRead(String id) {
        MessageEntity entity = messageRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Message not found: " + id));
        entity.setStatus("READ");
        return messageRepository.save(entity);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        messageRepository.findAll().stream()
                .filter(m -> (userId == null || userId.isEmpty() || userId.equals(m.getCreateBy())))
                .filter(m -> !"READ".equals(m.getStatus()))
                .forEach(m -> {
                    m.setStatus("READ");
                    messageRepository.save(m);
                });
    }

    @Transactional
    public void delete(String id) {
        messageRepository.deleteById(id);
    }
}