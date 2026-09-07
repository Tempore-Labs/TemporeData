package org.temporedata.modules.sys.role.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Mapping between a user and a role (table zy_user_role).
 *
 * <p>Bridges the identity domain (sys) with the authorization domain
 * (dev/perm): a user's roles determine which resource permissions apply.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zy_user_role")
public class UserRoleEntity {

    @EmbeddedId
    private UserRoleId id;

    /** Composite primary key of the user-role mapping. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class UserRoleId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String userId;

        private String roleId;
    }
}