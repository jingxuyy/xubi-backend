package com.xu.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xu.springbootinit.annotation.AuthCheck;
import com.xu.springbootinit.bizmq.BiMessageProducer;
import com.xu.springbootinit.common.BaseResponse;
import com.xu.springbootinit.common.DeleteRequest;
import com.xu.springbootinit.common.ErrorCode;
import com.xu.springbootinit.common.ResultUtils;
import com.xu.springbootinit.constant.CommonConstant;
import com.xu.springbootinit.constant.UserConstant;
import com.xu.springbootinit.exception.BusinessException;
import com.xu.springbootinit.exception.ThrowUtils;
import com.xu.springbootinit.manager.AiManager;
import com.xu.springbootinit.manager.RedisLimiterManager;
import com.xu.springbootinit.mapper.ChartDataMapper;
import com.xu.springbootinit.model.dto.chart.*;
import com.xu.springbootinit.model.entity.Chart;
import com.xu.springbootinit.model.entity.User;
import com.xu.springbootinit.model.vo.BiResponse;
import com.xu.springbootinit.service.ChartService;
import com.xu.springbootinit.service.UserService;
import com.xu.springbootinit.utils.ExcelUtils;
import com.xu.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;


    @Resource
    private ChartDataMapper chartDataMapper;


    @Resource
    private RedisLimiterManager redisLimiterManager;


    @Resource
    private ThreadPoolExecutor threadPoolExecutor;


    @Resource
    private BiMessageProducer biMessageProducer;

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long  id= deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage2(@RequestBody ChartQueryRequest chartQueryRequest, HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest, request));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();

        User loginUser = userService.getLoginUser(request);

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest, request));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest, request));
        return ResultUtils.success(chartPage);
    }


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }


    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest, HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            return null;
        }

        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
