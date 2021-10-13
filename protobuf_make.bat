@echo off
set SRC_DIR=.\core\src\main\resources\
set DST_DIR=.\core\src\main\java
set protoc_exe=.\protoc\bin\protoc.exe
set protobuf_folder=.\core\src\main\resources\serialization

:compile
%protoc_exe% --proto_path=%SRC_DIR% --java_out=%DST_DIR% %protobuf_folder%\persistence.proto
