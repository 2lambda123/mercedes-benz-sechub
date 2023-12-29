#!/usr/bin/env sh
# SPDX-License-Identifier: MIT

cd "$PDS_JOB_EXTRACTED_SOURCES_FOLDER/" || exit
mobsfscan --sarif --output "$PDS_JOB_RESULT_FILE" "."
