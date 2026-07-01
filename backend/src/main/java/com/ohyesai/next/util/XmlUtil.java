package com.ohyesai.next.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XmlUtil {

    private final static XmlMapper MAPPER =  new XmlMapper();

    public static String toXml(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("xml 解析失败：{}", object, e);
            throw new BusinessException(CodeEnum.Unknow, e);
        }
    }

    public static String toXml(String rootName,Object object) {
        try {
            return MAPPER.writer().withRootName(rootName).writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("xml 解析失败：{}", object, e);
            throw new BusinessException(CodeEnum.Unknow, e);
        }
    }
}
