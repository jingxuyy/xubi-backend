package com.xu.springbootinit.constant;

/**
 * 通用常量
 *
 * @author <a href="https://github.com/jingxuyy">程序员xu</a>
 */
public interface CommonConstant {

    /**
     * 升序
     */
    String SORT_ORDER_ASC = "ascend";

    /**
     * 降序
     */
    String SORT_ORDER_DESC = " descend";


    String BI_EXCHANGE_NAME = "bi_exchange";


    String BI_QUEUE_NAME = "bi_queue";


    String BI_ROUTING_KEY = "bi_routingKey";


    String INPUT_CHECK_CONTENT = "分析描述为空";

    int INPUT_CHECK_LENGTH = 100;

    String INPUT_CHECK_NAME = "名称过长";

    long FILE_CHECK_SIZE = 1024*1024;

    String FILE_CHECK_LARGE = "文件过大";

    String FILE_CHECK_SUFFIX = "xlsx";

    String FILE_CHECK_SUFFIX_ERROR = "文件非法";


    String LIMITER_KEY = "genChartByAi";


    long MODER_ID = 1L;

    String CHART_STATUS_WAIT = "wait";

    String CHART_STATUS_RUNNING = "running";

    String CHART_STATUS_SUCCEED = "succeed";

    String CHART_STATUS_FAILED = "failed";




}
