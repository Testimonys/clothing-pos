package com.huaxing.repository;

import com.huaxing.entity.Order;
import com.huaxing.enums.PayMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE "
            + "(:startTime IS NULL OR o.createTime >= :startTime) "
            + "AND (:endTime IS NULL OR o.createTime <= :endTime) "
            + "AND (:payMethod IS NULL OR o.payMethod = :payMethod)")
    Page<Order> search(@Param("startTime") LocalDateTime startTime,
                       @Param("endTime") LocalDateTime endTime,
                       @Param("payMethod") PayMethod payMethod,
                       Pageable pageable);

    @Query("SELECT MAX(o.id) FROM Order o WHERE o.createTime >= :todayStart")
    Long maxIdToday(@Param("todayStart") LocalDateTime todayStart);
}
