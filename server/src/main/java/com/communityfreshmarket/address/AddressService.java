package com.communityfreshmarket.address;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.address.dto.AddressResponse;
import com.communityfreshmarket.address.dto.AddressSaveRequest;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.UserAddress;
import com.communityfreshmarket.mapper.UserAddressMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AddressService {

  private final UserAddressMapper userAddressMapper;

  public AddressService(UserAddressMapper userAddressMapper) {
    this.userAddressMapper = userAddressMapper;
  }

  public List<AddressResponse> list() {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
            .eq(UserAddress::getUserId, userId)
            .eq(UserAddress::getStoreId, storeId)
            .orderByDesc(UserAddress::getIsDefault)
            .orderByDesc(UserAddress::getUpdatedAt))
        .stream()
        .map(AddressResponse::from)
        .toList();
  }

  public AddressResponse get(long id) {
    return AddressResponse.from(requireOwned(id));
  }

  @Transactional
  public AddressResponse create(AddressSaveRequest request) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || countAddresses(userId, storeId) == 0;

    LocalDateTime now = LocalDateTime.now();
    UserAddress address = new UserAddress();
    address.setUserId(userId);
    address.setStoreId(storeId);
    applyFields(address, request);
    address.setIsDefault(makeDefault);
    address.setCreatedAt(now);
    address.setUpdatedAt(now);
    if (makeDefault) {
      clearDefault(userId, storeId);
    }
    userAddressMapper.insert(address);
    return AddressResponse.from(address);
  }

  @Transactional
  public AddressResponse update(long id, AddressSaveRequest request) {
    UserAddress address = requireOwned(id);
    applyFields(address, request);
    if (Boolean.TRUE.equals(request.getIsDefault())) {
      clearDefault(address.getUserId(), address.getStoreId());
      address.setIsDefault(true);
    } else if (Boolean.FALSE.equals(request.getIsDefault())) {
      address.setIsDefault(false);
    }
    address.setUpdatedAt(LocalDateTime.now());
    userAddressMapper.updateById(address);
    return AddressResponse.from(address);
  }

  @Transactional
  public AddressResponse setDefault(long id) {
    UserAddress address = requireOwned(id);
    clearDefault(address.getUserId(), address.getStoreId());
    address.setIsDefault(true);
    address.setUpdatedAt(LocalDateTime.now());
    userAddressMapper.updateById(address);
    return AddressResponse.from(address);
  }

  @Transactional
  public void delete(long id) {
    UserAddress address = requireOwned(id);
    boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
    userAddressMapper.deleteById(id);
    if (wasDefault) {
      UserAddress next = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
          .eq(UserAddress::getUserId, address.getUserId())
          .eq(UserAddress::getStoreId, address.getStoreId())
          .orderByDesc(UserAddress::getUpdatedAt)
          .last("LIMIT 1"));
      if (next != null) {
        next.setIsDefault(true);
        next.setUpdatedAt(LocalDateTime.now());
        userAddressMapper.updateById(next);
      }
    }
  }

  public UserAddress requireOwned(long id) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    UserAddress address = userAddressMapper.selectById(id);
    if (address == null
        || !address.getUserId().equals(userId)
        || !address.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "地址不存在");
    }
    return address;
  }

  private long countAddresses(long userId, long storeId) {
    Long count = userAddressMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
        .eq(UserAddress::getUserId, userId)
        .eq(UserAddress::getStoreId, storeId));
    return count == null ? 0L : count;
  }

  private void clearDefault(long userId, long storeId) {
    userAddressMapper.update(
        null,
        new LambdaUpdateWrapper<UserAddress>()
            .eq(UserAddress::getUserId, userId)
            .eq(UserAddress::getStoreId, storeId)
            .eq(UserAddress::getIsDefault, true)
            .set(UserAddress::getIsDefault, false)
            .set(UserAddress::getUpdatedAt, LocalDateTime.now()));
  }

  private static void applyFields(UserAddress address, AddressSaveRequest request) {
    address.setPhone(request.getPhone().trim());
    address.setCommunity(request.getCommunity().trim());
    address.setBuilding(blankToNull(request.getBuilding()));
    address.setRoom(blankToNull(request.getRoom()));
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
