package org.temporedata.api.sys.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A leaf menu entry in a workspace domain. The optional <code>permission</code>
 * is an authorization point (resourceType) required to see the entry; super admin
 * and users holding that permission point are allowed.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItem {

    private String path;

    private String title;

    /** Required permission resourceType, or null for any authenticated user. */
    private String permission;
}