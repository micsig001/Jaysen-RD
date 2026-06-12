package com.RD.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.RD.entity.TaskStatusHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务状态历史 Mapper
 */
@Mapper
public interface TaskStatusHistoryMapper extends BaseMapper<TaskStatusHistory> {
}
