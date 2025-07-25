package com.wimoor.erp.stock.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wimoor.erp.common.pojo.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
 
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value="ShipInboundCase对象", description="货件装箱详情")
@TableName("t_erp_dispatch_oversea_case")
public class ErpDispatchOverseaCase extends BaseEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7808691102390409776L;
	@ApiModelProperty(value = "箱子ID")
	@NotNull(message="boxid不能为空")
    @TableField(value= "boxid")
    private String boxid;

	@ApiModelProperty(value = "平台SKU")
    @NotNull(message="SKU不能为空")
    @TableField(value= "sku")
    private String sku;
	
	@ApiModelProperty(value = "箱子内对应SKU的数量")
    @TableField(value= "quantity")
    private Integer quantity;
 
}