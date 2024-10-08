#!/bin/bash
set -eEuo pipefail
cd -P -- "$(dirname -- "$(readlink -f -- "$0")")"/..

################################################################################
# common util functions
################################################################################

color_print() {
  local color=$1
  shift

  # if stdout is terminal or running on GitHub Actions, turn on color output.
  #
  # about env var GITHUB_ACTIONS of GitHub Actions:
  #   Always set to true when GitHub Actions is running the workflow.
  #   You can use this variable to differentiate when tests are being run locally or by GitHub Actions.
  #   https://docs.github.com/en/actions/learn-github-actions/variables#default-environment-variables
  if [[ -t 1 || "${GITHUB_ACTIONS:-}" = true ]]; then
    printf "\e[1;${color}m%s\e[0m\n" "$*"
  else
    printf '%s\n' "$*"
  fi
}

info_print() {
  # blue text color
  color_print 36 "$@"
}

warn_print() {
  # yellow text color
  color_print 33 "$@"
}

error_print() {
  # red text color
  color_print 31 "$@"
}

head_line_print() {
  color_print "2;35;46" ================================================================================
  warn_print "$*"
  color_print "2;35;46" ================================================================================
}

log_then_run() {
  info_print "Run under work directory $PWD :"
  info_print "$*"
  "$@"
}

die() {
  error_print "Error: $*" >&2
  exit 1
}

################################################################################
# CI helper functions
################################################################################

switch_to_jdk_home() {
  local version="$1"
  local jh_var_name="JAVA${version//./_}_HOME"
  export JAVA_HOME="${!jh_var_name}"

  [ -x "$JAVA_HOME/bin/java" ] ||
    die "JAVA_HOME($JAVA_HOME) is invalid: \$JAVA_HOME/bin/java is not existed or executable!"
}

# The normalized JAVA_HOME env vars of the specified java versions
# are like JAVA8_HOME, JAVA21_HOME, etc.
#
# You can set the normalized JAVA_HOME env vars locally,
# then run this integration test script locally.
#
# Below function will detect JAVA_HOME vars when running on GitHub Actions,
# and set the normalized JAVA_HOME env vars.
__detect_java_homes_and_set_when_running_on_github_actions() {
  [[ "${GITHUB_ACTIONS:-}" = true ]] || return 0

  local jh_name matched_version jh expr detected=()

  # the JAVA_HOME env vars introduced by `actions/setup-java@v4`, e.g.
  #   JAVA_HOME_8_X64: /opt/hostedtoolcache/Java_Zulu_jdk/8.0.392-8/x64
  #   JAVA_HOME_21_X64: /opt/hostedtoolcache/Java_Zulu_jdk/21.0.1-12/x64
  for jh_name in "${!JAVA_HOME_@}"; do
    [[ "$jh_name" =~ ^JAVA_HOME_([0-9]+)_ ]] || continue
    matched_version="${BASH_REMATCH[1]}"

    jh="${!jh_name:-}"
    [ -d "$jh" ] || continue

    printf -v expr 'JAVA%q_HOME=%q' "$matched_version" "$jh"
    detected=(${detected[@]:+"${detected[@]}"} "$expr")
  done

  (("${#detected[@]}" > 0)) || return 0
  warn_print "Detected JAVA_HOME when running on GitHub Actions:"
  for expr in "${detected[@]}"; do
    # shellcheck disable=SC2163
    export "$expr"
    echo "  export $expr"
  done
}
__detect_java_homes_and_set_when_running_on_github_actions

################################################################################
# CI build logic
################################################################################

# shellcheck disable=SC2153
readonly default_build_jdk_version=21
readonly CI_JDK_VERSIONS=(
  8
  11
  17
  "$default_build_jdk_version"
)

##################################################
# build and test by default version jdk
##################################################

switch_to_jdk_home "$default_build_jdk_version"

head_line_print "Build and test with Java: $JAVA_HOME"
# `-V`: show the java version info (essential info when test multiple java versions)
# `--no-transfer-progress`: disable the (chatty) transfer progress when downloading or uploading
log_then_run mvn -V --no-transfer-progress clean package

##################################################
# test by multiply java versions
##################################################

for jdk_version in "${CI_JDK_VERSIONS[@]}"; do
  if [ "$jdk_version" == "$default_build_jdk_version" ]; then
    # skip default jdk, already tested above
    continue
  fi

  switch_to_jdk_home "$jdk_version"

  head_line_print "Test with Java: $JAVA_HOME"
  log_then_run mvn -V --no-transfer-progress surefire:test
done
