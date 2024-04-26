package com.xu.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.xu.springbootinit.common.ErrorCode;
import com.xu.springbootinit.constant.CommonConstant;
import com.xu.springbootinit.exception.BusinessException;
import com.xu.springbootinit.manager.AiManager;
import com.xu.springbootinit.mapper.ChartDataMapper;
import com.xu.springbootinit.model.entity.Chart;
import com.xu.springbootinit.service.ChartService;
import com.xu.springbootinit.utils.ExcelUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author: xuJing
 * @date: 2024/4/8 20:04
 */

@Component
@Slf4j
public class BiMessageConsumer {


    @Resource
    private ChartService chartService;


    @Resource
    private AiManager aiManager;


    @Resource
    private ChartDataMapper chartDataMapper;


    @RabbitListener(queues = {CommonConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("message = {}", message);
        if(StringUtils.isBlank(message)){
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if(chart==null){
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表为空");
        }

        // 将图表中的状态修改为执行中 running
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if(!b){
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表执行状态失败");
            return;
        }

        // 调用AI 拿到返回结果
//        String result = aiManager.doChat(biModelId, buildUserInput(chart));
        String result = aiManager.doChart(buildUserInput(chart));
        // 对结果进行处理
        String[] split = result.split("#####");
        // 拆分校验
        if(split.length < 3){
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }

        String genChart = split[1].trim();
        String genResult = split[2].trim();

        // AI调用完成，再更新一次
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        boolean updateResult = chartService.updateById(updateChartResult);
        if(!updateResult){
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId() , "更新图表成功状态失败");
        };


        channel.basicAck(deliveryTag, false);

    }


    private void handleChartUpdateError(Long id, String message) {
        Chart chart = new Chart();
        chart.setId(id);
        chart.setStatus("failed");
        chart.setExecMessage(message);
        boolean b = chartService.updateById(chart);
        if(!b){
            log.error("更新图表状态失败"+id + "," +message);
        }
    }

    private String buildUserInput(Chart chart){

        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        // 拼接分析目标
        String userGoal = goal;
        if(StringUtils.isNotBlank(chartType)){
            // 图标类型不为空，则把图标类型拼接上
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");


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

        return userInput.toString();
    }
}
