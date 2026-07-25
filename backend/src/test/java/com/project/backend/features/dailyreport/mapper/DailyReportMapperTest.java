package com.project.backend.features.dailyreport.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.employee.enums.ApprovalStatus;

class DailyReportMapperTest {

    private final DailyReportMapper mapper =
            Mappers.getMapper(DailyReportMapper.class);

    @Test
    void saveRequestの承認情報にかかわらずV1は承認済みで保存する() {
        DailyReportSaveRequest request =
                mock(DailyReportSaveRequest.class);
        when(request.approvalStatus())
                .thenReturn(ApprovalStatus.REJECTED);
        when(request.approvalComment())
                .thenReturn("クライアントから指定されたコメント");

        DailyReport entity = new DailyReport();
        entity.setApprovalStatus(ApprovalStatus.PENDING);
        entity.setApprovalComment("既存コメント");

        mapper.updateEntityFromRequest(request, entity);

        assertThat(entity.getApprovalStatus())
                .isEqualTo(ApprovalStatus.APPROVED);
        assertThat(entity.getApprovalComment()).isNull();
    }
}
