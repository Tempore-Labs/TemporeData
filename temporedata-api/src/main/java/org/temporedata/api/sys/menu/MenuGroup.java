package org.temporedata.api.sys.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A first-level workspace domain in the v2.0 information architecture.
 * When <code>admin</code> is true, the whole group is only visible to super admin.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuGroup {

    private String group;

    private boolean admin;

    private List<MenuItem> children;
}