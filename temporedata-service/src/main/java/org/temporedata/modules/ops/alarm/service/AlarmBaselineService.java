package org.temporedata.modules.ops.alarm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.dev.workflow.entity.WorkflowInstanceEntity;
import org.temporedata.modules.dev.workflow.repository.WorkflowInstanceRepository;
import org.temporedata.modules.ops.alarm.entity.AlarmBaselineEntity;
import org.temporedata.modules.ops.alarm.entity.AlarmRecordEntity;
import org.temporedata.modules.ops.alarm.repository.AlarmBaselineRepository;
import org.temporedata.modules.ops.alarm.repository.AlarmRecordRepository;
import org.temporedata.modules.sys.message.service.MessageCenterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * P2-8 absolute-time workflow baseline alarms. The checker compares each
 * workflow's latest instance against an expected finish time and produces
 * MISS / DELAY / FAIL records, suppressing duplicates and closing on recovery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmBaselineService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmBaselineRepository baselineRepository;
    private final AlarmRecordRepository recordRepository;
    private final WorkflowInstanceRepository wfInstanceRepository;
    private final MessageCenterService messageCenterService;

    // ---- Baseline CRUD ----

    @Transactional(readOnly = true)
    public List<AlarmBaselineEntity> list() {
        return baselineRepository.findAll();
    }

    @Transactional
    public AlarmBaselineEntity create(AlarmBaselineEntity b) {
        if (b.getName() == null || b.getName().isBlank()) throw new BusinessException("基线名必填");
        if (b.getWorkflowId() == null || b.getWorkflowId().isBlank()) throw new BusinessException("请选择工作流");
        if (b.getExpectTime() == null || b.getExpectTime().isBlank()) throw new BusinessException("期望完成时间必填");
        b.setId(null);
        b.setEnabled(b.getEnabled() == null ? Boolean.TRUE : b.getEnabled());
        b.setCreateTime(now());
        return baselineRepository.save(b);
    }

    @Transactional
    public AlarmBaselineEntity update(String id, AlarmBaselineEntity b) {
        AlarmBaselineEntity db = find(id);
        if (b.getName() != null) db.setName(b.getName());
        if (b.getWorkflowId() != null) db.setWorkflowId(b.getWorkflowId());
        if (b.getCalendarId() != null) db.setCalendarId(b.getCalendarId());
        if (b.getBizDateMode() != null) db.setBizDateMode(b.getBizDateMode());
        if (b.getTimezone() != null) db.setTimezone(b.getTimezone());
        if (b.getExpectTime() != null) db.setExpectTime(b.getExpectTime());
        if (b.getTriggerType() != null) db.setTriggerType(b.getTriggerType());
        if (b.getGraceSeconds() != null) db.setGraceSeconds(b.getGraceSeconds());
        if (b.getNotifyChannels() != null) db.setNotifyChannels(b.getNotifyChannels());
        if (b.getRemark() != null) db.setRemark(b.getRemark());
        return baselineRepository.save(db);
    }

    @Transactional
    public void delete(String id) {
        baselineRepository.deleteById(id);
    }

    @Transactional
    public AlarmBaselineEntity toggle(String id, boolean enabled) {
        AlarmBaselineEntity db = find(id);
        db.setEnabled(enabled);
        return baselineRepository.save(db);
    }

    // ---- Records ----

    @Transactional(readOnly = true)
    public List<AlarmRecordEntity> records(String baselineId, String status) {
        if (baselineId != null && !baselineId.isBlank()) {
            return recordRepository.findByBaselineIdOrderByCreateTimeDesc(baselineId);
        }
        List<AlarmRecordEntity> all = recordRepository.findAllByOrderByCreateTimeDesc();
        return status == null || status.isBlank() ? all
                : all.stream().filter(r -> status.equalsIgnoreCase(r.getStatus())).collect(Collectors.toList());
    }

    @Transactional
    public AlarmRecordEntity ack(String id) {
        AlarmRecordEntity r = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警记录不存在: " + id));
        r.setStatus("ACK");
        r.setAckBy("admin");
        r.setAckTime(now());
        return recordRepository.save(r);
    }

    @Transactional
    public AlarmRecordEntity close(String id) {
        AlarmRecordEntity r = recordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警记录不存在: " + id));
        r.setStatus("CLOSED");
        r.setCloseTime(now());
        return recordRepository.save(r);
    }

    @Transactional
    public String test(String id) {
        find(id);
        return "测试告警已送达（当前为占位发送）";
    }

    /**
     * Run the baseline evaluation loop for all enabled baselines.
     */
    @Transactional
    public Map<String, Object> checkBaselines() {
        int miss = 0, delay = 0, fail = 0, recovery = 0;
        for (AlarmBaselineEntity b : baselineRepository.findByEnabledTrue()) {
            BizEval eval = evaluate(b);
            if (eval == null) continue;
            switch (eval.kind) {
                case "MISS":
                    if (raise(b, eval, "MISS")) miss++;
                    break;
                case "DELAY":
                    if (raise(b, eval, "DELAY")) delay++;
                    break;
                case "FAIL":
                    if (raise(b, eval, "FAIL")) fail++;
                    break;
                case "RECOVERY":
                    if (recover(b, eval)) recovery++;
                    break;
                default:
                    break;
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("miss", miss);
        res.put("delay", delay);
        res.put("fail", fail);
        res.put("recovery", recovery);
        return res;
    }

    // ---- Evaluation ----

    private BizEval evaluate(AlarmBaselineEntity b) {
        ZoneId zone = toZone(b.getTimezone());
        LocalDate today = LocalDate.now(zone);
        String bizDate = baselineBizDate(b, today);

        Optional<WorkflowInstanceEntity> byBiz = wfInstanceRepository
                .findTopByWorkflowIdAndBizDateOrderByStartTimeDesc(b.getWorkflowId(), bizDate);
        Optional<WorkflowInstanceEntity> inst = byBiz.isPresent()
                ? byBiz : wfInstanceRepository.findFirstByWorkflowIdOrderByStartTimeDesc(b.getWorkflowId());

        LocalTime expect = parseTime(b.getExpectTime());
        int grace = b.getGraceSeconds() == null ? 300 : b.getGraceSeconds();
        LocalDateTime deadline = today.atTime(expect).plusSeconds(grace);
        LocalDateTime now = LocalDateTime.now(zone);

        BizEval eval = new BizEval();
        eval.bizDate = bizDate;
        eval.deadline = deadline.format(DTF);

        if (inst.isEmpty()) {
            if (now.isAfter(deadline) || now.isEqual(deadline)) {
                eval.kind = "MISS";
                eval.content = "到点未发现工作流实例";
            } else {
                return null;
            }
        } else {
            WorkflowInstanceEntity w = inst.get();
            eval.wfInstanceId = w.getId();
            String status = w.getStatus();
            if ("RUNNING".equals(status) || "PENDING".equals(status) || "STOPPED".equals(status)) {
                if (now.isAfter(deadline) || now.isEqual(deadline)) {
                    eval.kind = "DELAY";
                    eval.content = "实例仍在运行，超期未完成";
                } else {
                    return null;
                }
            } else if ("SUCCESS".equals(status)) {
                boolean late = w.getFinishTime() != null
                        && w.getFinishTime().isAfter(deadline);
                if (late) {
                    eval.kind = "DELAY";
                    eval.content = "实例完成超时";
                    eval.actualFinishTime = w.getFinishTime().format(DTF);
                } else {
                    eval.kind = "RECOVERY";
                    eval.actualFinishTime = w.getFinishTime() == null ? now() : w.getFinishTime().format(DTF);
                }
            } else { // FAILED or others
                eval.kind = "FAIL";
                eval.content = "实例执行失败";
                if (w.getFinishTime() != null) eval.actualFinishTime = w.getFinishTime().format(DTF);
            }
        }
        return eval;
    }

    private boolean raise(AlarmBaselineEntity b, BizEval eval, String type) {
        if (recordRepository.existsByBaselineIdAndBizDateAndAlarmTypeAndStatusNot(
                b.getId(), eval.bizDate, type, "CLOSED")) {
            return false; // already alerted (suppress storm)
        }
        AlarmRecordEntity r = AlarmRecordEntity.builder()
                .baselineId(b.getId())
                .wfInstanceId(eval.wfInstanceId)
                .bizDate(eval.bizDate)
                .alarmType(type)
                .expectFinishTime(eval.deadline)
                .actualFinishTime(eval.actualFinishTime)
                .content(buildContent(b, type, eval))
                .status("SENT")
                .createTime(now())
                .build();
        recordRepository.save(r);
        log.warn("[基线告警] {} workflow={} bizDate={} -> {}", type, b.getWorkflowId(), eval.bizDate, r.getContent());
        // Dispatch through the unified message center.
        messageCenterService.send("ALARM", "基线告警-" + type, r.getContent(),
                b.getOperator(), null);
        return true;
    }

    private boolean recover(AlarmBaselineEntity b, BizEval eval) {
        List<AlarmRecordEntity> open = recordRepository
                .findByBaselineIdAndBizDateAndStatusNot(b.getId(), eval.bizDate, "CLOSED");
        if (open.isEmpty()) return false;
        for (AlarmRecordEntity r : open) {
            r.setStatus("CLOSED");
            r.setCloseTime(now());
            recordRepository.save(r);
        }
        return true;
    }

    private String buildContent(AlarmBaselineEntity b, String type, BizEval eval) {
        return "基线「" + b.getName() + "」业务日期 " + eval.bizDate + "：" + type + " - " + eval.content;
    }

    private String baselineBizDate(AlarmBaselineEntity b, LocalDate today) {
        String mode = b.getBizDateMode() == null ? "DAY" : b.getBizDateMode();
        switch (mode.toUpperCase()) {
            case "WEEK":
                return today.with(DayOfWeek.MONDAY).toString();
            case "MONTH":
                return today.withDayOfMonth(1).toString();
            case "DAY":
            default:
                return today.toString();
        }
    }

    private LocalTime parseTime(String t) {
        try {
            return LocalTime.parse(t);
        } catch (Exception e) {
            throw new BusinessException("时间格式错误: " + t);
        }
    }

    private ZoneId toZone(String tz) {
        try {
            return tz == null || tz.isBlank() ? ZoneId.systemDefault() : ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    private AlarmBaselineEntity find(String id) {
        return baselineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("基线不存在: " + id));
    }

    private String now() {
        return LocalDateTime.now().format(DTF);
    }

    // ---- Evaluation value object ----

    private static final class BizEval {
        String kind;
        String bizDate;
        String deadline;
        String wfInstanceId;
        String actualFinishTime;
        String content;
    }
}