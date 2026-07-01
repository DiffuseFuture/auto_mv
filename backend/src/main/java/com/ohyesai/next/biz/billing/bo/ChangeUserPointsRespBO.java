package com.ohyesai.next.biz.billing.bo;

import com.ohyesai.next.biz.billing.entity.PointsTransactionLog;
import com.ohyesai.next.biz.user.entity.User;

public record ChangeUserPointsRespBO(
        User user,
        PointsTransactionLog pointsTransactionLog
) {
}
