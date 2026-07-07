package com.huaxing.entity;

import com.huaxing.enums.PayMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sys_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 30)
    private String orderNo;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal discount;

    @Column(name = "pay_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal payAmount;

    @Column(name = "receive_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal receiveAmount;

    @Column(name = "change_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_method", length = 20, nullable = false)
    private PayMethod payMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SysUser cashier;

    @Column(name = "member_id")
    private Long memberId;

    private LocalDateTime createTime;

    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> items;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
