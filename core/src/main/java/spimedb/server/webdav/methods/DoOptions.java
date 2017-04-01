/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spimedb.server.webdav.methods;

import spimedb.server.webdav.ITransaction;
import spimedb.server.webdav.IWebdavStore;
import spimedb.server.webdav.StoredObject;
import spimedb.server.webdav.WebdavStatus;
import spimedb.server.webdav.exceptions.AccessDeniedException;
import spimedb.server.webdav.exceptions.LockFailedException;
import spimedb.server.webdav.exceptions.WebdavException;
import spimedb.server.webdav.locking.ResourceLocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DoOptions extends DeterminableMethod {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoOptions.class);

    private final IWebdavStore _store;
    private final ResourceLocks _resourceLocks;

    public DoOptions(IWebdavStore store, ResourceLocks resLocks) {
        _store = store;
        _resourceLocks = resLocks;
    }

    public void run(ITransaction transaction, HttpServletRequest req,
                    HttpServletResponse resp) throws IOException, LockFailedException {

        LOG.trace("-- {}", this.getClass().getName());

        String tempLockOwner = "doOptions" + System.currentTimeMillis()
                + req.toString();
        String path = getRelativePath(req);
        if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0,
                TEMP_TIMEOUT, TEMPORARY)) {
            StoredObject so = null;
            try {
                resp.addHeader("DAV", "1, 2");

                so = _store.get(transaction, path);
                String methodsAllowed = determineMethodsAllowed(so);
                resp.addHeader("Allow", methodsAllowed);
                resp.addHeader("MS-Author-Via", "DAV");
            } catch (AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (WebdavException e) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path,
                        tempLockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
