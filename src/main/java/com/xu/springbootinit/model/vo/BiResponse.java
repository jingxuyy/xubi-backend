package com.xu.springbootinit.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: xuJing
 * @date: 2024/4/4 14:49
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiResponse {

    private String genChart;

    private String genResult;

    private Long chartId;
}
