package com.tencent.supersonic.headless.api.request;

import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
public class MetricBaseReq extends SchemaItem {

    private Long modelId;

    private String alias;

    private String dataFormatType;

    private DataFormat dataFormat;

    private List<String> tags;

    private RelateDimension relateDimension;

    private Map<String, Object> ext = new HashMap<>();

    public String getTag() {
        if (tags == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(tags)) {
            return "";
        }
        return StringUtils.join(tags, ",");
    }

}
