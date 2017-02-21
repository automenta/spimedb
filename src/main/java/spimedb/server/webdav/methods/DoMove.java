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
import spimedb.server.webdav.WebdavStatus;
import spimedb.server.webdav.exceptions.AccessDeniedException;
import spimedb.server.webdav.exceptions.LockFailedException;
import spimedb.server.webdav.exceptions.ObjectAlreadyExistsException;
import spimedb.server.webdav.exceptions.WebdavException;
import spimedb.server.webdav.locking.ResourceLocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Hashtable;

public class DoMove extends AbstractMethod {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoMove.class);

    private final ResourceLocks _resourceLocks;
    private final DoDelete _doDelete;
    private final DoCopy _doCopy;
    private final boolean _readOnly;

    public DoMove(ResourceLocks resourceLocks, DoDelete doDelete,
            DoCopy doCopy, boolean readOnly) {
        _resourceLocks = resourceLocks;
        _doDelete = doDelete;
        _doCopy = doCopy;
        _readOnly = readOnly;
    }

    public void run(ITransaction transaction, HttpServletRequest req,
                    HttpServletResponse resp) throws IOException, LockFailedException {

        if (!_readOnly) {
            LOG.trace("-- {}", this.getClass().getName());

            String sourcePath = getRelativePath(req);
            Hashtable<String, Integer> errorList = new Hashtable<>();

            if (!checkLocks(transaction, req, resp, _resourceLocks, sourcePath)) {
                errorList.put(sourcePath, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return;
            }

            String destinationPath = req.getHeader("Destination");
            if (destinationPath == null) {
                resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }

            if (!checkLocks(transaction, req, resp, _resourceLocks,
                    destinationPath)) {
                errorList.put(destinationPath, WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
                return;
            }

            String tempLockOwner = "doMove" + System.currentTimeMillis()
                    + req.toString();

            if (_resourceLocks.lock(transaction, sourcePath, tempLockOwner,
                    false, 0, TEMP_TIMEOUT, TEMPORARY)) {
                try {

                    if (_doCopy.copyResource(transaction, req, resp)) {

                        errorList = new Hashtable<>();
                        _doDelete.deleteResource(transaction, sourcePath,
                                errorList, req, resp);
                        if (!errorList.isEmpty()) {
                            sendReport(req, resp, errorList);
                        }
                    }

                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (ObjectAlreadyExistsException e) {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND, req
                            .getRequestURI());
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    _resourceLocks.unlockTemporaryLockedObjects(transaction,
                            sourcePath, tempLockOwner);
                }
            } else {
                errorList.put(req.getHeader("Destination"),
                        WebdavStatus.SC_LOCKED);
                sendReport(req, resp, errorList);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);

        }

    }

}
