const fs = require('fs');
const path = require('path');

const servicesDirectory = path.resolve(__dirname, '../src/services/topic-selection');

describe('11-controller frontend service modularization contract', () => {
  const controllerFiles = [
    'authController.ts',
    'userController.ts',
    'organizationController.ts',
    'teacherGroupController.ts',
    'topicController.ts',
    'topicSelectionController.ts',
    'topicQueryController.ts',
    'selectionPolicyController.ts',
    'systemController.ts',
    'fileController.ts',
    'aiController.ts',
  ];

  test('provides all 11 domain controller service files matching backend controllers', () => {
    controllerFiles.forEach(fileName => {
      expect(fs.existsSync(path.join(servicesDirectory, fileName))).toBe(true);
    });
  });

  test('exports all 11 controller namespaces from services index.ts', () => {
    const indexSource = fs.readFileSync(path.join(servicesDirectory, 'index.ts'), 'utf8');
    [
      'authController',
      'userController',
      'organizationController',
      'teacherGroupController',
      'topicController',
      'topicSelectionController',
      'topicQueryController',
      'selectionPolicyController',
      'systemController',
      'fileController',
      'aiController',
    ].forEach(moduleName => {
      expect(indexSource).toContain(moduleName);
    });
  });

  test('re-exports split /user domain modules in userController.ts and removes dead /user/add/count', () => {
    const userControllerSource = fs.readFileSync(
      path.join(servicesDirectory, 'userController.ts'),
      'utf8',
    );
    [
      './organizationController',
      './selectionPolicyController',
      './systemController',
      './teacherGroupController',
      './topicController',
      './topicQueryController',
      './topicSelectionController',
    ].forEach(reExportModule => {
      expect(userControllerSource).toContain(`export * from '${reExportModule}';`);
    });
    expect(userControllerSource).not.toContain('/user/add/count');
  });

  test('updates teacher quota through the topic-group quota endpoint', () => {
    const teacherGroupControllerSource = fs.readFileSync(
      path.join(servicesDirectory, 'teacherGroupController.ts'),
      'utf8',
    );
    const adjustLimitButtonSource = fs.readFileSync(
      path.resolve(__dirname, '../src/components/AdjustLimitButton/index.tsx'),
      'utf8',
    );

    expect(teacherGroupControllerSource).toContain('updateTeacherGroupQuotaUsingPost');
    expect(teacherGroupControllerSource).toContain('/user/teacher/group/quota');
    expect(adjustLimitButtonSource).toContain('updateTeacherGroupQuotaUsingPost');
    expect(adjustLimitButtonSource).toContain('topicGroupId: selectedGroup.topicGroupId');
    expect(adjustLimitButtonSource).not.toContain('setTeacherTopicAmountUsingPost');
  });

  test('renders organization names instead of database foreign-key IDs', () => {
    const pageFiles = [
      'MajorList/index.tsx',
      'TopicGroupList/index.tsx',
      'TopicLeaderList/index.tsx',
      'TeacherList/index.tsx',
      'StudentList/index.tsx',
      'setTopicTime/index.tsx',
      'ViewStudentTopic/index.tsx',
      'ViewTopic/index.tsx',
      'studentSelection/index.tsx',
    ];

    pageFiles.forEach(fileName => {
      const pageSource = fs.readFileSync(
        path.resolve(__dirname, '../src/pages', fileName),
        'utf8',
      );
      expect(pageSource).not.toMatch(/学院 ID|专业 ID|选题组 ID/);
    });

    const typingsSource = fs.readFileSync(path.join(servicesDirectory, 'typings.d.ts'), 'utf8');
    expect(typingsSource).toContain('collegeName?: string;');
    expect(typingsSource).toContain('topicGroupName?: string;');
  });
});
