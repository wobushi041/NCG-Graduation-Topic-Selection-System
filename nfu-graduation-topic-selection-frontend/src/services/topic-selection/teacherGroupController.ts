// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** 批量查询教师选题组额度 POST /user/teacher/groups/batch */
export async function getTeacherGroupsBatchUsingPost(
  body: { teacherAccounts?: string[] },
  options?: { [key: string]: any },
) {
  return request<{
    code?: number;
    message?: string;
    data?: Record<
      string,
      { topicGroupId: number; groupName: string; maxTopics: number; remaining: number }[]
    >;
  }>('/user/teacher/groups/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** 修改教师选题组额度 POST /user/teacher/group/quota */
export async function updateTeacherGroupQuotaUsingPost(
  body: { teacherAccount: string; topicGroupId: number; maxTopics: number },
  options?: { [key: string]: any },
) {
  return request<{ code?: number; message?: string; data?: boolean }>('/user/teacher/group/quota', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** 查询系统内现有选题组列表 GET /user/group/list */
export async function getGroupListUsingGet(options?: { [key: string]: any }) {
  return request<{
    code?: number;
    message?: string;
    data?: string[];
  }>('/user/group/list', {
    method: 'GET',
    ...(options || {}),
  });
}
