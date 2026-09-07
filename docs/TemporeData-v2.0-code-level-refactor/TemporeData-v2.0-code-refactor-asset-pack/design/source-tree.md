# TemporeData 源码树（Java 后端 + Vue 前端）

完整的源码结构，供代码浏览 / 模块定位 / 文档对照使用。生成于仓库；随代码结构变化可重新生成。

## 约定
- **后端**：`temporedata-*/src/main/java/org/temporedata/**` 下的 `.java`，按 Maven 模块分节。
- **前端**：`temporedata-ui/src/**`（Vue 组件/视图/状态/API/路由/本地化等）。
- 已排除构建产物（`target/`、`node_modules/`、`dist/`）与本地工具目录（`.trae/`、`.git/` 等）。
- 目录在文件之前排序；同级按字母序。

## 后端模块总览（Spring Boot 多模块）

| 模块 | 职责 |
|---|---|
| `temporedata-api` | 对外契约 DTO + 数据源 SPI |
| `temporedata-common` | 通用（BaseResponse/PageRes/异常/常量） |
| `temporedata-security` | 认证授权/JWT/租户/Trace |
| `temporedata-service` | 业务服务域（各模块 Controller/Service/Repository/Entity） |
| `temporedata-support` | 缓存/锁/异常/上下文/加密 |
| `temporedata-datasource-plugin` | 数据源插件聚合父 |
| `temporedata-server` | 启动装配/迁移/静态 + application.yml |
| `temporedata-bom` | Maven BOM 依赖管理 |

---

## 一、后端 Java 源码树


### temporedata-api/src/main/java  (`.java` × 170)

