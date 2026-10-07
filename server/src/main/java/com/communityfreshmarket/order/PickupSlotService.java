package com.communityfreshmarket.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.PickupSlot;
import com.communityfreshmarket.mapper.PickupSlotMapper;
import com.communityfreshmarket.order.dto.PickupSlotResponse;
import com.communityfreshmarket.order.dto.PickupSlotSaveRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickupSlotService {

  private final PickupSlotMapper pickupSlotMapper;

  public PickupSlotService(PickupSlotMapper pickupSlotMapper) {
    this.pickupSlotMapper = pickupSlotMapper;
  }

  public List<PickupSlotResponse> listEnabled() {
    long storeId = UserContext.storeId();
    return pickupSlotMapper.selectList(new LambdaQueryWrapper<PickupSlot>()
            .eq(PickupSlot::getStoreId, storeId)
            .eq(PickupSlot::getEnabled, true)
            .orderByAsc(PickupSlot::getSortOrder)
            .orderByAsc(PickupSlot::getId))
        .stream()
        .map(PickupSlotResponse::from)
        .toList();
  }

  public List<PickupSlotResponse> listForMerchant() {
    long storeId = UserContext.storeId();
    return pickupSlotMapper.selectList(new LambdaQueryWrapper<PickupSlot>()
            .eq(PickupSlot::getStoreId, storeId)
            .orderByAsc(PickupSlot::getSortOrder)
            .orderByAsc(PickupSlot::getId))
        .stream()
        .map(PickupSlotResponse::from)
        .toList();
  }

  @Transactional
  public PickupSlotResponse create(PickupSlotSaveRequest request) {
    long storeId = UserContext.storeId();
    LocalDateTime now = LocalDateTime.now();
    PickupSlot slot = new PickupSlot();
    slot.setStoreId(storeId);
    slot.setLabel(request.getLabel().trim());
    slot.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    slot.setEnabled(request.getEnabled() == null || request.getEnabled());
    slot.setCreatedAt(now);
    slot.setUpdatedAt(now);
    pickupSlotMapper.insert(slot);
    return PickupSlotResponse.from(slot);
  }

  @Transactional
  public PickupSlotResponse update(long id, PickupSlotSaveRequest request) {
    PickupSlot slot = requireOwned(id);
    slot.setLabel(request.getLabel().trim());
    if (request.getSortOrder() != null) {
      slot.setSortOrder(request.getSortOrder());
    }
    if (request.getEnabled() != null) {
      slot.setEnabled(request.getEnabled());
    }
    slot.setUpdatedAt(LocalDateTime.now());
    pickupSlotMapper.updateById(slot);
    return PickupSlotResponse.from(slot);
  }

  @Transactional
  public void delete(long id) {
    PickupSlot slot = requireOwned(id);
    pickupSlotMapper.deleteById(slot.getId());
  }

  public PickupSlot requireEnabled(long id, long storeId) {
    PickupSlot slot = pickupSlotMapper.selectById(id);
    if (slot == null || !slot.getStoreId().equals(storeId) || !Boolean.TRUE.equals(slot.getEnabled())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "自提时段不可用");
    }
    return slot;
  }

  private PickupSlot requireOwned(long id) {
    long storeId = UserContext.storeId();
    PickupSlot slot = pickupSlotMapper.selectById(id);
    if (slot == null || !slot.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "自提时段不存在");
    }
    return slot;
  }
}

