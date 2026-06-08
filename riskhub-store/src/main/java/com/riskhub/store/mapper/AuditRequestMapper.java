package com.riskhub.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.riskhub.store.entity.AuditRequestEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditRequestMapper extends BaseMapper<AuditRequestEntity> {
}
