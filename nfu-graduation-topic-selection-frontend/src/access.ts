/**
 * @see https://umijs.org/docs/max/access#access
 * */

export default function access(initialState: { // @ts-ignore
  currentUser?: API.CurrentUser
} | undefined) {
  const {currentUser} = initialState ?? {};
  return {
    canAccessStudent: [0].includes(<number>currentUser?.userRole),
    canAccessTeacher: [1].includes(<number>currentUser?.userRole),
    canAccessTopicLeader: [2].includes(<number>currentUser?.userRole),
    canAccessAdmin: [3].includes(<number>currentUser?.userRole),
    canAccessUsers: [0, 1, 2, 3].includes(<number>currentUser?.userRole),
    canAccessNotStudent: [1, 2, 3].includes(<number>currentUser?.userRole),
    canAccessNotStudentAndTeacher: [2, 3].includes(<number>currentUser?.userRole),
  };
}
