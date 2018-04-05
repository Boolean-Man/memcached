@echo off & setlocal enabledelayedexpansion

set LIB_JARS=""
cd ..\lib
for %%i in (*) do set LIB_JARS=!LIB_JARS!;..\lib\%%i
cd ..\bin

java -Dmemcached.server.host=${memcached.server.host} -Dmemcached.server.port=${memcached.server.port} -classpath ..\conf;%LIB_JARS% com.github.memcached.client.MemcachedClient
pause