declare namespace API {
  type AddCountRequest = {
    count?: number;
    id?: number;
  };

  type AddTopicRequest = {
    description?: string;
    requirement?: string;
    surplusQuantity?: number;
    teacherName?: string;
    topicGroupId?: number;
    topic?: string;
    type?: string;
  };

  type AIResult = {
    description?: string;
    level?: string;
  };

  type AiSendRequest = {
    content?: string;
  };

  type BaseResponseAIResult_ = {
    code?: number;
    data?: AIResult;
    message?: string;
  };

  type BaseResponseBoolean_ = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseCollegeConfigVO_ = {
    code?: number;
    data?: CollegeConfigVO;
    message?: string;
  };

  type BaseResponseInt_ = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponseListCollegeVO_ = {
    code?: number;
    data?: CollegeVO[];
    message?: string;
  };

  type BaseResponseListMajorVO_ = {
    code?: number;
    data?: MajorVO[];
    message?: string;
  };

  type BaseResponseListTeacherVO_ = {
    code?: number;
    data?: TeacherVO[];
    message?: string;
  };

  type BaseResponseListTopic_ = {
    code?: number;
    data?: Topic[];
    message?: string;
  };

  type BaseResponseListUser_ = {
    code?: number;
    data?: User[];
    message?: string;
  };

  type BaseResponseListUserNameVO_ = {
    code?: number;
    data?: UserNameVO[];
    message?: string;
  };

  type BaseResponseLoginUserVO_ = {
    code?: number;
    data?: LoginUserVO;
    message?: string;
  };

  type BaseResponseLong_ = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponsePageCollege_ = {
    code?: number;
    data?: PageCollege_;
    message?: string;
  };

  type BaseResponsePageTopicLeaderVO_ = {
    code?: number;
    data?: PageTopicLeaderVO_;
    message?: string;
  };

  type BaseResponsePageMajor_ = {
    code?: number;
    data?: PageMajor_;
    message?: string;
  };

  type BaseResponsePageTopic_ = {
    code?: number;
    data?: PageTopic_;
    message?: string;
  };

  type BaseResponsePageUser_ = {
    code?: number;
    data?: PageUser_;
    message?: string;
  };

  type BaseResponsePageUserVO_ = {
    code?: number;
    data?: PageUserVO_;
    message?: string;
  };

  type BaseResponseSituationVO_ = {
    code?: number;
    data?: SituationVO;
    message?: string;
  };

  type BaseResponseString_ = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseTheSystemInfoVO_ = {
    code?: number;
    data?: TheSystemInfoVO;
    message?: string;
  };

  type BaseResponseTopicLockVO_ = {
    code?: number;
    data?: TopicLockVO;
    message?: string;
  };

  type BaseResponseUser_ = {
    code?: number;
    data?: User;
    message?: string;
  };

  type BaseResponseUserVO_ = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type CaptchaRequest = {
    email?: string;
  };

  type CheckCaptchaRequest = {
    captcha?: string;
    email?: string;
  };

  type CheckTopicRequest = {
    id?: number;
    reason?: string;
    status?: number;
  };

  type DeleteCollegeRequest = {
    collegeId?: number;
  };

  type DeleteMajorRequest = {
    majorId?: number;
  };

  type DeleteRequest = {
    userAccount?: string;
  };

  type DeleteTopicRequest = {
    id?: number;
  };

  type College = {
    createTime?: string;
    collegeName?: string;
    id?: number;
    isDelete?: number;
    updateTime?: string;
  };

  type CollegeAddRequest = {
    collegeName?: string;
  };

  type CollegeConfigVO = {
    enableSelectCollegesList?: Record<string, any>;
  };

  type CollegeQueryRequest = {
    current?: number;
    collegeName?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
  };

  type TopicLeaderQueryRequest = {
    current?: number;
    collegeId?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    teacherName?: string;
  };

  type TopicLeaderVO = {
    collegeId?: number;
    collegeName?: string;
    selectAmount?: number;
    surplusQuantity?: number;
    teacherName?: string;
    topicAmount?: number;
  };

