package com.tencent.supersonic.headless.server.aspect;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.headless.api.request.QueryS2SQLReq;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.server.utils.QueryStructUtils;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.MINUS;

@Component
@Aspect
@Order(1)
@Slf4j
public class S2SQLDataAspect extends AuthCheckBaseAspect {

    @Autowired
    private QueryStructUtils queryStructUtils;
    @Autowired
    private DimensionService dimensionService;
    @Autowired
    private ModelService modelService;
    @Value("${permission.data.enable:true}")
    private Boolean permissionDataEnable;

    @Pointcut("@annotation(com.tencent.supersonic.headless.server.annotation.S2SQLDataPermission)")
    private void s2SQLPermissionCheck() {
    }

    @Around("s2SQLPermissionCheck()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("s2SQL permission check!");
        Object[] objects = joinPoint.getArgs();
        QueryS2SQLReq queryS2SQLReq = (QueryS2SQLReq) objects[0];
        User user = (User) objects[1];
        if (!permissionDataEnable) {
            log.info("not to check s2SQL permission!");
            return joinPoint.proceed();
        }
        if (Objects.isNull(user) || Strings.isNullOrEmpty(user.getName())) {
            throw new RuntimeException("please provide user information");
        }
        List<Long> modelIds = queryS2SQLReq.getModelIds();

        //1. determine whether admin of the model
        if (doModelAdmin(user, modelIds)) {
            log.info("determine whether admin of the model!");
            return joinPoint.proceed();
        }
        // 2. determine whether the subject field is visible
        doModelVisible(user, modelIds);
        // 3. fetch data permission meta information
        Set<String> res4Privilege = queryStructUtils.getResNameEnExceptInternalCol(queryS2SQLReq, user);
        log.info("modelId:{}, res4Privilege:{}", modelIds, res4Privilege);

        Set<String> sensitiveResByModel = getHighSensitiveColsByModelId(modelIds);
        Set<String> sensitiveResReq = res4Privilege.parallelStream()
                .filter(sensitiveResByModel::contains).collect(Collectors.toSet());
        log.info("this query domainId:{}, sensitiveResReq:{}", modelIds, sensitiveResReq);

        // query user privilege info
        AuthorizedResourceResp authorizedResource = getAuthorizedResource(user, modelIds, sensitiveResReq);
        // get sensitiveRes that user has privilege
        Set<String> resAuthSet = getAuthResNameSet(authorizedResource, modelIds);

        // 4.if sensitive fields without permission are involved in filter, thrown an exception
        doFilterCheckLogic(queryS2SQLReq, resAuthSet, sensitiveResReq);

        // 5.row permission pre-filter
        doRowPermission(queryS2SQLReq, authorizedResource);

        // 6.proceed
        QueryResultWithSchemaResp queryResultWithColumns = (QueryResultWithSchemaResp) joinPoint.proceed();

        if (CollectionUtils.isEmpty(sensitiveResReq) || allSensitiveResReqIsOk(sensitiveResReq, resAuthSet)) {
            // if sensitiveRes is empty
            log.info("sensitiveResReq is empty");
            return getQueryResultWithColumns(queryResultWithColumns, modelIds, authorizedResource);
        }

        // 6.if the column has no permission, hit *
        Set<String> need2Apply = sensitiveResReq.stream().filter(req -> !resAuthSet.contains(req))
                .collect(Collectors.toSet());
        log.info("need2Apply:{},sensitiveResReq:{},resAuthSet:{}", need2Apply, sensitiveResReq, resAuthSet);
        QueryResultWithSchemaResp queryResultAfterDesensitization =
                desensitizationData(queryResultWithColumns, need2Apply);
        addPromptInfoInfo(modelIds, queryResultAfterDesensitization, authorizedResource, need2Apply);

        return queryResultAfterDesensitization;
    }

    private void doRowPermission(QueryS2SQLReq queryS2SQLReq, AuthorizedResourceResp authorizedResource) {
        log.debug("start doRowPermission logic");
        StringJoiner joiner = new StringJoiner(" OR ");
        List<String> dimensionFilters = new ArrayList<>();
        if (!CollectionUtils.isEmpty(authorizedResource.getFilters())) {
            authorizedResource.getFilters().stream()
                    .forEach(filter -> dimensionFilters.addAll(filter.getExpressions()));
        }

        if (CollectionUtils.isEmpty(dimensionFilters)) {
            log.debug("dimensionFilters is empty");
            return;
        }

        dimensionFilters.stream().forEach(filter -> {
            if (StringUtils.isNotEmpty(filter) && StringUtils.isNotEmpty(filter.trim())) {
                joiner.add(" ( " + filter + " ) ");
            }
        });
        try {
            Expression expression = CCJSqlParserUtil.parseCondExpression(" ( " + joiner + " ) ");
            if (StringUtils.isNotEmpty(joiner.toString())) {
                String sql = SqlParserAddHelper.addWhere(queryS2SQLReq.getSql(), expression);
                log.info("before doRowPermission, queryS2SQLReq:{}", queryS2SQLReq.getSql());
                queryS2SQLReq.setSql(sql);
                log.info("after doRowPermission, queryS2SQLReq:{}", queryS2SQLReq.getSql());
            }
        } catch (JSQLParserException jsqlParserException) {
            log.info("jsqlParser has an exception:{}", jsqlParserException.toString());
        }

    }

    private void doFilterCheckLogic(QueryS2SQLReq queryS2SQLReq, Set<String> resAuthName,
            Set<String> sensitiveResReq) {
        Set<String> resFilterSet = queryStructUtils.getFilterResNameEnExceptInternalCol(queryS2SQLReq);
        Set<String> need2Apply = resFilterSet.stream()
                .filter(res -> !resAuthName.contains(res) && sensitiveResReq.contains(res)).collect(Collectors.toSet());
        Set<String> nameCnSet = new HashSet<>();

        List<Long> modelIds = Lists.newArrayList(queryS2SQLReq.getModelIds());
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setModelIds(modelIds);
        List<ModelResp> modelInfos = modelService.getModelList(modelFilter);
        String modelNameCn = Constants.EMPTY;
        if (!CollectionUtils.isEmpty(modelInfos)) {
            modelNameCn = modelInfos.get(0).getName();
        }
        MetaFilter metaFilter = new MetaFilter(modelIds);
        List<DimensionResp> dimensionDescList = dimensionService.getDimensions(metaFilter);
        String finalDomainNameCn = modelNameCn;
        dimensionDescList.stream().filter(dim -> need2Apply.contains(dim.getBizName()))
                .forEach(dim -> nameCnSet.add(finalDomainNameCn + MINUS + dim.getName()));

        if (!CollectionUtils.isEmpty(need2Apply)) {
            ModelResp modelResp = modelInfos.get(0);
            List<String> admins = modelService.getModelAdmin(modelResp.getId());
            log.info("in doFilterLogic, need2Apply:{}", need2Apply);
            String message = String.format("您没有以下维度%s权限, 请联系管理员%s开通", nameCnSet, admins);
            throw new InvalidPermissionException(message);
        }
    }
}
