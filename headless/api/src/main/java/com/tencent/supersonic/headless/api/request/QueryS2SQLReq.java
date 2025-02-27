package com.tencent.supersonic.headless.api.request;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.ToString;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@ToString
public class QueryS2SQLReq {

    private Set<Long> modelIds;

    private String sql;

    private Map<String, String> variables;

    public void setModelId(Long modelId) {
        modelIds = new HashSet<>();
        modelIds.add(modelId);
    }

    public List<Long> getModelIds() {
        return Lists.newArrayList(modelIds);
    }

    public String getModelIdStr() {
        return String.join(",", modelIds.stream().map(Object::toString).collect(Collectors.toList()));
    }

}
