import { startSession, loginAsAdmin, endSession, AdminTIGroups } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(2000);

    await loginAsAdmin(page);
    const adminGroups = new AdminTIGroups(page);
    await adminGroups.gotoAdminTIPage();
    await adminGroups.fillInGroupBasics("group name", "group description");
    await adminGroups.expectGroupExist("group name", "group description");
    await endSession(browser);
  })
})
