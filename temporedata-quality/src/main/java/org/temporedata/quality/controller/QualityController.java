package org.temporedata.quality.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.quality.entity.QualityGateEntity;
import org.temporedata.quality.entity.QualityRuleEntity;
import org.temporedata.quality.service.QualityService;

import java.math.BigDecimal;
import java.util.List;

/**
 * [ENT-P0] Quality rule &amp; gate REST endpoints.
 * Distinct bean name to coexist with the legacy gov QualityController.
 */
@RestController("enterpriseQualityController")
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    @GetMapping("/rule/page")
    public BaseResponse<Page<QualityRuleEntity>> pageRules(Pageable pageable,
                                                           @RequestParam(required = false) Long datasetId) {
        return BaseResponse.success(qualityService.pageRules(datasetId, pageable));
    }

    @GetMapping("/rule/list")
    public BaseResponse<List<QualityRuleEntity>> listRules(@RequestParam(required = false) Long datasetId) {
        return BaseResponse.success(qualityService.listRules(datasetId));
    }

    @PostMapping("/rule")
    public BaseResponse<QualityRuleEntity> createRule(@RequestBody QualityRuleEntity rule) {
        return BaseResponse.success(qualityService.createRule(rule));
    }

    @PutMapping("/rule/{id}")
    public BaseResponse<QualityRuleEntity> updateRule(@PathVariable Long id, @RequestBody QualityRuleEntity rule) {
        return BaseResponse.success(qualityService.updateRule(id, rule));
    }

    @DeleteMapping("/rule/{id}")
    public BaseResponse<Void> deleteRule(@PathVariable Long id) {
        qualityService.deleteRule(id);
        return BaseResponse.success();
    }

    @GetMapping("/gate/list")
    public BaseResponse<List<QualityGateEntity>> listGates(@RequestParam(required = false) Long datasetId) {
        return BaseResponse.success(qualityService.listGates(datasetId));
    }

    @PostMapping("/gate")
    public BaseResponse<QualityGateEntity> createGate(@RequestBody QualityGateEntity gate) {
        return BaseResponse.success(qualityService.createGate(gate));
    }

    @PostMapping("/dataset/{datasetId}/assess")
    public BaseResponse<QualityGateEntity> assess(@PathVariable Long datasetId, @RequestParam BigDecimal score) {
        return BaseResponse.success(qualityService.assess(datasetId, score));
    }
}