package com.wimoor.amazon.profit.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import cn.hutool.core.math.Calculator;
import com.wimoor.common.StringFormat;
import org.springframework.stereotype.Service;

import com.googlecode.aviator.AviatorEvaluator;
import com.wimoor.amazon.profit.pojo.entity.FBAFormat;
import com.wimoor.amazon.profit.pojo.entity.OutboundWeightFormat;
import com.wimoor.amazon.profit.pojo.entity.ProductTier;
import com.wimoor.amazon.profit.pojo.entity.ProfitConfigCountry;
import com.wimoor.amazon.profit.pojo.vo.InputDimensions;
import com.wimoor.amazon.profit.pojo.vo.ItemMeasure;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.mvc.BizException;

import cn.hutool.core.util.StrUtil;
 
@Service("UKProfitService")
public class UKProfitServiceImpl extends ProfitServiceImpl {

	@Override
	public String determineProductTier(String country, InputDimensions inputDimension, String isMedia)
			throws BizException {
		String UKProductTier = null;
		BigDecimal length_cm = inputDimension.getLength(getDimUnit(country)).getValue();
		BigDecimal width_cm = inputDimension.getWidth(getDimUnit(country)).getValue();
		BigDecimal height_cm = inputDimension.getHeight(getDimUnit(country)).getValue();
		BigDecimal weight = inputDimension.getWeight(getWeightUnit(country)).getValue();
		if(weight==null){
			weight=new BigDecimal("0");
		}
		weight = weight.setScale(3, RoundingMode.CEILING  );// 四舍五入进位取整数，模拟不足一克按一克算；
		InputDimensions inputDimension_new = new InputDimensions(length_cm, width_cm, height_cm, getDimUnit(country), weight, getWeightUnit(country));
		inputDimension_new.setDimensionalWeight(inputDimension_new.getDimensionalWeight(getDimUnit(country)));
		List<Map<String, Object>> tierList = productFormatService.findTierFormatByCountry(country);
		for (int i = 0; i < tierList.size(); i++) {
			Map<String, Object> tierMap = tierList.get(i);
			String format = tierMap.get("format").toString();
			Boolean result = (Boolean) AviatorEvaluator.exec(format, inputDimension_new);
			if (result) {
				UKProductTier = tierMap.get("producttierId").toString();
				break;
			}
		}
		return UKProductTier;// 对于英国站点，FBA不适用于任何重量超过30千克的商品。
	}

	//计算带包装重量，单位g
	@Override
	public BigDecimal determineOutboundWeight(String country, ProductTier productTier, InputDimensions dimensions,
			String isMedia) throws BizException {
		BigDecimal outboundWeight = new BigDecimal("0");
		BigDecimal weight = dimensions.getWeight(getWeightUnit(country)).getValue();
		if (weight == null) {
			weight = new BigDecimal("0");
		}
		weight = weight.setScale(3, RoundingMode.CEILING );// 四舍五入进位取整数，模拟不足一克按一克算；
		ItemMeasure dimweight = dimensions.getDimensionalWeight(getDimUnit(country));
	    if(dimweight!=null) {
			if(dimweight.getValue().compareTo(weight)>0) {
				weight=dimweight.getValue();
			}
		}
		BigDecimal boxweight = productTier.getBoxWeight();
		if (boxweight == null) {
			boxweight = new BigDecimal("0");
		}
		outboundWeight = weight.add(boxweight);// 进位取整数，模拟不足一克按一克算,单位：g
		OutboundWeightFormat outboundWeightFormat = outboundWeightFormatService.findByProductTierId(productTier.getId());
		if (outboundWeightFormat != null && outboundWeightFormat.getFormat() != null) {
			String format = outboundWeightFormat.getFormat();
			// 任何以大写字母N结尾的整数都被认为是big int；
			// 任何以大写字母M结尾的数字都被认为是decimal；
			// 其他的任何整数都将被转换为Long，
			// 其他任何浮点数都将被转换为Double。
			// 超过long范围的整数字面量都将自动转换为big int类型
			Object result = AviatorEvaluator.exec(format, dimensions);
			// System.out.println(result+" "+result.getClass());//有时得到BigDecimal类型，有时得到Double类型
			outboundWeight = new BigDecimal(result.toString());
			BigDecimal intOut = outboundWeight.setScale(0,RoundingMode.DOWN);// 进位取整数，模拟不足一磅按一磅算；
			BigDecimal flotOut = outboundWeight.subtract(intOut);
			if(flotOut.compareTo(new BigDecimal("0.5"))>0) {
				outboundWeight=intOut.add(new BigDecimal("1"));
			}else if(flotOut.compareTo(new BigDecimal("0.00001"))>=0) {
				outboundWeight=intOut.add(new BigDecimal("0.5"));
			}
		}
		return outboundWeight;
	}
	
