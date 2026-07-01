package com.ohyesai.next.biz.billing.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.alipay.api.domain.RechargeDTO;
import com.ohyesai.next.biz.billing.dto.TransactionsDTO;
import com.ohyesai.next.biz.billing.service.PayService;
import com.ohyesai.next.biz.billing.vo.TransactionsVO;
import com.ohyesai.next.common.properties.PayProperties;
import com.ohyesai.next.common.vo.Result;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.partnerpayments.nativepay.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Tag(name = "支付")
@RestController
@RequestMapping("/pay")
@Slf4j
@AllArgsConstructor
public class PayController {

    private final PayService payService;

    private final PayProperties properties;

    @Operation(summary = "微信支付回调", hidden = true)
    @PostMapping("/wx-pay-callback")
    @SaIgnore
    public ResponseEntity<Object> wxPayCallback(@RequestBody String payload, @RequestHeader HttpHeaders headers) {
        NotificationConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(properties.wx().merchantId())
                .privateKey(properties.wx().privateKey())
                .merchantSerialNumber(properties.wx().merchantSerialNumber())
                .apiV3Key(properties.wx().apiV3Key())
                .build();

        String timestamp = Objects.requireNonNull(headers.get("Wechatpay-Timestamp")).getFirst();
        String nonce = Objects.requireNonNull(headers.get("Wechatpay-Nonce")).getFirst();
        String signature = Objects.requireNonNull(headers.get("Wechatpay-Signature")).getFirst();
        String serial = Objects.requireNonNull(headers.get("Wechatpay-Serial")).getFirst();


        com.wechat.pay.java.core.notification.RequestParam requestParam = new com.wechat.pay.java.core.notification.RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(payload)
                .build();

        // 初始化 NotificationParser
        NotificationParser parser = new NotificationParser(config);

        try {
            // 以支付通知回调为例，验签、解密并转换成 Transaction
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            payService.payCallback(transaction.getOutTradeNo());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ValidationException e) {
            log.error("sign verification failed", e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

    }

    @Operation(summary = "统一支付下单")
    @PostMapping("/transactions")
    public Result<TransactionsVO> transactions(@RequestBody @Validated TransactionsDTO transactionsDTO) {
        return Result.success(payService.transactions(transactionsDTO));
    }

    @Operation(summary = "订单支付状态", description = "0 成功；2 失败 or 超时； 3 待支付；")
    @GetMapping("/transactions-state")
    public Result<Integer> transactionsState(@NotBlank @Validated String tradeNo) {
        return Result.success(payService.transactionsState(tradeNo));
    }
}
