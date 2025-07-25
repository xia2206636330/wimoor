package com.wimoor.erp.stock.pojo.dto;

import com.wimoor.erp.stock.pojo.entity.ErpDispatchOverseaBox;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


@Data
@ApiModel(value="ShipInboundShipment对象", description="货件")
public class ShipCartDTO {
	 
	@ApiModelProperty(value = "计划ID[必填]")
    private String formid;
	
	@ApiModelProperty(value = "箱子数量[非必填，仅在装箱时需要]")
	private Integer boxnum;
	
 	@ApiModelProperty(value = "配送方式[非必填，仅在装箱时需要]")
	private String  transtyle;
 	
 	@ApiModelProperty(value = "配送商[非必填，仅在装箱时需要]")
	private String  carrier;
 	
 	@ApiModelProperty(value = "操作类型[save:不提交亚马逊，仅本地保存；submit:保存并提交箱子信息到亚马逊")
    private String opttype;
 
 	@ApiModelProperty(value = "箱子详情")
    List<ErpDispatchOverseaBox> boxListDetail;

  }