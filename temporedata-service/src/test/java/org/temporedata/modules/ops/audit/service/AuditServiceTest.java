package org.temporedata.modules.ops.audit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.temporedata.modules.ops.audit.entity.AuditEventEntity;
import org.temporedata.modules.ops.audit.repository.AuditArchiveRepository;
import org.temporedata.modules.ops.audit.repository.AuditEventRepository;
import org.temporedata.modules.ops.audit.repository.AuditPolicyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditServiceTest {

    @Mock AuditEventRepository eventRepository;
    @Mock AuditPolicyRepository policyRepository;
    @Mock AuditArchiveRepository archiveRepository;

    private List<AuditEventEntity> store = new ArrayList<>();

    private AuditService newService() {
        when(eventRepository.save(any(AuditEventEntity.class))).thenAnswer(inv -> {
            AuditEventEntity e = inv.getArgument(0);
            store.add(e);
            return e;
        });
        when(eventRepository.findAll()).thenAnswer(inv -> new ArrayList<>(store));
        when(eventRepository.findTopByOrderBySeqDesc()).thenAnswer(inv -> {
            if (store.isEmpty()) return Optional.empty();
            Optional<AuditEventEntity> last = store.stream().max((a, b) -> Long.compare(a.getSeq(), b.getSeq()));
            return last;
        });
        return new AuditService(eventRepository, policyRepository, archiveRepository);
    }

    private void audit(AuditService s, String action, int i) {
        s.record("workflow", action, "workflow", "wf-" + i,
                "admin", "127.0.0.1", "req-" + i, "SUCCESS", "{\"v\":" + i + "}");
    }

    @Test
    void hashChainBuildsSequentially() {
        AuditService s = newService();
        audit(s, "create", 1);
        audit(s, "update", 2);
        audit(s, "execute", 3);

        assertThat(store).hasSize(3);
        assertThat(store.get(0).getSeq()).isEqualTo(1L);
        assertThat(store.get(0).getPrevHash()).isEqualTo("GENESIS");
        assertThat(store.get(1).getPrevHash()).isEqualTo(store.get(0).getEventHash());
        assertThat(store.get(2).getPrevHash()).isEqualTo(store.get(1).getEventHash());
        assertThat(store.get(2).getEventHash()).isNotBlank();
    }

    @Test
    void verifyDetectsTamperedRecord() {
        AuditService s = newService();
        audit(s, "create", 1);
        audit(s, "update", 2);
        audit(s, "execute", 3);

        // chain is intact before tampering
        Map<String, Object> before = s.verifyRange(null, null);
        assertThat((Boolean) before.get("ok")).isTrue();

        // tamper the middle record
        store.get(1).setEventHash("tampered");

        Map<String, Object> after = s.verifyRange(null, null);
        assertThat((Boolean) after.get("ok")).isFalse();
        @SuppressWarnings("unchecked")
        List<String> broken = (List<String>) after.get("brokenIds");
        assertThat(broken).isNotEmpty();
    }

    @Test
    void exportProducesRootHash() {
        AuditService s = newService();
        audit(s, "create", 1);
        audit(s, "update", 2);
        Map<String, Object> out = s.export(null, null);
        assertThat((Integer) out.get("records")).isEqualTo(2);
        assertThat((String) out.get("csv")).contains("seq,event_time");
        assertThat((String) out.get("rootHash")).isNotBlank();
    }
}