package com.yupi.springbootinit.controller;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.StatusConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.manager.WebSocketManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
//import com.yupi.springbootinit.model.vo.ChartVO;
import com.yupi.springbootinit.model.vo.BiModelVO;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
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
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private WebSocketManager webSocketManager;

    private final static Gson GSON = new Gson();

    // region 增删改查

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
        long id = deleteRequest.getId();
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
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiModelVO> genChartByAi2(@RequestPart("file") MultipartFile multipartFile,
                                           GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String target = genChartByAiRequest.getTarget();

        User loginUser = userService.getLoginUser(request);
        //做接口限流
        redisLimiterManager.doLimit("genChartByAi_" + loginUser.getId());

        //校验
        ThrowUtils.throwIf(StrUtil.isBlank(target), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StrUtil.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        /**
         * 校验文件
         * 首先拿到用户的输入文件
         * 查看文件的大小
         */
        long fileSize = multipartFile.getSize();
        //取得原始文件名
        String filename = multipartFile.getOriginalFilename();

        /**
         * 校验文件大小
         *
         * 1MB = 1024*1024字节（Byte）
         */
        final long ONE_MB = 1024*1024L;
        //如果文件大于1兆，就抛异常
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件不能大于1MB");

        /**
         * 校验文件后缀(一般文件是aaa.png, 要拿到png后缀)
         * 利用FileUtil工具类中的getSuffix方法获得文件后缀名
         */
        String suffix = FileUtil.getSuffix(filename);

        //定义白名单
        final List<String> validSuffixList = Arrays.asList("xlsx", "xls");
        //验证后缀
        ThrowUtils.throwIf(!validSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型暂不支持");

        long modelId = 1737318879266828289L;
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("\n");
        userInput.append(target).append("\n");
        if (StrUtil.isNotBlank(chartType)){
            userInput.append("使用" + chartType).append("\n");
        }
        //压缩的数据
        //读取excel文件进行压缩处理
        String result = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据:").append("\n");
        userInput.append(result);

        String genChart = aiManager.doChat(modelId, userInput.toString());

        String[] split = genChart.split("【【【【【");
        if (split.length != 3){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误（未按规定格式输出）");
        }

        String getChart = split[1].trim();
        String getResult = split[2].trim();

        // 先把图表保存到数据库中
        Chart chart = new Chart();
        if (StrUtil.isNotBlank(name)){
            chart.setName(name);
        }
        chart.setTarget(target);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        //设置任务状态为排队中
        chart.setGetChart(getChart);
        chart.setGetResult(getResult);
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "图表保存失败");


        BiModelVO biModelVO = new BiModelVO();
        biModelVO.setGetChart(getChart);
        biModelVO.setGetResult(getResult);
        biModelVO.setId(chart.getId());



        return ResultUtils.success(biModelVO);

    }
    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen2")
    public BaseResponse<BiModelVO> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String name = genChartByAiRequest.getName();
        String target = genChartByAiRequest.getTarget();

        User loginUser = userService.getLoginUser(request);

        redisLimiterManager.doLimit("genChartByAi_" + loginUser.getId());

        //校验
        ThrowUtils.throwIf(StrUtil.isBlank(target), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StrUtil.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        /**
         * 校验文件
         * 首先拿到用户的输入文件
         * 查看文件的大小
         */
        long fileSize = multipartFile.getSize();
        //取得原始文件名
        String filename = multipartFile.getOriginalFilename();

        /**
         * 校验文件大小
         *
         * 1MB = 1024*1024字节（Byte）
         */
        final long ONE_MB = 1024*1024L;
        //如果文件大于1兆，就抛异常
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件不能大于1MB");

        /**
         * 校验文件后缀(一般文件是aaa.png, 要拿到png后缀)
         * 利用FileUtil工具类中的getSuffix方法获得文件后缀名
         */
        String suffix = FileUtil.getSuffix(filename);

        //定义白名单
        final List<String> validSuffixList = Arrays.asList("xlsx", "xls");
        //验证后缀
        ThrowUtils.throwIf(!validSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型暂不支持");

        long modelId = 1737318879266828289L;
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append("\n");
        userInput.append(target).append("\n");
        if (StrUtil.isNotBlank(chartType)){
            userInput.append("使用" + chartType).append("\n");
        }
        //压缩的数据
        //读取excel文件进行压缩处理
        String result = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据:").append("\n");
        userInput.append(result);

        // 先把图表保存到数据库中
        Chart chart = new Chart();
        if (StrUtil.isNotBlank(name)){
            chart.setName(name);
        }
        chart.setTarget(target);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        //设置任务状态为排队中
        chart.setStatus(StatusConstant.WAIT);
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "图表保存失败");

        // 在最终的返回结果前提交一个任务
        // todo 建议处理任务队列满了后,抛异常的情况(因为提交任务报错了,前端会返回异常)
        CompletableFuture.runAsync(() -> {
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(StatusConstant.RUNNING);
            boolean saved = chartService.updateById(updateChart);
            if (saved) {
                handleChartUpdateError(chart.getId(), "更新running状态失败");
                try {
                    webSocketManager.sendMessage("更新running状态失败");
                } catch (IOException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "web连接异常");
                }
                return;
            }
            String genChart = aiManager.doChat(modelId, userInput.toString());

            String[] split = genChart.split("【【【【【");
            if (split.length != 3){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误（未按规定格式输出）");
            }

            String getChart = split[1].trim();
            String getResult = split[2].trim();

            Chart updateChartSucceed = new Chart();
            updateChartSucceed.setId(chart.getId());
            updateChartSucceed.setGetChart(getChart);
            updateChartSucceed.setGetResult(getResult);
            updateChartSucceed.setStatus(StatusConstant.SUCCEED);
            boolean save3 = chartService.updateById(updateChartSucceed);
            if (!save3){
                handleChartUpdateError(chart.getId(), "更新succeed状态失败");
                try {
                    webSocketManager.sendMessage("更新succeed状态失败");
                } catch (IOException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "web连接异常");
                }
                return;
            }
        }, threadPoolExecutor);

        BiModelVO biModelVO = new BiModelVO();
        biModelVO.setId(chart.getId());



        return ResultUtils.success(biModelVO);

    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartFailed = new Chart();
        updateChartFailed.setId(chartId);
        updateChartFailed.setStatus(StatusConstant.FAILED);
        updateChartFailed.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChartFailed);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }

    }
