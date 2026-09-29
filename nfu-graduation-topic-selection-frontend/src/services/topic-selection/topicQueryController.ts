// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** getTopicList POST /user/get/topic/page */
export async function getTopicListUsingPost(
  body: API.TopicQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageTopic_>('/user/get/topic/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getTopicListByAdmin POST /user/get/topic/list/by/admin */
export async function getTopicListByAdminUsingPost(
  body: API.TopicQueryByAdminRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageTopic_>('/user/get/topic/list/by/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getTeacher POST /user/get/teacher */
export async function getTeacherUsingPost1(
  body: API.TeacherQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListTeacherVO_>('/user/get/teacher', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getTeacher POST /user/get/college/teacher */
export async function getTeacherUsingPost(
  body: API.TopicLeaderQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageTopicLeaderVO_>('/user/get/college/teacher', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  });
}

/** getTeacherByAdmin POST /user/get/college/teacher/by/admin */
export async function getTeacherByAdminUsingPost(
  body: API.TopicLeaderQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageTopicLeaderVO_>('/user/get/college/teacher/by/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getUserList POST /user/get/user/list */
export async function getUserListUsingPost(
  body: API.GetUserListRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListUserNameVO_>('/user/get/user/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getUserByAccount POST /user/get/user/by/account */
export async function getUserByAccountUsingPost(
  body: { userAccount?: string },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseUser_>('/user/get/user/by/account', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getUnSelectTopicStudentList POST /user/get/unselect/topic/student/list */
export async function getUnSelectTopicStudentListUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseListUser_>('/user/get/unselect/topic/student/list', {
    method: 'POST',
    ...(options || {}),
  });
}

/** getSelectTopicSituation POST /user/get/select/topic/situation */
export async function getSelectTopicSituationUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseSituationVO_>('/user/get/select/topic/situation', {
    method: 'POST',
    ...(options || {}),
  });
}
