package com.tencent.supersonic.integration.plugin;

import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.core.query.plugin.WebBase;
import com.tencent.supersonic.chat.core.query.plugin.webpage.WebPageQuery;
import com.tencent.supersonic.chat.core.query.plugin.webpage.WebPageResp;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneLauncher.class)
@ActiveProfiles("local")
@Slf4j
public class BasePluginTest {

    protected void assertPluginRecognizeResult(QueryResult queryResult) {
        Assert.assertEquals(queryResult.getQueryState(), QueryState.SUCCESS);
        Assert.assertEquals(queryResult.getQueryMode(), WebPageQuery.QUERY_MODE);
        WebPageResp webPageResponse = (WebPageResp) queryResult.getResponse();
        WebBase webPage = webPageResponse.getWebPage();
        Assert.assertEquals(webPage.getUrl(), "www.yourbi.com");
        Assert.assertEquals(1, webPage.getParams().size());
        Assert.assertEquals("alice", webPage.getParams().get(0).getValue());
    }

}
