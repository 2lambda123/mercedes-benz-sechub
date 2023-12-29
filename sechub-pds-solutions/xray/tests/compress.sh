#!/usr/bin/env bash
# SPDX-License-Identifier: MIT

current_test_folder="$1"
test_file="$2"
	
if [[ ! -d "$current_test_folder" ]]
then
	echo "Target folder is empty"
	exit 1
fi

if [[ -z "$test_file" ]]
then
	echo "No test file provided"
	exit 1
fi
cp -r "$test_file" "$current_test_folder/$test_file"
cd "$current_test_folder" || exit
tar -cvf "$test_file".tar "$test_file"



