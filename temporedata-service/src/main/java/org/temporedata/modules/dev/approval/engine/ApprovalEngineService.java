package org.temporedata.modules.dev.approval.engine;

import org.temporedata.api.base.exceptions.BusinessException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Shared approval state machine (P4-SYS 4.4).
 *
 * <p>Delegates persistence to the {@link ApprovalStrategy} bound to each
 * business type, so all approval flows (generic, permission, ...) share the
 * same apply / approve / reject semantics without duplicating the state logic.
 */
@org.springframework.stereotype.Service
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ApprovalEngineService {

    private final java.util.List<ApprovalStrategy<?>> strategies;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ApprovalStrategy strategy(String type) {
        for (ApprovalStrategy<?> s : strategies) {
            if (s.type().equals(type)) return s;
        }
        throw new BusinessException("未知审批类型: " + type);
    }

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    @Transactional
    public <T> T apply(String type, T entity) {
        ApprovalStrategy st = strategy(type);
        markPending(entity);
        return (T) st.save(entity);
    }

    @Transactional
    public <T> T approve(String type, String id) {
        ApprovalStrategy st = strategy(type);
        T e = (T) st.get(id);
        if (!isPending(e)) throw new BusinessException("仅待审批状态可审批");
        markApproved(e);
        return (T) st.save(e);
    }

    @Transactional
    public <T> T reject(String type, String id) {
        ApprovalStrategy st = strategy(type);
        T e = (T) st.get(id);
        if (!isPending(e)) throw new BusinessException("仅待审批状态可驳回");
        markRejected(e);
        return (T) st.save(e);
    }

    public <T> T get(String type, String id) {
        return (T) strategy(type).get(id);
    }

    public <T> List<T> listAll(String type) {
        return strategy(type).listAll();
    }

    public <T> List<T> listPending(String type) {
        return strategy(type).listPending();
    }

    public <T> List<T> listByApplicant(String type, String applicantId) {
        return strategy(type).listByApplicant(applicantId);
    }

    @Transactional
    public <T> void delete(String type, String id) {
        strategy(type).delete(id);
    }

    // ---- status helpers via reflection on common "status" bean property ----

    private void markPending(Object e) {
        setField(e, "status", PENDING);
    }

    private void markApproved(Object e) {
        setField(e, "status", APPROVED);
    }

    private void markRejected(Object e) {
        setField(e, "status", REJECTED);
    }

    private boolean isPending(Object e) {
        Object v = getField(e, "status");
        return PENDING.equals(v);
    }

    private void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Method w = target.getClass().getMethod("set" + cap(name), String.class);
            w.invoke(target, value);
        } catch (Exception ex) {
            throw new BusinessException("审批实体缺少字段: " + name);
        }
    }

    private Object getField(Object target, String name) {
        try {
            java.lang.reflect.Method r = target.getClass().getMethod("get" + cap(name));
            return r.invoke(target);
        } catch (Exception ex) {
            return null;
        }
    }

    private String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}