//        Long userId = chartQueryRequest.getUserId();
        Long userId = loginUser.getId();
        String name = chartQueryRequest.getName();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id>0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", goal);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(true, false,"createTime");
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_DESC),
                sortField);
        return queryWrapper;
    }



    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
         String name = genChartByAiRequest.getName();
         String goal = genChartByAiRequest.getGoal();
         String chartType = genChartByAiRequest.getChartType();
         // 校验
         ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, CommonConstant.INPUT_CHECK_CONTENT);
         ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > CommonConstant.INPUT_CHECK_LENGTH, ErrorCode.PARAMS_ERROR, CommonConstant.INPUT_CHECK_NAME);

         // 文件校验
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验大小
        ThrowUtils.throwIf(size > CommonConstant.FILE_CHECK_SIZE, ErrorCode.PARAMS_ERROR, CommonConstant.FILE_CHECK_LARGE);
        // 校验后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!CommonConstant.FILE_CHECK_SUFFIX.equals(suffix), ErrorCode.PARAMS_ERROR, CommonConstant.FILE_CHECK_SUFFIX_ERROR);

        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit(CommonConstant.LIMITER_KEY + loginUser.getId());


        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        // 拼接分析目标
        String userGoal = goal;
        if(StringUtils.isNotBlank(chartType)){
            // 图标类型不为空，则把图标类型拼接上
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");

        // 先把图表信息保存到数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setStatus(CommonConstant.CHART_STATUS_WAIT);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 分表
        String[] dataLine = csvData.split("\n");
        String[] tableHeader = dataLine[0].split(",");
        // 创建表名
        String tableName = "chart_"+chart.getId();
        chartDataMapper.createTable(tableName, tableHeader);

        // 插入数据
        for (int i = 1; i < dataLine.length; i++) {
            String[] data = dataLine[i].split(",");
            chartDataMapper.insertData(tableName, data);
        }
        // 将表名设置到chart表的chartData列
        chart.setChartData(tableName);
        chartService.updateById(chart);

        // 异步调用AI
        CompletableFuture.runAsync(()->{
            // 将图表中的状态修改为执行中 running
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(CommonConstant.CHART_STATUS_RUNNING);
            boolean b = chartService.updateById(updateChart);
            if(!b){
                handleChartUpdateError(chart.getId(), "更新图表执行状态失败");
                return;
            }

            // 调用AI 拿到返回结果
//            String result = aiManager.doChat(CommonConstant.MODER_ID, userInput.toString());
            String result = aiManager.doChart(userInput.toString());
            // 对结果进行处理
            String[] split = result.split("#####");
            // 拆分校验
            if(split.length < 3){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
            }

            String genChart = split[1].trim();
            String genResult = split[2].trim();

            // AI调用完成，再更新一次
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(CommonConstant.CHART_STATUS_SUCCEED);
            boolean updateResult = chartService.updateById(updateChartResult);
            if(!updateResult){
                handleChartUpdateError(chart.getId() , "更新图表成功状态失败");
            }
        }, threadPoolExecutor);



        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());


        return ResultUtils.success(biResponse);
    }

    private void handleChartUpdateError(Long id, String message) {
        Chart chart = new Chart();
        chart.setId(id);
        chart.setStatus(CommonConstant.CHART_STATUS_FAILED);
        chart.setExecMessage(message);
        boolean b = chartService.updateById(chart);
        if(!b){
            log.error("更新图表状态失败"+id + "," +message);
        }
    }


    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, CommonConstant.INPUT_CHECK_CONTENT);
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > CommonConstant.INPUT_CHECK_LENGTH, ErrorCode.PARAMS_ERROR, CommonConstant.INPUT_CHECK_NAME);

        // 文件校验
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验大小
        ThrowUtils.throwIf(size > CommonConstant.FILE_CHECK_SIZE
                , ErrorCode.PARAMS_ERROR, CommonConstant.FILE_CHECK_LARGE);
        // 校验后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!CommonConstant.FILE_CHECK_SUFFIX.equals(suffix), ErrorCode.PARAMS_ERROR, CommonConstant.FILE_CHECK_SUFFIX_ERROR);

        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit(CommonConstant.LIMITER_KEY + loginUser.getId());


        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        // 拼接分析目标
        String userGoal = goal;
        if(StringUtils.isNotBlank(chartType)){
            // 图标类型不为空，则把图标类型拼接上
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");

        // 拿到返回结果
//        String result = aiManager.doChat(CommonConstant.MODER_ID, userInput.toString());
        String result = aiManager.doChart(userInput.toString());
        System.out.println(result);
        // 对结果进行处理
        String[] split = result.split("#####");
        // 拆分校验
        if(split.length < 3){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }

        String genChart = split[1].trim();
        String genResult = split[2].trim();



        // 插入到数据库, 并把原始数据设置为null
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);

        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());

        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        String[] dataLine = csvData.split("\n");
        String[] tableHeader = dataLine[0].split(",");
        // 创建表名
        String tableName = "chart_"+chart.getId();
        chartDataMapper.createTable(tableName, tableHeader);

        // 插入数据
        for (int i = 1; i < dataLine.length; i++) {
            String[] data = dataLine[i].split(",");
            chartDataMapper.insertData(tableName, data);
        }
        // 将表名设置到chart表的chartData列
        chart.setChartData(tableName);
        chart.setStatus(CommonConstant.CHART_STATUS_SUCCEED);

        chartService.updateById(chart);


        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());


        return ResultUtils.success(biResponse);
    }



    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, CommonConstant.INPUT_CHECK_CONTENT);
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > CommonConstant.INPUT_CHECK_LENGTH, ErrorCode.PARAMS_ERROR, CommonConstant.INPUT_CHECK_NAME);

        // 文件校验
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验大小
        ThrowUtils.throwIf(size > CommonConstant.FILE_CHECK_SIZE
                , ErrorCode.PARAMS_ERROR, CommonConstant.FILE_CHECK_LARGE);
        // 校验后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(!CommonConstant.FILE_CHECK_SUFFIX.equals(suffix), ErrorCode.PARAMS_ERROR, CommonConstant.FILE_CHECK_SUFFIX_ERROR);

        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit(CommonConstant.LIMITER_KEY + loginUser.getId());


        String csvData = ExcelUtils.excelToCsv(multipartFile);

        // 先把图表信息保存到数据库中
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setStatus(CommonConstant.CHART_STATUS_WAIT);
        chart.setChartData(csvData);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");


        biMessageProducer.sendMessage(chart.getId().toString());

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());


        return ResultUtils.success(biResponse);
    }





    @PostMapping("/chat")
    public BaseResponse<BiResponse> getChartByAi(@RequestBody String message, HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit(CommonConstant.LIMITER_KEY + loginUser.getId());

        String requestMessage = aiManager.doChartByListener(message);

        BiResponse biResponse = new BiResponse();
        biResponse.setGenResult(requestMessage);

        return ResultUtils.success(biResponse);
    }




}