//     * 根据 id 获取
//     *
//     * @param id
//     * @return
//     */
//    @GetMapping("/get/vo")
//    public BaseResponse<ChartVO> getChartVOById(long id, HttpServletRequest request) {
//        if (id <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        Chart chart = chartService.getById(id);
//        if (chart == null) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
//        }
//        return ResultUtils.success(chartService.getChartVO(chart, request));
//    }
//
//    /**
//     * 分页获取列表（封装类）
//     *
//     * @param chartQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/list/page/vo")
//    public BaseResponse<Page<ChartVO>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
//                                                         HttpServletRequest request) {
//        long current = chartQueryRequest.getCurrent();
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
//                chartService.getQueryWrapper(chartQueryRequest));
//        return ResultUtils.success(chartService.getChartVOPage(chartPage, request));
//    }
//
//    /**
//     * 分页获取当前用户创建的资源列表
//     *
//     * @param chartQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/my/list/page/vo")
//    public BaseResponse<Page<ChartVO>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
//            HttpServletRequest request) {
//        if (chartQueryRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        chartQueryRequest.setUserId(loginUser.getId());
//        long current = chartQueryRequest.getCurrent();
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
//                chartService.getQueryWrapper(chartQueryRequest));
//        return ResultUtils.success(chartService.getChartVOPage(chartPage, request));
//    }
//
//    // endregion
//
//    /**
//     * 分页搜索（从 ES 查询，封装类）
//     *
//     * @param chartQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/search/page/vo")
//    public BaseResponse<Page<ChartVO>> searchChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
//            HttpServletRequest request) {
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> chartPage = chartService.searchFromEs(chartQueryRequest);
//        return ResultUtils.success(chartService.getChartVOPage(chartPage, request));
//    }
//
//    /**
//     * 编辑（用户）
//     *
//     * @param chartEditRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/edit")
//    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
//        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        Chart chart = new Chart();
//        BeanUtils.copyProperties(chartEditRequest, chart);
//        List<String> tags = chartEditRequest.getTags();
//        if (tags != null) {
//            chart.setTags(GSON.toJson(tags));
//        }
//        // 参数校验
//        chartService.validChart(chart, false);
//        User loginUser = userService.getLoginUser(request);
//        long id = chartEditRequest.getId();
//        // 判断是否存在
//        Chart oldChart = chartService.getById(id);
//        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或管理员可编辑
//        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        boolean result = chartService.updateById(chart);
//        return ResultUtils.success(result);
//    }

}
