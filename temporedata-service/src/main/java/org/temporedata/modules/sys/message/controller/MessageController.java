package org.temporedata.modules.sys.message.controller;

import org.temporedata.modules.sys.message.entity.MessageEntity;
import org.temporedata.modules.sys.message.service.MessageService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public BaseResponse<List<MessageEntity>> list(@RequestParam(required = false) String userId,
                                                  @RequestParam(required = false) String tenantId) {
        return BaseResponse.success(messageService.list(userId, tenantId));
    }

    @GetMapping("/unread-count")
    public BaseResponse<Long> getUnreadCount(@RequestParam(required = false) String userId) {
        return BaseResponse.success(messageService.getUnreadCount(userId));
    }

    /** My messages (personal center / message center). */
    @GetMapping("/my")
    public BaseResponse<List<MessageEntity>> myList() {
        return BaseResponse.success(messageService.myList());
    }

    /** My unread count (topbar badge). */
    @GetMapping("/my/unread-count")
    public BaseResponse<Long> myUnreadCount() {
        return BaseResponse.success(messageService.myUnreadCount());
    }

    @PostMapping
    public BaseResponse<MessageEntity> send(@RequestBody MessageEntity entity) {
        return BaseResponse.success(messageService.send(entity));
    }

    @PutMapping("/{id}/read")
    public BaseResponse<MessageEntity> markAsRead(@PathVariable String id) {
        return BaseResponse.success(messageService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public BaseResponse<Void> markAllAsRead(@RequestParam(required = false) String userId) {
        messageService.markAllAsRead(userId);
        return BaseResponse.success();
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        messageService.delete(id);
        return BaseResponse.success();
    }
}