	@Override
	public BigDecimal calculateFBA(String country, String productTierId, InputDimensions inputDimension, String isMedia, String type,
			BigDecimal outboundWeight, ProfitConfigCountry profitConfigX, String shipmentType) throws BizException {
		BigDecimal FBA = new BigDecimal("0");// 当标准尺寸售价£300或以上时，FBA免费，待完善
	
		String fenpeiType = "PAN_EU";
		boolean hasAddedSite = false;
		if (profitConfigX != null) {
			String tempType = profitConfigX.getFenpeiType();
			if(StrUtil.isNotBlank(tempType)) {
				fenpeiType=tempType;
			}
			//选PAN-EU之后, 可选 Fulfilment Network Expansion (Central Europe Program),
			hasAddedSite = profitConfigX.isHasAddedSite();
		}
		if (fenpeiType!=null&&fenpeiType.equals("PAN_EU") && hasAddedSite) {
			if(country.equals("PL") ||country.equals("CZ")) {
				country = "DE";
			}
		}
		if (fenpeiType.equals("EFN")) {
			String warehousesite = profitConfigX.getWarehousesite();
			if(warehousesite!=null && warehousesite.equals(country.toLowerCase())){
				fenpeiType = "PAN_EU";
			} 
			else {
				if(warehousesite.equals("UK")) {
					 if(country.equals("DE")||country.equals("IT")||country.equals("ES")) {
						 country = "UK-(DE,IT,ES)";
					 }
					 if(country.equals("FR")) {
						 country = "UK-(FR)";
					 }
				}else {
					if(country.equals("DE")||country.equals("IT")||country.equals("ES")){
						country = "DE,IT,ES";
					}else if(country.equals("UK")) {
						if(country.equals("DE")||country.equals("FR")||country.equals("IT")||country.equals("ES")) {
							country = "(DE,FR,IT,ES)-UK";
						}else {
							fenpeiType = "PAN_EU";
						}
					}
				}
			
			}
		}
		
		FBAFormat fbaFormat = fbaFormatService.findEUfbaFormat(fenpeiType, productTierId, country, outboundWeight);
		if (fbaFormat != null) {
			String format = fbaFormat.getFbaFormat();
			if (format.contains("?")) {// 如果有逻辑判断
				format = (String) AviatorEvaluator.exec(format, outboundWeight);
			}
			if(format.contains("{0}")) {
				String result = StringFormat.format(format, outboundWeight);
				format =  new BigDecimal(Calculator.conversion(result)).toString();
			}
			if(format.contains("value")) {
				format =  AviatorEvaluator.exec(format, outboundWeight).toString();
			}
			if(GeneralUtil.isDouble(format)) {
				FBA = new BigDecimal(format);
			}
			if("PAN_EU".equals(fenpeiType) && productTierId.contains("oversize")){//Pan-European Surcharge Oversize
				if(country.equals("UK")){
					FBA = FBA.add(new BigDecimal("0.93"));
				}
				if(country.equals("IT")||country.equals("ES")){
					FBA = FBA.add(new BigDecimal("2.09"));
				}
				if(country.equals("FR")){
					FBA = FBA.add(new BigDecimal("2.15"));
				}
				if(country.equals("SE")) {
					FBA=FBA.add(new BigDecimal("20"));
				}
				
			}
		}
		
		return FBA;
	}
	
	@Override
	public BigDecimal calculateStorageFee(String country, ProfitConfigCountry profitConfigX, InputDimensions inputDimension, boolean isStandard) 
			throws BizException {//每月每立方英尺，按比例收费
		if(country.equals("uk")) {
			BigDecimal storageFee =new BigDecimal("0");
			BigDecimal monthlyFee =  profitConfigX.getStoragefee();
			BigDecimal month = profitConfigX.getAmonthBigDecimal();
			BigDecimal volume = inputDimension.getVolume(InputDimensions.unit_in).getValue().divide(new BigDecimal("1728"),
					4, RoundingMode.HALF_UP);// 立方英寸转换成立方英尺
			storageFee = monthlyFee.multiply(volume).multiply(month);
			return storageFee;
		}else {
			BigDecimal storageFee =new BigDecimal("0");
			BigDecimal monthlyFee =  profitConfigX.getStoragefee();
			BigDecimal month = profitConfigX.getAmonthBigDecimal();
			BigDecimal volume = inputDimension.getVolume(InputDimensions.unit_cm).getValue().divide(new BigDecimal("1000000"),
					4, RoundingMode.HALF_UP);//  立方厘米转换成立方米
			storageFee = monthlyFee.multiply(volume).multiply(month);
			return storageFee;
		}
		
	}
	
