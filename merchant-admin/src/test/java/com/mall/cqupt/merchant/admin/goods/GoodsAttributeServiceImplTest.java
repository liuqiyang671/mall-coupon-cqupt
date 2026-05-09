package com.mall.cqupt.merchant.admin.goods;

import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsAttributeDO;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsAttributeMapper;
import com.mall.cqupt.merchant.admin.dto.req.GoodsAttributeSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsAttributeRespDTO;
import com.mall.cqupt.merchant.admin.service.impl.GoodsAttributeServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoodsAttributeServiceImplTest {

    @Mock
    private GoodsAttributeMapper goodsAttributeMapper;

    private GoodsAttributeServiceImpl goodsAttributeService;

    @BeforeEach
    void setUp() {
        goodsAttributeService = new GoodsAttributeServiceImpl(goodsAttributeMapper);
        UserContext.setUser(UserInfoDTO.builder()
                .userId("1")
                .username("platform")
                .roleType(UserRoleEnum.PLATFORM.getType())
                .build());
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void createAttributeRejectsSingleChoiceWithoutValues() {
        GoodsAttributeSaveReqDTO request = new GoodsAttributeSaveReqDTO();
        request.setName("color");
        request.setInputType(1);

        assertThrows(ClientException.class, () -> goodsAttributeService.createAttribute(request));
    }

    @Test
    void createAttributeInsertsEnabledAttribute() {
        GoodsAttributeSaveReqDTO request = new GoodsAttributeSaveReqDTO();
        request.setName("color");
        request.setInputType(1);
        request.setValues("red,blue");
        request.setSortOrder(1);

        goodsAttributeService.createAttribute(request);

        ArgumentCaptor<GoodsAttributeDO> captor = ArgumentCaptor.forClass(GoodsAttributeDO.class);
        verify(goodsAttributeMapper).insert(captor.capture());
        assertEquals("color", captor.getValue().getName());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void updateAttributeRejectsMissingAttribute() {
        GoodsAttributeSaveReqDTO request = new GoodsAttributeSaveReqDTO();
        request.setName("size");
        when(goodsAttributeMapper.selectById("10")).thenReturn(null);

        assertThrows(ClientException.class, () -> goodsAttributeService.updateAttribute("10", request));
    }

    @Test
    void listAllAttributesMapsEnabledAttributesToResponse() {
        when(goodsAttributeMapper.selectList(any())).thenReturn(List.of(GoodsAttributeDO.builder()
                .id(1L)
                .name("color")
                .inputType(1)
                .values("red,blue")
                .sortOrder(1)
                .status(0)
                .delFlag(0)
                .build()));

        List<GoodsAttributeRespDTO> attributes = goodsAttributeService.listAllAttributes();

        assertEquals(1, attributes.size());
        assertEquals("color", attributes.get(0).getName());
        assertEquals("red,blue", attributes.get(0).getValues());
    }
}
