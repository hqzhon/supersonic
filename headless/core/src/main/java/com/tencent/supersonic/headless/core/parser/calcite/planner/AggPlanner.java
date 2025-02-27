package com.tencent.supersonic.headless.core.parser.calcite.planner;


import com.tencent.supersonic.headless.api.enums.AggOption;
import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.core.parser.calcite.Configuration;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.parser.calcite.schema.HeadlessSchema;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SchemaBuilder;
import com.tencent.supersonic.headless.core.parser.calcite.sql.Renderer;
import com.tencent.supersonic.headless.core.parser.calcite.sql.TableView;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.DataSourceNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.SemanticNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.render.FilterRender;
import com.tencent.supersonic.headless.core.parser.calcite.sql.render.OutputRender;
import com.tencent.supersonic.headless.core.parser.calcite.sql.render.SourceRender;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Stack;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlValidatorScope;

/**
 * parsing from query dimensions and metrics
 */
@Slf4j
public class AggPlanner implements Planner {

    private MetricQueryReq metricReq;
    private HeadlessSchema schema;
    private SqlValidatorScope scope;
    private Stack<TableView> dataSets = new Stack<>();
    private SqlNode parserNode;
    private String sourceId;
    private boolean isAgg = false;
    private AggOption aggOption = AggOption.DEFAULT;

    public AggPlanner(HeadlessSchema schema) {
        this.schema = schema;
    }

    public void parse() throws Exception {
        // find the match Datasource
        scope = SchemaBuilder.getScope(schema);
        List<DataSource> datasource = getMatchDataSource(scope);
        if (datasource == null || datasource.isEmpty()) {
            throw new Exception("datasource not found");
        }
        isAgg = getAgg(datasource.get(0));
        sourceId = String.valueOf(datasource.get(0).getSourceId());

        // build  level by level
        LinkedList<Renderer> builders = new LinkedList<>();
        builders.add(new SourceRender());
        builders.add(new FilterRender());
        builders.add(new OutputRender());
        ListIterator<Renderer> it = builders.listIterator();
        int i = 0;
        Renderer previous = null;
        while (it.hasNext()) {
            Renderer renderer = it.next();
            if (previous != null) {
                previous.render(metricReq, datasource, scope, schema, !isAgg);
                renderer.setTable(previous.builderAs(DataSourceNode.getNames(datasource) + "_" + String.valueOf(i)));
                i++;
            }
            previous = renderer;
        }
        builders.getLast().render(metricReq, datasource, scope, schema, !isAgg);
        parserNode = builders.getLast().builder();


    }

    private List<DataSource> getMatchDataSource(SqlValidatorScope scope) throws Exception {
        return DataSourceNode.getMatchDataSources(scope, schema, metricReq);
    }

    private boolean getAgg(DataSource dataSource) {
        if (!AggOption.DEFAULT.equals(aggOption)) {
            return AggOption.isAgg(aggOption);
        }
        // default by dataSource time aggregation
        if (Objects.nonNull(dataSource.getAggTime()) && !dataSource.getAggTime().equalsIgnoreCase(
                Constants.DIMENSION_TYPE_TIME_GRANULARITY_NONE)) {
            if (!metricReq.isNativeQuery()) {
                return true;
            }
        }
        return isAgg;
    }

    @Override
    public void explain(QueryStatement queryStatement, AggOption aggOption) throws Exception {
        this.metricReq = queryStatement.getMetricReq();
        if (metricReq.getMetrics() == null) {
            metricReq.setMetrics(new ArrayList<>());
        }
        if (metricReq.getDimensions() == null) {
            metricReq.setDimensions(new ArrayList<>());
        }
        if (metricReq.getLimit() == null) {
            metricReq.setLimit(0L);
        }
        this.aggOption = aggOption;
        // build a parse Node
        parse();
        // optimizer
        optimize();
    }

    @Override
    public String getSql() {
        return SemanticNode.getSql(parserNode);
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public String simplify(String sql) {
        return optimize(sql);
    }

    public void optimize() {
        if (Objects.isNull(schema.getRuntimeOptions()) || Objects.isNull(schema.getRuntimeOptions().getEnableOptimize())
                || !schema.getRuntimeOptions().getEnableOptimize()) {
            return;
        }
        SqlNode optimizeNode = optimizeSql(SemanticNode.getSql(parserNode));
        if (Objects.nonNull(optimizeNode)) {
            parserNode = optimizeNode;
        }
    }

    public String optimize(String sql) {
        try {
            SqlNode sqlNode = SqlParser.create(sql, Configuration.getParserConfig()).parseStmt();
            if (Objects.nonNull(sqlNode)) {
                return SemanticNode.getSql(SemanticNode.optimize(scope, schema, sqlNode));
            }
        } catch (Exception e) {
            log.error("optimize error {}", e);
        }
        return "";
    }

    private SqlNode optimizeSql(String sql) {
        try {
            log.info("before optimize:[{}]", sql);
            SqlNode sqlNode = SqlParser.create(sql, Configuration.getParserConfig()).parseStmt();
            if (Objects.nonNull(sqlNode)) {
                return SemanticNode.optimize(scope, schema, sqlNode);
            }
        } catch (Exception e) {
            log.error("optimize error {}", e);
        }
        return null;
    }
}