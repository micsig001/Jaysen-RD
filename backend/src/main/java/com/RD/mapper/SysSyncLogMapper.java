package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.SysSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步日志 Mapper
 */
@Mapper
public interface SysSyncLogMapper extends BaseMapper<SysSyncLog> {
}
