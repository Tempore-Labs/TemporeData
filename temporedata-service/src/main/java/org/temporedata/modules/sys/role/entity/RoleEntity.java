package org.temporedata.modules.sys.role.entity;

import lombok.Data; import lombok.Builder; import lombok.NoArgsConstructor; import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*; import java.time.LocalDateTime;

/** Role entity (table zy_role: id, code, name, remark, protected_role, data_scope, create_time). */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "zy_role")
public class RoleEntity {

    @Id @GeneratedValue(generator = "uuid2") @GenericGenerator(name = "uuid2", strategy = "uuid2") @Column(length = 36)
    private String id;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 100, nullable = false)
    private String name;

    @Column
    private String remark;

    @Column(name = "protected_role")
    private Boolean protectedRole;

    @Column(name = "data_scope")
    private String dataScope;

    @CreationTimestamp @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public String getCreatedAt() {
        return createTime == null ? "" : createTime.toString().replace("T", " ");
    }
}