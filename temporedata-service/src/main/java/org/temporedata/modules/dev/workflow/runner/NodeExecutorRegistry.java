package org.temporedata.modules.dev.workflow.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a {@link NodeExecutor} for a node type, falling back to a placeholder
 * executor for types that do not have a real engine yet.
 */
@Slf4j
@Component
public class NodeExecutorRegistry {

    private final List<NodeExecutor> executors;
    private final Map<String, NodeExecutor> byType = new HashMap<>();
    private NodeExecutor placeholder;

    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        this.executors = executors;
    }

    @PostConstruct
    void init() {
        for (NodeExecutor e : executors) {
            if (e.type().isEmpty()) {
                placeholder = e;
            } else {
                for (String t : e.type()) {
                    byType.put(normalize(t), e);
                }
            }
        }
        log.info("Node executor registry ready: types={}", byType.keySet());
    }

    public NodeExecutor get(String nodeType) {
        NodeExecutor e = byType.get(normalize(nodeType));
        return e != null ? e : placeholder;
    }

    private String normalize(String type) {
        return type == null ? "" : type.toUpperCase(Locale.ROOT);
    }
}