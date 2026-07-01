package com.ohyesai.next.biz.baidu.controller;

import com.ohyesai.next.biz.baidu.dto.UploadCovertDataDTO;
import com.ohyesai.next.biz.baidu.service.ConvertDataService;
import com.ohyesai.next.common.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cover-data")
@Tag(name = "转化数据")
@AllArgsConstructor
@Slf4j
public class ConvertDataController {

    private final ConvertDataService convertDataService;

    @Operation(summary = "上传转化数据")
    @PostMapping("upload-covert-data")
    public Result<Void> uploadCovertData(@RequestBody @Validated UploadCovertDataDTO uploadCovertDataDTO) {
        if (!uploadCovertDataDTO.getLogidUrl().contains("bd_vid=")) {
            log.warn("忽略本次，转化链接非百度跳转 {}", uploadCovertDataDTO.getLogidUrl());
            return Result.success();
        }

        convertDataService.uploadCovertData(uploadCovertDataDTO);
        return Result.success();
    }
}
