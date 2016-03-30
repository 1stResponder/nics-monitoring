@REM
@REM Copyright (c) 2008-2016, Massachusetts Institute of Technology (MIT)
@REM All rights reserved.
@REM
@REM Redistribution and use in source and binary forms, with or without
@REM modification, are permitted provided that the following conditions are met:
@REM
@REM 1. Redistributions of source code must retain the above copyright notice, this
@REM list of conditions and the following disclaimer.
@REM
@REM 2. Redistributions in binary form must reproduce the above copyright notice,
@REM this list of conditions and the following disclaimer in the documentation
@REM and/or other materials provided with the distribution.
@REM
@REM 3. Neither the name of the copyright holder nor the names of its contributors
@REM may be used to endorse or promote products derived from this software without
@REM specific prior written permission.
@REM
@REM THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
@REM AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
@REM IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
@REM DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
@REM FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
@REM DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
@REM SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
@REM CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
@REM OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
@REM OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
@REM

@echo on
REM alias for the application to be run (process tag, config file name)
set NAME=host-ping-alert

set LIBDIR=%LIBDIR%;%CD%\dependencies
REM For local dev, need to copy jar to /dependencies folder, or specify both
copy /y %NAME%-0.0.1-SNAPSHOT.jar .\dependencies\

echo Configuration file: %NAME%.xml
REM dump content of config file
type %NAME%.xml   

java  -DappName=%NAME% -Xmx512M -server -cp "%LIBDIR%\*" org.apache.camel.spring.Main -fa %NAME%.xml
