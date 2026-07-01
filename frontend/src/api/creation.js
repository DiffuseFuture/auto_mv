import request from '@/utils/request'
import { fetchEventSource } from '@microsoft/fetch-event-source'

/**
 * 发送对话消息（SSE 流式接口）
 * @param {Object} data - { sessionId, prompt, audioFileId, model, audioLyrics?, subjectImgs?, editContext?, subtitle?, lipSync? }
 *   - subtitle / lipSync：仅 SCENE「完成制作」路径会传；未勾选时传 false（后端 null/false 等效）
 * @param {Object} callbacks - { onOpen, onMessage, onError, onClose }
 * @param {AbortSignal} signal - 用于外部取消连接
 * @returns {Promise}
 */
export function sendChatMessageSSE(data, callbacks = {}, signal) {
  const { onOpen, onMessage, onError, onClose } = callbacks
  const token = localStorage.getItem('token')
  
  return fetchEventSource('/ohyesai-next/sse/vio/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'token': token || ''
    },
    body: JSON.stringify(data),
    signal,
    // 页面切到后台时保持连接，防止 fetchEventSource 自动断开后重发请求
    openWhenHidden: true,
    onopen(response) {
      onOpen && onOpen(response)
    },
    onmessage(event) {
      try {
        const data = JSON.parse(event.data)
        onMessage && onMessage(data)
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    },
    onerror(error) {
      console.error('SSE 连接错误:', error)
      onError && onError(error)
      throw error // 停止重连
    },
    onclose() {
      onClose && onClose()
    }
  })
}

/**
 * 语义澄清（ACTION_REQUIRED 提交）SSE 流式接口
 * @param {Object} data - { sessionId, clarificationId, optionId, customText? }
 * @param {Object} callbacks - { onOpen, onMessage, onError, onClose }
 * @param {AbortSignal} signal - 用于外部取消连接
 * @returns {Promise}
 */
export function clarifyChatSSE(data, callbacks = {}, signal) {
  const { onOpen, onMessage, onError, onClose } = callbacks
  const token = localStorage.getItem('token')

  return fetchEventSource('/ohyesai-next/sse/vio/chat/clarify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'token': token || ''
    },
    body: JSON.stringify(data),
    signal,
    // 页面切到后台时保持连接，防止 fetchEventSource 自动断开后重发请求
    openWhenHidden: true,
    onopen(response) {
      onOpen && onOpen(response)
    },
    onmessage(event) {
      try {
        const data = JSON.parse(event.data)
        onMessage && onMessage(data)
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    },
    onerror(error) {
      console.error('SSE 连接错误:', error)
      onError && onError(error)
      throw error // 停止重连
    },
    onclose() {
      onClose && onClose()
    }
  })
}

/**
 * 获取对话列表
 * @param {Object} params - { page, size, sessionName? }
 * @returns {Promise}
 */
export function getChatList(params) {
  return request({
    url: '/ohyesai-next/sse/vio/chat-list',
    method: 'get',
    params
  })
}

/**
 * 获取历史消息
 * @param {string} chatSessionId
 * @returns {Promise}
 */
export function getHistoryMessage(chatSessionId) {
  return request({
    url: '/ohyesai-next/sse/vio/history-message',
    method: 'get',
    params: { chatSessionId }
  })
}

/**
 * 继续对话（SSE 流式接口）
 * @param {Object} data - { sessionId, historyMessageId }
 * @param {Object} callbacks - { onMessage, onError, onClose }
 * @returns {Promise}
 */
export function resumeChatSSE(data, callbacks = {}, signal) {
  const { onMessage, onError, onClose } = callbacks
  const token = localStorage.getItem('token')
  
  return fetchEventSource('/ohyesai-next/sse/vio/resume-chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'token': token || ''
    },
    body: JSON.stringify(data),
    signal,
    openWhenHidden: true,
    onmessage(event) {
      try {
        const data = JSON.parse(event.data)
        onMessage && onMessage(data)
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    },
    onerror(error) {
      console.error('SSE 连接错误:', error)
      onError && onError(error)
      throw error // 停止重连
    },
    onclose() {
      onClose && onClose()
    }
  })
}

/**
 * 上传文件
 * @param {FormData} formData
 * @returns {Promise}
 */
