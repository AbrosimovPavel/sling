/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.factories.subsystems.impl;

import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.subsystem.Subsystem;

/**
 * Uninstall a subsystem
 */
public class UninstallSubsystemTask extends InstallTask {

    private static final String INSTALL_ORDER = "52-";

    private final ServiceReference<Subsystem> subsystemReference;

    private final BundleContext bundleContext;

    public UninstallSubsystemTask(final TaskResourceGroup grp,
            final BundleContext bundleContext,
            final ServiceReference<Subsystem> ref) {
        super(grp);
        this.bundleContext = bundleContext;
        this.subsystemReference = ref;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        final TaskResource tr = this.getResource();
        ctx.log("Uninstalling subsystem from {}", tr);

        Subsystem subsystem = null;
        try {
            subsystem = this.bundleContext.getService(this.subsystemReference);
            if ( subsystem != null ) {
                subsystem.uninstall();
                ctx.addTaskToCurrentCycle(new ChangeStateTask(this.getResourceGroup(), ResourceState.UNINSTALLED));
                ctx.log("Uninstalled subsystem {}", subsystem);
            } else {
                ctx.log("Unable to uninstall subsystem {}.", tr);
                ctx.addTaskToCurrentCycle(new ChangeStateTask(this.getResourceGroup(), ResourceState.IGNORED));
            }
        } finally {
            if ( subsystem != null ) {
                this.bundleContext.ungetService(this.subsystemReference);
            }
        }
    }

    @Override
    public String getSortKey() {
        return INSTALL_ORDER + getResource().getURL();
    }
}
