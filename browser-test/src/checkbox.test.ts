import {test, expect} from '@playwright/test'
import {
  createTestContext,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from './support'

test.describe('Checkbox question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  test.describe('single checkbox question', () => {
    const programName = 'Test program for single checkbox'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      // As admin, create program with single checkbox question.
      await loginAsAdmin(page)

      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-color-q',
        options: [
          {adminName: 'red_admin', text: 'red'},
          {adminName: 'green_admin', text: 'green'},
          {adminName: 'orange_admin', text: 'orange'},
          {adminName: 'blue_admin', text: 'blue'},
        ],
        minNum: 1,
        maxNum: 2,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['checkbox-color-q'],
        programName,
      )

      await logout(page)
    })

    test('Updates options in preview', async () => {
      const {page, adminQuestions} = ctx
      await loginAsAdmin(page)

      await adminQuestions.createCheckboxQuestion(
        {
          questionName: 'not-used-in-test',
          questionText: 'Sample question text',
          helpText: 'Sample question help text',
          options: [
            {adminName: 'red_admin', text: 'red'},
            {adminName: 'green_admin', text: 'green'},
            {adminName: 'orange_admin', text: 'orange'},
            {adminName: 'blue_admin', text: 'blue'},
          ],
        },
        /* clickSubmit= */ false,
      )

      // Verify question preview has the default values.
      await adminQuestions.expectCommonPreviewValues({
        questionText: 'Sample question text',
        questionHelpText: 'Sample question help text',
      })
      await adminQuestions.expectPreviewOptions([
        'red',
        'green',
        'orange',
        'blue',
      ])

      // Empty options renders default text.
      await adminQuestions.createCheckboxQuestion(
        {
          questionName: '',
          questionText: 'Sample question text',
          helpText: 'Sample question help text',
          options: [],
        },
        /* clickSubmit= */ false,
      )
      await adminQuestions.expectPreviewOptions(['Sample question option'])
    })

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'checkbox')
    })

    test('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'checkbox-errors')
    })

    test('with single checked box submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCheckboxQuestion(['blue'])
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with no checked boxes does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      // No validation errors on first page load.
      const checkBoxError = '.cf-applicant-question-errors'
      expect(await page.isHidden(checkBoxError)).toEqual(true)

      // Click next without selecting anything.
      await applicantQuestions.clickNext()

      // Check checkbox error and required error are present.
      expect(await page.isHidden(checkBoxError)).toEqual(false)
      const checkboxId = '.cf-question-checkbox'
      expect(await page.innerText(checkboxId)).toContain(
        'This question is required.',
      )
    })

    test('with greater than max allowed checked boxes does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const checkBoxError = '.cf-applicant-question-errors'
      // No validation errors on first page load.
      expect(await page.isHidden(checkBoxError)).toEqual(true)

      // Max of two checked boxes are allowed, but we select three.
      await applicantQuestions.answerCheckboxQuestion([
        'blue',
        'green',
        'orange',
      ])
      await applicantQuestions.clickNext()

      // Check error is shown.
      expect(await page.isHidden(checkBoxError)).toEqual(false)
    })
  })

  test.describe('multiple checkbox questions', () => {
    const programName = 'Test program for multiple checkboxes'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-fave-color-q',
        options: [
          {adminName: 'red_admin', text: 'red'},
          {adminName: 'green_admin', text: 'green'},
          {adminName: 'orange_admin', text: 'orange'},
          {adminName: 'blue_admin', text: 'blue'},
        ],
        minNum: 1,
        maxNum: 2,
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-vacation-q',
        options: [
          {adminName: 'beach_admin', text: 'beach'},
          {adminName: 'mountains_admin', text: 'mountains'},
          {adminName: 'city_admin', text: 'city'},
          {adminName: 'cruise_admin', text: 'cruise'},
        ],
        minNum: 1,
        maxNum: 2,
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['checkbox-fave-color-q'],
        'checkbox-vacation-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with valid checkboxes submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCheckboxQuestion(['blue'])
      await applicantQuestions.answerCheckboxQuestion(['beach'])
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async () => {
      const {applicantQuestions} = ctx
      // Only answer required question.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCheckboxQuestion(['red'])
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const checkboxError = '.cf-applicant-question-errors'
      // No validation errors on first page load.
      expect(await page.isHidden(checkboxError)).toEqual(true)

      // Max of 2 answers allowed.
      await applicantQuestions.answerCheckboxQuestion([
        'red',
        'green',
        'orange',
      ])
      await applicantQuestions.answerCheckboxQuestion(['beach'])
      await applicantQuestions.clickNext()

      expect(await page.isHidden(checkboxError)).toEqual(false)
    })

    test('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const checkboxError = '.cf-applicant-question-errors'
      // No validation errors on first page load.
      expect(await page.isHidden(checkboxError)).toEqual(true)

      await applicantQuestions.answerCheckboxQuestion(['red'])
      // Max of 2 answers allowed.
      await applicantQuestions.answerCheckboxQuestion([
        'beach',
        'mountains',
        'city',
      ])
      await applicantQuestions.clickNext()

      expect(await page.isHidden(checkboxError)).toEqual(false)
    })

    test('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
