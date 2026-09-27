const fs = require('fs');
const path = require('path');

const servicesDirectory = path.resolve(__dirname, '../src/services/work-topic-selection');
const authSource = fs.readFileSync(path.join(servicesDirectory, 'authController.ts'), 'utf8');
const legacyUserSource = fs.readFileSync(path.join(servicesDirectory, 'userController.ts'), 'utf8');

describe('authentication API contract', () => {
  test('declares all ten /auth endpoints', () => {
    const paths = [
      '/auth/login',
      '/auth/logout',
      '/auth/role-switch',
      '/auth/role-switch/availability',
      '/auth/password/admin-reset',
      '/auth/password/change',
      '/auth/password/reset',
      '/auth/password/reset-code/send',
      '/auth/email-verification/code/send',
      '/auth/email-verification/code/verify',
    ];

    paths.forEach(apiPath => expect(authSource).toContain(`'${apiPath}'`));
  });

  test('removes the nine legacy authentication endpoints from frontend services', () => {
    const legacyPaths = [
      '/user/login',
      '/user/logout',
      '/user/toggle/login',
      '/user/toggle/available',
      '/user/reset/password',
      '/user/updata/password',
      '/user/send/code',
      '/user/send/captcha',
      '/user/check/captcha',
    ];
    const serviceSources = authSource + legacyUserSource;

    legacyPaths.forEach(apiPath => expect(serviceSources).not.toContain(`'${apiPath}'`));
  });
});