export function uploadFile(formData) {
  return request({
    url: '/ohyesai-next/sse/vio/upload',
    method: 'post',
    timeout: 0, // 上传文件不设超时
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取视频元信息
 * @param {string} mvFileId
 * @returns {Promise<{ mvId: string, scenes: Array, fileId: string, fileUrl: string }>}
 */
export function getVideoMeta(mvFileId) {
  return request({
    url: '/ohyesai-next/vio/resource/video-meta',
    method: 'get',
    params: { mvFileId }
  })
}

/**
 * 提交重新生成分镜任务（异步）
 * @param {{ mvId: string, model?: string, scenes: Array<{ reMake: boolean, sceneId: string, visualPrompt: string, subject: Array<{ subjectId: string, fileId: string, fileUrl: string }> }> }} data
 * @returns {Promise<string>} taskId
 */
export function remakeVideoSubmit(data) {
  return request({
    url: '/ohyesai-next/vio/resource/re-make-video-submit',
    method: 'post',
    data
  })
}

/**
 * 查询重新生成分镜任务状态（异步）
 * 响应结构示例：
 * {
 *   state: number, // 0 完成；1 处理中；2 失败
 *   data: {
 *     type: string,
 *     data: Array<{ videoFileId: string, videoUrl: string, coverFileId: string, coverUrl: string }>,
 *     offset: number
 *   }
 * }
 * @param {{ taskId: string }} params
 * @returns {Promise<{ state?: number, data?: { type?: string, data?: Array<{ videoFileId: string, videoUrl: string, coverFileId: string, coverUrl: string }>, offset?: number } }>}
 */
export function remakeVideoQuery(params) {
  return request({
    url: '/ohyesai-next/vio/resource/re-make-video-query',
    method: 'get',
    params
  })
}

/**
 * 直接编辑主体参考图-提交
 * 字段契约：sessionId / chunkId(=messageChunkId)：string；refImageFileIds：array<string>
 * @param {{ sessionId: string, chunkId: string, prompt: string, refImageFileIds?: string[] }} data
 * @returns {Promise<string>} taskId
 */
export function directEditSubjectSubmit(data) {
  return request({
    url: '/ohyesai-next/vio/resource/direct-edit-subject',
    method: 'post',
    data
  })
}

/**
 * 直接编辑分镜视频-提交
 * 字段契约：sessionId / chunkId(=messageChunkId) / model / visualPrompt：string；subject：array
 * @param {{ sessionId: string, chunkId: string, model?: string, visualPrompt: string, subject?: Array<{ subjectId?: string, fileId?: string, fileUrl?: string }> }} data
 * @returns {Promise<string>} taskId
 */
export function directEditSceneSubmit(data) {
  return request({
    url: '/ohyesai-next/vio/resource/direct-edit-scene',
    method: 'post',
    data
  })
}

/**
 * 切换 SUBJECT / SCENE 版本
 * 后端 API 字段名是 chunkId，值取自前端数据的 messageChunkId。
 * @param {{chunkId: string, activeVersion: number}} data
 * @returns {Promise<any>}
 */
export function switchVersion(data) {
  return request({
    url: '/ohyesai-next/vio/resource/switch-version',
    method: 'post',
    data,
  })
}

/**
 * 更新分镜脚本单行（提示词 / 参考图）
 * 后端 API 字段名是 chunkId，值取自前端数据的 messageChunkId。
 * @param {{ chunkId: string, scriptIdx: number, subjectRefs?: Array<{subjectId:string,fileId:string,url:string}>, visualPrompt?: string }} data
 * @returns {Promise<any>}
 */
export function updateSceneScript(data) {
  return request({
    url: '/ohyesai-next/vio/resource/update-scene-script',
    method: 'post',
    data,
  })
}

/**
 * 直接编辑任务查询
 * @param {{ taskId: string }} params
 * @returns {Promise<{ state?: number, data?: { type?: string, data?: any } }>} 
 */
export function directEditQuery(params) {
  return request({
    url: '/ohyesai-next/vio/resource/direct-edit-query',
    method: 'get',
    params
  })
}

/**
 * 重命名对话
 * @param {{ sessionId: string, name: string }} data
 */
export function renameChat(data) {
  return request({
    url: '/ohyesai-next/sse/vio/rename',
    method: 'post',
    data
  })
}

/**
 * 删除对话
 * @param {{ sessionId: string }} data
 */
export function deleteChat(data) {
  return request({
    url: '/ohyesai-next/sse/vio/delete',
    method: 'post',
    data
  })
}

/**
 * 添加反馈
 * 字段契约：messageId / attitudeType / content：string
 * @param {{ messageId: string, attitudeType: 'SATISFIED' | 'NOT_SATISFIED', content?: string }} data
 * @returns {Promise<any>}
 */
export function addFeedback(data) {
  return request({
    url: '/ohyesai-next/feedback/add',
    method: 'post',
    data,
  })
}

/**
 * 查询反馈
 * @param {{ messageId: string }} params
 * @returns {Promise<{ attitudeType: string, content: string }>}
 */
export function queryFeedback(params) {
  return request({
    url: '/ohyesai-next/feedback/query',
    method: 'get',
    params,
  })
}

/**
 * 获取预消耗积分
 * @param {{ modelName: string, taskType: string, duration?: number, resolution?: string }} params
 *   resolution（如 P720/P1080）：视频类（MAKE_MV）始终带上当前生效分辨率（effectiveResolution，前端可稳定获取）；图片类（MAKE_IMAGE）不涉及分辨率，可不传
 * @returns {Promise<number>} 预计消耗的积分数
 */
export function getPointsPrice(params) {
  return request({
    url: '/ohyesai-next/billing/get-points-price',
    method: 'get',
    params
  })
}

/**
 * 获取积分交易日志
 * @param {{ page: number, size: number }} params
 * @returns {Promise<{ total: number, data: Array<{ id: number, transactionType: string, amount: number, balanceAfter: number, description: string, createTime: string }> }>}
 */
export function getPointsLog(params) {
  return request({
    url: '/ohyesai-next/billing/points-log',
    method: 'get',
    params
  })
}

/**
 * 获取当前用户订阅计划信息（含积分余额）
 * @returns {Promise<{ pointsBalance: number, tierCode: string, tierName: string, expireTime: string }>}
 */
export function getUserPlan() {
  return request({
    url: '/ohyesai-next/billing/user-plan',
    method: 'get'
  })
}

/**
 * 获取对话 session 下的积分交易日志
 * @param {{ sessionId: string }} params
 * @returns {Promise<Array<{ id: number, transactionType: string, amount: number, balanceAfter: number, description: string, createTime: string }>>}
 */
export function getSessionPoints(params) {
  return request({
    url: '/ohyesai-next/billing/session-points',
    method: 'get',
    params
  })
}

/**
 * 获取订阅计划列表（免费 + 月付/年付分组）
 * @returns {Promise<{ free: Array<{ tierCode: string, tierName: string, price: string, grantedPoints: number }>, monthly: Array, yearly: Array }>}
 */
export function getSubscriptionPlan() {
  return request({
    url: '/ohyesai-next/billing/subscription-plan',
    method: 'get'
  })
}

/**
 * 获取积分包列表
 */
export function getPointsPackage() {
  return request({
    url: '/ohyesai-next/billing/points-package',
    method: 'get'
  })
}

/**
 * 统一支付下单
 * @param {{ tierCode: string, payKind: 'ALI_PC' | 'WX_PC' | 'WX_MP' }} data
 * @returns {Promise<{ tradeNo: string, payContent: string, wxMpPayContent: object, amountF: string }>}
 *   amountF: 实际支付金额（单位：分，已含升级折算），前端显示需 / 100 保留两位小数
 */
export function createTransaction(data) {
  return request({
    url: '/ohyesai-next/pay/transactions',
    method: 'post',
    data
  })
}

/**
 * 查询订单支付状态（0 成功；2 失败/超时；3 待支付）
 * @param {{ tradeNo: string }} params
 * @returns {Promise<number>}
 */
export function getTransactionState(params) {
  return request({
    url: '/ohyesai-next/pay/transactions-state',
    method: 'get',
    params
  })
}

/**
 * 获取口型同步预估积分（口型同步确认弹窗专用）。
 * 入参 messageChunkId 是"最近一条 SCENE 卡片的 messageChunkId"（不是 AI 消息的 messageId）。
 * 后端会按当前会话状态聚合所有需要应用口型同步的分镜，返回展示所需的全部字段——
 * 前端不再自己遍历 messages 算时长/积分/分镜列表。
 * @param {{ messageChunkId: string|number }} params
 * @returns {Promise<{
 *   totalPoints: number,
 *   totalDuration: number,
 *   scenes: Array<{ sceneId: string, duration: number, points: number }>
 * }>}
 */
export function getLipSyncPoints(params) {
  return request({
    url: '/ohyesai-next/vio/resource/get-lipsync-points',
    method: 'get',
    params
  })
}
