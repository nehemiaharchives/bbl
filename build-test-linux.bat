docker build --target release --output type=local,dest=. .
gradlew cookbook && CD /D %~dp0\bbl_install && bundle exec kitchen test
