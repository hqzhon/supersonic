package com.tencent.supersonic.headless.api.request;

import com.tencent.supersonic.headless.api.enums.QueryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExplainSqlReq<T> {

    private QueryType queryTypeEnum;

    private T queryReq;
}
