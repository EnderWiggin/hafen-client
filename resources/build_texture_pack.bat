@echo off
echo Compiling the Texture Pack

call compile-item.bat gfx/invobjs/stoneaxe.res

robocopy "custom/res/" "../res" /mir