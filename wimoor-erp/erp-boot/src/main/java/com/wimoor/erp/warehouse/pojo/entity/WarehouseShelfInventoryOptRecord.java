package com.wimoor.erp.warehouse.pojo.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wimoor.erp.common.pojo.entity.BaseEntity;
import com.wimoor.erp.material.pojo.entity.Material;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 操作记录
 * </p>
 *
 * @author wimoor team
 * @since 2022-03-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_erp_warehouse_shelf_inventory_opt_record")
@ApiModel(value="WarehouseShelfInventoryOptRecordVo对象", description="操作记录")
public class WarehouseShelfInventoryOptRecord extends BaseEntity{

    private static final long serialVersionUID=1L;

 
    @ApiModelProperty(value = "货柜ID")
    private BigInteger shelfid;

    @ApiModelProperty(value = "产品ID")
    private BigInteger materialid;
    
    @ApiModelProperty(value = "公司ID")
    private BigInteger shopid;

    @ApiModelProperty(value = "仓库ID")
    private BigInteger warehouseid;
    
    @ApiModelProperty(value = "操作数量")
    private Integer quantity;

    @ApiModelProperty(value = "操作数量对应的体积")
    private Float size;
    
    @ApiModelProperty(value = "表单ID")
    private String formid;
    
    @ApiModelProperty(value = "表单类型")
    private String formtype;
    
    @ApiModelProperty(value = "操作后结余数量")
    private Integer balanceQty;

    @ApiModelProperty(value = "操作后结余体积")
    private Float balanceSize;

    @ApiModelProperty(value = "0：出库；1：入库;2：修正下架；3：修正上架")
    private Integer opt;

    @ApiModelProperty(value = "操作人")
    private BigInteger operator;

    @ApiModelProperty(value = "操作时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime opttime;
    
    @TableField(exist=false)
    private WarehouseShelf shelf;
    
    @TableField(exist=false)
    private Material material;
    
    @TableField(exist=false)
    String shelfname;
    
    @TableField(exist=false)
    private WarehouseShelfInventory shelfInventory;
     
}
