package com.xu.springbootinit.manager;

import com.xu.springbootinit.common.ErrorCode;
import com.xu.springbootinit.constant.CommonConstant;
import com.xu.springbootinit.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.exception.SparkException;
import io.github.briqt.spark4j.listener.SparkBaseListener;
import io.github.briqt.spark4j.listener.SparkSyncChatListener;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import io.github.briqt.spark4j.model.response.SparkTextUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: xuJing
 * @date: 2024/4/4 14:29
 */

@Service
@Slf4j
public class AiManager {

    @Resource
    private SparkClient sparkClient;

    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * AI 对话
     * @param message 问题
     * @return
     */
    public String doChat(String message){
        // 构造请求参数
        DevChatRequest devChatRequest = new DevChatRequest();
        // 设置模型id, 加载消息
        devChatRequest.setModelId(CommonConstant.MODER_ID);
        devChatRequest.setMessage(message);

        // 获取响应结果
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if(response==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应错误");
        }
        return response.getData().getContent();

    }


    public String doChat(long modelId, String message){
        // 构造请求参数
        DevChatRequest devChatRequest = new DevChatRequest();
        // 设置模型id, 加载消息
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);

        // 获取响应结果
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if(response==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应错误");
        }
        return response.getData().getContent();

    }

    public String doChart(String message){
        // 设置prompt
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.systemContent("{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容，内容包含“#####”（此外不要输出任何多余的开头、结尾、注释）\n" +
                "#####\n" +
                "{前端Echarts V5的option配置对象的准确json代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                "#####\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}"));
        messages.add(SparkMessage.userContent(message));

        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                .messages(messages)
                .maxTokens(2048)
                .temperature(0.5)
                .apiVersion(SparkApiVersion.V3_5)
                .build();
        String result = "";
        String useToken = " ";
        try {
            // 同步调用
            SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
            SparkTextUsage textUsage = chatResponse.getTextUsage();
            result = chatResponse.getContent();
            useToken = "提问tokens：" + textUsage.getPromptTokens()
                    + "，回答tokens：" + textUsage.getCompletionTokens()
                    + "，总消耗tokens：" + textUsage.getTotalTokens();
            log.info(useToken);
        } catch (SparkException e) {
            log.error("Ai调用发生异常了：" + e.getMessage());
        }
        return result;
    }

    public String doChartByListener(String message){
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.userContent(message));
        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                .messages(messages)
                .maxTokens(2048)
                .temperature(0.2)
                .apiVersion(SparkApiVersion.V3_5)
                .build();
        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        String content = chatResponse.getContent();
        return content;
    }
}
