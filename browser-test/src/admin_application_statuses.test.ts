import {
  startSession,
  logout,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsAdmin,
  selectApplicantLanguage,
  ApplicantQuestions,
  AdminPrograms,
  userDisplayName,
  AdminProgramStatuses,
  enableFeatureFlag,
  gotoEndpoint,
  disableFeatureFlag,
} from './support'
import {Page} from 'playwright'
import EmptyFunction = jest.EmptyFunction

const describeIf = (
  condition: () => boolean,
  name: string,
  fn: EmptyFunction,
) => (condition() ? describe(name, fn) : describe.skip(name, fn))

describeIf(
  () => !process.env.BROWSER_TESTS_ONLY_RUN_PROBER_TESTS,
'view program statuses', () => {
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let applicantQuestions: ApplicantQuestions
  let adminProgramStatuses: AdminProgramStatuses

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    adminPrograms = new AdminPrograms(pageObject)
    applicantQuestions = new ApplicantQuestions(pageObject)
    adminProgramStatuses = new AdminProgramStatuses(pageObject)
    await enableFeatureFlag(pageObject, 'application_status_tracking_enabled')
    // Need to navigate back to the Admin page after enabling the feature flag.
    await gotoEndpoint(pageObject, '')
  })

  afterAll(async () => {
    await disableFeatureFlag(pageObject, 'application_status_tracking_enabled')
  })

  describe('without program statuses', () => {
    const programWithoutStatusesName = 'test program without statuses'
    beforeAll(async () => {
      await loginAsAdmin(pageObject)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(
        programWithoutStatusesName,
      )
      await applicantQuestions.submitFromPreviewPage()

      await logout(pageObject)
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('does not Show status options', async () => {
      await loginAsProgramAdmin(pageObject)

      await adminPrograms.viewApplications(programWithoutStatusesName)

      await adminPrograms.viewApplicationForApplicant(userDisplayName())

      expect(await adminPrograms.isStatusSelectorVisible()).toBe(false)
    })
  })

  describe('with program statuses', () => {
    const programWithStatusesName = 'test program with statuses'
    const statusName = 'Status 1'
    beforeAll(async () => {
      await loginAsAdmin(pageObject)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithStatusesName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(
        programWithStatusesName,
      )
      await adminProgramStatuses.createStatus(statusName)
      await adminPrograms.publishProgram(programWithStatusesName)
      await adminPrograms.expectActiveProgram(programWithStatusesName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage()

      await logout(pageObject)
      await loginAsProgramAdmin(pageObject)

      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant(userDisplayName())
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('shows status selector', async () => {
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(true)
    })

    it('shows default option as placeholder', async () => {
      expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
    })
  })
})
