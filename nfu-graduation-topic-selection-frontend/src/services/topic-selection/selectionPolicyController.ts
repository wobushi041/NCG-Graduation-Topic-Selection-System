// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** setCollegeConfig POST /user/set/college/config */
export async function setCollegeConfigUsingPost(
  body: API.SetCollegeConfigRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/set/college/config', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getCollegeConfig GET /user/get/college/config */
export async function getCollegeConfigUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseCollegeConfigVO_>('/user/get/college/config', {
    method: 'GET',
    ...(options || {}),
  });
}

/** delCollegeConfig POST /user/del/college/config */
export async function delCollegeConfigUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/user/del/college/config', {
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