```text
└── org
    └── temporedata
        └── api
            ├── asset
            │   ├── datacenter
            │   │   ├── DataCenterOverviewRes.java
            │   │   ├── DataCenterReq.java
            │   │   └── DataCenterRes.java
            │   ├── indicator
            │   │   ├── IndicatorLineageRes.java
            │   │   ├── IndicatorPageRes.java
            │   │   ├── IndicatorReq.java
            │   │   ├── IndicatorRes.java
            │   │   ├── IndicatorRunRes.java
            │   │   └── IndicatorStatsRes.java
            │   ├── mydata
            │   │   ├── MyDataGrantReq.java
            │   │   ├── MyDataReq.java
            │   │   └── MyDataRes.java
            │   ├── permapproval
            │   │   ├── PermApprovalReq.java
            │   │   └── PermApprovalRes.java
            │   └── tag
            │       ├── TagBindingReq.java
            │       ├── TagBindingRes.java
            │       ├── TagReq.java
            │       └── TagRes.java
            ├── datasource
            │   ├── annotation
            │   │   └── DatasourcePlugin.java
            │   ├── conn
            │   │   ├── DatasourceConnection.java
            │   │   └── PluginNotFoundException.java
            │   ├── context
            │   │   ├── DatasourcePluginContext.java
            │   │   └── PluginManager.java
            │   ├── param
            │   │   ├── DatasourceParamDTO.java
            │   │   └── GenericDatasourceParamDTO.java
            │   ├── sql
            │   │   ├── ColumnLineage.java
            │   │   ├── Dialect.java
            │   │   ├── SqlDialectContribution.java
            │   │   ├── SqlParser.java
            │   │   └── SqlParseResult.java
            │   ├── AbstractDatasourceProcessor.java
            │   ├── DatasourcePluginInfo.java
            │   ├── DatasourceProcessor.java
            │   └── DatasourceType.java
            ├── dev
            │   ├── approval
            │   │   ├── ApprovalReq.java
            │   │   └── ApprovalRes.java
            │   ├── func
            │   │   ├── FuncReq.java
            │   │   └── FuncRes.java
            │   ├── query
            │   │   ├── QueryLogRes.java
            │   │   ├── QueryReq.java
            │   │   └── QueryRes.java
            │   ├── schedule
            │   │   ├── BizDatePreviewItem.java
            │   │   ├── CalendarDaySaveItem.java
            │   │   ├── CalendarDayView.java
            │   │   ├── CalendarReq.java
            │   │   ├── CalendarRes.java
            │   │   ├── ScheduleReq.java
            │   │   ├── TaskDefineReq.java
            │   │   ├── TaskDefineRes.java
            │   │   ├── TaskInstanceRes.java
            │   │   └── TaskLogRes.java
            │   └── workflow
            │       ├── InstanceLogRes.java
            │       ├── RuntimeCommandRes.java
            │       ├── SqlParseRes.java
            │       ├── WorkflowEdgeReq.java
            │       ├── WorkflowEdgeRes.java
            │       ├── WorkflowExecuteRes.java
            │       ├── WorkflowInstanceRes.java
            │       ├── WorkflowNodeInstanceRes.java
            │       ├── WorkflowNodeReq.java
            │       ├── WorkflowNodeRes.java
            │       ├── WorkflowReq.java
            │       ├── WorkflowRes.java
            │       └── WorkflowRunRes.java
            ├── gov
            │   ├── audit
            │   │   └── AuditReportRes.java
            │   ├── catalog
            │   │   ├── CatalogTreeRes.java
            │   │   ├── ColumnMetadataRes.java
            │   │   ├── CommentUpdateReq.java
            │   │   ├── GovernanceUpdateReq.java
            │   │   ├── LineageRes.java
            │   │   └── TableMetadataRes.java
            │   ├── lineage
            │   │   ├── ColumnLineageRecord.java
            │   │   ├── CycleAnalysisRes.java
            │   │   ├── EntityLineage.java
            │   │   ├── EntityReference.java
            │   │   ├── HotNodeRes.java
            │   │   ├── LineageDetails.java
            │   │   ├── LineageEdge.java
            │   │   ├── LineagePathRes.java
            │   │   ├── SearchLineageResult.java
            │   │   └── Source.java
            │   ├── meta
            │   │   ├── ImpactAnalysisRes.java
            │   │   ├── LineageAnalysisRes.java
            │   │   ├── LineageGraphRes.java
            │   │   ├── MetaColumnRes.java
            │   │   ├── MetaLineageRes.java
            │   │   ├── MetaSyncReq.java
            │   │   ├── MetaTableRes.java
            │   │   └── SqlLineageRes.java
            │   ├── quality
            │   │   ├── QualityCheckRes.java
            │   │   ├── QualityRuleReq.java
            │   │   ├── QualityRuleRes.java
            │   │   └── QualityRunRes.java
            │   ├── security
            │   │   ├── AccessPolicy.java
            │   │   ├── DataCategoryReq.java
            │   │   ├── DataCategoryRes.java
            │   │   ├── DataLevelReq.java
            │   │   ├── DataLevelRes.java
            │   │   ├── GovernedResult.java
            │   │   ├── GovExecResult.java
            │   │   ├── MaskRuleReq.java
            │   │   └── MaskRuleRes.java
            │   └── sensitive
            │       ├── SensitiveDataReq.java
            │       └── SensitiveDataRes.java
            ├── integration
            │   ├── datasource
            │   │   ├── DatasourceReq.java
            │   │   └── DatasourceRes.java
            │   ├── file
            │   │   └── FileRes.java
            │   └── secret
            │       ├── SecretReq.java
            │       └── SecretRes.java
            ├── ops
            │   ├── alarm
            │   │   ├── AlarmConfigReq.java
            │   │   ├── AlarmConfigRes.java
            │   │   └── AlarmRecordRes.java
            │   ├── audit
            │   │   ├── AuditLogReq.java
            │   │   └── AuditLogRes.java
            │   ├── cluster
            │   │   ├── ClusterNodeReq.java
            │   │   ├── ClusterNodeRes.java
            │   │   ├── ClusterReq.java
            │   │   └── ClusterRes.java
            │   ├── dashboard
            │   │   └── DashboardRes.java
            │   ├── engine
            │   │   ├── FlinkJobReq.java
            │   │   ├── FlinkJobRes.java
            │   │   ├── SparkJobReq.java
            │   │   └── SparkJobRes.java
            │   ├── ingestion
            │   │   ├── IngestionTaskReq.java
            │   │   └── IngestionTaskRes.java
            │   ├── real
            │   │   ├── RealJobReq.java
            │   │   └── RealJobRes.java
            │   └── sync
            │       ├── SyncTaskReq.java
            │       └── SyncTaskRes.java
            ├── svc
            │   ├── apilog
            │   │   ├── ApiLogReq.java
            │   │   └── ApiLogRes.java
            │   ├── blacklist
            │   │   ├── BlacklistReq.java
            │   │   └── BlacklistRes.java
            │   ├── form
            │   │   ├── FormReq.java
            │   │   ├── FormRes.java
            │   │   ├── FormSubmissionReq.java
            │   │   └── FormSubmissionRes.java
            │   ├── report
            │   │   ├── ReportReq.java
            │   │   └── ReportRes.java
            │   └── service
            │       ├── DataApiReq.java
            │       ├── DataApiRes.java
            │       └── DataApiTestReq.java
            └── sys
                ├── account
                │   ├── ChangePasswordReq.java
                │   ├── ContactUpdateReq.java
                │   └── UpdateProfileReq.java
                ├── agent
                │   ├── AgentChatReq.java
                │   ├── AgentChatRes.java
                │   ├── AgentConfigReq.java
                │   ├── AgentConfigRes.java
                │   ├── AgentSessionRes.java
                │   └── AgentTestRes.java
                ├── auth
                │   ├── LoginReq.java
                │   ├── LoginRes.java
                │   └── UserInfoRes.java
                ├── menu
                │   ├── MenuGroup.java
                │   └── MenuItem.java
                ├── org
                │   └── OrgMemberRes.java
                ├── passwordless
                │   ├── PasswordlessConfigReq.java
                │   ├── PasswordlessConfigRes.java
                │   ├── PasswordlessSendReq.java
                │   └── PasswordlessVerifyReq.java
                ├── preference
                │   ├── PreferenceBatchReq.java
                │   ├── PreferenceReq.java
                │   └── PreferenceRes.java
                ├── role
                │   └── RoleMemberRes.java
                ├── tenant
                │   ├── TenantMemberReq.java
                │   ├── TenantMemberRes.java
                │   ├── TenantReq.java
                │   └── TenantRes.java
                └── user
                    └── UserRes.java
```

### temporedata-common/src/main/java  (`.java` × 5)

