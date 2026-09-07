package org.temporedata.modules.sys.menu;

import org.temporedata.api.sys.menu.MenuGroup;
import org.temporedata.api.sys.menu.MenuItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Server-side navigation registry matching the v2.0 information architecture
 * (9 functional groups -&gt; 7 workspace domains + admin center + personal space).
 *
 * <p>Each leaf may carry a required permission resourceType. The menus endpoint
 * filters this registry against the current user's roles/permissions, so the
 * frontend sidebar stays in sync with server-side authorization (&sect;19).</p>
 */
@Component
public class MenuRegistry {

    private final List<MenuGroup> groups;

    public MenuRegistry() {
        this.groups = build();
    }

    /** The full, unfiltered navigation tree. */
    public List<MenuGroup> all() {
        return groups;
    }

    /** Filtered navigation for a user (admin flag + permission points). */
    public List<MenuGroup> menusFor(boolean isAdmin, java.util.Set<String> permissions) {
        List<MenuGroup> visible = new ArrayList<>();
        for (MenuGroup group : groups) {
            if (group.isAdmin() && !isAdmin) continue;
            List<MenuItem> children = new ArrayList<>();
            for (MenuItem item : group.getChildren()) {
                if (item.getPermission() == null || item.getPermission().isBlank()) {
                    children.add(item);
                } else if (isAdmin || permissions.contains(item.getPermission())) {
                    children.add(item);
                }
            }
            if (!children.isEmpty() || group.isAdmin() && isAdmin) {
                MenuGroup copy = new MenuGroup(group.getGroup(), group.isAdmin(), children);
                visible.add(copy);
            }
        }
        return visible;
    }

    private List<MenuGroup> build() {
        return Arrays.asList(
                group("数据开发", false,
                        item("/workflow", "工作流", "workflow"),
                        item("/sqleditor", "SQL 查询", "workflow"),
                        item("/realtime", "实时计算", "realtime"),
                        item("/viewpage", "实时大屏", "realtime"),
                        item("/globalvar", "全局变量", "workflow"),
                        item("/funcrepo", "函数仓库", "workflow"),
                        item("/dependency", "依赖合集", "workflow"),
                        item("/approval", "发布中心", "workflow")),
                group("数据资产", false,
                        item("/dataasset", "数据目录", "datasource"),
                        item("/datasource", "数据源", "datasource"),
                        item("/meta", "元数据", "metadata"),
                        item("/datacenter", "数据中心", "datasource"),
                        item("/indicator", "指标与标签", "indicator"),
                        item("/tag", "数据标签", "indicator"),
                        item("/qualityrule", "数据质量", "quality"),
                        item("/lineage", "血缘分析", "metadata")),
                group("数据服务", false,
                        item("/dataapi", "API 管理", "api"),
                        item("/report", "数据报表", "api"),
                        item("/form", "表单管理", "api"),
                        item("/apilog", "服务日志", "api"),
                        item("/blacklist", "黑白名单", "api")),
                group("运行中心", false,
                        item("/monitor", "运行总览", "monitor"),
                        item("/scheduler", "调度任务", "schedule"),
                        item("/calendar", "业务日历", "schedule"),
                        item("/alarm", "告警配置", "alarm"),
                        item("/baseline", "基线告警", "alarm"),
                        item("/ops", "集成配置", "ops")),
                group("安全治理", false,
                        item("/perm", "权限中心", "perm"),
                        item("/role", "角色授权", "role"),
                        item("/mydata", "我的数据", "data"),
                        item("/permapproval", "权限审批", "perm"),
                        item("/datalevel", "分类分级", "security"),
                        item("/sensitive", "敏感数据", "security"),
                        item("/datamask", "数据脱敏", "security"),
                        item("/auditlog", "审计日志", "audit"),
                        item("/changeaudit", "变更记录", "audit"),
                        item("/audit-center", "审计中心", "audit"),
                        item("/security-gov", "安全治理", "security")),
                group("平台资源", false,
                        item("/cluster", "计算集群", "cluster"),
                        item("/container", "计算容器", "cluster"),
                        item("/resource", "资源中心", "cluster"),
                        item("/driver", "驱动管理", "cluster")),
                group("管理中心", true,
                        item("/tenant", "租户与成员", null),
                        item("/org", "组织架构", null),
                        item("/passwordless", "免密登录", null),
                        item("/notify", "通知配置", null),
                        item("/settings", "系统配置", null)),
                group("个人空间", false,
                        item("/profile", "个人中心", null),
                        item("/message", "消息中心", null))
        );
    }

    private static MenuGroup group(String name, boolean admin, MenuItem... items) {
        return new MenuGroup(name, admin, Arrays.asList(items));
    }

    private static MenuItem item(String path, String title, String permission) {
        return new MenuItem(path, title, permission);
    }
}