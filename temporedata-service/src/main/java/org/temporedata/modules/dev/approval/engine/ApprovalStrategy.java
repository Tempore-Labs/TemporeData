package org.temporedata.modules.dev.approval.engine;

import java.util.List;

/**
 * Strategy used by the unified approval engine to act on a concrete approval
 * entity (generic dev approval / asset permission approval / ...).
 *
 * @param <T> concrete approval entity type
 */
public interface ApprovalStrategy<T> {

    /** Business type key, e.g. GENERIC or RESOURCE_PERM. */
    String type();

    T get(String id);

    List<T> listAll();

    List<T> listPending();

    List<T> listByApplicant(String applicantId);

    T save(T entity);

    void delete(String id);
}