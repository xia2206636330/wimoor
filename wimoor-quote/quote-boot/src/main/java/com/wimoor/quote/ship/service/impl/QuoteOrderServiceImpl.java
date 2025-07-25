package com.wimoor.quote.ship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.service.ISerialNumService;
import com.wimoor.quote.api.dto.QuotePriceDTO;
import com.wimoor.quote.api.entity.*;
import com.wimoor.quote.api.pojo.vo.ShipmentAddressSummaryVO;
import com.wimoor.quote.ship.mapper.*;
import com.wimoor.quote.api.dto.QuoteDTO;
import com.wimoor.quote.ship.service.IQuoteOrderService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuoteOrderServiceImpl extends ServiceImpl<QuoteOrderMapper, QuoteOrder> implements IQuoteOrderService {

    final UserBuyerMapper userBuyerMapper;
    final UserSupplierMapper userSupplierMapper;
    final ShipmentMapper shipmentMapper;
    final ShipmentBoxMapper shipmentBoxMapper;
    final ShipmentItemMapper shipmentItemMapper;
    final ShipmentDestinationAddressMapper shipmentDestinationAddressMapper;
    final SupplyRelationshipMapper supplyRelationshipMapper;
    final QuotationPriceMapper quotationPriceMapper;
    final QuoteOrderShipmentMapper quoteOrderShipmentMapper;
    final QuoteOrderMapper quoteOrderMapper;
    final ISerialNumService iSerialNumService;
    final QuoteOrderSupplierMapper quoteOrderSupplierMapper;
    @Override
    public UserBuyer getBuyerInfo(String token) {
        if (token != null) {
            LambdaQueryWrapper<UserBuyer> query=new LambdaQueryWrapper<UserBuyer>();
            query.eq(UserBuyer::getToken,token);
            return userBuyerMapper.selectOne(query);
        }
        return null;
    }

    @Override
    public UserSupplier getSupplierInfo(String token) {
        if (token != null) {
            LambdaQueryWrapper<UserSupplier> query=new LambdaQueryWrapper<UserSupplier>();
            query.eq(UserSupplier::getToken,token);
            return userSupplierMapper.selectOne(query);
        }
        return null;
    }

    @Override
    public Boolean saveShipment(UserBuyer buyer, QuoteDTO dto) {
        for(Shipment shipment:dto.getShipments()){
            Shipment old = shipmentMapper.selectById(shipment.getShipmentid());
            shipment.setBuyerid(buyer.getId());
            shipment.setBuyerothername(buyer.getName());
            if(shipment.getStatus()==null){
                shipment.setStatus(0);
            }
            if(old==null){
                shipment.setOpttime(new Date());
                shipment.setStatus(0);
                shipmentMapper.insert(shipment);
            }else{
                shipment.setOpttime(new Date());
                shipmentMapper.updateById(shipment);
            }
            for(ShipmentBox box:shipment.getBoxList()){
                box.setShipmentid(shipment.getShipmentid());
                ShipmentBox oldbox = shipmentBoxMapper.selectById(box.getBoxid());
                if(oldbox==null){
                    shipmentBoxMapper.insert(box);
                }else{
                    shipmentBoxMapper.updateById(box);
                }
            }
            for(ShipmentItem item:shipment.getItemList()){
                item.setShipmentid(shipment.getShipmentid());
                ShipmentItem olditem = shipmentItemMapper.selectOne(new LambdaQueryWrapper<ShipmentItem>().eq(ShipmentItem::getSku,item.getSku()).eq(ShipmentItem::getShipmentid,shipment.getShipmentid()));
                if(olditem==null){
                    shipmentItemMapper.insert(item);
                }else{
                    item.setId(olditem.getId());
                    shipmentItemMapper.updateById(item);
                }
            }
            ShipmentDestinationAddress oldaddress = shipmentDestinationAddressMapper.selectById(shipment.getAddress().getCode());
            if(oldaddress==null){
                shipmentDestinationAddressMapper.insert(shipment.getAddress());
            }else{
                shipmentDestinationAddressMapper.updateById(shipment.getAddress());
            }
        }
        return true;
    }

    @Override
    public IPage<Shipment> listShipment(UserBuyer buyer, QuoteDTO dto) {
        dto.setBuyerid(buyer.getId());
        if(StrUtil.isNotBlank(dto.getSearch())){
            dto.setSearch(dto.getSearch().trim());
        }else{
            dto.setSearch(null);
        }
        List<String> numbers=null;
        if(StrUtil.isNotBlank(dto.getNumber())){
            String mnumber=dto.getNumber();
            mnumber=mnumber.replace("\n",",");
            String[] split = mnumber.trim().split(",");
            numbers=Arrays.asList(split);
            numbers.removeIf(String::isEmpty);
        }
        dto.setNumbers(numbers);
        List<String> shipmentids=null;
        if(StrUtil.isNotBlank(dto.getShipmentid())){
            String mshipmentid=dto.getShipmentid();
            mshipmentid=mshipmentid.replace(" ","");
            mshipmentid=mshipmentid.replace("\n",",");
            String[] split = mshipmentid.trim().split(",");
            shipmentids=Arrays.asList(split);
            shipmentids.removeIf(String::isEmpty);
        }
        dto.setNumbers(numbers);
        dto.setShipmentids(shipmentids);
        IPage<Shipment> list = shipmentMapper.findByCondition(dto.getPage(),dto);
        for (Shipment shipment : list.getRecords()) {
            List<ShipmentBox> boxlist = shipmentBoxMapper.selectList(new LambdaQueryWrapper<ShipmentBox>().eq(ShipmentBox::getShipmentid, shipment.getShipmentid()));
            shipment.setBoxList(boxlist);
            ShipmentDestinationAddress oldAddress = shipmentDestinationAddressMapper.selectById(shipment.getDestination());
            shipment.setAddress(oldAddress);
            List<ShipmentItem> itemlist = shipmentItemMapper.selectList(new LambdaQueryWrapper<ShipmentItem>().eq(ShipmentItem::getShipmentid, shipment.getShipmentid()));
            shipment.setItemList(itemlist);
            List<QuoteOrderShipment> ordershipmentlist = quoteOrderShipmentMapper.selectList(new LambdaQueryWrapper<QuoteOrderShipment>().eq(QuoteOrderShipment::getShipmentid, shipment.getShipmentid()));

            List<QuoteOrder> orderList=new ArrayList<QuoteOrder>();
            for(QuoteOrderShipment orderShipment:ordershipmentlist){
                QuoteOrder order = this.quoteOrderMapper.selectById(orderShipment.getOrderid());
                List<QuotationPrice> listPrice = quotationPriceMapper.selectList(new LambdaQueryWrapper<QuotationPrice>()
                                                                                .eq(QuotationPrice::getOrderid, orderShipment.getOrderid())
                                                                                .eq(QuotationPrice::getShipmentid,shipment.getShipmentid()));
                for(QuotationPrice itemPrice:listPrice){
                    itemPrice.setSupplier(userSupplierMapper.selectById(itemPrice.getSupplierid()));
                }
                order.setQuotationPriceList(listPrice);
                orderList.add(order);
            }
            shipment.setOrderList(orderList);
        }
        return list;

    }

    @Override
    public Boolean addSupplier(UserBuyer buyer, UserSupplier dto) {
        List<UserSupplier> list = userSupplierMapper.selectList(new LambdaQueryWrapper<UserSupplier>().eq(UserSupplier::getName, dto.getName()));
        if(list!=null&&list.size()>0){
            throw new BizException("已经存在对应供应商");
        }
        dto.setCreatetime(new java.util.Date());
        Calendar c=Calendar.getInstance();
        c.add(Calendar.YEAR, 1);
        dto.setTokentime(c.getTime());
        dto.setToken(UUID.randomUUID().toString());
        boolean result = userSupplierMapper.insert(dto) > 0;
        SupplyRelationship old = supplyRelationshipMapper.selectOne(new LambdaQueryWrapper<SupplyRelationship>().eq(SupplyRelationship::getBuyerid, buyer.getId()).eq(SupplyRelationship::getSupplierid, dto.getId()));
        if(old==null){
            SupplyRelationship supplyRelationship = new SupplyRelationship();
            supplyRelationship.setBuyerid(buyer.getId());
            supplyRelationship.setSupplierid(dto.getId());
            supplyRelationshipMapper.insert(supplyRelationship);
        }
        return result;
    }

    @Override
    public List<UserSupplier> listSupplier(String id) {
        return userSupplierMapper.listSupplier(id);
    }

    @Override
    public List<Map<String,Object>> listPrice(String id) {
        return quotationPriceMapper.list(id);
    }

    @Override
    public boolean saveOrder(QuoteOrder dto) {
        if(dto.getStatus()==null){
            dto.setStatus(1);
        }
        if(dto.getFtype()==null){
            dto.setFtype(2);
        }
        dto.setCreatetime(new java.util.Date());
        String num=null;
        try {
            num=iSerialNumService.readSerialNumber(dto.getBuyerid(),"PI");
        } catch (Exception e) {
            try {
                num=iSerialNumService.readSerialNumber(dto.getBuyerid(),"PI");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        dto.setNumber(num);
        BigDecimal weight= BigDecimal.ZERO;
        BigDecimal volume= BigDecimal.ZERO;
        for(String item:dto.getShipmentids()){
            Shipment shipment = shipmentMapper.selectById(item);
            weight=weight.add(shipment.getWeight());
            volume=volume.add(shipment.getVolume());
        }
        dto.setWeight(weight);
        dto.setVolume(volume);
        quoteOrderMapper.insert(dto);

        for(String item:dto.getShipmentids()){
            QuoteOrderShipment entity = new QuoteOrderShipment();
            entity.setOrderid(dto.getId());
            entity.setShipmentid(item);
            Shipment old = shipmentMapper.selectById(item);
            if(old.getStatus()==0){
                old.setStatus(1);
                shipmentMapper.updateById(old);
            }
            quoteOrderShipmentMapper.insert(entity);
        }

        return true;
    }

    @Override
    public IPage<QuoteOrder> listOrder(UserBuyer buyer, QuoteDTO dto) {
        if(StrUtil.isNotBlank(dto.getSearch())){
            dto.setSearch(dto.getSearch().trim());
        }else{
            dto.setSearch(null);
        }
        IPage<QuoteOrder> page = this.baseMapper.findByCondition(dto.getPage(), dto);
        getShipmentToAddress(page.getRecords(),null,dto.getDisplayType());
        return page;
    }

    String getKey(Shipment shipment,String type){
        if(type.equals("center")){
            return shipment.getDestination();
        }else{
            return shipment.getNumber();
        }
    }
    private UserSupplier getSupplierByCacheMap(Map<String,UserSupplier> cacheMap,String supplierid){
        if(cacheMap.containsKey(supplierid)){
            return cacheMap.get(supplierid);
        }else{
            return userSupplierMapper.selectById(supplierid);
        }
    }
    private List< ShipmentAddressSummaryVO> getShipmentToAddress(List<QuoteOrder> orderList,UserSupplier supplier,String displayType){
        Map<String, ShipmentAddressSummaryVO> centerlist= new HashMap<String, ShipmentAddressSummaryVO>();
        List<String> orderids = new ArrayList<String>();
        Map<String,ShipmentDestinationAddress> destinationMap=new HashMap<String,ShipmentDestinationAddress>();
        Map<String,UserSupplier> cacheSupplierMap=new HashMap<String,UserSupplier>();
        Set<String> destinationList = new HashSet<String>();
        Map<String,QuoteOrder> orderMap = new HashMap<String,QuoteOrder>();
        for(QuoteOrder order:orderList){
            orderids.add(order.getId());
            orderMap.put(order.getId(),order);
        }
        if(orderids.isEmpty()){
            return null;
        }
        List<QuotationPrice> listPrice = quotationPriceMapper.selectList(new LambdaQueryWrapper<QuotationPrice>().in(QuotationPrice::getOrderid, orderids));
        List<QuoteOrderSupplier> supplierList=quoteOrderSupplierMapper.selectList(new LambdaQueryWrapper<QuoteOrderSupplier>().in(QuoteOrderSupplier::getOrderid, orderids));
        List<QuoteOrderShipment> list = quoteOrderShipmentMapper.selectList(new LambdaQueryWrapper<QuoteOrderShipment>().in(QuoteOrderShipment::getOrderid, orderids));
        List<String> shipmentids = new ArrayList<String>();
        for(QuoteOrderShipment item:list){
            QuoteOrder order = orderMap.get(item.getOrderid());
            if(order.getShipmentids()==null){
                List<String> mlist = new ArrayList<String>();
                order.setShipmentids(mlist);
                order.getShipmentids().add(item.getShipmentid());
            }else{
                order.getShipmentids().add(item.getShipmentid());
            }
            shipmentids.add(item.getShipmentid());
        }
        if(shipmentids.isEmpty()){
            return null;
        }
        String type=StrUtil.isBlank(displayType)?"center":displayType;
        List<Shipment> shipments = shipmentMapper.selectList(new LambdaQueryWrapper<Shipment>().in(Shipment::getShipmentid, shipmentids));
        List<ShipmentBox>  boxlist = shipmentBoxMapper.selectList(new LambdaQueryWrapper<ShipmentBox>().in(ShipmentBox::getShipmentid, shipmentids));
        List<ShipmentItem> shipitems =shipmentItemMapper.selectList(new LambdaQueryWrapper<ShipmentItem>().in(ShipmentItem::getShipmentid, shipmentids));
        Map<String,Shipment> shipmentMap = new HashMap<String,Shipment>();
        Map<String,List<ShipmentBox>> boxMap = new HashMap<String,List<ShipmentBox>>();
        Map<String,List<ShipmentItem>> itemMap = new HashMap<String,List<ShipmentItem>>();
        Map<String,List<QuotationPrice>> priceMap = new HashMap<String,List<QuotationPrice>>();
        Map<String,List<QuoteOrderSupplier>> supplierMap = new HashMap<String,List<QuoteOrderSupplier>>();
        for(Shipment item:shipments){
            shipmentMap.put(item.getShipmentid(),item);
            destinationList.add(item.getDestination());
        }
        List<String> supplierIds=new ArrayList<String>();
        for(QuoteOrderSupplier ositem:supplierList){
            List<QuoteOrderSupplier> osupplierList = supplierMap.get(ositem.getOrderid());
            if(osupplierList==null){
                osupplierList=new ArrayList<QuoteOrderSupplier>();
                supplierMap.put(ositem.getOrderid(),osupplierList);
            }
            supplierIds.add(ositem.getSupplierid());
            osupplierList.add(ositem);
        }
        if(supplierIds!=null && supplierIds.size()>0){
            List<UserSupplier> userSupplierList=userSupplierMapper.selectList(new LambdaQueryWrapper<UserSupplier>().in(UserSupplier::getId, supplierIds));
            for(UserSupplier userSupplier:userSupplierList){
                cacheSupplierMap.put(userSupplier.getId(),userSupplier);
            }
        }
        for(ShipmentBox box:boxlist){
            List<ShipmentBox> boxitem = boxMap.get(box.getShipmentid());
            if(boxitem==null){
                boxitem=new ArrayList<ShipmentBox>();
                boxMap.put(box.getShipmentid(),boxitem);
            }
            boxitem.add(box);

        }
        for(ShipmentItem item :shipitems){
            List<ShipmentItem> sitem = itemMap.get(item.getShipmentid());
            if(sitem==null){
                sitem=new ArrayList<ShipmentItem>();
                itemMap.put(item.getShipmentid(),sitem);
            }
            sitem.add(item);
        }
        for(QuotationPrice price :listPrice){
            String key=price.getOrderid();
            key=key+(price.getDestination()==null?"":price.getDestination());
            key=key+(price.getShipmentid()==null?"":price.getShipmentid());
            List<QuotationPrice> itemPrice = priceMap.get(key);
            if(itemPrice==null){
                itemPrice=new ArrayList<QuotationPrice>();
                priceMap.put(key,itemPrice);
            }
            itemPrice.add(price);
        }
        for(QuoteOrder order:orderList){
            if(order.getShipmentids()!=null && order.getShipmentids().size()>0){
                for(String item:order.getShipmentids()){
                    if(order.getShipmentList()==null){
                        order.setShipmentList(new ArrayList<Shipment>());
                        order.getShipmentList().add(shipmentMap.get(item));
                    }else{
                        order.getShipmentList().add(shipmentMap.get(item));
                    }
                }
            }
        }
        List<ShipmentDestinationAddress> addresslist = shipmentDestinationAddressMapper.selectList(new LambdaQueryWrapper<ShipmentDestinationAddress>().in(ShipmentDestinationAddress::getCode, destinationList));
        for(ShipmentDestinationAddress code:addresslist){
            destinationMap.put(code.getCode(),code);
        }

        for(QuoteOrder order:orderList){
            List<QuotationPrice> omlistPrice =priceMap.get(order.getId());
            Map<String,QuotationPrice> pricemap=new HashMap<String,QuotationPrice>();
            if(omlistPrice!=null&&omlistPrice.size()>0){
                for(QuotationPrice itemprice:omlistPrice){
                    if(itemprice.getConfirm()!=null&&itemprice.getConfirm()==true){
                        order.setBase(itemprice.getBase());
                    }
                    if(pricemap.get(itemprice.getSupplierid())==null){
                        pricemap.put(itemprice.getSupplierid(),itemprice);
                    }else{
                        QuotationPrice oldprice = pricemap.get(itemprice.getSupplierid());
                        if(oldprice.getTotalfee().compareTo(itemprice.getTotalfee())>0){
                            pricemap.put(itemprice.getSupplierid(),itemprice);
                       }
                    }
                }
            }
            List<QuoteOrderSupplier> msupplierList =supplierMap.get(order.getId());
            if(supplier!=null&&msupplierList!=null){
                QuoteOrderSupplier orderSupplier =  msupplierList.stream().filter(itemv->itemv.getSupplierid().equals(supplier.getId())).findFirst().get();
                order.setBase(orderSupplier.getBase());
                if(omlistPrice!=null){
                    List<QuotationPrice>  supplierlistPrice=new ArrayList<QuotationPrice>();
                    if(supplier!=null){
                        for(QuotationPrice itemprice:omlistPrice){
                            if(itemprice.getSupplierid().equals(supplier.getId())){
                                supplierlistPrice.add(itemprice);
                            }
                        }
                        omlistPrice=supplierlistPrice;
                    }
                    QuotationPrice supplierQuotationPrice=null;
                    BigDecimal minfee=new BigDecimal("10000000");
                    List<QuotationPrice> supplierListPrice =new ArrayList<QuotationPrice>();
                    for(QuotationPrice item:omlistPrice){
                        if(item.getConfirm()!=null&&item.getConfirm()==true){
                            order.setBase(item.getBase());
                        }
                        if(item.getSupplierid().equals(supplier.getId())){
                            supplierQuotationPrice=item;
                            supplierQuotationPrice.setSupplier(this.getSupplierByCacheMap(cacheSupplierMap,item.getSupplierid()));
                            supplierListPrice.add(supplierQuotationPrice);
                        }
                        if(minfee.compareTo(item.getTotalfee())>0){
                            minfee=item.getTotalfee();
                        }
                    }
                    order.setMinfee(minfee);
                    if(supplierQuotationPrice!=null&&supplierListPrice.size()>0){
                        order.setQuotationPriceList(supplierListPrice);
                    }
                }

            }else{
                List<QuotationPrice> listPriceResult= new ArrayList<QuotationPrice>();
                if(msupplierList!=null&& !msupplierList.isEmpty()){
                    for(QuoteOrderSupplier item:msupplierList){
                        QuotationPrice price = pricemap.get(item.getSupplierid());
                        if(price==null){
                            price=new QuotationPrice();
                            price.setBase(item.getBase());
                            price.setOrderid(order.getId());
                            price.setSupplierid(item.getSupplierid());
                        }
                        price.setSupplier(this.getSupplierByCacheMap(cacheSupplierMap,item.getSupplierid()));
                        listPriceResult.add(price);
                    }
                    order.setQuotationPriceList(listPriceResult);
                }
            }
            if(order.getShipmentList()!=null && order.getShipmentList().size()>0){
                for(Shipment shipment:order.getShipmentList()){
                    ShipmentAddressSummaryVO address =null;
                    ShipmentDestinationAddress myaddress = destinationMap.get(shipment.getDestination());
                    if(centerlist.get(getKey(shipment,type))!=null){
                        address=  centerlist.get(getKey(shipment,type));
                    }else{
                        address=new ShipmentAddressSummaryVO();
                        if(type.equals("center")){
                            BeanUtil.copyProperties(myaddress,address);
                        }else{
                            address.setCode(shipment.getNumber());
                        }
                        String key= order.getId();
                        key=key+shipment.getDestination();
                        List<QuotationPrice> mlistPrice = priceMap.get(key);
                        if(mlistPrice!=null){
                            List<QuotationPrice>  supplierlistPrice=new ArrayList<QuotationPrice>();
                            if(supplier!=null){
                                for(QuotationPrice itemprice:mlistPrice){
                                    if(itemprice.getSupplierid().equals(supplier.getId())){
                                        supplierlistPrice.add(itemprice);
                                    }
                                }
                                mlistPrice=supplierlistPrice;
                            }
                            for(QuotationPrice itemprice:mlistPrice){
                                itemprice.setSupplier( getSupplierByCacheMap(cacheSupplierMap,itemprice.getSupplierid()) );
                            }
                        }
                        address.setQuotationPriceList(mlistPrice);
                        centerlist.put(getKey(shipment,type),address);
                    }
                    shipment.setAddress(myaddress);
                    if(supplier!=null&&order.getIsbidding()!=null && order.getIsbidding()){
                        String key=order.getId();
                        key=key+(shipment.getDestination()==null?"":shipment.getDestination());
                        key=key+(shipment.getShipmentid()==null?"":shipment.getShipmentid());
                        List<QuotationPrice> mlistPrice = priceMap.get(key);
                        if(mlistPrice!=null){
                            List<QuotationPrice>  supplierlistPrice=new ArrayList<QuotationPrice>();
                            if(supplier!=null){
                                for(QuotationPrice itemprice:mlistPrice){
                                    if(itemprice.getSupplierid().equals(supplier.getId())){
                                        supplierlistPrice.add(itemprice);
                                        order.setBase(itemprice.getBase());
                                    }
                                }
                                mlistPrice=supplierlistPrice;
                            }
                        }
                        QuotationPrice supplierQuotationPrice=null;
                        BigDecimal minfee=new BigDecimal("10000000");
                        List<QuotationPrice> supplierListPrice =new ArrayList<QuotationPrice>();
                        if(mlistPrice!=null&&mlistPrice.size()>0){
                        for(QuotationPrice itemprice:mlistPrice){
                            if(itemprice.getSupplierid().equals(supplier.getId())){
                                supplierQuotationPrice=itemprice;
                                supplierQuotationPrice.setSupplier(getSupplierByCacheMap(cacheSupplierMap,supplierQuotationPrice.getSupplierid()) );
                                supplierListPrice.add(supplierQuotationPrice);
                            }
                            if(minfee.compareTo(itemprice.getTotalfee())>0){
                                minfee=itemprice.getTotalfee();
                            }
                        }
                        }
                        shipment.setMinfee(minfee);
                        if(supplierListPrice!=null&&supplierListPrice.size()>0){
                            shipment.setQuotationPriceList(supplierListPrice);
                        }
                    }else{
                        String key=order.getId();
                        key=key+( shipment.getDestination()!=null?shipment.getDestination():"");
                        key=key+( shipment.getShipmentid()!=null?shipment.getShipmentid():"");
                        List<QuotationPrice> mlistPrice = priceMap.get(key);
                        if(mlistPrice!=null&&mlistPrice.size()>0){
                            if(mlistPrice!=null){
                                List<QuotationPrice>  supplierlistPrice=new ArrayList<QuotationPrice>();
                                if(supplier!=null){
                                    for(QuotationPrice itemprice:mlistPrice){
                                        if(itemprice.getSupplierid().equals(supplier.getId())){
                                            supplierlistPrice.add(itemprice);
                                        }
                                    }
                                    mlistPrice=supplierlistPrice;
                                }
                            }
                            for(QuotationPrice itemprice:mlistPrice){
                                itemprice.setSupplier(this.getSupplierByCacheMap(cacheSupplierMap,itemprice.getSupplierid()));
                            }
                            shipment.setQuotationPriceList(mlistPrice);
                        }
                    }

                    shipment.setBoxList(boxMap.get(shipment.getShipmentid()));
                    if(order.getBase() == null||order.getBase().intValue()==0){
                        order.setBase(new BigDecimal(6000));
                    }
                    calculateWeight(shipment,order.getBase());
                    shipment.setItemList(itemMap.get(shipment.getShipmentid()));
                    if(address != null){
                        address.setWeight(address.getWeight()!=null?address.getWeight().add(shipment.getWeight()):shipment.getWeight());
                        address.setVolume(address.getVolume()!=null?address.getVolume().add(shipment.getVolume()):shipment.getVolume());
                        address.setCalweight(address.getCalweight()!=null?address.getCalweight().add(shipment.getCalweight()):shipment.getCalweight());
                        if(address.getShipmentList()!=null){
                            address.getShipmentList().add(shipment);
                        }else{
                            address.setShipmentList(new ArrayList<Shipment>());
                            address.getShipmentList().add(shipment);
                        }
                    }
                }
            }
            order.setAddressList( new ArrayList<ShipmentAddressSummaryVO>(centerlist.values()) );
            centerlist.clear();
        }
        return null;
    }

    private void calculateWeight(Shipment shipment, BigDecimal baseWeight) {
        if(baseWeight != null&&baseWeight.intValue()>0){
            BigDecimal weight = BigDecimal.ZERO;
            BigDecimal calweight = BigDecimal.ZERO;
            BigDecimal volume = BigDecimal.ZERO;
            for(ShipmentBox box:shipment.getBoxList()){
                BigDecimal singleVolume=BigDecimal.ZERO;
                BigDecimal singleWeight=BigDecimal.ZERO;
                BigDecimal singleCalWeight =BigDecimal.ZERO;
                if(box.getWeight()!=null){
                    singleWeight =box.getWeight();
                }
                if(box.getHeight()!=null){
                    BigDecimal length = box.getLength();
                    BigDecimal width = box.getWidth();
                    BigDecimal height = box.getHeight();
                    singleVolume=length.multiply(width.multiply(height));
                }
                singleCalWeight=singleVolume.divide(baseWeight,2, RoundingMode.UP);
                volume = volume.add(singleVolume);
                weight = weight.add(singleWeight);
                calweight = calweight.add(singleCalWeight);
                box.setDimweight(singleCalWeight);
                if(singleCalWeight.compareTo(singleWeight)>0){
                    box.setCalweight(singleCalWeight);
                }else{

                    box.setCalweight(singleWeight);
                }
            }
            shipment.setDimweight(calweight);
            if(calweight.compareTo(weight)>0){
                shipment.setCalweight(calweight);
            }else{
                shipment.setCalweight(weight);
            }
        }else{
            shipment.setCalweight(shipment.getWeight());
        }

        shipment.setCalweight(shipment.getCalweight().setScale(1,RoundingMode.DOWN).setScale(0,RoundingMode.UP));
    }

    @Override
    public IPage<QuoteOrder> listSupplierOrder(UserSupplier supplier, QuoteDTO dto) {
        Map<String,Object> param=new HashMap<String,Object>();
        param.put("supplierid",supplier.getId());
        param.put("status",dto.getStatus());
        IPage<QuoteOrder> page = this.baseMapper.findSupplierByCondition(dto.getPage(),param);
        getShipmentToAddress( page.getRecords(),supplier,dto.getDisplayType());
        return page;
    }

    @Override
    public boolean sendOrderToSupplier(QuotePriceDTO dto) {
        for(String item:dto.getOrderids()){
            QuoteOrder order = this.baseMapper.selectById(item);
            if(order.getStatus()==1){
                order.setStatus(3);
                this.baseMapper.updateById(order);
            }
            for(String supplier:dto.getSupplierid()){
                QuoteOrderSupplier old = quoteOrderSupplierMapper.selectOne(new LambdaQueryWrapper<QuoteOrderSupplier>()
                        .eq(QuoteOrderSupplier::getOrderid, item)
                        .eq(QuoteOrderSupplier::getSupplierid, supplier));
                if(old==null){
                    QuoteOrderSupplier entity = new QuoteOrderSupplier();
                    entity.setOrderid(item);
                    entity.setStatus(1);
                    entity.setBase(new BigDecimal("6000"));
                    entity.setSupplierid(supplier);
                    entity.setOpttime(new Date());
                    quoteOrderSupplierMapper.insert(entity);
                }
            }
        }
      return true;
    }

    @Override
    public boolean submitPrice(List<QuotationPrice> dto) {
        QuotationPrice lastone=null;
        for(QuotationPrice item:dto){
            lastone=item;
            if(item.isNullId()){
              quotationPriceMapper.insert(item);
            }else{
              quotationPriceMapper.updateById(item);
            }
        }
        QuoteOrder order = this.baseMapper.selectById(lastone.getOrderid());
        if(order.getPricetime()==null){
            order.setPricetime(new Date());
            order.setStatus(4);
            this.baseMapper.updateById(order);
        }
        LambdaQueryWrapper<QuoteOrderSupplier> qoQuery = new LambdaQueryWrapper<QuoteOrderSupplier>()
                .eq(QuoteOrderSupplier::getOrderid, lastone.getOrderid())
                .eq(QuoteOrderSupplier::getSupplierid, lastone.getSupplierid());
        QuoteOrderSupplier old = quoteOrderSupplierMapper.selectOne(qoQuery);
        if(old!=null){
            old.setStatus(4);
            quoteOrderSupplierMapper.update(old,qoQuery);
        }
        return true;
    }

    @Override
    public boolean confirmPrice(String priceId) {
        QuotationPrice price = quotationPriceMapper.selectById(priceId);
        price.setConfirm(true);
        quotationPriceMapper.updateById(price);
        QuoteOrder order = this.baseMapper.selectById(price.getOrderid());
        order.setStatus(5);
        this.baseMapper.updateById(order);
        LambdaQueryWrapper<QuoteOrderSupplier> query = new LambdaQueryWrapper<QuoteOrderSupplier>()
                .eq(QuoteOrderSupplier::getOrderid, price.getOrderid())
                .eq(QuoteOrderSupplier::getSupplierid, price.getSupplierid());
        QuoteOrderSupplier old = quoteOrderSupplierMapper.selectOne(query);
        old.setStatus(5);
        quoteOrderSupplierMapper.update(old,query);
        List<QuoteOrderShipment> list = quoteOrderShipmentMapper.selectList(new LambdaQueryWrapper<QuoteOrderShipment>().eq(QuoteOrderShipment::getOrderid, order.getId()));
        for(QuoteOrderShipment item:list) {
            Shipment shipment = shipmentMapper.selectById(item.getShipmentid());
            shipment.setOpttime(new Date());
            shipment.setStatus(5);
            shipmentMapper.updateById(shipment);
        }
        return true;
    }

    @Override
    public String addBuyer(UserBuyer dto) {
        if(!dto.isNullId()){
            UserBuyer old = userBuyerMapper.selectById(dto.getId());
            old.setName(dto.getName());
            old.setMobile(dto.getMobile());
            old.setAddress(dto.getAddress());
            old.setContact(dto.getContact());
            old.setCompany(dto.getCompany());
            userBuyerMapper.updateById(old);
            return old.getToken();
        }
        List<UserBuyer> oldList = userBuyerMapper.selectList(new LambdaQueryWrapper<UserBuyer>().eq(UserBuyer::getName, dto.getName()));
        List<UserBuyer> oldList2 = userBuyerMapper.selectList(new LambdaQueryWrapper<UserBuyer>().eq(UserBuyer::getMobile, dto.getMobile()));
        if(oldList!=null&&oldList.size()>0){
            throw new BizException("您添加的物流商已存在");
        }
        if(oldList2!=null&&oldList2.size()>0){
            throw new BizException("您添加的物流商已存在");
        }
        Calendar cache = Calendar.getInstance();
        cache.add(Calendar.YEAR, 1);
        dto.setTokentime(cache.getTime());
        dto.setCreatetime(new Date());
        dto.setToken(UUID.randomUUID().toString());
        userBuyerMapper.insert(dto);
        return dto.getToken();
    }

    @Override
    public Boolean updatesupplier(UserSupplier dto) {
        this.userSupplierMapper.updateById(dto);
        return true;
    }

    @Override
    public Boolean deleteSupplier(String id) {
        UserSupplier entity = this.userSupplierMapper.selectById(id);
        if(entity!=null){
            this.userSupplierMapper.deleteById(entity);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public Boolean deleteSupplierPrice(String orderid,String supplierid) {
        return quoteOrderSupplierMapper.delete(new LambdaQueryWrapper<QuoteOrderSupplier>()
                .eq(QuoteOrderSupplier::getOrderid, orderid)
                .eq(QuoteOrderSupplier::getSupplierid, supplierid))>0;
    }

    @Override
    public Boolean updateShipmentRemark(String shipmentid, String remark) {
        Shipment shipment = shipmentMapper.selectById(shipmentid);
        shipment.setRemark(remark);
        return shipmentMapper.updateById(shipment)>0;
    }

    @Override
    public Boolean updateOrderRemark(String orderid, String remark) {
        QuoteOrder old = this.baseMapper.selectById(orderid);
        old.setRemark(remark);
        return this.baseMapper.updateById(old)>0;
    }

    @Override
    public Boolean deleteShipment(List<String> shipmentIds) {
        for(String shipmentId:shipmentIds){
            Shipment old = shipmentMapper.selectById(shipmentId);
            old.setStatus(6);
            this.shipmentMapper.updateById(old);
        }
        return true;
    }

    @Override
    public Boolean updateOrderSupplierBase(UserSupplier supplier, String orderId, String base) {
        LambdaQueryWrapper<QuoteOrderSupplier> query = new LambdaQueryWrapper<QuoteOrderSupplier>();
        query.eq(QuoteOrderSupplier::getOrderid, orderId);
        query.eq(QuoteOrderSupplier::getSupplierid, supplier.getId());
        QuoteOrderSupplier old = this.quoteOrderSupplierMapper.selectOne(query);
        if(StringUtils.isNotBlank(base)){
            old.setBase(new BigDecimal(base.trim()));
        }
        return this.quoteOrderSupplierMapper.update(old,query)>0;
    }

    @Override
    public Boolean updateOrderStatus(String orderid, String status) {
        QuoteOrder order = this.baseMapper.selectById(orderid);
        order.setStatus(Integer.parseInt(status));
        return this.baseMapper.updateById(order)>0;
    }

    @Override
    public Boolean deleteOrderShipment(String orderid, String shipmentid) {
        LambdaQueryWrapper<QuoteOrderShipment> query = new LambdaQueryWrapper<QuoteOrderShipment>().eq(QuoteOrderShipment::getOrderid, orderid)
                                                                                                   .eq(QuoteOrderShipment::getShipmentid, shipmentid);
        return quoteOrderShipmentMapper.delete(query)>0;
    }

    @Override
    public Boolean addOrderShipment(List<QuoteOrderShipment> list) {
        for(QuoteOrderShipment item:list){
            LambdaQueryWrapper<QuoteOrderShipment> query = new LambdaQueryWrapper<QuoteOrderShipment>().eq(QuoteOrderShipment::getOrderid, item.getOrderid())
                    .eq(QuoteOrderShipment::getShipmentid, item.getShipmentid());
            Shipment shipment = shipmentMapper.selectById(item.getShipmentid());
            if(shipment!=null&&shipment.getStatus()==0){
                shipment.setStatus(1);
                shipmentMapper.updateById(shipment);
            }
            if(quoteOrderShipmentMapper.selectOne(query)==null){
                quoteOrderShipmentMapper.insert(item);
            }
        }
        return true;
    }

}
