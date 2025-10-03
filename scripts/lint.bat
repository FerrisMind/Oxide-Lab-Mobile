@echo off

REM Script to run all linters

echo Running ktlint...
call gradlew.bat ktlintCheck

echo Running detekt...
call gradlew.bat detekt

echo All linters completed!