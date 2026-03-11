@echo off
echo Showing Arete blocker logs...
echo Press Ctrl+C to stop
adb logcat -s BlockerController:D ShouldBlockAppUseCase:D
