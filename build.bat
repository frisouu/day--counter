@echo off
cd /d "%~dp0"
echo === Version de Java ===
java -version
echo.
echo === Compilation en cours, patiente... ===
call gradlew.bat build --stacktrace > build-log.txt 2>&1
type build-log.txt
echo.
echo === Termine. Le detail complet est dans build-log.txt ===
pause
