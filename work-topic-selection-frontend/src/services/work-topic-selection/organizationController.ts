// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';

/** addDept POST /user/add/dept */
export async function addDeptUsingPost(body: API.DeptAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/user/add/dept', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteDept POST /user/delete/dept */
export async function deleteDeptUsingPost(
  body: API.DeleteDeptRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/delete/dept', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getDept POST /user/get/dept/page */
export async function getDeptUsingPost(
  body: API.DeptQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageDept_>('/user/get/dept/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getDeptList POST /user/get/dept/list */
export async function getDeptListUsingPost(
  body: API.DeptQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListDeptVO_>('/user/get/dept/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** addProject POST /user/add/project */
export async function addProjectUsingPost(
  body: API.ProjectAddRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong_>('/user/add/project', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteProject POST /user/delete/project */
export async function deleteProjectUsingPost(
  body: API.DeleteProjectRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean_>('/user/delete/project', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getProject POST /user/get/project/page */
export async function getProjectUsingPost(
  body: API.ProjectQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageProject_>('/user/get/project/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getProjectList POST /user/get/project/list */
export async function getProjectListUsingPost(
  body: API.ProjectQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseListProjectVO_>('/user/get/project/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** getTeacher POST /user/get/dept/teacher */
export async function getTeacherUsingPost(
  body: API.DeptTeacherQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageDeptTeacherVO_>('/user/get/dept/teacher', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}
