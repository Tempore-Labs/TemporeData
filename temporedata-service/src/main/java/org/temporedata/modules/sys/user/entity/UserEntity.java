package org.temporedata.modules.sys.user.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.time.LocalDateTime;

/** User entity (table zy_user). */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_user")
public class UserEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 32)
    private String id;

    @CreationTimestamp @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column
    private String density;

    @Column(name = "must_change_password")
    private Boolean mustChangePassword;

    @Column(nullable = false, length = 200)
    private String password;

    /** Account status: 1 = active, 0 = disabled. */
    @Column
    private Integer status;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 50)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    /** True when the account is not active. */
    public boolean isDisabled() {
        return status != null && status == 0;
    }

    /** Frontend-friendly time string. */
    public String getCreatedAt() {
        return createTime == null ? "" : createTime.toString().replace("T", " ");
    }
}