package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.request.DimensionReq;
import com.tencent.supersonic.headless.api.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;

import java.util.List;

public interface DimensionService {

    List<DimensionResp> getDimensions(MetaFilter metaFilter);

    DimensionResp getDimension(String bizName, Long modelId);

    DimensionResp getDimension(Long id);

    void batchUpdateStatus(MetaBatchReq metaBatchReq, User user);

    void createDimension(DimensionReq dimensionReq, User user) throws Exception;

    void createDimensionBatch(List<DimensionReq> dimensionReqs, User user) throws Exception;

    void updateDimension(DimensionReq dimensionReq, User user) throws Exception;

    PageInfo<DimensionResp> queryDimension(PageDimensionReq pageDimensionReq);

    void deleteDimension(Long id, User user);

    List<DimensionResp> getDimensionInModelCluster(Long modelId);

    List<String> mockAlias(DimensionReq dimensionReq, String mockType, User user);

    List<DimValueMap> mockDimensionValueAlias(DimensionReq dimensionReq, User user);

    void sendDimensionEventBatch(List<Long> modelIds, EventType eventType);
}
