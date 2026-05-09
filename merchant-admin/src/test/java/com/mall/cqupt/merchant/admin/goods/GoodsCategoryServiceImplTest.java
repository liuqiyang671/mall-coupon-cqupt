package com.mall.cqupt.merchant.admin.goods;

import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import com.mall.cqupt.merchant.admin.common.enums.GoodsCategoryStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsCategoryDO;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsCategoryMapper;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsMapper;
import com.mall.cqupt.merchant.admin.dto.req.GoodsCategorySaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsCategoryRespDTO;
import com.mall.cqupt.merchant.admin.service.impl.GoodsCategoryServiceImpl;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoodsCategoryServiceImplTest {

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @Mock
    private GoodsMapper goodsMapper;

    private GoodsCategoryServiceImpl goodsCategoryService;

    @BeforeEach
    void setUp() {
        goodsCategoryService = new GoodsCategoryServiceImpl(goodsCategoryMapper, goodsMapper);
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
    void createCategoryCreatesRootCategoryWhenParentIdIsMissing() {
        GoodsCategorySaveReqDTO request = new GoodsCategorySaveReqDTO();
        request.setName("digital");
        request.setSortOrder(1);
        when(goodsCategoryMapper.selectCount(any())).thenReturn(0L);

        goodsCategoryService.createCategory(request);

        ArgumentCaptor<GoodsCategoryDO> captor = ArgumentCaptor.forClass(GoodsCategoryDO.class);
        verify(goodsCategoryMapper).insert(captor.capture());
        assertEquals(0L, captor.getValue().getParentId());
        assertEquals(1, captor.getValue().getLevel());
        assertEquals(GoodsCategoryStatusEnum.ENABLED.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void createCategoryRejectsFourthLevelCategory() {
        GoodsCategorySaveReqDTO request = new GoodsCategorySaveReqDTO();
        request.setName("leaf");
        request.setParentId(10L);
        when(goodsCategoryMapper.selectById(10L)).thenReturn(GoodsCategoryDO.builder()
                .id(10L)
                .parentId(9L)
                .level(3)
                .delFlag(0)
                .build());

        assertThrows(ClientException.class, () -> goodsCategoryService.createCategory(request));
    }

    @Test
    void listCategoryTreeBuildsNestedChildren() {
        when(goodsCategoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, 0L, "root", 1),
                category(2L, 1L, "child", 2),
                category(3L, 2L, "leaf", 3)
        ));

        List<GoodsCategoryRespDTO> tree = goodsCategoryService.listCategoryTree();

        assertEquals(1, tree.size());
        assertEquals("root", tree.get(0).getName());
        assertEquals("child", tree.get(0).getChildren().get(0).getName());
        assertEquals("leaf", tree.get(0).getChildren().get(0).getChildren().get(0).getName());
    }

    @Test
    void deleteCategoryRejectsCategoryWithChildren() {
        when(goodsCategoryMapper.selectById("1")).thenReturn(category(1L, 0L, "root", 1));
        when(goodsCategoryMapper.selectCount(any())).thenReturn(1L);

        assertThrows(ClientException.class, () -> goodsCategoryService.deleteCategory("1"));
    }

    @Test
    void createCategoryRejectsUserWithoutCategoryPermission() {
        UserContext.removeUser();
        GoodsCategorySaveReqDTO request = new GoodsCategorySaveReqDTO();
        request.setName("digital");

        assertThrows(ClientException.class, () -> goodsCategoryService.createCategory(request));
        verifyNoInteractions(goodsCategoryMapper, goodsMapper);
    }

    private GoodsCategoryDO category(Long id, Long parentId, String name, Integer level) {
        return GoodsCategoryDO.builder()
                .id(id)
                .parentId(parentId)
                .name(name)
                .sortOrder(level)
                .level(level)
                .status(GoodsCategoryStatusEnum.ENABLED.getStatus())
                .delFlag(0)
                .build();
    }
}
