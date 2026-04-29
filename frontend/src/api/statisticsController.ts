/* eslint-disable */
// @ts-nocheck - 自动生成的 API 文件，忽略类型检查
import request from '@/request'

/** 获取系统统计数据 GET /statistics/overview */
export async function getStatistics(options?: { [key: string]: any }) {
  return request<API.BaseResponseStatisticsVO>('/statistics/overview', {
    method: 'GET',
    ...(options || {}),
  })
}
