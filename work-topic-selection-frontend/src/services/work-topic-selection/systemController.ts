// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** getViewTopicStatus GET /user/view_topic */
export async function getViewTopicStatusUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/user/view_topic', {
    method: 'GET',
    ...(options || {}),
  });
}

/** setViewTopicStatus POST /user/view_topic */
export async function setViewTopicStatusUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.setViewTopicStatusUsingPOSTParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/view_topic', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getSwitchSingleChoiceStatus GET /user/switch_single_choice */
export async function getSwitchSingleChoiceStatusUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/user/switch_single_choice', {
    method: 'GET',
    ...(options || {}),
  });
}

/** setSwitchSingleChoiceStatus POST /user/switch_single_choice */
export async function setSwitchSingleChoiceStatusUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.setSwitchSingleChoiceStatusUsingPOSTParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/switch_single_choice', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getCrossTopicStatus GET /user/cross_topic */
export async function getCrossTopicStatusUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/user/cross_topic', {
    method: 'GET',
    ...(options || {}),
  });
}

/** setCrossTopicStatus POST /user/cross_topic */
export async function setCrossTopicStatusUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.setCrossTopicStatusUsingPOSTParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/cross_topic', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getSystemInfo GET /user/get/system/info */
export async function getSystemInfoUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseTheSystemInfoVO_>('/user/get/system/info', {
    method: 'GET',
    ...(options || {}),
  });
}

/** test GET /user/test */
export async function testUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseString_>('/user/test', {
    method: 'GET',
    ...(options || {}),
  });
}
