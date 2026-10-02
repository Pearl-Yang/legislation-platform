package com.legal.legislation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.legislation.entity.LegislativeStageTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LegislativeStageTemplateMapper extends BaseMapper<LegislativeStageTemplate> {

    /**
     * 按项目类型取出模板（按 stage_order 升序）
     */
    @Select("SELECT * FROM legislative_stage_template WHERE type = #{type} ORDER BY stage_order ASC")
    List<LegislativeStageTemplate> selectByType(String type);
}