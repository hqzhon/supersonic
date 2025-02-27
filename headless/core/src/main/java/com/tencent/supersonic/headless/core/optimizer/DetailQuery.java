package com.tencent.supersonic.headless.core.optimizer;

import com.google.common.base.Strings;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Remove the default metric added by the system when the query only has dimensions
 */
@Slf4j
@Component("DetailQuery")
public class DetailQuery implements QueryOptimizer {

    @Override
    public void rewrite(QueryStructReq queryStructCmd, QueryStatement queryStatement) {
        String sqlRaw = queryStatement.getSql().trim();
        if (Strings.isNullOrEmpty(sqlRaw)) {
            throw new RuntimeException("sql is empty or null");
        }
        log.debug("before handleNoMetric, sql:{}", sqlRaw);
        if (isDetailQuery(queryStructCmd)) {
            if (queryStructCmd.getMetrics().size() == 0 && !CollectionUtils.isEmpty(queryStructCmd.getGroups())) {
                String sqlForm = "select %s from ( %s ) src_no_metric";
                String sql = String.format(sqlForm, queryStructCmd.getGroups().stream().collect(
                        Collectors.joining(",")), sqlRaw);
                queryStatement.setSql(sql);
            }
        }
        log.debug("after handleNoMetric, sql:{}", queryStatement.getSql());
    }

    public boolean isDetailQuery(QueryStructReq queryStructCmd) {
        return Objects.nonNull(queryStructCmd) && queryStructCmd.getQueryType().isNativeAggQuery()
                && CollectionUtils.isEmpty(queryStructCmd.getMetrics());
    }
}
