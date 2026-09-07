package org.temporedata.modules.ops.git.provider;

import org.temporedata.api.base.exceptions.BusinessException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of Ops providers (P2-10), dispatches by providerId.
 */
@Component
public class OpsProviderRegistry {

    private final Map<String, OpsProvider> byId = new LinkedHashMap<>();

    public OpsProviderRegistry(List<OpsProvider> providers) {
        for (OpsProvider p : providers) {
            byId.put(p.providerId(), p);
        }
    }

    public OpsProvider get(String providerId) {
        OpsProvider p = byId.get(providerId);
        if (p == null) throw new BusinessException("不支持的供应商: " + providerId);
        return p;
    }

    public List<OpsProvider> all() {
        return new java.util.ArrayList<>(byId.values());
    }
}