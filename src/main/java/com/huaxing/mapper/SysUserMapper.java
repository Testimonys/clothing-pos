package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    Optional<SysUser> findByUsername(String username);

    @Select("SELECT COUNT(*) > 0 FROM sys_user WHERE username = #{username}")
    boolean existsByUsername(String username);
}
