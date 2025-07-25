package com.wimoor.erp.purchase.mapper;

import java.util.Map;

import com.wimoor.erp.purchase.pojo.entity.PurchaseFormEntryChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wimoor team
 * @since 2024-03-12
 */
@Mapper
public interface PurchaseFormEntryChangeMapper extends BaseMapper<PurchaseFormEntryChange> {

	IPage<Map<String, Object>> findByCondition(Page<?>  page,@Param("params")Map<String,Object> map);

}
