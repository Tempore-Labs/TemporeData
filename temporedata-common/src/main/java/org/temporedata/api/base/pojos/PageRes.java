package org.temporedata.api.base.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Unified pagination contract (v2.0 §6).
 *
 * <p>Controllers wrap page data in {@link PageRes} so the frontend consumes a
 * stable shape: {@code { items, total, page, size }} regardless of backend
 * repository implementation (Spring Data Page, plain lists, custom queries).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRes<T> {

    private List<T> items;

    private long total;

    /** 1-based current page. */
    private int page;

    private int size;

    public static <T> PageRes<T> of(List<T> items, long total, int page, int size) {
        return new PageRes<>(items, total, page, size);
    }

    public static <T> PageRes<T> empty() {
        return new PageRes<>(Collections.emptyList(), 0, 1, 0);
    }
}