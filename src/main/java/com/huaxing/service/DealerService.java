package com.huaxing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huaxing.entity.Dealer;
import com.huaxing.mapper.DealerMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** luohuai codeX generate: centralize immutable dealer codes and safe lifecycle rules. */
@Service
public class DealerService {
    private final DealerMapper dealers;

    public DealerService(DealerMapper dealers) {
        this.dealers = dealers;
    }

    public List<Dealer> list(boolean enabledOnly) {
        LambdaQueryWrapper<Dealer> query = new LambdaQueryWrapper<Dealer>().orderByAsc(Dealer::getCode);
        if (enabledOnly) query.eq(Dealer::getEnabled, true);
        return dealers.selectList(query);
    }

    @Transactional
    public synchronized Dealer create(Dealer request) {
        String name = required(request == null ? null : request.getName(), "经销商名称");
        if (dealers.selectCount(new LambdaQueryWrapper<Dealer>().eq(Dealer::getName, name)) > 0) throw conflict("经销商名称已存在");
        String max = dealers.findMaxCode();
        int next = max == null ? 100 : Integer.parseInt(max) + 1;
        if (next > 999) throw invalid("经销商编码已用完（100-999）");
        Dealer dealer = Dealer.builder().code(String.format("%03d", next)).name(name)
                .contactName(text(request.getContactName())).phone(text(request.getPhone()))
                .address(text(request.getAddress())).remark(text(request.getRemark())).enabled(true).build();
        dealers.insert(dealer);
        return dealer;
    }

    @Transactional
    public Dealer update(Long id, Dealer request) {
        Dealer dealer = require(id);
        String name = required(request == null ? null : request.getName(), "经销商名称");
        if (dealers.selectCount(new LambdaQueryWrapper<Dealer>().eq(Dealer::getName, name).ne(Dealer::getId, id)) > 0) throw conflict("经销商名称已存在");
        // luohuai codeX  modify: never copy request.code because barcode identities must remain immutable.
        dealer.setName(name);
        dealer.setContactName(text(request.getContactName()));
        dealer.setPhone(text(request.getPhone()));
        dealer.setAddress(text(request.getAddress()));
        dealer.setRemark(text(request.getRemark()));
        if (request.getEnabled() != null) dealer.setEnabled(request.getEnabled());
        dealers.updateById(dealer);
        return dealer;
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        // luohuai codeX  modify: never physically delete codes because even unused issued values cannot be recycled.
        throw conflict("经销商编码永久保留，请改为停用");
    }

    public Dealer requireEnabled(Long id) {
        Dealer dealer = require(id);
        if (!Boolean.TRUE.equals(dealer.getEnabled())) throw invalid("请选择启用的经销商");
        return dealer;
    }

    public Dealer findByName(String name) {
        return dealers.selectOne(new LambdaQueryWrapper<Dealer>().eq(Dealer::getName, text(name)).last("LIMIT 1"));
    }

    private Dealer require(Long id) {
        Dealer dealer = id == null ? null : dealers.selectById(id);
        if (dealer == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "经销商不存在");
        return dealer;
    }

    private static String required(String value, String label) {
        String result = text(value);
        if (result.isEmpty() || result.length() > 100) throw invalid(label + "必填且不超过100字");
        return result;
    }
    private static String text(String value) { return value == null ? "" : value.trim(); }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