```text
└── org
    └── temporedata
        └── api
            └── base
                ├── constants
                │   └── HttpConstants.java
                ├── exceptions
                │   ├── BusinessException.java
                │   └── ZyException.java
                └── pojos
                    ├── BaseResponse.java
                    └── PageRes.java
```

### temporedata-security/src/main/java  (`.java` × 5)

```text
└── org
    └── temporedata
        └── security
            ├── context
            │   └── TenantContext.java
            ├── jwt
            │   ├── JwtAuthenticationFilter.java
            │   └── JwtTokenProvider.java
            ├── trace
            │   └── TraceIdFilter.java
            └── SecurityConfig.java
```

### temporedata-service/src/main/java  (`.java` × 367)

```text
└── org
    └── temporedata
        └── modules
            ├── asset
            │   ├── datacenter
            │   │   ├── controller
            │   │   │   └── DatacenterController.java
            │   │   ├── entity
            │   │   │   └── DatacenterEntity.java
            │   │   ├── repository
            │   │   │   └── DatacenterRepository.java
            │   │   └── service
            │   │       └── DatacenterService.java
            │   ├── indicator
            │   │   ├── controller
            │   │   │   └── IndicatorController.java
            │   │   ├── entity
            │   │   │   └── IndicatorEntity.java
            │   │   ├── repository
            │   │   │   └── IndicatorRepository.java
            │   │   └── service
            │   │       └── IndicatorService.java
            │   ├── mydata
            │   │   ├── controller
            │   │   │   └── MydataController.java
            │   │   ├── entity
            │   │   │   └── MydataEntity.java
            │   │   ├── repository
            │   │   │   └── MydataRepository.java
            │   │   └── service
            │   │       └── MydataService.java
            │   ├── permapproval
            │   │   ├── controller
            │   │   │   └── PermapprovalController.java
            │   │   ├── entity
            │   │   │   └── PermapprovalEntity.java
            │   │   ├── repository
            │   │   │   └── PermapprovalRepository.java
            │   │   └── service
            │   │       └── PermapprovalService.java
            │   └── tag
            │       ├── controller
            │       │   └── TagController.java
            │       ├── entity
            │       │   └── TagEntity.java
            │       ├── repository
            │       │   └── TagRepository.java
            │       └── service
            │           └── TagService.java
            ├── dev
            │   ├── approval
            │   │   ├── controller
            │   │   │   └── ApprovalController.java
            │   │   ├── engine
            │   │   │   ├── ApprovalEngineService.java
            │   │   │   ├── ApprovalStrategy.java
            │   │   │   ├── GenericApprovalStrategy.java
            │   │   │   └── ResourcePermApprovalStrategy.java
            │   │   ├── entity
            │   │   │   └── ApprovalEntity.java
            │   │   ├── repository
            │   │   │   └── ApprovalRepository.java
            │   │   └── service
            │   │       └── ApprovalService.java
            │   ├── dependency
            │   │   ├── controller
            │   │   │   └── DependencyController.java
            │   │   ├── entity
            │   │   │   └── DependencyEntity.java
            │   │   ├── repository
            │   │   │   └── DependencyRepository.java
            │   │   └── service
            │   │       └── DependencyService.java
            │   ├── func
            │   │   ├── controller
            │   │   │   └── FuncController.java
            │   │   ├── entity
            │   │   │   └── FuncEntity.java
            │   │   ├── repository
            │   │   │   └── FuncRepository.java
            │   │   └── service
            │   │       └── FuncService.java
            │   ├── globalvar
            │   │   ├── controller
            │   │   │   └── GlobalvarController.java
            │   │   ├── entity
            │   │   │   └── GlobalvarEntity.java
            │   │   ├── repository
            │   │   │   └── GlobalvarRepository.java
            │   │   └── service
            │   │       └── GlobalvarService.java
            │   ├── perm
            │   │   ├── controller
            │   │   │   └── PermissionController.java
            │   │   ├── entity
            │   │   │   ├── PermissionEntity.java
            │   │   │   └── ResourceEntity.java
            │   │   ├── repository
            │   │   │   ├── PermissionRepository.java
            │   │   │   └── ResourceRepository.java
            │   │   └── service
            │   │       └── PermissionService.java
            │   ├── query
            │   │   ├── controller
            │   │   │   └── QueryController.java
            │   │   ├── entity
            │   │   │   └── QueryEntity.java
            │   │   ├── repository
            │   │   │   └── QueryRepository.java
            │   │   └── service
            │   │       └── QueryService.java
            │   ├── resource
            │   │   ├── controller
            │   │   │   └── ResourceController.java
            │   │   ├── entity
            │   │   │   └── ResourceEntity.java
            │   │   ├── repository
            │   │   │   └── ResourceRepository.java
            │   │   └── service
            │   │       └── ResourceService.java
            │   ├── schedule
            │   │   ├── calendar
            │   │   │   ├── entity
            │   │   │   │   ├── BizDateQueueEntity.java
            │   │   │   │   ├── CalendarCutTimeEntity.java
            │   │   │   │   ├── CalendarDayEntity.java
            │   │   │   │   └── CalendarEntity.java
            │   │   │   ├── repository
            │   │   │   │   ├── BizDateQueueRepository.java
            │   │   │   │   ├── CalendarCutTimeRepository.java
            │   │   │   │   ├── CalendarDayRepository.java
            │   │   │   │   └── CalendarRepository.java
            │   │   │   ├── BizDateCalculator.java
            │   │   │   ├── CalendarController.java
            │   │   │   └── CalendarService.java
            │   │   ├── controller
            │   │   │   ├── ScheduleController.java
            │   │   │   └── TaskScheduleController.java
            │   │   ├── entity
            │   │   │   ├── ScheduleEntity.java
            │   │   │   ├── TaskDefineEntity.java
            │   │   │   ├── TaskInstanceEntity.java
            │   │   │   └── TaskLogEntity.java
            │   │   ├── repository
            │   │   │   ├── ScheduleRepository.java
            │   │   │   ├── TaskDefineRepository.java
            │   │   │   ├── TaskInstanceRepository.java
            │   │   │   └── TaskLogRepository.java
            │   │   ├── scheduler
            │   │   │   └── SchedulerCoordinator.java
            │   │   └── service
            │   │       ├── InProcessTaskDispatcher.java
            │   │       ├── ScheduleService.java
            │   │       ├── TaskDispatcher.java
            │   │       ├── TaskDispatchResult.java
            │   │       └── TaskScheduleService.java
            │   ├── work
            │   │   ├── controller
            │   │   │   └── WorkController.java
            │   │   ├── entity
            │   │   │   └── WorkEntity.java
            │   │   ├── repository
            │   │   │   └── WorkRepository.java
            │   │   └── service
            │   │       └── WorkService.java
            │   └── workflow
            │       ├── controller
            │       │   ├── WorkflowController.java
            │       │   ├── WorkflowImportController.java
            │       │   └── WorkflowLineageController.java
            │       ├── entity
            │       │   ├── WorkflowColumnLineageEntity.java
            │       │   ├── WorkflowEdgeEntity.java
            │       │   ├── WorkflowEntity.java
            │       │   ├── WorkflowInstanceEntity.java
            │       │   ├── WorkflowInstanceLogEntity.java
            │       │   ├── WorkflowLineageEntity.java
            │       │   ├── WorkflowNodeEntity.java
            │       │   ├── WorkflowNodeInstanceEntity.java
            │       │   └── WorkflowRunCommandEntity.java
            │       ├── repository
            │       │   ├── WorkflowColumnLineageRepository.java
            │       │   ├── WorkflowInstanceLogRepository.java
            │       │   ├── WorkflowInstanceRepository.java
            │       │   ├── WorkflowLineageRepository.java
            │       │   ├── WorkflowNodeInstanceRepository.java
            │       │   ├── WorkflowRepository.java
            │       │   └── WorkflowRunCommandRepository.java
            │       ├── runner
            │       │   ├── ConditionEvaluator.java
            │       │   ├── DruidSqlParser.java
            │       │   ├── HttpNodeExecutor.java
            │       │   ├── JdbcSupport.java
            │       │   ├── NodeExecutionContext.java
            │       │   ├── NodeExecutor.java
            │       │   ├── NodeExecutorRegistry.java
            │       │   ├── NodeRunResult.java
            │       │   ├── PlaceholderNodeExecutor.java
            │       │   ├── QualityNodeExecutor.java
            │       │   ├── RuntimeControlRegistry.java
            │       │   ├── SqlDialectRegistry.java
            │       │   ├── SqlFirewall.java
            │       │   ├── SqlLineageParser.java
            │       │   ├── SqlNodeExecutor.java
            │       │   ├── SqlParserRegistry.java
            │       │   └── WorkflowRunner.java
            │       └── service
            │           ├── WorkflowImportService.java
            │           ├── WorkflowLineageService.java
            │           ├── WorkflowRuntimeService.java
            │           └── WorkflowService.java
            ├── gov
            │   ├── catalog
            │   │   ├── controller
            │   │   │   └── CatalogController.java
            │   │   ├── entity
            │   │   │   └── CatalogEntity.java
            │   │   ├── repository
            │   │   │   └── CatalogRepository.java
            │   │   └── service
            │   │       └── CatalogService.java
            │   ├── lineage
            │   │   ├── assembler
            │   │   │   └── LineageAssembler.java
            │   │   ├── config
            │   │   │   └── LineageProperties.java
            │   │   ├── controller
            │   │   │   └── LineageController.java
            │   │   ├── entity
            │   │   │   └── LineageEntity.java
            │   │   ├── explorer
            │   │   │   ├── ExplorationStrategy.java
            │   │   │   ├── LineageGraphExplorer.java
            │   │   │   └── LineageStrategySelector.java
            │   │   ├── repository
            │   │   │   └── LineageRepository.java
            │   │   ├── security
            │   │   │   ├── LineageAuthHelper.java
            │   │   │   └── LineageDomainFilter.java
            │   │   ├── service
            │   │   │   ├── LineageExplorationService.java
            │   │   │   └── LineageService.java
            │   │   └── support
            │   │       ├── LineageGraphCache.java
            │   │       ├── LineageMapper.java
            │   │       └── LineageWriteGate.java
            │   ├── meta
            │   │   ├── controller
            │   │   │   └── MetaCollectController.java
            │   │   ├── entity
            │   │   │   ├── MetaChangeEntity.java
            │   │   │   ├── MetaColumnEntity.java
            │   │   │   ├── MetaSyncLogEntity.java
            │   │   │   └── MetaTableEntity.java
            │   │   ├── repository
            │   │   │   ├── MetaChangeRepository.java
            │   │   │   ├── MetaColumnRepository.java
            │   │   │   ├── MetaSyncLogRepository.java
            │   │   │   └── MetaTableRepository.java
            │   │   ├── service
            │   │   │   ├── CatalogAssetPostProcessor.java
            │   │   │   ├── ChangeLinkPostProcessor.java
            │   │   │   ├── DefaultMaskRulePostProcessor.java
            │   │   │   ├── LineagePostProcessor.java
            │   │   │   ├── MetaCollector.java
            │   │   │   ├── MetaCoordinator.java
            │   │   │   ├── MetaPipeline.java
            │   │   │   ├── MetaSyncScheduler.java
            │   │   │   └── SensitiveClassifierPostProcessor.java
            │   │   └── spi
            │   │       └── MetaPostProcessor.java
            │   ├── quality
            │   │   ├── controller
            │   │   │   └── QualityController.java
            │   │   ├── entity
            │   │   │   └── QualityEntity.java
            │   │   ├── repository
            │   │   │   └── QualityRepository.java
            │   │   └── service
            │   │       ├── QualityRuleEngine.java
            │   │       └── QualityService.java
            │   ├── security
            │   │   ├── controller
            │   │   │   ├── AutoGovernedQueryController.java
            │   │   │   ├── DataGovernorDemoController.java
            │   │   │   ├── GovernedExportController.java
            │   │   │   ├── GovernedQueryController.java
            │   │   │   └── SecurityController.java
            │   │   ├── entity
            │   │   │   ├── QaReportEntity.java
            │   │   │   ├── SecurityEntity.java
            │   │   │   └── VulnEntity.java
            │   │   ├── repository
            │   │   │   ├── QaReportRepository.java
            │   │   │   ├── SecurityRepository.java
            │   │   │   └── VulnRepository.java
            │   │   ├── service
            │   │   │   └── SecurityService.java
            │   │   ├── AccessPolicyResolver.java
            │   │   ├── DataAccessGovernor.java
            │   │   ├── DataDesensitizer.java
            │   │   ├── GovernanceAudit.java
            │   │   ├── GovernedSqlExecutor.java
            │   │   └── SqlPlanRewriter.java
            │   └── sensitive
            │       ├── controller
            │       │   └── SensitiveController.java
            │       ├── entity
            │       │   └── SensitiveEntity.java
            │       ├── repository
            │       │   └── SensitiveRepository.java
            │       └── service
            │           └── SensitiveService.java
            ├── integration
            │   ├── datasource
            │   │   ├── controller
            │   │   │   └── DatasourceController.java
            │   │   ├── driver
            │   │   │   ├── controller
            │   │   │   │   └── DriverPluginController.java
            │   │   │   ├── entity
            │   │   │   │   └── DriverPluginEntity.java
            │   │   │   ├── repository
            │   │   │   │   └── DriverPluginRepository.java
            │   │   │   └── service
            │   │   │       ├── DriverPluginReq.java
            │   │   │       ├── DriverPluginRes.java
            │   │   │       ├── DriverPluginService.java
            │   │   │       └── MavenDependencyResolver.java
            │   │   ├── entity
            │   │   │   └── DatasourceEntity.java
            │   │   ├── repository
            │   │   │   └── DatasourceRepository.java
            │   │   └── service
            │   │       └── DatasourceService.java
            │   ├── file
            │   │   ├── config
            │   │   │   └── FileProperties.java
            │   │   ├── controller
            │   │   │   └── FileController.java
            │   │   ├── entity
            │   │   │   └── FileEntity.java
            │   │   ├── repository
            │   │   │   └── FileRepository.java
            │   │   ├── service
            │   │   │   ├── FileCleanupTask.java
            │   │   │   └── FileService.java
            │   │   └── storage
            │   │       ├── CompressCodec.java
            │   │       ├── FileStorageProvider.java
            │   │       ├── FileStorageProviderConfig.java
            │   │       ├── LocalFileStorageProvider.java
            │   │       └── S3FileStorageProvider.java
            │   ├── secret
            │   │   ├── controller
            │   │   │   └── SecretController.java
            │   │   ├── entity
            │   │   │   └── SecretEntity.java
            │   │   ├── repository
            │   │   │   └── SecretRepository.java
            │   │   └── service
            │   │       └── SecretService.java
            │   └── security
            │       ├── controller
            │       │   └── MaskRuleController.java
            │       ├── entity
            │       │   └── MaskRuleEntity.java
            │       ├── repository
            │       │   └── MaskRuleRepository.java
            │       └── service
            │           └── MaskRuleService.java
            ├── ops
            │   ├── alarm
            │   │   ├── controller
            │   │   │   ├── AlarmBaselineController.java
            │   │   │   └── AlarmController.java
            │   │   ├── entity
            │   │   │   ├── AlarmBaselineEntity.java
            │   │   │   ├── AlarmEntity.java
            │   │   │   └── AlarmRecordEntity.java
            │   │   ├── repository
            │   │   │   ├── AlarmBaselineRepository.java
            │   │   │   ├── AlarmRecordRepository.java
            │   │   │   └── AlarmRepository.java
            │   │   ├── runner
            │   │   │   └── BaselineChecker.java
            │   │   └── service
            │   │       ├── AlarmBaselineService.java
            │   │       └── AlarmService.java
            │   ├── audit
            │   │   ├── controller
            │   │   │   └── AuditController.java
            │   │   ├── entity
            │   │   │   ├── AuditArchiveEntity.java
            │   │   │   ├── AuditEventEntity.java
            │   │   │   └── AuditPolicyEntity.java
            │   │   ├── repository
            │   │   │   ├── AuditArchiveRepository.java
            │   │   │   ├── AuditEventRepository.java
            │   │   │   └── AuditPolicyRepository.java
            │   │   └── service
            │   │       └── AuditService.java
            │   ├── cluster
            │   │   ├── controller
            │   │   │   └── ClusterController.java
            │   │   ├── entity
            │   │   │   └── ClusterEntity.java
            │   │   ├── repository
            │   │   │   └── ClusterRepository.java
            │   │   └── service
            │   │       └── ClusterService.java
            │   ├── container
            │   │   ├── controller
            │   │   │   └── ContainerController.java
            │   │   ├── entity
            │   │   │   └── ContainerEntity.java
            │   │   ├── repository
            │   │   │   └── ContainerRepository.java
            │   │   └── service
            │   │       └── ContainerService.java
            │   ├── dashboard
            │   │   ├── controller
            │   │   │   └── DashboardController.java
            │   │   ├── entity
            │   │   │   └── DashboardEntity.java
            │   │   ├── repository
            │   │   │   └── DashboardRepository.java
            │   │   └── service
            │   │       ├── DashboardOverviewService.java
            │   │       └── DashboardService.java
            │   ├── engine
            │   │   ├── controller
            │   │   │   └── EngineController.java
            │   │   ├── entity
            │   │   │   └── EngineEntity.java
            │   │   ├── repository
            │   │   │   └── EngineRepository.java
            │   │   └── service
            │   │       └── EngineService.java
            │   ├── git
            │   │   ├── controller
            │   │   │   └── OpsController.java
            │   │   ├── entity
            │   │   │   ├── OpsBuildEntity.java
            │   │   │   └── OpsRepoEntity.java
            │   │   ├── provider
            │   │   │   ├── GitHubOpsProvider.java
            │   │   │   ├── GitLabOpsProvider.java
            │   │   │   ├── OpsProvider.java
            │   │   │   └── OpsProviderRegistry.java
            │   │   ├── repository
            │   │   │   ├── OpsBuildRepository.java
            │   │   │   └── OpsRepoRepository.java
            │   │   ├── OpsException.java
            │   │   ├── OpsHttpClient.java
            │   │   └── OpsService.java
            │   ├── ha
            │   │   ├── controller
            │   │   │   └── HaController.java
            │   │   ├── entity
            │   │   │   └── HaEntity.java
            │   │   ├── repository
            │   │   │   └── HaRepository.java
            │   │   └── service
            │   │       └── HaService.java
            │   ├── ingestion
            │   │   ├── controller
            │   │   │   └── IngestionController.java
            │   │   ├── entity
            │   │   │   └── IngestionEntity.java
            │   │   ├── repository
            │   │   │   └── IngestionRepository.java
            │   │   └── service
            │   │       └── IngestionService.java
            │   ├── monitor
            │   │   ├── controller
            │   │   │   └── MonitorController.java
            │   │   ├── entity
            │   │   │   └── MonitorEntity.java
            │   │   ├── repository
            │   │   │   └── MonitorRepository.java
            │   │   └── service
            │   │       └── MonitorService.java
            │   ├── real
            │   │   ├── controller
            │   │   │   └── RealController.java
            │   │   ├── entity
            │   │   │   └── RealEntity.java
            │   │   ├── repository
            │   │   │   └── RealRepository.java
            │   │   └── service
            │   │       └── RealService.java
            │   └── sync
            │       ├── controller
            │       │   └── SyncController.java
            │       ├── entity
            │       │   └── SyncEntity.java
            │       ├── repository
            │       │   └── SyncRepository.java
            │       └── service
            │           └── SyncService.java
            ├── svc
            │   ├── apilog
            │   │   ├── controller
            │   │   │   └── ApilogController.java
            │   │   ├── entity
            │   │   │   └── ApilogEntity.java
            │   │   ├── repository
            │   │   │   └── ApilogRepository.java
            │   │   └── service
            │   │       └── ApilogService.java
            │   ├── blacklist
            │   │   ├── controller
            │   │   │   └── BlacklistController.java
            │   │   ├── entity
            │   │   │   └── BlacklistEntity.java
            │   │   ├── repository
            │   │   │   └── BlacklistRepository.java
            │   │   └── service
            │   │       └── BlacklistService.java
            │   ├── form
            │   │   ├── controller
            │   │   │   └── FormController.java
            │   │   ├── entity
            │   │   │   ├── FormEntity.java
            │   │   │   └── FormSubmissionEntity.java
            │   │   ├── repository
            │   │   │   ├── FormRepository.java
            │   │   │   └── FormSubmissionRepository.java
            │   │   └── service
            │   │       └── FormService.java
            │   ├── report
            │   │   ├── controller
            │   │   │   └── ReportController.java
            │   │   ├── entity
            │   │   │   └── ReportEntity.java
            │   │   ├── repository
            │   │   │   └── ReportRepository.java
            │   │   └── service
            │   │       └── ReportService.java
            │   └── service
            │       ├── controller
            │       │   ├── DataApiController.java
            │       │   └── ServiceController.java
            │       ├── entity
            │       │   ├── DataApiEntity.java
            │       │   └── ServiceEntity.java
            │       ├── repository
            │       │   ├── DataApiRepository.java
            │       │   └── ServiceRepository.java
            │       └── service
            │           ├── DataApiService.java
            │           └── ServiceService.java
            └── sys
                ├── agent
                │   ├── controller
                │   │   └── AgentController.java
                │   ├── entity
                │   │   └── AgentEntity.java
                │   ├── repository
                │   │   └── AgentRepository.java
                │   └── service
                │       └── AgentService.java
                ├── auth
                │   ├── controller
                │   │   └── AuthController.java
                │   ├── entity
                │   │   └── AuthEntity.java
                │   ├── repository
                │   │   └── AuthRepository.java
                │   └── service
                │       └── AuthService.java
                ├── menu
                │   └── MenuRegistry.java
                ├── message
                │   ├── controller
                │   │   └── MessageController.java
                │   ├── entity
                │   │   └── MessageEntity.java
                │   ├── repository
                │   │   └── MessageRepository.java
                │   └── service
                │       ├── MessageCenterService.java
                │       └── MessageService.java
                ├── notify
                │   ├── controller
                │   │   └── NotifyController.java
                │   ├── entity
                │   │   └── NotifyEntity.java
                │   ├── repository
                │   │   └── NotifyRepository.java
                │   └── service
                │       └── NotifyService.java
                ├── org
                │   ├── controller
                │   │   └── OrgController.java
                │   ├── entity
                │   │   └── OrgEntity.java
                │   ├── repository
                │   │   └── OrgRepository.java
                │   └── service
                │       └── OrgService.java
                ├── passwordless
                │   ├── controller
                │   │   └── PasswordlessController.java
                │   ├── entity
                │   │   └── PasswordlessEntity.java
                │   ├── repository
                │   │   └── PasswordlessRepository.java
                │   └── service
                │       └── PasswordlessService.java
                ├── preference
                │   ├── controller
                │   │   └── PreferenceController.java
                │   ├── entity
                │   │   └── PreferenceEntity.java
                │   ├── repository
                │   │   └── PreferenceRepository.java
                │   └── service
                │       └── PreferenceService.java
                ├── role
                │   ├── controller
                │   │   └── RoleController.java
                │   ├── entity
                │   │   ├── RoleEntity.java
                │   │   └── UserRoleEntity.java
                │   ├── repository
                │   │   ├── RoleRepository.java
                │   │   └── UserRoleRepository.java
                │   └── service
                │       └── RoleService.java
                ├── settings
                │   ├── controller
                │   │   └── SettingsController.java
                │   ├── entity
                │   │   └── SettingsEntity.java
                │   ├── repository
                │   │   └── SettingsRepository.java
                │   └── service
                │       └── SettingsService.java
                ├── tenant
                │   ├── controller
                │   │   └── TenantController.java
                │   ├── entity
                │   │   └── TenantEntity.java
                │   ├── repository
                │   │   └── TenantRepository.java
                │   └── service
                │       └── TenantService.java
                ├── user
                │   ├── controller
                │   │   └── UserController.java
                │   ├── entity
                │   │   └── UserEntity.java
                │   ├── repository
                │   │   └── UserRepository.java
                │   └── service
                │       ├── UserDetailsServiceImpl.java
                │       └── UserService.java
                └── view
                    ├── controller
                    │   └── ViewController.java
                    ├── entity
                    │   └── ViewEntity.java
                    ├── repository
                    │   └── ViewRepository.java
                    └── service
                        └── ViewService.java
```