  type CollegeVO = {
    label?: string;
    value?: number;
  };

  type GetSelectTopicById = {
    id?: number;
  };

  type GetSelectTopicRequest = {
    topicId?: number;
  };

  type GetStudentByTopicId = {
    id?: number;
  };

  type GetTeacherTopicAmountRequest = {
    teacherId?: number;
  };

  type GetTopicReviewLevelRequest = {
    description?: string;
    requirement?: string;
    topic?: string;
    type?: string;
  };

  type getUserByIdUsingGETParams = {
    /** id */
    id?: number;
  };

  type GetUserListRequest = {
    userRole?: number;
  };

  type getUserVOByIdUsingGETParams = {
    /** id */
    id?: number;
  };

  type LoginUserVO = {
    createTime?: string;
    collegeId?: number;
    email?: string;
    id?: number;
    majorId?: number;
    topicGroupId?: number;
    updateTime?: string;
    userAvatar?: string;
    userName?: string;
    userRole?: number;
  };

  type OrderItem = {
    asc?: boolean;
    column?: string;
  };

  type PageCollege_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: College[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type PageTopicLeaderVO_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: TopicLeaderVO[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type PageMajor_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: Major[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type PageTopic_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: Topic[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type PageUser_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: User[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type PageUserVO_ = {
    countId?: string;
    current?: number;
    maxLimit?: number;
    optimizeCountSql?: boolean;
    orders?: OrderItem[];
    pages?: number;
    records?: UserVO[];
    searchCount?: boolean;
    size?: number;
    total?: number;
  };

  type Major = {
    createTime?: string;
    collegeId?: number;
    topicGroupId?: number;
    id?: number;
    isDelete?: number;
    majorName?: string;
    updateTime?: string;
  };

  type MajorAddRequest = {
    collegeId?: number;
    topicGroupId?: number;
    majorName?: string;
  };

  type MajorGroupUpdateRequest = {
    topicGroupId?: number;
    majorId?: number;
  };

  type MajorQueryRequest = {
    current?: number;
    collegeId?: number;
    topicGroupId?: number;
    pageSize?: number;
    majorName?: string;
    sortField?: string;
    sortOrder?: string;
  };

  type MajorVO = {
    label?: string;
    value?: number;
  };

  type ResetPasswordRequest = {
    userAccount?: string;
    userName?: string;
  };

  type SelectStudentRequest = {
    topic?: string;
    userAccount?: string;
  };

  type SelectTopicByIdRequest = {
    id?: number;
    status?: number;
  };

  type SendCodeRequest = {
    userAccount?: string;
  };

  type setCrossTopicStatusUsingPOSTParams = {
    /** enabled */
    enabled: boolean;
  };

  type SetCollegeConfigRequest = {
    enableSelectCollegesList?: Record<string, any>;
  };

  type setSwitchSingleChoiceStatusUsingPOSTParams = {
    /** enabled */
    enabled: boolean;
  };

  type SetTeacherTopicAmountRequest = {
    teacherId?: number;
    topicAmount?: number;
  };

  type SetTimeRequest = {
    endTime?: string;
    startTime?: string;
    topicIds?: number[];
  };

  type setTopicLockUsingPOSTParams = {
    /** enabled */
    enabled: boolean;
    /** timestamp */
    timestamp?: string;
  };

  type setViewTopicStatusUsingPOSTParams = {
    /** enabled */
    enabled: boolean;
  };

  type SituationVO = {
    amount?: number;
    selectAmount?: number;
    unselectAmount?: number;
  };

  type TeacherQueryRequest = {
    userRole?: number;
  };

  type TeacherVO = {
    label?: string;
    value?: string;
  };

  type TheSystemInfoVO = {
    auditBackTopicCount?: number;
    auditPassTopicCount?: number;
    auditTopicCount?: number;
    cpuUsage?: string;
    diskUsage?: string;
    jvmMemoryUsage?: string;
    loginUserCount?: number;
    memoryUsage?: string;
    releaseTopicCount?: number;
    totalCollegeCount?: number;
    totalStudentCount?: number;
    totalTeacherCount?: number;
  };

  type Topic = {
    createTime?: string;
    topicGroupId?: number;
    topicGroupName?: string;
    description?: string;
    endTime?: string;
    id?: number;
    isDelete?: number;
    reason?: string;
    requirement?: string;
    selectAmount?: number;
    startTime?: string;
    status?: number;
    surplusQuantity?: number;
    teacherName?: string;
    teacherAccount?: string;
    topic?: string;
    type?: string;
    updateTime?: string;
  };

  type TopicLockVO = {
    islock?: boolean;
    lockTime?: string;
  };

  type TopicQueryByAdminRequest = {
    current?: number;
    topicGroupId?: number;
    endTime?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    startTime?: string;
    teacherName?: string;
    topic?: string;
    type?: string;
  };

  type TopicQueryRequest = {
    current?: number;
    topicGroupId?: number;
    endTime?: string;
    isNoOneSelectedTopic?: boolean;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    startTime?: string;
    status?: number;
    teacherName?: string;
    topic?: string;
    type?: string;
  };

  type UnSetTimeRequest = {
    topicIds?: number[];
  };

  type UpdateTopicRequest = {
    description?: string;
    requirement?: string;
    surplusQuantity?: number;
    topicGroupId?: number;
    topicName?: string;
    type?: string;
  };

  type WithdrawRequest = {
    id?: number;
    userAccount?: string;
  };

  type uploadFileUsingPOSTParams = {
    status?: number;
  };

  type User = {
    createTime?: string;
    collegeId?: number;
    email?: string;
    id?: number;
    isDelete?: number;
    majorId?: number;
    topicGroupId?: number;
    status?: string;
    topicAmount?: number;
    updateTime?: string;
    userAccount?: string;
    userName?: string;
    userPassword?: string;
    userRole?: number;
  };

  type UserAddRequest = {
    collegeId?: number;
    majorId?: number;
    topicGroupId?: number;
    userAccount?: string;
    userName?: string;
    userRole?: number;
  };

  type UserLoginRequest = {
    userAccount?: string;
    userPassword?: string;
  };

  type UserNameVO = {
    userName?: string;
  };

  type UserQueryRequest = {
    current?: number;
    collegeId?: number;
    majorId?: number;
    topicGroupId?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userAccount?: string;
    userName?: string;
    userRole?: number;
  };

  type UserToggleRequest = {
    userRole?: number;
  };

  type UserUpdatePassword = {
    code?: string;
    email?: string;
    updatePassword?: string;
    userAccount?: string;
    userPassword?: string;
  };

  type UserUpdateRequest = {
    id?: number;
    userName?: string;
    userRole?: number;
    collegeId?: number;
    majorId?: number;
    topicGroupId?: number;
  };

  type UserVO = {
    createTime?: string;
    id?: number;
    status?: string;
    userAvatar?: string;
    userName?: string;
    userRole?: number;
    collegeId?: number;
    majorId?: number;
    topicGroupId?: number;
  };

  type TopicGroup = {
    id?: number;
    collegeId?: number;
    groupName?: string;
    createTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type TopicGroupVO = { label?: string; value?: number; collegeId?: number };
  type TopicGroupAddRequest = { collegeId?: number; groupName?: string };
  type TopicGroupUpdateRequest = { id?: number; collegeId?: number; groupName?: string };
  type TopicGroupDeleteRequest = { id?: number };
  type TopicGroupQueryRequest = { current?: number; pageSize?: number; collegeId?: number; groupName?: string; sortField?: string; sortOrder?: string };
  type PageTopicGroup_ = { current?: number; size?: number; total?: number; records?: TopicGroup[] };
  type BaseResponsePageTopicGroup_ = { code?: number; data?: PageTopicGroup_; message?: string };
  type BaseResponseListTopicGroupVO_ = { code?: number; data?: TopicGroupVO[]; message?: string };
}
