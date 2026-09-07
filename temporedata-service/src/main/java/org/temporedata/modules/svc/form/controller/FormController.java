package org.temporedata.modules.svc.form.controller;

import org.temporedata.modules.svc.form.entity.FormEntity;
import org.temporedata.modules.svc.form.entity.FormSubmissionEntity;
import org.temporedata.modules.svc.form.service.FormService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    @GetMapping("/list")
    public BaseResponse<List<FormEntity>> list() {
        return BaseResponse.success(formService.list());
    }

    @PostMapping("/create")
    public BaseResponse<FormEntity> create(@RequestBody FormEntity entity) {
        return BaseResponse.success(formService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<FormEntity> update(@PathVariable String id, @RequestBody FormEntity entity) {
        return BaseResponse.success(formService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        formService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping("/{id}/submissions")
    public BaseResponse<List<FormSubmissionEntity>> getSubmissions(@PathVariable String id) {
        return BaseResponse.success(formService.getSubmissions(id));
    }

    @PostMapping("/{id}/share-token")
    public BaseResponse<String> generateShareToken(@PathVariable String id) {
        return BaseResponse.success(formService.generateShareToken(id));
    }
}