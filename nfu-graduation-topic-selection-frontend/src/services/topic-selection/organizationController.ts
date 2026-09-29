// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** addCollege POST /organization/add/college */
export async function addCollegeUsingPost(body: API.CollegeAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/organization/add/college', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteCollege POST /organization/delete/college */
export async function deleteCollegeUsingPost(
  body: API.DeleteCollegeRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/organization/delete/college', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getCollege POST /organization/get/college/page */
export async function getCollegeUsingPost(
  body: API.CollegeQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageCollege_>('/organization/get/college/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getCollegeList POST /organization/get/college/list */
export async function getCollegeListUsingPost(
  body: API.CollegeQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListCollegeVO_>('/organization/get/college/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** addMajor POST /organization/add/major */
export async function addMajorUsingPost(
  body: API.MajorAddRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong_>('/organization/add/major', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteMajor POST /organization/delete/major */
export async function deleteMajorUsingPost(
  body: API.DeleteMajorRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/organization/delete/major', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getMajor POST /organization/get/major/page */
export async function getMajorUsingPost(
  body: API.MajorQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageMajor_>('/organization/get/major/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getMajorList POST /organization/get/major/list */
export async function getMajorListUsingPost(
  body: API.MajorQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListMajorVO_>('/organization/get/major/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** updateMajorGroup POST /organization/update/major/group */
export async function updateMajorGroupUsingPost(
  body: API.MajorGroupUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/organization/update/major/group', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** addTopicGroup POST /organization/topic-group/add */
export async function addTopicGroupUsingPost(body: API.TopicGroupAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/organization/topic-group/add', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  });
}

/** updateTopicGroup POST /organization/topic-group/update */
export async function updateTopicGroupUsingPost(body: API.TopicGroupUpdateRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/organization/topic-group/update', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  });
}

/** deleteTopicGroup POST /organization/topic-group/delete */
export async function deleteTopicGroupUsingPost(body: API.TopicGroupDeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/organization/topic-group/delete', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  });
}

/** getTopicGroupPage POST /organization/topic-group/page */
export async function getTopicGroupPageUsingPost(body: API.TopicGroupQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageTopicGroup_>('/organization/topic-group/page', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  });
}

/** getTopicGroupList POST /organization/topic-group/list */
export async function getTopicGroupListUsingPost(body: API.TopicGroupQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseListTopicGroupVO_>('/organization/topic-group/list', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  });
}
