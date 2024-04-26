package com.xu.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.springbootinit.model.entity.Chart;
import com.xu.springbootinit.service.ChartService;
import com.xu.springbootinit.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author 86136
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-03-30 15:01:59
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




