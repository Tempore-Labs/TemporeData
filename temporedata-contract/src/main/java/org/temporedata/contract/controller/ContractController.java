package org.temporedata.contract.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.contract.CompatibilityMode;
import org.temporedata.contract.entity.ContractEntity;
import org.temporedata.contract.service.ContractService;

import java.util.List;

/**
 * [ENT-P1] Data contract REST endpoints.
 */
@RestController("enterpriseContractController")
@RequestMapping("/api/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    public BaseResponse<ContractEntity> register(@RequestParam Long datasetId,
                                                 @RequestParam String yaml,
                                                 @RequestParam(required = false) String mode,
                                                 @RequestParam(required = false) String owner) {
        return BaseResponse.success(contractService.register(datasetId, yaml,
                CompatibilityMode.from(mode), owner));
    }

    @PostMapping("/{id}/activate")
    public BaseResponse<ContractEntity> activate(@PathVariable Long id) {
        return BaseResponse.success(contractService.activate(id));
    }

    /** Compare promised vs provided column signatures; breaks the contract if incompatible. */
    @PostMapping("/check")
    public BaseResponse<ContractEntity> check(@RequestParam Long datasetId,
                                              @RequestParam String promised,
                                              @RequestParam String provided) {
        return BaseResponse.success(contractService.check(datasetId, promised, provided));
    }

    @GetMapping("/dataset/{datasetId}")
    public BaseResponse<ContractEntity> byDataset(@PathVariable Long datasetId) {
        return contractService.getByDataset(datasetId)
                .map(BaseResponse::success)
                .orElseGet(() -> BaseResponse.error(404, "contract not found"));
    }

    @GetMapping("/list")
    public BaseResponse<List<ContractEntity>> list() {
        return BaseResponse.success(contractService.list());
    }
}