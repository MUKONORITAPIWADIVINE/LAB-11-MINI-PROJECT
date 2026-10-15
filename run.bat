@echo off
REM Compile and start the Library Management System (run from this folder)
if not exist out mkdir out
javac -d out src\*.java || exit /b 1
java -cp out LibraryApp
