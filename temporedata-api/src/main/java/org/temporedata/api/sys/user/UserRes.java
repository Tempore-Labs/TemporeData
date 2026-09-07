package org.temporedata.api.sys.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Safe user contract for the frontend (v2.0 §6.3: entity is never the API contract).
 *
 * <p>Deliberately excludes the password field so the hashed credential can never
 * leak through a read endpoint.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRes {

    private String id;

    private String username;

    private String tenantId;

    /** Account status: 1 = active, 0 = disabled. */
    private Integer status;

    private String density;

    private Boolean mustChangePassword;

    /** Personal center profile fields. */
    private String nickname;

    private String phone;

    private String email;

    /** Frontend-friendly creation time string (yyyy-MM-dd HH:mm:ss). */
    private String createdAt;
}