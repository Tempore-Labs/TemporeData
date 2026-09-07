package org.temporedata.modules.dev.workflow.runner;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory runtime control signals shared between the command thread
 * (pause/resume/stop requests) and the running workflow thread. Process-local;
 * adequate for single-node deployment. Distributed coordination would back
 * this with a shared store/message bus.
 */
@Component
public class RuntimeControlRegistry {

    private final ConcurrentHashMap<String, RuntimeAction> actions = new ConcurrentHashMap<>();

    public RuntimeAction action(String instanceId) {
        return actions.computeIfAbsent(instanceId, k -> new RuntimeAction(instanceId));
    }

    public void remove(String instanceId) {
        actions.remove(instanceId);
    }

    /**
     * Per-instance control state. All flags are volatile so the runner thread
     * observes command-thread writes immediately.
     */
    public static class RuntimeAction {

        final String instanceId;
        volatile boolean pauseRequested;
        volatile boolean stopRequested;
        final Object monitor = new Object();

        RuntimeAction(String instanceId) {
            this.instanceId = instanceId;
        }

        public boolean isStopRequested() {
            return stopRequested;
        }

        public void requestPause() {
            pauseRequested = true;
        }

        public void requestResume() {
            pauseRequested = false;
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        public void requestStop() {
            stopRequested = true;
            pauseRequested = false;
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }
}