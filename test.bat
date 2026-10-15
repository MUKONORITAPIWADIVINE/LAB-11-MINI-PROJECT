@echo off
REM Compile and run the automated tests
if not exist out mkdir out
javac -d out src\*.java || exit /b 1
java -cp out LibraryTest
