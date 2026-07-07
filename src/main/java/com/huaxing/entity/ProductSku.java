package com.huaxing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_sku")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product productId;

    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String size;

    @Column(length = 100, unique = true)
    private String barcode;

    @Column(name = "stock_qty")
    private Integer stockQty;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
