@echo off
call compile-item.bat %1
echo Copying files
cp src/custom/%1 ../build/%1