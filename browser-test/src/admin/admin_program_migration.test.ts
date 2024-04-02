import {test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('program migration', {tag: ['@uses-fixtures']}, () => {
  test('export a program', async ({
    page,
    adminPrograms,
    adminProgramMigration,
  }) => {
    await test.step('add two active programs', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram('program-1')
      await adminPrograms.addProgram('program-2')
      await adminPrograms.publishAllDrafts()
    })

    await test.step('load export page', async () => {
      await enableFeatureFlag(page, 'program_migration_enabled')
      await adminProgramMigration.goToExportPage()
      await validateScreenshot(page, 'export-page')
    })

    await test.step('export program 2', async () => {
      await adminProgramMigration.selectProgramToExport('program-2')
      await adminProgramMigration.downloadProgram()
      // TODO(#7087): Assert JSON file with correct content is downloaded.
    })
  })

  test.only('import a program', async ({    page,
                                       adminPrograms,
                                       adminProgramMigration,}) => {
             await test.step('load import page', async () => {
                   await loginAsAdmin(page)
               await enableFeatureFlag(page, 'program_migration_enabled')
               await adminProgramMigration.goToImportPage()
               await validateScreenshot(page, 'import-page-no-data')
             })


             await test.step('import a program', async () => {
             // TODO(#7087): We should also have a test that exports JSON and then imports that same JSON
             // so that we can verify export and import work together.
               await adminProgramMigration.uploadProgramJson('src/assets/import-program-sample.json')

                await validateScreenshot(page, 'import-page-with-data')
             })
      })
})
