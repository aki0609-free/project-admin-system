package com.project.backend.features.system.excelbook.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "excel_book_master",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "book_code"})
        }
)
@Getter
@Setter
public class ExcelBookMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_code", nullable = false, length = 100)
    private String bookCode;

    @Column(name = "book_name", nullable = false, length = 200)
    private String bookName;

    @Column(name = "template_file_path", nullable = false, length = 500)
    private String templateFilePath;

    @Column(name = "output_file_path", nullable = false, length = 500)
    private String outputFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ExcelBookSourceType sourceType = ExcelBookSourceType.SNAPSHOT;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "template_sheet_name", nullable = false, length = 100)
    private String templateSheetName = "TEMPLATE";

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;
}