package org.temporedata.api.datasource.context;

import org.temporedata.api.datasource.DatasourcePluginInfo;
import org.temporedata.api.datasource.DatasourceProcessor;
import org.temporedata.api.datasource.DatasourceType;
import org.temporedata.api.datasource.conn.PluginNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry of all registered {@link DatasourceProcessor} instances.
 *
 * <p>Built-in plugins are auto-collected by Spring from {@code List<DatasourceProcessor>}
 * constructor injection and remembered as the baseline; uploaded external plugins
 * are added through {@link #register(DatasourceType, DatasourceProcessor)} which may
 * override a built-in of the same type. {@link #unregister} restores the built-in
 * instance (instead of removing it) so unloading an external plugin never drops a
 * built-in dialect.</p>
 */
@Component
public class DatasourcePluginContext {

    /** Spring-collected built-in processors (stable baseline). */
    private final Map<DatasourceType, DatasourceProcessor> builtins;

    /** Current active processors: built-ins plus any external override. */
    private final Map<DatasourceType, DatasourceProcessor> processors;

    public DatasourcePluginContext(List<DatasourceProcessor> builtinProcessors) {
        this.builtins = builtinProcessors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        DatasourceProcessor::type,
                        Function.identity(),
                        (a, b) -> a));
        this.processors = new ConcurrentHashMap<>(this.builtins);
    }

    /** Resolve a processor by type (case-insensitive string). */
    public DatasourceProcessor resolve(String type) {
        DatasourceType dt = DatasourceType.of(type);
        if (dt == null) {
            throw new PluginNotFoundException(unsupportedMessage(type));
        }
        DatasourceProcessor processor = processors.get(dt);
        if (processor == null) {
            throw new PluginNotFoundException(unsupportedMessage(type));
        }
        return processor;
    }

    /** Resolve a processor by type key. */
    public DatasourceProcessor resolve(DatasourceType type) {
        DatasourceProcessor processor = processors.get(type);
        if (processor == null) {
            throw new PluginNotFoundException(unsupportedMessage(type == null ? null : type.name()));
        }
        return processor;
    }

    /** Whether a processor exists for the given type (case-insensitive). */
    public boolean supports(String type) {
        DatasourceType dt = DatasourceType.of(type);
        return dt != null && processors.containsKey(dt);
    }

    /** Ordered types supported by registered processors. */
    public List<DatasourceType> listTypes() {
        return processors.values().stream()
                .map(DatasourceProcessor::type)
                .sorted(java.util.Comparator.comparing(DatasourceType::name))
                .collect(Collectors.toList());
    }

    /** Read-only descriptor list of all currently supported plugins (built-in + uploaded). */
    public List<DatasourcePluginInfo> listPlugins() {
        return processors.entrySet().stream()
                .map(e -> new DatasourcePluginInfo(
                        e.getKey().name(),
                        e.getValue().dialectName(),
                        e.getValue().defaultPort(),
                        builtins.containsKey(e.getKey())))
                .sorted(java.util.Comparator.comparing(DatasourcePluginInfo::getType))
                .collect(Collectors.toList());
    }

    /** Register an external (uploaded) processor, possibly shadowing a built-in. */
    public void register(DatasourceType type, DatasourceProcessor processor) {
        processors.put(type, processor);
    }

    /**
     * Unload an external processor. If the type has a built-in baseline, restore
     * it; otherwise remove the type entirely.
     */
    public void unregister(DatasourceType type) {
        DatasourceProcessor builtin = builtins.get(type);
        if (builtin != null) {
            processors.put(type, builtin);
        } else {
            processors.remove(type);
        }
    }

    private String unsupportedMessage(String type) {
        String supported = processorTypesText();
        return "Unsupported datasource type: " + type + ", supported: [" + supported + "]";
    }

    private String processorTypesText() {
        return processors.keySet().stream()
                .map(DatasourceType::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}