### temporedata-support/src/main/java  (`.java` × 9)

```text
└── org
    └── temporedata
        └── common
            ├── cache
            │   ├── CacheConfig.java
            │   └── CacheManageController.java
            ├── context
            │   └── TraceContext.java
            ├── exception
            │   └── GlobalExceptionHandler.java
            ├── locker
            │   ├── DistributedLock.java
            │   ├── DistributedLockProvider.java
            │   └── RedisDistributedLock.java
            └── util
                ├── Crypto.java
                └── CryptoUtil.java
```

### `temporedata-datasource-plugin`

（无 `src/main/java`）


### temporedata-server/src/main/java  (`.java` × 7)

```text
└── org
    └── temporedata
        └── app
            ├── config
            │   ├── AsyncConfig.java
            │   ├── FlywayConfig.java
            │   ├── HttpClientConfig.java
            │   ├── SwaggerConfig.java
            │   └── WebMvcConfig.java
            ├── controller
            │   └── SpaForwardController.java
            └── TemporeDataApplication.java
```

### `temporedata-bom`

（无 `src/main/java`）


---

## 二、前端 Vue/JS 源码树  (`temporedata-ui/src`)

文件数：`156`

```text
├── api
│   ├── modules
│   │   ├── agent.js
│   │   ├── alarm.js
│   │   ├── apilog.js
│   │   ├── approval.js
│   │   ├── audit.js
│   │   ├── auth.js
│   │   ├── blacklist.js
│   │   ├── calendar.js
│   │   ├── catalog.js
│   │   ├── cluster.js
│   │   ├── container.js
│   │   ├── dashboard.js
│   │   ├── dataapi.js
│   │   ├── datacenter.js
│   │   ├── datasource.js
│   │   ├── datasourcePlugin.js
│   │   ├── dependency.js
│   │   ├── engine.js
│   │   ├── file.js
│   │   ├── form.js
│   │   ├── func.js
│   │   ├── globalvar.js
│   │   ├── ha.js
│   │   ├── indicator.js
│   │   ├── ingestion.js
│   │   ├── lineage.js
│   │   ├── message.js
│   │   ├── meta.js
│   │   ├── monitor.js
│   │   ├── mydata.js
│   │   ├── notify.js
│   │   ├── ops.js
│   │   ├── org.js
│   │   ├── passwordless.js
│   │   ├── perm.js
│   │   ├── permapproval.js
│   │   ├── preference.js
│   │   ├── quality.js
│   │   ├── query.js
│   │   ├── realtime.js
│   │   ├── report.js
│   │   ├── resource.js
│   │   ├── role.js
│   │   ├── scheduler.js
│   │   ├── secret.js
│   │   ├── security.js
│   │   ├── sensitive.js
│   │   ├── settings.js
│   │   ├── sync.js
│   │   ├── tag.js
│   │   ├── tenant.js
│   │   ├── user.js
│   │   ├── view.js
│   │   ├── work.js
│   │   ├── workflow.js
│   │   ├── workflowImport.js
│   │   └── workflowLineage.js
│   └── index.js
├── components
│   ├── lineage
│   │   ├── vueflow
│   │   │   └── sourceLogoRegistry.js
│   │   ├── LgAssetTree.vue
│   │   ├── LgColumnList.vue
│   │   ├── LgCopyFqn.vue
│   │   ├── LgDialogActionBar.vue
│   │   ├── LgDqBadge.vue
│   │   ├── LgEdgeFormFields.vue
│   │   ├── LgEmptyState.vue
│   │   ├── LgLoadingSkeleton.vue
│   │   ├── LgNeighborCard.vue
│   │   ├── LgStatusTab.vue
│   │   └── LineageGraph.vue
│   ├── AssetTagSelect.vue
│   ├── Breadcrumb.vue
│   ├── DagNode.vue
│   ├── EmptyState.vue
│   ├── LineageGraph.vue
│   ├── NodeConfigPanel.vue
│   ├── PageHeader.vue
│   ├── SearchTable.vue
│   ├── StatusTag.vue
│   └── WorkflowDagEditor.vue
├── composables
│   ├── useCRUD.js
│   ├── useDialog.js
│   ├── useLineageEdgeFormSchema.js
│   ├── useLineageKeyboard.js
│   └── useTable.js
├── config
│   └── menu.config.js
├── lib
│   └── permission.js
├── locales
│   ├── lineage.en-US.js
│   └── lineage.zh-CN.js
├── router
│   └── index.js
├── stores
│   ├── auth.js
│   └── lineage.js
├── styles
│   └── global.css
├── views
│   ├── Agent.vue
│   ├── Alarm.vue
│   ├── ApiLog.vue
│   ├── Approval.vue
│   ├── AuditCenter.vue
│   ├── AuditLog.vue
│   ├── Baseline.vue
│   ├── Blacklist.vue
│   ├── Calendar.vue
│   ├── ChangeAudit.vue
│   ├── Cluster.vue
│   ├── Container.vue
│   ├── Dashboard.vue
│   ├── DataApi.vue
│   ├── DataAsset.vue
│   ├── DataCenter.vue
│   ├── DataLevel.vue
│   ├── DataMask.vue
│   ├── Datasource.vue
│   ├── Dependency.vue
│   ├── Engine.vue
│   ├── Form.vue
│   ├── FuncRepo.vue
│   ├── GlobalVar.vue
│   ├── Ha.vue
│   ├── Indicator.vue
│   ├── Ingestion.vue
│   ├── Layout.vue
│   ├── Lineage.vue
│   ├── LineageEntry.vue
│   ├── LineageV2.vue
│   ├── Login.vue
│   ├── Message.vue
│   ├── Meta.vue
│   ├── Monitor.vue
│   ├── MyData.vue
│   ├── Notify.vue
│   ├── Ops.vue
│   ├── Org.vue
│   ├── Passwordless.vue
│   ├── PermApproval.vue
│   ├── PermissionCenter.vue
│   ├── Preference.vue
│   ├── Profile.vue
│   ├── Quality.vue
│   ├── Realtime.vue
│   ├── Report.vue
│   ├── ResourceCenter.vue
│   ├── Role.vue
│   ├── Scheduler.vue
│   ├── Secret.vue
│   ├── SecurityGov.vue
│   ├── Sensitive.vue
│   ├── Settings.vue
│   ├── SqlEditor.vue
│   ├── SyncTask.vue
│   ├── Tag.vue
│   ├── Tenant.vue
│   ├── ViewPage.vue
│   ├── Work.vue
│   └── Workflow.vue
├── App.vue
└── main.js
```

---

*文档由 `scripts/` 侧生成维护；如源码有变动，重新运行生成器即可。*