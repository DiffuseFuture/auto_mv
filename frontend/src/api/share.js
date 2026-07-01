import request from '@/utils/request'

/**
 * 获取分享链接
 * @param params
 * @returns {Promise}
 */
export function shareLink(params) {
    return request({
        url: '/ohyesai-next/vio/resource/share-link',
        method: 'get',
        params,
    })
}

/**
 * 获取分享数据
 * @param params
 * @returns {Promise}
 */
export function shareData(params) {
    return request({
        url: '/ohyesai-next/vio/resource/share-data',
        method: 'get',
        params,
    })
}

/**
 * 获取邀请码
 * @returns {Promise<string>}
 */
export function getInviteCode() {
    return request({
        url: '/ohyesai-next/user/invite-code',
        method: 'get',
    })
}
