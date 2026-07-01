package com.ohyesai.next.common.vo;

import com.ohyesai.next.common.enums.CodeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<PageResult.PageData<T>> {

    public static <T> PageResult<T> success(Long total, List<T> data) {
        return custom(CodeEnum.Success, total, data);
    }

    public static <T> PageResult<T> custom(CodeEnum codeEnum, Long total, List<T> data) {
        PageResult<T> result = new PageResult<>();
        result.setCode(codeEnum.code);
        result.setMessage(codeEnum.message);
        PageData<T> pageData = new PageData<>(total, data);
        result.setData(pageData);
        return result;
    }

    @Data
    public static class PageData<T> {
        private Long total;
        private List<T> data;

        public PageData() {
        }

        public PageData(Long total, List<T> data) {
            this.total = total;
            this.data = data;
        }
    }
}
