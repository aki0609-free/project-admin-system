package com.project.backend.features.system.excelbook.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.excelbook.enums.ExcelBookLayoutType;
import com.project.backend.features.system.excelbook.enums.ExcelBookGenerationUnit;
import com.project.backend.features.system.excelbook.enums.ExcelBookPrintOrientation;
import com.project.backend.features.system.excelbook.enums.ExcelBookSelectionMode;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "layout_type", nullable = false, length = 30)
    private ExcelBookLayoutType layoutType =
            ExcelBookLayoutType.REPEATING_ROW;

    /**
     * 帳票固有のセル配置実装をRegistryから解決するキー。
     *
     * <p>既存データはNULLを許容し、layoutType名へフォールバックする。</p>
     */
    @Column(name = "renderer_key", length = 100)
    private String rendererKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_mode", nullable = false, length = 20)
    private ExcelBookSelectionMode selectionMode =
            ExcelBookSelectionMode.NONE;

    /** 選択一覧を取得する許可済みデータソースコード。 */
    @Column(name = "selection_source_name", length = 100)
    private String selectionSourceName;

    @Column(name = "selection_value_column", length = 100)
    private String selectionValueColumn;

    /** 一覧へ表示する許可済み列をカンマ区切りで保持する。 */
    @Column(name = "selection_display_columns", length = 1000)
    private String selectionDisplayColumns;

    @Column(name = "allow_select_all", nullable = false)
    private Boolean allowSelectAll = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_unit", nullable = false, length = 30)
    private ExcelBookGenerationUnit generationUnit =
            ExcelBookGenerationUnit.ONE_FILE;

    @Column(name = "print_paper_size", nullable = false, length = 20)
    private String printPaperSize = "A4";

    @Enumerated(EnumType.STRING)
    @Column(name = "print_orientation", nullable = false, length = 20)
    private ExcelBookPrintOrientation printOrientation =
            ExcelBookPrintOrientation.PORTRAIT;

    @Column(name = "print_fit_to_one_page", nullable = false)
    private Boolean printFitToOnePage = false;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "template_sheet_name", nullable = false, length = 100)
    private String templateSheetName = "TEMPLATE";

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;

    @OneToMany(
            mappedBy = "master",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("orderNo ASC, id ASC")
    private List<ExcelBookVariableMapping> variableMappings =
            new ArrayList<>();

    public void addVariableMapping(ExcelBookVariableMapping mapping) {
        mapping.setMaster(this);
        variableMappings.add(mapping);
    }

    public void clearVariableMappings() {
        variableMappings.clear();
    }
}
