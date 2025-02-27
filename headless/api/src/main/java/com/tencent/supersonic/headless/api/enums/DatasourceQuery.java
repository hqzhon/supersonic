package com.tencent.supersonic.headless.api.enums;

/**
 * model datasource define type:
 * sql_query : view sql begin as select
 * table_query: dbName.tableName
 */
public enum DatasourceQuery {

    SQL_QUERY("sql_query"),
    TABLE_QUERY("table_query");

    private String name;


    DatasourceQuery(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
