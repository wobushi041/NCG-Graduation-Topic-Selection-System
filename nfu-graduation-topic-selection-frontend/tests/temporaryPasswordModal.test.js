const fs = require('fs');
const path = require('path');

const srcDir = path.resolve(__dirname, '../src');
const utilSource = fs.readFileSync(
  path.join(srcDir, 'utils/showTemporaryPasswordModal.tsx'),
  'utf8',
);

const extractTemporaryPassword = (rawMessage) => {
  if (!rawMessage) {
    return '';
  }
  const match = rawMessage.match(/临时密码[^：:]*[：:]\s*(.+)$/);
  if (match && match[1]) {
    return match[1].trim();
  }
  return rawMessage.trim();
};

describe('temporary password ModalForm integration', () => {
  test('extracts one-time temporary password from /user/add response message', () => {
    expect(
      extractTemporaryPassword('成功；临时密码（仅显示一次）：a6bYPsvx_pk4uC@N'),
    ).toBe('a6bYPsvx_pk4uC@N');
    expect(
      extractTemporaryPassword('重置成功，临时密码: TempPass#2026'),
    ).toBe('TempPass#2026');
  });

  test('uses @ant-design/pro-components ModalForm and ProFormText with copy action', () => {
    expect(utilSource).toContain("from '@ant-design/pro-components'");
    expect(utilSource).toContain('<ModalForm');
    expect(utilSource).toContain('<ProFormText');
    expect(utilSource).toContain('maskClosable: false');
    expect(utilSource).toContain('复制密码并关闭');
  });

  test.each([
    'pages/AdminList/index.tsx',
    'pages/TopicLeaderList/index.tsx',
    'pages/TeacherList/index.tsx',
    'pages/StudentList/index.tsx',
  ])('%s mounts temporaryPasswordModalNode and calls showTemporaryPasswordModal', (pagePath) => {
    const pageSource = fs.readFileSync(path.join(srcDir, pagePath), 'utf8');
    expect(pageSource).toContain('useTemporaryPasswordModal');
    expect(pageSource).toContain('{temporaryPasswordModalNode}');
    expect(pageSource).toContain('showTemporaryPasswordModal');
  });
});
