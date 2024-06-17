package com.aibi.springbootinit.controller;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.aibi.springbootinit.common.BaseResponse;
import com.aibi.springbootinit.common.ErrorCode;
import com.aibi.springbootinit.constant.UserConstant;
import com.aibi.springbootinit.exception.BusinessException;
import com.aibi.springbootinit.exception.ThrowUtils;
import com.aibi.springbootinit.model.dto.chart.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aibi.springbootinit.annotation.AuthCheck;
import com.aibi.springbootinit.bizmq.BiMessageProducer;
import com.aibi.springbootinit.common.DeleteRequest;
import com.aibi.springbootinit.common.ResultUtils;
import com.aibi.springbootinit.constant.CommonConstant;
import com.aibi.springbootinit.manager.AIManager;
import com.aibi.springbootinit.manager.RedisLimiterManager;
import com.aibi.springbootinit.model.dto.chart.*;
import com.aibi.springbootinit.model.entity.Chart;
import com.aibi.springbootinit.model.entity.User;
import com.aibi.springbootinit.model.vo.BIResponse;
import com.aibi.springbootinit.service.ChartService;
import com.aibi.springbootinit.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.aibi.springbootinit.utils.ExcelUtils;
import com.aibi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 请对自己写的shit moutain保持自信，如果无法生成图表，不是代码的问题是ai接口次数调用有限
 */

// todo 异步化competablefuture+自定义线程池
/**
 * 图表管理和生成
 */
