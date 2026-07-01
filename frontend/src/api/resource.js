import request from '@/utils/request'

/**
 * 查询 MV 资源列表
 * @param {{ page: number, size: number, projectName?: string }} params
 * @returns {Promise<{ total: number, data: Array }>}
 */
export function findMv(params) {
  return request({
    url: '/ohyesai-next/vio/resource/find-mv',
    method: 'get',
    params,
  })
}

/**
 * 首页公共预览资源列表
 * @param {{ page: number, size: number, projectName?: string }} params
 * @returns {Promise<{ total: number, data: Array<{ sessionId: string, messageId: string, projectId: string, projectName: string, fileId: string, fileUrl: string, fileCoverId: string, fileCoverUrl: string, duration: number, nickName: string, avatar: string, createTime: string }> }>}
 */
export function getCommonPreview(params) {
  return request({
    url: '/ohyesai-next/vio/resource/common-preview',
    method: 'get',
    params,
  })
}

/**
 * 查询音乐资源列表
 * @param {{ page: number, size: number, projectName?: string }} params
 * @returns {Promise<{ total: number, data: Array }>}
 */
export function findMusic(params) {
  return request({
    url: '/ohyesai-next/vio/resource/find-music',
    method: 'get',
    params,
  })
}

/**
 * 删除资源
 */
export function deleteProject(data) {
  return request({
    url: '/ohyesai-next/vio/resource/delete-project',
    method: 'post',
    data,
  })
}

/**
 * 重命名资源
 */
export function renameProject(data) {
  return request({
    url: '/ohyesai-next/vio/resource/rename-project',
    method: 'post',
    data,
  })
}
