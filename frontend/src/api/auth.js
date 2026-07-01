import request from '@/utils/request'

/**
 * 发送验证码
 * @param {Object} data
 * @returns {Promise}
 */
export function sendSmsCode(data) {
  return request({
    url: '/ohyesai-next/user/send-sms-code',
    method: 'post',
    data,
  })
}

/**
 * 手机号验证码登录
 * @param {Object} data
 * @returns {Promise}
 */
export function loginByPhone(data) {
  return request({
    url: '/ohyesai-next/user/mobile-signin',
    method: 'post',
    data,
  })
}

/**
 * 退出登录
 * @returns {Promise}
 */
export function logout() {
  return request({
    url: '/ohyesai-next/user/logout',
    method: 'post'
  })
}

/**
 * 获取用户信息
 * @returns {Promise}
 */
export function getUserInfo() {
  return request({
    url: '/ohyesai-next/user/info',
    method: 'get'
  })
}

/**
 * 修改用户信息
 * @param {Object} data
 * @returns {Promise}
 */
export function updateUserInfo(data) {
  return request({
    url: '/ohyesai-next/user/update',
    method: 'post',
    data,
  })
}

/**
 * 修改用户头像
 * @param data
 * @returns {Promise}
 */
export function updateAvatar(data) {
  return request({
    url: '/ohyesai-next/user/update-avatar-img',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 创建 API Key
 * @returns {Promise}
 */
export function createApiKey() {
  return request({
    url: '/ohyesai-next/user/create-api-key',
    method: 'post',
  })
}

