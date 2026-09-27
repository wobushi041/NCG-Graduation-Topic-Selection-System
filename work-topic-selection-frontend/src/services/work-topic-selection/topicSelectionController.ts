// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** selectTopicById POST /user/select/topic/by/id */
export async function selectTopicByIdUsingPost(
  body: API.SelectTopicByIdRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong_>('/user/select/topic/by/id', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** preSelectTopicById POST /user/preselect/topic/by/id */
export async function preSelectTopicByIdUsingPost(
  body: API.SelectTopicByIdRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong_>('/user/preselect/topic/by/id', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** selectStudent POST /user/select/student */
export async function selectStudentUsingPost(
  body: API.SelectStudentRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/select/student', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** withdraw POST /user/withdraw */
export async function withdrawUsingPost(
  body: API.WithdrawRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/withdraw', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getSelectTopic POST /user/get/select/topic */
export async function getSelectTopicUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseListTopic_>('/user/get/select/topic', {
    method: 'POST',
    ...(options || {}),
  });
}

/** getPreTopic POST /user/get/preselect/topic */
export async function getPreTopicUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseListTopic_>('/user/get/preselect/topic', {
    method: 'POST',
    ...(options || {}),
  });
}

/** getSelectTopicById POST /user/get/select/topic/by/id */
export async function getSelectTopicByIdUsingPost(
  body: API.GetSelectTopicById,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListUser_>('/user/get/select/topic/by/id', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getSelectTopicTime POST /user/get/select/topic/choice_time */
export async function getSelectTopicTimeUsingPost(
  body: API.GetSelectTopicRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString_>('/user/get/select/topic/choice_time', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getStudentByTopicId POST /user/get/student/by/topicId */
export async function getStudentByTopicIdUsingPost(
  body: API.GetStudentByTopicId,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListUser_>('/user/get/student/by/topicId', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}