	@Override
	public BigDecimal calculateSmlFBA(String country, String productTierId, BigDecimal outboundWeight,ProfitConfigCountry profitConfigCountry,String type) {
		BigDecimal FBA = new BigDecimal("0");
		BigDecimal weight = outboundWeight;
		if(weight==null){
			weight=new BigDecimal("0");
		}
		weight = weight.setScale(3, RoundingMode.CEILING);// 四舍五入进位取整数，模拟不足一克按一克算；
		
		String fenpeiType = "PAN_EU";
		boolean hasAddedSite = false;
		if (profitConfigCountry != null) {
			fenpeiType = profitConfigCountry.getFenpeiType();
			//选PAN-EU之后, 可选 Fulfilment Network Expansion (Central Europe Program),
			hasAddedSite = profitConfigCountry.isHasAddedSite();
		}
		if (fenpeiType.equals("PAN_EU") && hasAddedSite) {
			if(country.equals("PL") ||country.equals("CZ")) {
				country = "DE";
			}
		}
		if (fenpeiType.equals("EFN")) {
			String warehousesite = profitConfigCountry.getWarehousesite();
			if(warehousesite!=null && warehousesite.equals(country.toLowerCase())){
				fenpeiType = "PAN_EU";
			} 
			else {
				if(country.equals("DE")||country.equals("IT")||country.equals("ES")) {
					country="DE,IT,ES";
				} 
			}
		}
		FBAFormat fbaFormat = fbaFormatService.findByProductTierIdAndWeightSL(productTierId, outboundWeight,fenpeiType,country);
		if (fbaFormat!=null) {
			String format = fbaFormat.getFbaFormat();
			FBA = new BigDecimal(format);
		}
		return FBA;
	}
	
	//单位：g
	@Override
	public BigDecimal getFBAFormatWeight(String country, String productTierId,  InputDimensions inputDimension, BigDecimal outboundWeight,
			ProfitConfigCountry profitConfigX) throws BizException {
		BigDecimal weight = inputDimension.getWeight(getWeightUnit(country)).getValue();
		if(weight==null){
			weight=new BigDecimal("0");
		}
		weight = weight.setScale(3, RoundingMode.CEILING);// 四舍五入进位取整数，模拟不足一克按一克算；
		
		String fenpeiType = "PAN_EU";
		boolean hasAddedSite = false;
		if (profitConfigX != null) {
			fenpeiType = profitConfigX.getFenpeiType();
			//选PAN-EU之后, 可选 Fulfilment Network Expansion (Central Europe Program),
			hasAddedSite = profitConfigX.isHasAddedSite();
		}
		if ("DE/PL/CZ".contains(country) && fenpeiType.equals("PAN_EU") && hasAddedSite) {
			country = "DE/PL/CZ";
		}
		if (fenpeiType.equals("EFN") ) {
			String warehousesite = profitConfigX.getWarehousesite();
			if(warehousesite!=null && warehousesite.equalsIgnoreCase("UK")){
				if("DE,IT,ES,FR".contains(country)){
					country = "UK-(DE,IT,ES,FR)";
				}
				if("NL,BE".contains(country)){
					country = "UK-(NL,BE)";
				}
				if("SE".contains(country)){
					country = "UK-(SE)";
				}
			}
			if(warehousesite!=null && !warehousesite.equalsIgnoreCase("UK")){
				if("DE,IT,ES,FR".contains(country)){
					country = "DE,IT,ES,FR";
				}else if("NL,BE".contains(country)){
					country = "NL,BE";
				}else if("BE,PL".contains(country)){
					country = country;
				}else if("UK".contains(country)){
					country = "(DE,IT,ES,FR)-UK";
				}
			}
		}
		FBAFormat fbaFormat = fbaFormatService.findEUfbaFormat(fenpeiType, productTierId, country, weight);
		if (fbaFormat==null) {
			return new BigDecimal("0");
		}
		if(fbaFormat.getWeight()==null) {
			fbaFormat.setWeight(new BigDecimal("0"));
		}
		return fbaFormat.getWeight().multiply(new BigDecimal("1000")).subtract(weight.multiply(new BigDecimal("1000"))).setScale(0);
	}
	public BigDecimal getSLOutboundWeight(String country, ProductTier productTier, InputDimensions inputDimension) {
		BigDecimal weight = inputDimension.getWeight(getWeightUnit(country)).getValue();
		if (weight == null) {
			weight = new BigDecimal("0");
		}
		BigDecimal boxweight = productTier.getBoxWeight();
		if (boxweight==null) {
			boxweight = new BigDecimal("0");

		}
/*
For small parcels (excluding items fulfilled via Low-Price FBA), standard parcels and oversize items (excluding Special oversize), Amazon uses dimensional weight to determine the weight tier whenever it is greater than the unit weight.
 */
//		inputDimension.setDimensionalWeight(inputDimension.getDimensionalWeight(getDimUnit(country)));
//		if (inputDimension.getDimensionalWeight().getValue().compareTo(weight) > 0) {
//			weight = inputDimension.getDimensionalWeight().getValue();
//		}
		BigDecimal outboundWeight = weight.add(boxweight).setScale(2, RoundingMode.CEILING);//进位取整数，模拟不足一盎司/克按一盎司/克算
		return outboundWeight;
	}

}
