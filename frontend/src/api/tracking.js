import request from '@/utils/request'
import {isMobileClient} from '@/utils/index'

/**
 * 保存用户行为追踪
 * @param {{ referer?: string, target: string }} data
 */
export function saveUserTracking(data) {
  const platform = isMobileClient() ? 'H5' : 'WEB'
  return request({
    url: '/ohyesai-next/user-tracking/save',
    method: 'post',
    data: {
      ...data,
      platform,
      referer: sessionStorage.getItem("refererUrl"),
    },
  })
}

/**
 * 上传百度推广转化数据
 * @param {{ logidUrl: string, newType: 'SERVICE_SUCCESS'|'FORM_SUCCESS' }} data
 */
export function uploadCovertData(data) {
  return request({
    url: '/ohyesai-next/cover-data/upload-covert-data',
    method: 'post',
    data,
  })
}
