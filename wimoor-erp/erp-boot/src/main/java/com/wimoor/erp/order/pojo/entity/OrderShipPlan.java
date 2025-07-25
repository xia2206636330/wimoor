package com.wimoor.erp.order.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel(value="OrderPlan", description="计划")
@TableName("t_erp_order_ship_plan")
public class OrderShipPlan {
    @TableField(value = "materialid")
    private String materialid;
    @TableField(value = "shopid")
    private String shopid;
}