@RestController
@RequestMapping("/chart")
@Slf4j
@Profile({"dev","local"})
@SuppressWarnings("ALL")
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AIManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;
    // region 增删改查
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addchart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newchartId = chart.getId();
        return ResultUtils.success(newchartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletechart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldchart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
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
    public BaseResponse<Boolean> updatechart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     * @throws FileNotFoundException
     */

    @PostMapping("/gen/async/mq")
    public BaseResponse<BIResponse> genChartByAIAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                        genChartByAIRequest genChartByAIRequest, HttpServletRequest request) throws FileNotFoundException {

        //获取用户输入以及文件
        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();
        //校验,只有登录的用户才能查询
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        User user = userService.getLoginUser(request);
        //校验文件大小和后缀
        Long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("csv", "xlsx", "xls");//后缀白名单
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
        //关于校验文件内容的合规性一般利用第三方。比如接入腾讯云的图片万象数据审核（COS对象的存储功能）
        //限流每个用户这个方法的限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + String.valueOf(user.getId()));
        //系统预设,在调用的接口模型已经有了    以及用户输入
        String csvData = ExcelUtils.excel2Csv(multipartFile);
        //保存用户输入的数据到数据库，再去调用ai
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(user.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表相关数据保存失败");

        long newChartId = chart.getId();
        //发送消息后直接返回然后就可以去处理别的业务，消费者消费完会自动返回保存最新的结果
        log.info("我开始发消息啦");
        biMessageProducer.sendMessage(String.valueOf(newChartId));
        //返回结果没有图表结论和图表信息了。
        BIResponse biResponse = new BIResponse();
        biResponse.setChartID(chart.getId());
        return ResultUtils.success(biResponse);
    }


    /**
     * 根据用户上传的文件和文本信息，通过AI生成图表
     */
    @PostMapping("/gen")
    public BaseResponse<BIResponse> genChartByAIAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      genChartByAIRequest genChartByAIRequest, HttpServletRequest request) throws FileNotFoundException {

        //获取用户输入以及文件
        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();
        //校验,只有登录的用户才能查询
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        User user = userService.getLoginUser(request);
        //校验文件大小和后缀
        Long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        String suffix = FileUtil.getSuffix(originalFilename);
        //后缀白名单
        final List<String> validFileSuffix = Arrays.asList("csv", "xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
        //关于校验文件内容的合规性一般利用第三方。比如接入腾讯云的图片万象数据审核（COS对象的存储功能）
        //限流每个用户这个方法的限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + String.valueOf(user.getId()));
        //系统预设,在调用的接口模型已经有了    以及用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析目标：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        //读取到用户上传的excel文件，进行处理变成csv格式  压缩后的数据
        String csvData = ExcelUtils.excel2Csv(multipartFile);
        userInput.append(csvData).append("\n");

        //先保存用户输入的数据到数据库，再去调用ai
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(user.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表相关数据保存失败");
        //调用ai，执行任务
        CompletableFuture.runAsync(() -> {
            //修改图表任务状态为”执行中“，等待执行成功后，修改为”已完成“，保存执行结果；执行失败后状态修改为”失败”，记录失败信息
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            //修改表，将刚才注入的修改信息提交
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                HandleChartUpdateError(chart.getId(), "图表状态更改失败");
                return;
            }
            String result = aiManager.doChat(userInput.toString());
            String[] splits = result.split("####");
            if (splits.length < 3) {
                HandleChartUpdateError(chart.getId(), "AI生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGemResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                HandleChartUpdateError(chart.getId(), "更新图表状态更改失败");
            }
        }, threadPoolExecutor);

        //返回结果没有图表结论和图表信息了。
        BIResponse biResponse = new BIResponse();
        biResponse.setChartID(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 当很多处都需要抛出错误的时候，可以定义一个处理错误类
     *
     * @param chartID
     * @param execMessage
     */
    private void HandleChartUpdateError(long chartID, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(updateChart.getId());
        updateChart.setStatus("failed");
        updateChart.setExecMessage("execMessage");
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult) {
            log.error("图表失败信息更改失败" + chartID + "," + execMessage);
        }
    }

    //同步调用的方式
    /*@PostMapping("/gen")
    public BaseResponse<BIResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 genChartByAIRequest genChartByAiRequest, HttpServletRequest request) throws FileNotFoundException {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验,只有登录的用户才能查询
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length()>100,ErrorCode.PARAMS_ERROR,"名称过长");

        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");

        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        User loginUser = userService.getLoginUser(request);

        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        long biModelId = 1659171950288818178L;

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求").append("\n");


        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "请给我" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");

        String csvData = ExcelUtils.excel2Csv(multipartFile);
        userInput.append(csvData).append("\n");

        String result = aiManager.doChat(biModelId, userInput.toString());
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成失败");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 鎻掑叆鍒版暟鎹簱
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGemResult(genResult);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "AI生成失败");
        BIResponse biResponse = new BIResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartID(chart.getId());
        return ResultUtils.success(biResponse);
    }*/

    /**
     * 根据 id 获取
     *todo 我都缓存预热过了为什么第一次查询仍然需要去数据库
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getchartVOById(long id, HttpServletRequest request) {
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
     * 分页获取条件查询的列表
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page/condition")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }


    /**
     * 检查缓存是否命中，并根据需要从缓存中获取部分数据。
     * 对未命中的部分数据进行数据库查询。
     * 将数据库查询的结果和缓存的结果合并，返回给前端。
     * 更新缓存，以便下次查询时能直接命中。
     * 预热问题
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPageRedis(@RequestBody ChartQueryRequest chartQueryRequest,
                                                          HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        String keyPrefix="bichart";
         //实现了缓存预热在ScheduleJobManager中，将所有数据图表先存数据缓存
        QueryWrapper<Chart> queryWrapper = getQueryWrapper(chartQueryRequest);
        // 然后实现缓存分页查询，如果查询的数据不在，就从读数据库，反正是根据索引
        long start = (current - 1) * size;
        long end = start + size - 1;
        // 从 Redis 获取数据
        List<String> cachedChartJsonList = redisTemplate.opsForList().range(keyPrefix+"_" +chartQueryRequest.getId(), start, end);
        //List<String> cachedChartJsonList = redisTemplate.opsForZSet().range(keyPrefix+"_" +chartQueryRequest.getId(), start, end);
        List<Chart> cachedCharts = new ArrayList<>();
        if (cachedChartJsonList != null) {
            for (String chartJson : cachedChartJsonList) {
                Chart chart = JSONUtil.toBean(chartJson, Chart.class);
                cachedCharts.add(chart);
            }
            System.out.println("可以从缓存中拿到数据");
        }

// 如果 Redis 中没有足够的数据，从数据库中获取缺失的数据并更新缓存
        if (cachedCharts.size() < size) {
            //计算出需要从数据库中获取多少数据,避免重复拿
            long dbFetchSize = size - cachedCharts.size();
            Page<Chart> dbPage = chartService.page(new Page<>(current, dbFetchSize), queryWrapper);
            List<Chart> dbCharts = dbPage.getRecords();
            // 将缺失的数据更新到 Redis
            for (Chart chart : dbCharts) {
                redisTemplate.opsForList().rightPush(keyPrefix+"_" +chartQueryRequest.getId(), JSONUtil.toJsonStr(chart));
                System.out.println("缺失数据放入缓存："+chartQueryRequest.getId());
            }
            cachedCharts.addAll(dbCharts);
            System.out.println("完善缓存数据完毕");
        }

        Set<Long> seenIds = new HashSet<>();
        List<Chart> uniqueCharts = cachedCharts.stream()
                .filter(chart -> seenIds.add(chart.getId()))
                .collect(Collectors.toList());

        Page<Chart> chartPage = new Page<>(current, size);
        chartPage.setRecords(cachedCharts);
        chartPage.setTotal(chartService.count(getQueryWrapper(chartQueryRequest)));

// 返回去重后的结果
        return ResultUtils.success(chartPage);
    }

    /**
     * 检查缓存是否命中，并根据需要从缓存中获取部分数据。
     * 对未命中的部分数据进行数据库查询。
     * 将数据库查询的结果和缓存的结果合并，返回给前端。
     * 更新缓存，以便下次查询时能直接命中。
     *  使用zset做分页，自动去重
     *  todo 有时候不会命中缓存。但明明预热过了
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/byset")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPageRedisByZset(@RequestBody ChartQueryRequest chartQueryRequest,
                                                          HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        String keyPrefix = "bichart";
        QueryWrapper<Chart> queryWrapper = getQueryWrapper(chartQueryRequest);
        long start = (current - 1) * size;
        long end = start + size - 1;

        // 从 Redis 获取数据
        Set<String> cachedChartJsonSet = redisTemplate.opsForZSet().range(keyPrefix + "_" + chartQueryRequest.getId(), start, end);
        List<Chart> cachedCharts = new ArrayList<>();
        if (cachedChartJsonSet != null) {
            for (String chartJson : cachedChartJsonSet) {
                Chart chart = JSONUtil.toBean(chartJson, Chart.class);
                cachedCharts.add(chart);
            }
            System.out.println("可以从缓存中拿到数据");
        }

        // 如果 Redis 中没有足够的数据，从数据库中获取缺失的数据并更新缓存
        if (cachedCharts.size() < size) {
            long dbFetchSize = size - cachedCharts.size();
            Page<Chart> dbPage = chartService.page(new Page<>(current, dbFetchSize), queryWrapper);
            List<Chart> dbCharts = dbPage.getRecords();
            LocalDateTime now = LocalDateTime.now();
            double timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000.0;
            for (Chart chart : dbCharts) {
                redisTemplate.opsForZSet().addIfAbsent(keyPrefix + "_" + chartQueryRequest.getId(), JSONUtil.toJsonStr(chart), timestamp);
                redisTemplate.expire(keyPrefix + "_" + chartQueryRequest.getId(), 120 + RandomUtil.randomInt(), TimeUnit.MINUTES);
                System.out.println("缺失数据放入缓存：" + chartQueryRequest.getId());
            }
            cachedCharts.addAll(dbCharts);
            System.out.println("完善缓存数据完毕");
        }

        // 构造分页结果
        Page<Chart> chartPage = new Page<>(current, size);
        chartPage.setRecords(cachedCharts);
        chartPage.setTotal(chartService.count(getQueryWrapper(chartQueryRequest)));

        return ResultUtils.success(chartPage);
    }


    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMychartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
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
                getQueryWrapper(chartQueryRequest));
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
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldchart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 查询
     * @param chartQueryRequest
     * @return
     */

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        System.out.println("###############################正在执行查询方法");
        Long id=chartQueryRequest.getId();
        String goal=chartQueryRequest.getGoal();
        String chartType=chartQueryRequest.getChartType();
        Long userId=chartQueryRequest.getUserId();
        String sortField=chartQueryRequest.getSortField();
        String sortOrder=chartQueryRequest.getSortOrder();
        String name=chartQueryRequest.getName();
        //System.out.println("============================="+id+","+goal+","+userId);
        queryWrapper.eq(id != null&&id>0,"id",id);
        queryWrapper.eq(StringUtils.isNotBlank(goal),"goal",goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType),"chartType",chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        //模糊查询
        queryWrapper.like(StringUtils.isNotBlank(name),"name",name);
        queryWrapper.eq("isDelete",false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),sortOrder.equals(CommonConstant.SORT_ORDER_ASC),sortField);
        return queryWrapper;
    }

}
