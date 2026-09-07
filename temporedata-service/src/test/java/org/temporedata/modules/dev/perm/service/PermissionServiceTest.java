package org.temporedata.modules.dev.perm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.temporedata.modules.dev.perm.entity.PermissionEntity;
import org.temporedata.modules.dev.perm.repository.PermissionRepository;
import org.temporedata.modules.dev.perm.repository.ResourceRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionServiceTest {

    @Mock PermissionRepository permissionRepository;
    @Mock ResourceRepository resourceRepository;

    private PermissionEntity perm(String role, String type, String key, String action, String scope) {
        PermissionEntity p = new PermissionEntity();
        p.setRoleId(role);
        p.setResourceType(type);
        p.setResourceKey(key);
        p.setAction(action);
        p.setScope(scope);
        return p;
    }

    private PermissionService service(List<PermissionEntity> perms) {
        when(permissionRepository.findByResourceTypeAndRoleIdIn(any(), any())).thenReturn(perms);
        return new PermissionService(resourceRepository, permissionRepository);
    }

    @Test
    void superAdminBypasses() {
        PermissionService s = service(List.of());
        assertThat(s.verify(List.of(), true, "workflow", "EXECUTE", "wf1")).isTrue();
    }

    @Test
    void emptyRolesDenies() {
        PermissionService s = service(List.of());
        assertThat(s.verify(List.of(), false, "workflow", "READ", "wf1")).isFalse();
    }

    @Test
    void deniedBecauseActionNotCovered() {
        PermissionService s = service(List.of(perm("r1", "workflow", "*", "READ", "ALLOW")));
        assertThat(s.verify(List.of("r1"), false, "workflow", "EXECUTE", "wf1")).isFalse();
    }

    @Test
    void seniorActionCoversLower() {
        PermissionService s = service(List.of(perm("r1", "workflow", "wf1", "ADMIN", "ALLOW")));
        assertThat(s.verify(List.of("r1"), false, "workflow", "READ", "wf1")).isTrue();
        assertThat(s.verify(List.of("r1"), false, "workflow", "EXECUTE", "wf1")).isTrue();
    }

    @Test
    void wildcardMatchesAllKeys() {
        PermissionService s = service(List.of(perm("r1", "workflow", "*", "READ", "ALLOW")));
        assertThat(s.verify(List.of("r1"), false, "workflow", "READ", "wf-anything")).isTrue();
    }

    @Test
    void denyOverridesAllow() {
        PermissionService s = service(List.of(
                perm("r1", "workflow", "*", "ADMIN", "DENY"),
                perm("r1", "workflow", "*", "ADMIN", "ALLOW")));
        assertThat(s.verify(List.of("r1"), false, "workflow", "ADMIN", "wf1")).isFalse();
    }

    @Test
    void resourceScopedExactMatch() {
        PermissionService s = service(List.of(perm("r1", "workflow", "wf-42", "EXECUTE", "ALLOW")));
        assertThat(s.verify(List.of("r1"), false, "workflow", "EXECUTE", "wf-42")).isTrue();
        assertThat(s.verify(List.of("r1"), false, "workflow", "EXECUTE", "wf-99")).isFalse();
    }
}