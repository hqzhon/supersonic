package com.tencent.supersonic.headless.api.enums;


public enum EngineType {

    TDW(0, "tdw"),
    MYSQL(1, "mysql"),
    DORIS(2, "doris"),
    CLICKHOUSE(3, "clickhouse"),
    KAFKA(4, "kafka"),
    H2(5, "h2"),
    POSTGRESQL(6, "postgresql");


    private Integer code;

    private String name;

    EngineType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
