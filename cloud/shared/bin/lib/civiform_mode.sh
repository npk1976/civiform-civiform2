#######################################
# Returns
# Globals:
#  CIVIFORM_MODE
#######################################
function civiform_mode::is_test() {
  [[ "${CIVIFORM_MODE}" == "test" ]]
}

function civiform_mode::is_dev() {
  [[ "${CIVIFORM_MODE}" == "dev" ]]
}

function civiform_mode::is_ci() {
  [[ "${CIVIFORM_MODE}" == "ci" ]]
}
