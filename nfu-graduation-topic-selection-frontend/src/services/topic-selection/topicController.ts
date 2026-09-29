// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** addTopic POST /user/add/topic */
export async function addTopicUsingPost(
  body: API.AddTopicRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong_>('/user/add/topic', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** updateTopic POST /user/update/topic */
export async function updateTopicUsingPost(
  body: API.UpdateTopicRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/update/topic', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteTopic POST /user/delete/topic */
export async function deleteTopicUsingPost(
  body: API.DeleteTopicRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/delete/topic', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** unsetTimeById POST /user/unset/time/by/id */
export async function unsetTimeByIdUsingPost(
  body: API.UnSetTimeRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseUnpublishTopicResultVO_>('/user/unset/time/by/id', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** checkTopic POST /user/check/topic */
export async function checkTopicUsingPost(
  body: API.CheckTopicRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/check/topic', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** setTimeById POST /user/set/time/by/id */
export async function setTimeByIdUsingPost(
  body: API.SetTimeRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/set/time/by/id', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** setTopicLock POST /user/topic_lock */
export async function setTopicLockUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.setTopicLockUsingPOSTParams,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/topic_lock', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getTopicLock GET /user/topic_lock */
export async function getTopicLockUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseTopicLockVO_>('/user/topic_lock', {
    method: 'GET',
    ...(options || {}),
  });
}

/** getTopicReviewLevel POST /user/get/topic/review_level */
export async function getTopicReviewLevelUsingPost(
  body: API.GetTopicReviewLevelRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseAIResult_>('/user/get/topic/review_level', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}
