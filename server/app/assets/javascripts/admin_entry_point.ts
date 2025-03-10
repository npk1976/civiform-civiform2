/**
 * Entry point for admin bundle. Admin bundle is served on all pages that are
 * accessed by CiviForm and Program admins.
 */

import * as main from './main'
import * as radio from './radio'
import * as toast from './toast'
import * as toggle from './toggle'
import * as adminApplicationView from './admin_application_view'
import * as adminApplications from './admin_applications'
import * as adminPredicates from './admin_predicate_configuration'
import * as adminProgramImage from './admin_program_image'
import * as adminPrograms from './admin_programs'
import * as adminProgramStatusesView from './admin_program_statuses_view'
import * as adminSettingsView from './admin_settings_view'
import * as adminValidation from './admin_validation'
import * as apiDocs from './api_docs'
import * as devIcons from './dev_icons'
import * as modal from './modal'
import * as questionBank from './questionBank'
import * as preview from './preview'
import * as enumerator from './enumerator'
import * as phoneNumber from './phone'
import * as adminQuestionEdit from './admin_question_edit'
import * as adminExportView from './admin_export_view'
import * as trustedIntermediaryController from './admin_trusted_intermediary_list'

import htmx from 'htmx.org'

declare global {
  interface Window {
    // eslint-disable-next-line  @typescript-eslint/no-explicit-any
    htmx: any
  }
}

window.htmx = htmx

window.addEventListener('load', () => {
  main.init()
  radio.init()
  toast.init()
  toggle.init()
  adminApplicationView.init()
  adminApplications.init()
  adminPredicates.init()
  adminProgramImage.init()
  adminPrograms.init()
  adminProgramStatusesView.init()
  adminSettingsView.init()
  adminValidation.init()
  apiDocs.init()
  devIcons.init()
  modal.init()
  questionBank.init()
  preview.init()
  enumerator.init()
  phoneNumber.init()
  adminQuestionEdit.init()
  adminExportView.init()
  trustedIntermediaryController.init()
})
