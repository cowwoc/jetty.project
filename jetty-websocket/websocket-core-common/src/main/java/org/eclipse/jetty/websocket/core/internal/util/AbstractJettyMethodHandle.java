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

package org.eclipse.jetty.websocket.core.internal.util;

import java.lang.invoke.MethodHandle;

public abstract class AbstractJettyMethodHandle implements JettyMethodHandle
{
    @Override
    public JettyMethodHandle bindTo(Object arg)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public JettyMethodHandle bindTo(Object arg, int idx)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public JettyMethodHandle filterReturnValue(MethodHandle filter)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public JettyMethodHandle changeReturnType(Class<Object> objectClass)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> parameterType(int idx)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> returnType()
    {
        throw new UnsupportedOperationException();
    }
}