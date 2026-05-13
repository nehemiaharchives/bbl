@echo off
set CHEF_LICENSE=accept-no-persist
set CHEF_CLIENT_RUBYGEM_SCRIPT=%~dp0chef-client-rubygem.rb
if defined RUBY_EXE (
  "%RUBY_EXE%" "%CHEF_CLIENT_RUBYGEM_SCRIPT%" %*
  exit /b %ERRORLEVEL%
)
if exist C:\Ruby32-x64\bin\ruby.exe (
  C:\Ruby32-x64\bin\ruby.exe "%CHEF_CLIENT_RUBYGEM_SCRIPT%" %*
  exit /b %ERRORLEVEL%
)
ruby "%CHEF_CLIENT_RUBYGEM_SCRIPT%" %*
