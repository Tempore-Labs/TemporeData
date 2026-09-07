package org.temporedata.api.sys.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Change password request (self-service, verifies the old password first).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordReq {

    private String oldPassword;

    private String newPassword;
}