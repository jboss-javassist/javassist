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
  export JAVA_HOME="$1"
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

__find_latest_specified_java_version_and_export() {
  local specified_java_version="$1"
  local major_version="${specified_java_version%%.*}"

  local reference_jh_var_name="JAVA${major_version}_HOME"
  local reference_dir=${!reference_jh_var_name}
  reference_dir="${reference_dir%/}" # remove tailing slash

  local bkp_shopt_null_glob=0
  shopt -q nullglob && bkp_shopt_null_glob=1
  shopt -s nullglob

  local found_arr found
  if [ "${GITHUB_ACTIONS:-}" = true ]; then
    # the JAVA_HOME env vars introduced by `actions/setup-java@v4`, e.g.
    #   JAVA_HOME_8_X64: /opt/hostedtoolcache/Java_Zulu_jdk/8.0.392-8/x64
    #   JAVA_HOME_21_X64: /opt/hostedtoolcache/Java_Zulu_jdk/21.0.1-12/x64
    local extra_child_dir_name="${reference_dir##*/}" # get basename, e.g. "x64"
    found_arr=(
      "$reference_dir/../../$specified_java_version"-*/
      "$reference_dir/../../$specified_java_version".*/
    )
  else
    found_arr=(
      "$JAVA11_HOME/../$specified_java_version".*/
      "$JAVA11_HOME/../$specified_java_version"-*/
    )
  fi

  ((${#found_arr[@]} > 0)) || die "Fail to find java home for version $specified_java_version!"

  if ((${#found_arr[@]} > 1)); then
    # TODO `ls -v` is not robust, but it works for now.
    # shellcheck disable=SC2012
    found="$(ls -v -d "${found_arr[@]}" | tail -n 1)"
  else
    found="${found_arr[0]}"
  fi
  found="$(cd "$found" && pwd)" # normalize path

  if [ "${GITHUB_ACTIONS:-}" = true ]; then
    found="${found}/$extra_child_dir_name"
  fi

  ((${#found_arr[@]} == 1)) || echo "Found multiply java homes for the specified java version $specified_java_version: ${found_arr[*]}; use the last one"

  local expr
  printf -v expr 'JAVA%q_HOME=%q' "${specified_java_version//./_}" "$found"
  # shellcheck disable=SC2163
  export "$expr"
  warn_print "Found the specified java version($specified_java_version): export $expr"

  # restore null glob shopt
  [ "$bkp_shopt_null_glob" -eq 0 ] && shopt -u nullglob
}

__find_latest_specified_java_version_and_export 11.0.3
# correct the latest version of java 11 back;
# because actions/setup-java set up 2 versions of java 11 and last old version wins in `JAVA11_HOME`.
__find_latest_specified_java_version_and_export 11

################################################################################
# CI build logic
################################################################################

# shellcheck disable=SC2153
readonly default_build_jdk_home="$JAVA21_HOME"
readonly JDK_HOMES=(
  "$JAVA8_HOME"
  "$JAVA11_0_3_HOME"
  "$JAVA11_HOME"
  "$JAVA17_HOME"
  "$default_build_jdk_home"
)

##################################################
# build and test by default version jdk
##################################################

switch_to_jdk_home "$default_build_jdk_home"

head_line_print "Build and test with Java: $JAVA_HOME"
# `-V`: show the java version info (essential info when test multiple java versions)
# `--no-transfer-progress`: disable the (chatty) transfer progress when downloading or uploading
log_then_run mvn -V --no-transfer-progress clean package

##################################################
# test by multiply java versions
##################################################

for jdk_version in "${JDK_HOMES[@]}"; do
  if [ "$jdk_version" == "$default_build_jdk_home" ]; then
    # skip default jdk, already tested above
    continue
  fi

  switch_to_jdk_home "$jdk_version"

  head_line_print "Test with Java: $JAVA_HOME"
  log_then_run mvn -V --no-transfer-progress surefire:test
done
