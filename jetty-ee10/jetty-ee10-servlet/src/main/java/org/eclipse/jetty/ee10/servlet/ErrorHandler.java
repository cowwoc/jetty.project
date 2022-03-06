//
// ========================================================================
// Copyright (c) 1995-2022 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.ee10.servlet;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

public class ErrorHandler extends Handler.Abstract
{
    public static final String ERROR_CONTEXT = "org.eclipse.jetty.server.error_context";

    @Override
    public Request.Processor handle(Request request) throws Exception
    {
        return null;
    }

    public void setServer(Server server)
    {

    }

    public interface ErrorPageMapper
    {
        String getErrorPage(HttpServletRequest request);
    }
} // TODO