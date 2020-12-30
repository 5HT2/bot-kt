#!/bin/sh

__dir="$(cd "$(dirname "$0")" && pwd)"

# Define our logger. In our case, we have a silent (-s) option, which shifts the arguments by 1.
log() {
  if [ "$1" = "-s" ]; then
    return
  fi

  if [ ! "$1" = "ERROR:" ]; then
    shift
  fi

  if [ "$1" = "--no-lint" ]; then
    shift
  fi

  echo "[$(date +"%H:%M:%S")] $*"
}

# Define our error logging. This does not accept a silent flag.
error() {
  log "ERROR:" "$*"
}

# This allows calling the script from other folders. Entirely unnecessary but it doesn't hurt.
log "$1" "Found script in dir '$__dir', trying to cd into script folder"
cd "$__dir" || exit $?

# We need git installed in order to download the submodules.
log "$1" "Checking if git is installed..."
if [ -z "$(which git)" ]; then
  error "Git is not installed, please make sure you install the CLI version of git, not some desktop wrapper for it"
  exit 1
fi
log "$1" "Git is installed!"

# Ktlint is our enforced style guide, and is required for contributions.
if [ "$1" = "--no-lint" ] || [ "$2" = "--no-lint" ]; then
  log "$1" "WARN: Option '--no-lint' is selected"
else
  log "$1" "Checking if ktlint is installed..."
  if [ -z "$(which ktlint)" ]; then
    error "Ktlint is not installed. Install it from 'https://github.com/pinterest/ktlint/'"
    exit 1
  fi
  log "$1" "Ktlint is installed!"
fi

# The git repo should obviously be present, to prevent any dumb mistakes with git modules
log "$1" "Checking for .git dir..."
if [ ! -d ".git" ]; then
  error "Could not detect git repository, exiting"
  exit 1
fi
log "$1" "Found git repository!"

# Download aforementioned git modules
log "$1" "Downloading git submodules..."
git submodule update --init --recursive || {
  error "Failed to init git submodules"
  exit 1
}
log "$1" "Downloaded git submodules!"

# Actually lint the project, as mentioned above
if [ "$1" = "--no-lint" ] || [ "$2" = "--no-lint" ]; then
  log "$1" "WARN: Not linting project"
else
  log "$1" "Linting project..."
  ktlint -F "src/main/kotlin/org/kamiblue/botkt/" >/dev/null 2>&1 || {
    error "Failed to lint project, manually correct linting issues"
    ktlint "src/main/kotlin/org/kamiblue/botkt/"
    exit 1
  }
  log "$1" "Linted project!"
fi

# Now we are finally ready to build, suppress the build output as it's usually unnecessary
log "$1" "Running gradlew build..."
./gradlew build >/dev/null 2>&1 || {
  error "Running './gradlew build' failed! Run './gradlew build' manually"
  exit 1
}

# Pass an empty first argument to ignore the silent arg
log "" "Build project successfully!"
