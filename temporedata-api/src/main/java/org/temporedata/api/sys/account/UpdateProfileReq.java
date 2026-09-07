package org.temporedata.api.sys.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update own basic profile (nickname / phone / email).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileReq {

    private String nickname;

    private String phone;

    private String email;
}