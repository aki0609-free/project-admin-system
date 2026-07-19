package com.project.backend.features.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DormitoryInfo {

    /** 入寮有無 */
    @Column(name = "needs_dormitory")
    private String needsDormitory;

    /** 部屋タイプ */
    @Column(name = "room_type")
    private String roomType;

    /** 寮費 */
    @Column(name = "rent")
    private String rent;
}