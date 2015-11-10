====
    Copyright (c) 2008-2015, Massachusetts Institute of Technology (MIT)
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

    3. Neither the name of the copyright holder nor the names of its contributors
    may be used to endorse or promote products derived from this software without
    specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
====

component: machAgent
README created: 20120110
README updated: n/a

========
Summary
========

This component is basically just to hold and test the capability of the machAgent camel route
and processor.  The route and processor will be used in other components as a means of allowing
the component to communicate with mach w/o direct modification of the component (other than modifying
the route in xml).

So for spring-configured components, we can easily drop in this mach agent.  For non-spring-configured
components, the mach agent can still be used, but it will have to be hardcoded into the route, requiring
a modification.

==============
Files of Note
==============

./src/main/resources/machAgent.xml:

Contains a test route that delivers periodic messages to a queue, and that queue can be
filtered and split/processed, etc.  Individual route implementations will vary per component, so this
component will eventually have several example routes that can be set up to cover the most popular
use cases.

------------------------------------------------------------------------------------------------------

./src/main/resources/machAgent.properties

Contains properties that can be set and used in machAgent.xml (or <componentName.xml> when deployed on a 
component).  For example, you can set a "machUri" property to a specific camel rabbit URI to use throughout
your routes.

------------------------------------------------------------------------------------------------------

./src/main/resources/runAgent.sh

This script runs this test implementation of the machAgent.  Specifically, it will run the machAgent.xml as
a Spring application.  It assumes all dependencies are in ./dependencies.

------------------------------------------------------------------------------------------------------

./mcp

This script cleans, builds, and packages the component, as well as copies over the resources files, to the
/target directory that the build created so that you can execute runAgent.sh, and all the files will be 
there.

------------------------------------------------------------------------------------------------------
