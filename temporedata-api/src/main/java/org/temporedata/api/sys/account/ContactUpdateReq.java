package org.temporedata.api.sys.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Change own phone / email. v1 updates directly; {@code verifyCode} is reserved
 * for a future OTP-enabled flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactUpdateReq {

    /** The new phone number or email address. */
    private String value;

    /** Reserved for future verification-code flow. */
    private String verifyCode;
}