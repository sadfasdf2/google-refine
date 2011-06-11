/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.commands.importing;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.commands.Command;
import com.google.refine.commands.importing.ImportJob.State;

public class GetImportJobStatusCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("get-import-job-status_command");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        long jobID = Long.parseLong(request.getParameter("jobID"));
        ImportJob job = ImportManager.singleton().getJob(jobID);
        
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            if (job == null) {
                writer.key("code"); writer.value("error");
                writer.key("message"); writer.value("No such import job");
            } else {
                writer.key("code"); writer.value("ok");
                writer.key("state");
                if (job.state == State.NEW) {
                    writer.value("new");
                } else if (job.state == State.RETRIEVING_DATA) {
                    writer.value("retrieving");
                    writer.key("progress"); writer.value(job.retrievingProgress);
                    writer.key("bytesSaved"); writer.value(job.bytesSaved);
                } else if (job.state == State.READY) {
                    writer.value("ready");
                } else if (job.state == State.ERROR) {
                    writer.value("error");
                    writer.key("message"); writer.value(job.errorMessage);
                    if (job.exception != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        job.exception.printStackTrace(pw);
                        pw.flush();
                        sw.flush();

                        writer.key("stack"); writer.value(sw.toString());
                    }
                }
            }
            writer.endObject();
        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            w.flush();
            w.close();
        }
    }
}