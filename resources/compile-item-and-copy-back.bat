@echo off
call compile-item.bat %1
copy custom/res/%1 ../res/%1