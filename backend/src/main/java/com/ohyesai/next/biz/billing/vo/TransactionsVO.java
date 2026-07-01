package com.ohyesai.next.biz.billing.vo;

import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付回调参数")
public class TransactionsVO {

    @Schema(description = "订单号，用来获取当前订单支付状态")
    private String tradeNo;

    @Schema(description = "支付内容；支付宝:html片段; 微信pc:支付链接需要转换为二维码; 微信移动端h5:url地址直接打开可以唤起微信界面;")
    private String payContent;

    @Schema(description = "微信小程序专用支付结果;")
    private PrepayWithRequestPaymentResponse wxMpPayContent;

    @Schema(description = "支付金额/分")
    private int amountF;

    public static TransactionsVO fromMp(String tradeNo,PrepayWithRequestPaymentResponse wxMpPayContent) {
        TransactionsVO transactionsVO = new TransactionsVO();
        transactionsVO.setWxMpPayContent(wxMpPayContent);
        transactionsVO.setTradeNo(tradeNo);
        return transactionsVO;
    }

    public static TransactionsVO from(String tradeNo, String payContent) {
        TransactionsVO transactionsVO = new TransactionsVO();
        transactionsVO.setTradeNo(tradeNo);
        transactionsVO.setPayContent(payContent);
        return transactionsVO;
    }


}
