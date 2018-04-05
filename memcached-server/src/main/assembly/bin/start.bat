@echo off & setlocal enabledelayedexpansion

set LIB_JARS=""
cd ..\lib
for %%i in (*) do set LIB_JARS=!LIB_JARS!;..\lib\%%i
cd ..\bin

java -Dmemcached.server.port=${memcached.server.port} -Dmemcached.server.pool.size=${memcached.server.pool.size} -classpath ..\conf;%LIB_JARS% com.github.memcached.server.MemcachedServer
pause