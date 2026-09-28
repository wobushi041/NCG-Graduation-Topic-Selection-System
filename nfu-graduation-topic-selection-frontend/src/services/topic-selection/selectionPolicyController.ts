// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** setDeptConfig POST /user/set/dept/config */
export async function setDeptConfigUsingPost(
  body: API.SetDeptConfigRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/set/dept/config', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getDeptConfig GET /user/get/dept/config */
export async function getDeptConfigUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseDeptConfigVO_>('/user/get/dept/config', {
    method: 'GET',
    ...(options || {}),
  });
}

/** delDeptConfig POST /user/del/dept/config */
export async function delDeptConfigUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/user/del/dept/config', {
    method: 'POST',
    ...(options || {}),
  });
}

/** setTeacherTopicAmount POST /user/set/teacher/topicAmount */
export async function setTeacherTopicAmountUsingPost(
  body: API.SetTeacherTopicAmountRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/set/teacher/topicAmount', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getTeacherTopicAmount POST /user/get/teacher/topicAmount */
export async function getTeacherTopicAmountUsingPost(
  body: API.GetTeacherTopicAmountRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseInt_>('/user/get/teacher/topicAmount', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}
