package com.project.backend.features.system.rule.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rule_data_source")
@Getter
@Setter
public class RuleDataSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleMaster rule;

    @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Column(name = "table_name", nullable = false, length = 200)
    private String tableName;

    /**
     * 例:
     * employee_id = :employeeId
     * target_date = :targetDate
     */
    @Column(name = "where_clause", length = 1000)
    private String whereClause;

    @Column(name = "single_row_flag", nullable = false)
    private boolean singleRowFlag = true;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @Column(name = "order_no", nullable = false)
    private int orderNo = 1;

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleColumnMapping> columns = new ArrayList<>();

    public void addColumn(RuleColumnMapping column) {
        column.setDataSource(this);
        this.columns.add(column);
    }

    public void clearColumns() {
        this.columns.clear();
    }
}