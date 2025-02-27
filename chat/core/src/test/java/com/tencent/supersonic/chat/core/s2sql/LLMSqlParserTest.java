package com.tencent.supersonic.chat.core.s2sql;

import static org.mockito.Mockito.when;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaValueMap;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LLMSqlParserTest {

    @Test
    void setFilter() {
        SemanticSchema mockSemanticSchema = Mockito.mock(SemanticSchema.class);

        List<SchemaElement> dimensions = new ArrayList<>();
        List<SchemaValueMap> schemaValueMaps = new ArrayList<>();
        SchemaValueMap value1 = new SchemaValueMap();
        value1.setBizName("杰伦");
        value1.setTechName("周杰伦");
        value1.setAlias(Arrays.asList("周杰倫", "Jay Chou", "周董", "周先生"));
        schemaValueMaps.add(value1);

        SchemaElement schemaElement = SchemaElement.builder()
                .bizName("singer_name")
                .name("歌手名")
                .model(2L)
                .schemaValueMaps(schemaValueMaps)
                .build();
        dimensions.add(schemaElement);

        SchemaElement schemaElement2 = SchemaElement.builder()
                .bizName("publish_time")
                .name("发布时间")
                .model(2L)
                .build();
        dimensions.add(schemaElement2);

        when(mockSemanticSchema.getDimensions()).thenReturn(dimensions);

        List<SchemaElement> metrics = new ArrayList<>();
        SchemaElement metric = SchemaElement.builder()
                .bizName("play_count")
                .name("播放量")
                .model(2L)
                .build();
        metrics.add(metric);

        when(mockSemanticSchema.getMetrics()).thenReturn(metrics);
    }
}