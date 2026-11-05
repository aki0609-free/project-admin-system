package com.project.backend.features.operation.monthly.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.admin.business.dto.YearEndAdjustmentResponse;
import com.project.backend.features.admin.business.dto.YearEndAdjustmentSaveRequest;
import com.project.backend.features.admin.business.service.YearEndAdjustmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 月次締めに反映する年末調整を、締め処理の業務導線から管理するAPI。
 *
 * <p>保存・月次反映の既存ロジックは {@link YearEndAdjustmentService} に集約したまま、
 * 管理画面の配置だけを業務管理から締め処理へ移している。</p>
 */
@RestController
@RequestMapping("/api/operation/year-end-adjustments")
@RequiredArgsConstructor
public class YearEndAdjustmentOperationController {

    private final YearEndAdjustmentService yearEndAdjustmentService;

    @GetMapping("/{taxYear}")
    public List<YearEndAdjustmentResponse> findByYear(@PathVariable int taxYear) {
        return yearEndAdjustmentService.findByYear(taxYear);
    }

    @PostMapping
    public YearEndAdjustmentResponse save(
            @Valid @RequestBody YearEndAdjustmentSaveRequest request
    ) {
        return yearEndAdjustmentService.save(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        yearEndAdjustmentService.delete(id);
    }
}
