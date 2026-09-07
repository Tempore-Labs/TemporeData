# 17 Vue Component Blueprint

## Global

```text
AppShell
TopHeader
SideNavigation
CommandPalette
NotificationCenter
GlobalSearch
ResourceDrawer
ConfirmActionDialog
PermissionGate
```

## Dashboard

```text
HealthOverview
ResourceSummary
IncidentSummary
JobSummary
RecentEvents
ClusterHealthGrid
```

## Cluster

```text
ClusterTable
ClusterStatus
NodeTable
ServiceTable
ServiceHealth
ClusterTopology
ClusterConfigDrawer
```

## Job

```text
JobTable
JobFilterBar
JobStatus
JobDefinitionDrawer
JobExecutionTable
ExecutionTimeline
ExecutionLogViewer
ExecutionActionBar
```

## Incident

```text
IncidentTable
IncidentSeverity
IncidentTimeline
IncidentEvidence
IncidentRcaPanel
IncidentActionPanel
```

## Automation

```text
RunbookTable
RunbookEditor
ActionPlanPreview
ApprovalPanel
ExecutionTimeline
AuditTrail
```

## Data

保留成熟：

```text
MetadataExplorer
LineageGraph
LineagePath
ColumnLineage
DatasourceTable
```

Lineage 图继续使用 G6，不进行不必要重写。
