package com.huaxing.entity;

import com.huaxing.enums.StockType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductSku sku;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "sku_spec", nullable = false, length = 100)
    private String skuSpec;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StockType type;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "before_qty", nullable = false)
    private Integer beforeQty;

    @Column(name = "after_qty", nullable = false)
    private Integer afterQty;

    @Column(name = "operator_id")
    private Long operatorId;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
